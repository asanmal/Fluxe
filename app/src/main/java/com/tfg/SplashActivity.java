package com.tfg;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splash);

        //Esto demora la pantalla de carga
        final int duracion = 1500;

        new Handler().postDelayed(() -> {
            //Se ejecuta pasado el tiempo de duracion que le hemos puesto
            Intent intent = new Intent(SplashActivity.this, HomeActivity.class);
            startActivity(intent);
            //Nos dirije de esta actividad, al mainACtivity
        }, duracion);
    }
}