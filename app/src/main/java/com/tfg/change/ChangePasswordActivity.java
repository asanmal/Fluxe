package com.tfg.change;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserInfo;
import com.google.firebase.database.*;
import com.tfg.MainActivity;
import com.tfg.R;

public class ChangePasswordActivity extends AppCompatActivity {

    TextView myCredentials, currentEmail, currentEmailTxt;
    EditText currentPwdChg, newPwd;
    Button updatePwd;

    DatabaseReference users;
    FirebaseAuth firebaseAuth;
    FirebaseUser user;

    ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_change_password);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle(getString(R.string.title_change_password));
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        myCredentials = findViewById(R.id.myCredentials);
        currentEmail = findViewById(R.id.currentEmail);
        currentEmailTxt = findViewById(R.id.currentEmailTxt);
        currentPwdChg = findViewById(R.id.currentPwdChg);
        newPwd = findViewById(R.id.newPwd);
        updatePwd = findViewById(R.id.updatePwd);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        users = FirebaseDatabase.getInstance().getReference("users");
        progressDialog = new ProgressDialog(this);

        // Cambiar la fuente a las letras
        changeFont();

        // Mostrar email actual
        Query query = users.orderByChild("email").equalTo(user.getEmail());
        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    String email = ds.child("email").getValue(String.class);
                    currentEmail.setText(email);
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) { }
        });

        // Evento para cambiar la password
        updatePwd.setOnClickListener(v -> {
            FirebaseUser u = FirebaseAuth.getInstance().getCurrentUser();
            if (u == null) return;

            // Comprueba si viene de Google
            boolean isGoogle = false;
            for (UserInfo info : u.getProviderData()) {
                if ("google.com".equals(info.getProviderId())) {
                    isGoogle = true;
                    break;
                }
            }
            if (isGoogle) {
                // Si es Google, enviamos email de restablecimiento y volvemos al Main
                firebaseAuth.sendPasswordResetEmail(u.getEmail())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(this,
                                    getString(R.string.reset_email_sent),
                                    Toast.LENGTH_LONG).show();
                            startActivity(new Intent(this, MainActivity.class));
                            finish();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(this,
                                        getString(R.string.reset_email_error, e.getMessage()),
                                        Toast.LENGTH_LONG).show()
                        );
                return;
            }

            // Leer campos
            String before = currentPwdChg.getText().toString().trim();
            String after = newPwd.getText().toString().trim();

            // Validaciones
            if (before.isEmpty()) {
                Toast.makeText(this,
                        getString(R.string.current_pwd_empty),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (after.isEmpty()) {
                newPwd.setError(getString(R.string.new_pwd_required));
                newPwd.requestFocus();
                return;
            }
            if (after.length() < 8 || after.length() > 16) {
                newPwd.setError(getString(R.string.pwd_length_error));
                newPwd.requestFocus();
                return;
            }
            if (!after.matches("[A-Za-z0-9]+")) {
                newPwd.setError(getString(R.string.pwd_chars_error));
                newPwd.requestFocus();
                return;
            }

            // Reautenticamos y actualizamos
            AuthCredential cred = EmailAuthProvider
                    .getCredential(u.getEmail(), before);
            progressDialog.setTitle(getString(R.string.updating_title));
            progressDialog.setMessage(getString(R.string.updating_message));
            progressDialog.setCancelable(false);
            progressDialog.show();
            u.reauthenticate(cred)
                    .addOnSuccessListener(aVoid -> {
                        u.updatePassword(after)
                                .addOnSuccessListener(aVoid2 -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this,
                                            getString(R.string.pwd_changed_success),
                                            Toast.LENGTH_SHORT).show();
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                })
                                .addOnFailureListener(e -> {
                                    progressDialog.dismiss();
                                    Toast.makeText(this,
                                            getString(R.string.pwd_update_failed, e.getMessage()),
                                            Toast.LENGTH_SHORT).show();
                                });
                    })
                    .addOnFailureListener(e -> {
                        progressDialog.dismiss();
                        Toast.makeText(this,
                                getString(R.string.current_pwd_incorrect),
                                Toast.LENGTH_SHORT).show();
                    });
        });
    }

    // Metodo para cambiar la fuente
    private void changeFont(){
        Typeface tf = Typeface.createFromAsset(getAssets(), "fuente/sans_ligera.ttf");
        myCredentials.setTypeface(tf);
        currentEmail.setTypeface(tf);
        currentEmailTxt.setTypeface(tf);
        currentPwdChg.setTypeface(tf);
        newPwd.setTypeface(tf);
        updatePwd.setTypeface(tf);
    }

    // Acción de retroceso
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return super.onSupportNavigateUp();
    }
}
