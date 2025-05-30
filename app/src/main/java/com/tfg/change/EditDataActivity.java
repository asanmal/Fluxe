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

    private TextInputEditText editUsername, editFirstName, editLastName, editSecondName, editEmail;
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
            actionBar.setTitle(getString(R.string.title_change_data));
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // referencias vistas
        editUsername = findViewById(R.id.editUsername);
        editFirstName = findViewById(R.id.editFirstName);
        editLastName = findViewById(R.id.editLastName);
        editSecondName = findViewById(R.id.editSecondName);
        editEmail = findViewById(R.id.editEmail);
        btnSaveData = findViewById(R.id.btnSaveData);

        // Firebase
        user = FirebaseAuth.getInstance().getCurrentUser();
        db = FirebaseDatabase.getInstance()
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
        // Leer y trim
        String u  = editUsername.getText().toString().trim().toLowerCase(); // minúsculas
        String fn = editFirstName.getText().toString().trim();
        String ln = editLastName.getText().toString().trim();
        String sn = editSecondName.getText().toString().trim();
        String em = editEmail.getText().toString().trim();

        // Validaciones de campos requeridos
        if (u.isEmpty() || fn.isEmpty() || ln.isEmpty() || sn.isEmpty() || em.isEmpty()) {
            Toast.makeText(this,
                    getString(R.string.error_required_fields),
                    Toast.LENGTH_SHORT).show();
            return;
        }
        // Validar espacios (no permitidos)
        if (u.contains(" ")) {
            editUsername.setError(getString(R.string.error_no_spaces));
            editUsername.requestFocus();
            return;
        }
        if (fn.contains(" ")) {
            editFirstName.setError(getString(R.string.error_no_spaces));
            editFirstName.requestFocus();
            return;
        }
        if (ln.contains(" ")) {
            editLastName.setError(getString(R.string.error_no_spaces));
            editLastName.requestFocus();
            return;
        }
        if (sn.contains(" ")) {
            editSecondName.setError(getString(R.string.error_no_spaces));
            editSecondName.requestFocus();
            return;
        }
        if (em.contains(" ")) {
            editEmail.setError(getString(R.string.error_no_spaces));
            editEmail.requestFocus();
            return;
        }
        // Validar solo letras en nombre y apellidos
        if (!fn.matches("[a-zA-Z]+")) {
            editFirstName.setError(getString(R.string.error_only_letters));
            editFirstName.requestFocus();
            return;
        }
        if (!ln.matches("[a-zA-Z]+")) {
            editLastName.setError(getString(R.string.error_only_letters));
            editLastName.requestFocus();
            return;
        }
        if (!sn.matches("[a-zA-Z]+")) {
            editSecondName.setError(getString(R.string.error_only_letters));
            editSecondName.requestFocus();
            return;
        }
        // Validar longitud de username (mín:3, máx:16)
        if (u.length() < 3 || u.length() > 16) {
            editUsername.setError(getString(R.string.error_username_length));
            editUsername.requestFocus();
            return;
        }
        // Validar longitud máxima de 40 caracteres en nombres
        if (fn.length() > 40) {
            editFirstName.setError(getString(R.string.error_max_length));
            editFirstName.requestFocus();
            return;
        }
        if (ln.length() > 40) {
            editLastName.setError(getString(R.string.error_max_length));
            editLastName.requestFocus();
            return;
        }
        if (sn.length() > 40) {
            editSecondName.setError(getString(R.string.error_max_length));
            editSecondName.requestFocus();
            return;
        }
        // Validar caracteres permitidos en username
        if (!u.matches("[a-z0-9]+")) {
            editUsername.setError(getString(R.string.error_username_chars));
            editUsername.requestFocus();
            return;
        }
        // Validar formato de email
        if (!Patterns.EMAIL_ADDRESS.matcher(em).matches()) {
            editEmail.setError(getString(R.string.error_invalid_email));
            editEmail.requestFocus();
            return;
        }

        // Validar unicidad de username
        usersRef.orderByChild("username")
                .equalTo(u)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snapU) {
                        for (DataSnapshot ds : snapU.getChildren()) {
                            if (!ds.getKey().equals(user.getUid())) {
                                editUsername.setError(getString(R.string.error_username_taken));
                                editUsername.requestFocus();
                                return;
                            }
                        }
                        // Validar unicidad de email
                        usersRef.orderByChild("email")
                                .equalTo(em)
                                .addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override public void onDataChange(@NonNull DataSnapshot snapE) {
                                        for (DataSnapshot ds : snapE.getChildren()) {
                                            if (!ds.getKey().equals(user.getUid())) {
                                                editEmail.setError(getString(R.string.error_email_taken));
                                                editEmail.requestFocus();
                                                return;
                                            }
                                        }
                                        // aplicar cambios
                                        Map<String,Object> cambios = new HashMap<>();
                                        cambios.put("username", u);
                                        cambios.put("firstName", fn);
                                        cambios.put("lastName", ln);
                                        cambios.put("secondName", sn);
                                        cambios.put("email", em);

                                        db.updateChildren(cambios)
                                                .addOnCompleteListener(task -> {
                                                    if (task.isSuccessful()) {
                                                        Toast.makeText(EditDataActivity.this,
                                                                getString(R.string.msg_data_updated),
                                                                Toast.LENGTH_SHORT).show();
                                                        finish();
                                                    } else {
                                                        Toast.makeText(EditDataActivity.this,
                                                                getString(R.string.error_update, task.getException().getMessage()),
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
