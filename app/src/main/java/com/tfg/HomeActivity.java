package com.tfg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class HomeActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;

    Button signoutBtn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("Home");
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        signoutBtn = findViewById(R.id.signoutBtn);

        signoutBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Metodo para cerrar sesison
                signOut();
            }
        });
    }

    //LLamamos a onStart
    @Override
    protected void onStart(){
        verifyLogin();
        super.onStart();
    }

    //Creamos un metodo que permita verificar si el usuario ya nicio sesion antes
    private void verifyLogin(){
        if (firebaseUser != null){
            Toast.makeText(this, "Logged in successfully", Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        }
    }

    //Metodo para cerrar sesion
    private void signOut(){
        firebaseAuth.signOut();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        startActivity(new Intent(HomeActivity.this, MainActivity.class));
    }
}