package com.tfg;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class SignUpActivity extends AppCompatActivity {

    EditText username, firstName, lastName, secondName, birthdate, email, pwd;
    Button singUpUser;

    FirebaseAuth firebaseAuth;

    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_signup);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("SignUpActivity");
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }


        username = findViewById(R.id.username);
        firstName = findViewById(R.id.firstName);
        lastName = findViewById(R.id.lastName);
        secondName = findViewById(R.id.secondName);
        birthdate = findViewById(R.id.birthdate);
        email = findViewById(R.id.email);
        pwd = findViewById(R.id.pwd);
        singUpUser = findViewById(R.id.singUpUser);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(SignUpActivity.this);

        singUpUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = pwd.getText().toString();
                String e_mail = email.getText().toString();
                String singup_user = singUpUser.getText().toString();

                if(!Patterns.EMAIL_ADDRESS.matcher(e_mail).matches()){
                    email.setError("Invalid email address");
                } else if(password.length()<8){
                    pwd.setError("Password must be at least 8 characters");
                    pwd.setFocusable(true);
                } else {
                    SignUp(e_mail, password);
                }
            }
        });
    }

    //Metodo para hacer el registro de usuario
    private void SignUp(String e_mail, String password){
        progressDialog.setTitle("Signing up");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        firebaseAuth.createUserWithEmailAndPassword(e_mail, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>(){
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task){
                        //Condicional para un registro exitoso
                        if (task.isSuccessful()) {
                            progressDialog.dismiss(); //Cerrar el programa
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            //Datos a registrar
                            assert user != null;
                            String uid = user.getUid();
                            String user_Name = username.getText().toString();
                            String first_Name = firstName.getText().toString();
                            String last_Name = lastName.getText().toString();
                            String second_Name = secondName.getText().toString();
                            String password = pwd.getText().toString();
                            String e_mail = email.getText().toString();

                            //Hashmap con los datos de usuario para pasarlo a la bbdd
                            HashMap<Object, String> UserData = new HashMap<>();
                            UserData.put("id", uid);
                            UserData.put("username", user_Name);
                            UserData.put("firstName", first_Name);
                            UserData.put("lastName", last_Name);
                            UserData.put("secondName", second_Name);
                            UserData.put("email", e_mail);
                            UserData.put("password", password);
                            UserData.put("profile_picure", "");

                            //HomeActivity una instancia de la bbdd
                            FirebaseDatabase database = FirebaseDatabase.getInstance("https://fluxe-a2d2d-default-rtdb.europe-west1.firebasedatabase.app");

                            //Creo la bbdd
                            DatabaseReference reference = database.getReference("users");
                            reference.child(uid).setValue(UserData)
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    Toast.makeText(SignUpActivity.this, "Data saved to Firebase", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(SignUpActivity.this, "Error saving data: " + task.getException(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                            Toast.makeText(SignUpActivity.this, "Sign up completed successfully", Toast.LENGTH_SHORT).show();

                            //Una vez registrado, nos lleva a la pantalla de inicio
                            startActivity(new Intent(SignUpActivity.this, HomeActivity.class));
                        } else{
                            progressDialog.dismiss(); //Cerrar el programa
                            Toast.makeText(SignUpActivity.this, "Something went wrong", Toast.LENGTH_SHORT).show();

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss(); //Cerrar el programa
                        Toast.makeText(SignUpActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Accion de retroceso
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return super.onSupportNavigateUp();
    }
}