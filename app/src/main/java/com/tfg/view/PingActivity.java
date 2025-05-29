package com.tfg.view;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.tfg.R;

import java.io.BufferedReader;
import java.io.InputStreamReader;

public class PingActivity extends AppCompatActivity {

    private TextView logTextView;
    private ScrollView logScrollView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);

        // Barra de acción con titulo y boton para atras
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.test);
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        // Referencia de las vistas
        logTextView = findViewById(R.id.tvOutput);
        logScrollView = findViewById(R.id.scroll);
        Button pingButton = findViewById(R.id.btnRunPing);

        // Al pulsar, ejecuta el metodo de ping
        pingButton.setOnClickListener(v -> executePing());
    }

    //Ejecuta un ping en segundo plano y muestra el resultado.
    private void executePing() {
        //Avisamos al usuario que inicia la prueba
        logTextView.setText(getString(R.string.ping_running) + "\n");

        // Creamos la tarea para no bloquear la UI
        Runnable pingTask = () -> {
            try {
                //Preparamos el comando ping
                ProcessBuilder processBuilder = new ProcessBuilder(
                        "/system/bin/ping",   // ejecutable
                        "-c", "4",            // envía 4 paquetes
                        "firebase.google.com" // destino
                );
                Process pingProcess = processBuilder.start();

                // Leemos la salida estandar linea a linea
                BufferedReader reader = new BufferedReader(new InputStreamReader(pingProcess.getInputStream()));

                StringBuilder outputBuffer = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    outputBuffer.append(line).append("\n");
                }
                reader.close();

                //Volcamos el resultado en la UI
                runOnUiThread(() -> {
                    logTextView.setText(outputBuffer.toString());
                    // desplazamos hasta el final
                    logScrollView.post(() ->
                            logScrollView.fullScroll(ScrollView.FOCUS_DOWN)
                    );
                });

            } catch (Exception ex) {
                //Si hay error, lo mostramos
                runOnUiThread(() ->
                        logTextView.setText(
                                getString(R.string.ping_error, ex.getMessage())
                        )
                );
            }
        };

        //Arrancamos la tarea en un hilo aparte
        new Thread(pingTask).start();
    }

    //Boton de retroceso en el ActionBar.
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
