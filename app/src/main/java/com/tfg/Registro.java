package com.tfg;

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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class Registro extends AppCompatActivity {

    EditText username, firstName, lastName, secondName, birthdate, email, pwd;
    Button singUpUser;

    FirebaseAuth firebaseAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_registro);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("Registro");
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

        singUpUser.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String password = pwd.getText().toString();
                String e_mail = email.getText().toString();
                String singup_user = singUpUser.getText().toString();

                if(!Patterns.EMAIL_ADDRESS.matcher(e_mail).matches()){
                    email.setError("Correo no válido");
                } else if(password.length()<8){
                    pwd.setError("Contraseña debe ser mayor de 8");
                    pwd.setFocusable(true);
                } else {
                    SignUp(e_mail, password);
                }
            }
        });
    }

    //Metodo para hacer el registro de usuario
    private void SignUp(String e_mail, String password){
        firebaseAuth.createUserWithEmailAndPassword(e_mail, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>(){
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task){
                        if (task.isSuccessful()) {
                            //Si el registro esta bien
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
                            HashMap<Object, String> DatosUsuario = new HashMap<>();
                            DatosUsuario.put("id", uid);
                            DatosUsuario.put("username", user_Name);
                            DatosUsuario.put("firstName", first_Name);
                            DatosUsuario.put("lastName", last_Name);
                            DatosUsuario.put("secondName", second_Name);
                            DatosUsuario.put("email", e_mail);
                            DatosUsuario.put("password", password);
                            DatosUsuario.put("profile_picure", "");

                            //Inicio una instancia de la bbdd
                            FirebaseDatabase database = FirebaseDatabase.getInstance("https://fluxe-a2d2d-default-rtdb.europe-west1.firebasedatabase.app");

                            //Creo la bbdd
                            DatabaseReference reference = database.getReference("users");
                            reference.child(uid).setValue(DatosUsuario)
                                            .addOnCompleteListener(task1 -> {
                                                if (task1.isSuccessful()) {
                                                    Toast.makeText(Registro.this, "Datos guardados en Firebase", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    Toast.makeText(Registro.this, "Error al guardar datos: " + task.getException(), Toast.LENGTH_SHORT).show();
                                                }
                                            });
                            Toast.makeText(Registro.this, "Registro completado con éxito", Toast.LENGTH_SHORT).show();

                            //Una vez registrado, nos lleva a la pantalla de inicio
                            startActivity(new Intent(Registro.this, Inicio.class));
                        } else{
                            Toast.makeText(Registro.this, "Algo ha salido mal", Toast.LENGTH_SHORT).show();

                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(Registro.this, e.getMessage(), Toast.LENGTH_SHORT).show();

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