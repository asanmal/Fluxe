package com.tfg.change;

import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.tfg.R;

import java.util.HashMap;
import java.util.Map;

public class EditDataActivity extends AppCompatActivity {

    private TextInputEditText editUsername,
            editFirstName,
            editLastName,
            editSecondName,
            editEmail;
    private Button btnSaveData;

    private FirebaseUser user;
    private DatabaseReference db, usersRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_data);

        // ----- ActionBar -----
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("Change Data");
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // referencias vistas
        editUsername   = findViewById(R.id.editUsername);
        editFirstName  = findViewById(R.id.editFirstName);
        editLastName   = findViewById(R.id.editLastName);
        editSecondName = findViewById(R.id.editSecondName);
        editEmail      = findViewById(R.id.editEmail);
        btnSaveData    = findViewById(R.id.btnSaveData);

        // Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        db   = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(user.getUid());
        usersRef = FirebaseDatabase.getInstance()
                .getReference("users");

        loadExistingData();

        btnSaveData.setOnClickListener(v -> saveChange());
    }

    private void loadExistingData() {
        db.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!snapshot.exists()) return;
                editUsername.setText(snapshot.child("username").getValue(String.class));
                editFirstName.setText(snapshot.child("firstName").getValue(String.class));
                editLastName.setText(snapshot.child("lastName").getValue(String.class));
                editSecondName.setText(snapshot.child("secondName").getValue(String.class));
                editEmail.setText(snapshot.child("email").getValue(String.class));
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void saveChange() {
        // 1) Leer y trim
        String u  = editUsername.getText().toString().trim().toLowerCase(); // minúsculas
        String fn = editFirstName.getText().toString().trim();
        String ln = editLastName.getText().toString().trim();
        String sn = editSecondName.getText().toString().trim();
        String em = editEmail.getText().toString().trim();

        // 2) Validaciones básicas
        if (u.isEmpty() || fn.isEmpty() || em.isEmpty()) {
            Toast.makeText(this,
                    "Username, name and email are required",
                    Toast.LENGTH_SHORT).show();
            return;
        }
        if (u.length() < 3 || u.length() > 16) {
            editUsername.setError("Username must be 3–16 characters");
            editUsername.requestFocus();
            return;
        }
        if (!u.matches("[a-z0-9]+")) {
            editUsername.setError("Use only letters and digits");
            editUsername.requestFocus();
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(em).matches()) {
            editEmail.setError("Invalid email address");
            editEmail.requestFocus();
            return;
        }

        // 3) Validar unicidad de username
        usersRef.orderByChild("username")
                .equalTo(u)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapU) {
                        boolean conflict = false;
                        for (DataSnapshot ds : snapU.getChildren()) {
                            if (!ds.getKey().equals(user.getUid())) {
                                conflict = true; break;
                            }
                        }
                        if (conflict) {
                            editUsername.setError("Username already taken");
                            editUsername.requestFocus();
                            return;
                        }
                        // 4) Validar unicidad de email
                        usersRef.orderByChild("email")
                                .equalTo(em)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override public void onDataChange(@NonNull DataSnapshot snapE) {
                                        boolean conflictE = false;
                                        for (DataSnapshot ds : snapE.getChildren()) {
                                            if (!ds.getKey().equals(user.getUid())) {
                                                conflictE = true; break;
                                            }
                                        }
                                        if (conflictE) {
                                            editEmail.setError("Email already in use");
                                            editEmail.requestFocus();
                                            return;
                                        }
                                        // 5) Todo OK: aplicar cambios
                                        Map<String,Object> cambios = new HashMap<>();
                                        cambios.put("username",    u);
                                        cambios.put("firstName",   fn);
                                        cambios.put("lastName",    ln);
                                        cambios.put("secondName",  sn);
                                        cambios.put("email",       em);

                                        db.updateChildren(cambios)
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(EditDataActivity.this,
                                                                "Data updated",
                                                                Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    } else {
                                                        Toast.makeText(EditDataActivity.this,
                                                                "Error: " + task.getException().getMessage(),
                                                                Toast.LENGTH_LONG).show();
                                                    }
                                                });
                                    }
                                    @Override public void onCancelled(@NonNull DatabaseError e) { }
                                });
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { }
                });
    }

    // Acción de retroceso desde la ActionBar
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
