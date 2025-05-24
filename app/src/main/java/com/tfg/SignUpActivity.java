package com.tfg;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    EditText username, firstName, lastName, secondName, email, pwd;
    Button singUpUser;

    FirebaseAuth firebaseAuth;
    DatabaseReference usersRef;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("Sign Up");
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Referencias a vistas
        username     = findViewById(R.id.username);
        firstName    = findViewById(R.id.firstName);
        lastName     = findViewById(R.id.lastName);
        secondName   = findViewById(R.id.secondName);
        email        = findViewById(R.id.email);
        pwd          = findViewById(R.id.pwd);
        singUpUser   = findViewById(R.id.singUpUser);

        firebaseAuth   = FirebaseAuth.getInstance();
        usersRef       = FirebaseDatabase
                .getInstance("https://fluxe-a2d2d-default-rtdb.europe-west1.firebasedatabase.app")
                .getReference("users");
        progressDialog = new ProgressDialog(SignUpActivity.this);

        //Metodo para cambiar la fuente
        changeFont();

        //Evento del boton de registro
        singUpUser.setOnClickListener(v -> {
            // 1) Lectura y validacion basica
            String rawUser = username.getText().toString().trim();
            String userName = rawUser.toLowerCase(); // siempre minúsculas
            String e_mail  = email.getText().toString().trim();
            String password= pwd.getText().toString();

            // 2) Validar username: solo a–z y 0–9
            if (userName.isEmpty() || !userName.matches("[a-z0-9]+")) {
                username.setError("Only lowercase letters and digits");
                username.requestFocus();
                return;
            }
            // 3) Validar email correcto
            if (!Patterns.EMAIL_ADDRESS.matcher(e_mail).matches()) {
                email.setError("Invalid email address");
                email.requestFocus();
                return;
            }
            // 4) Validar contraseña 8–16 chars, sin espacios
            if (password.length() < 8 || password.length() > 16 || password.contains(" ")) {
                pwd.setError("Password must be 8–16 characters, no spaces");
                pwd.requestFocus();
                return;
            }

            // 5) Comprobar usuario único en RTDB
            usersRef.orderByChild("username")
                    .equalTo(userName)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snapUser) {
                            if (snapUser.exists()) {
                                username.setError("Username already taken");
                                username.requestFocus();
                                return;
                            }
                            // 6) Comprobar email único en RTDB
                            usersRef.orderByChild("email")
                                    .equalTo(e_mail)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override public void onDataChange(@NonNull DataSnapshot snapEmail) {
                                            if (snapEmail.exists()) {
                                                email.setError("Email already in use");
                                                email.requestFocus();
                                                return;
                                            }
                                            // 7) Todo ok: procedemos al registro en Firebase Auth + RTDB
                                            SignUp(userName, e_mail, password);
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                                    });
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
        });
    }

    //Metodo para cambiar la fuente
    private void changeFont(){
        String locate = "fuente/sans_ligera.ttf";
        Typeface tf = Typeface.createFromAsset(SignUpActivity.this.getAssets(), locate);
        username.setTypeface(tf);
        firstName.setTypeface(tf);
        lastName.setTypeface(tf);
        secondName.setTypeface(tf);
        email.setTypeface(tf);
        pwd.setTypeface(tf);
        singUpUser.setTypeface(tf);
    }

    //Metodo para hacer el registro de usuario
    private void SignUp(String userName, String e_mail, String password){
        progressDialog.setTitle("Signing up");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // 1) Registramos en Firebase Auth
        firebaseAuth.createUserWithEmailAndPassword(e_mail, password)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();  // Cerrar diálogo
                    if (task.isSuccessful()) {
                        FirebaseUser fbUser = firebaseAuth.getCurrentUser();
                        assert fbUser != null;
                        String uid = fbUser.getUid();

                        // 2) Preparamos datos para RTDB
                        String first_Name  = firstName.getText().toString().trim();
                        String last_Name   = lastName.getText().toString().trim();
                        String second_Name = secondName.getText().toString().trim();

                        HashMap<Object,String> UserData = new HashMap<>();
                        UserData.put("id", uid);
                        UserData.put("username", userName);
                        UserData.put("firstName", first_Name);
                        UserData.put("lastName", last_Name);
                        UserData.put("secondName", second_Name);
                        UserData.put("email", e_mail);
                        UserData.put("profile_picture", "");

                        // 3) Guardamos en RTDB
                        usersRef.child(uid)
                                .setValue(UserData)
                                .addOnSuccessListener(unused ->
                                        Toast.makeText(SignUpActivity.this,
                                                "Data saved to Firebase",
                                                Toast.LENGTH_SHORT).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(SignUpActivity.this,
                                                "Error saving data: "+e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );

                        Toast.makeText(SignUpActivity.this,
                                "Sign up completed successfully",
                                Toast.LENGTH_SHORT).show();
                        // 4) Vamos al Home
                        startActivity(new Intent(SignUpActivity.this, HomeActivity.class));
                        finish();

                    } else {
                        Toast.makeText(SignUpActivity.this,
                                "Something went wrong: "+task.getException().getMessage(),
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(SignUpActivity.this,
                            "SignUp failed: "+e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                });
    }

    //Accion de retroceso
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return super.onSupportNavigateUp();
    }
}
