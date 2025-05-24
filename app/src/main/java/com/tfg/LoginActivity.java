package com.tfg;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.*;
import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    EditText emailLogin, pwdLogin;
    Button registerLogin, googleLogin;
    TextView tvForgotPassword;

    private GoogleSignInClient mGoogleSignInClient;
    private final static int RC_SIGN_IN = 123;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("Login");
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        emailLogin = findViewById(R.id.emailLogin);
        pwdLogin = findViewById(R.id.pwdLogin);
        registerLogin = findViewById(R.id.registerLogin);
        googleLogin = findViewById(R.id.googleLogin);
        tvForgotPassword= findViewById(R.id.tvForgotPassword);

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(LoginActivity.this);
        dialog = new Dialog(LoginActivity.this);

        // Creamos la solicitud de inicio para Google
        createRequest();

        // Método para cambiar la fuente
        changeFont();

        // Forgot password: validamos email y enviamos reset
        tvForgotPassword.setOnClickListener(v -> {
            String email = emailLogin.getText().toString().trim();
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                emailLogin.setError(getString(R.string.invalid_email));
                emailLogin.requestFocus();
                return;
            }
            firebaseAuth
                    .sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this,
                                    getString(R.string.reset_email_sent),
                                    Toast.LENGTH_LONG).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    getString(R.string.reset_email_error, e.getMessage()),
                                    Toast.LENGTH_LONG).show()
                    );
        });

        // Asignamos evento al botón de ingresar
        registerLogin.setOnClickListener(v -> {
            String email = emailLogin.getText().toString();
            String pwd = pwdLogin.getText().toString();

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                emailLogin.setError("Invalid email address");
                emailLogin.setFocusable(true);
            } else if (pwd.length()<8) {
                pwdLogin.setError("Password must be at least 8 characters");
                pwdLogin.setFocusable(true);
            } else {
                userLogin(email, pwd);
            }
        });

        // Asignamos evento al botón de ingresar con Google
        googleLogin.setOnClickListener(v -> {
            // Primero deslogueamos para que siempre aparezca el selector
            mGoogleSignInClient.signOut()
                    .addOnCompleteListener(LoginActivity.this, task -> signIn());
        });
    }

    // Método para cambiar la fuente de las letras
    private void changeFont(){
        // Fuente de letra
        String locate = "fuente/sans_ligera.ttf";
        Typeface tf = Typeface.createFromAsset(getAssets(), locate);

        emailLogin.setTypeface(tf);
        pwdLogin.setTypeface(tf);
        registerLogin.setTypeface(tf);
        googleLogin.setTypeface(tf);
        tvForgotPassword.setTypeface(tf);
    }

    // Método para crear una solicitud de inicio con Google
    private void createRequest() {
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    // Método para lanzar la pantalla de Google Sign-In
    private void signIn(){
        Intent signIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signIntent, RC_SIGN_IN);
    }

    // Resultado de Google Sign-In
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Si Google sign-in ok, autenticamos en Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthentication(account);
            } catch (ApiException e){
                Toast.makeText(this,
                        getString(R.string.error_google_signin, e.getStatusCode()),
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    // Autenticación con Firebase tras Google Sign-In
    private void firebaseAuthentication(GoogleSignInAccount account){
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()){
                        FirebaseUser user = firebaseAuth.getCurrentUser();
                        // Si es un usuario nuevo, le asignamos username por defecto único
                        if (task.getResult().getAdditionalUserInfo().isNewUser() && user != null){
                            assignDefaultUsername(user);
                        } else {
                            // Usuario ya existente, vamos a Home
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        }
                    } else {
                        dialogNoSeason();
                    }
                });
    }

    // Genera un username "defaultfluxeX" único
    private void assignDefaultUsername(FirebaseUser user) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance(
                "https://fluxe-a2d2d-default-rtdb.europe-west1.firebasedatabase.app"
        ).getReference("users");

        final int[] index = {1};
        recursiveCheckUsername(usersRef, user, index[0]);
    }

    // Comprueba recursivamente si "defaultfluxeN" existe; si no, lo usa
    private void recursiveCheckUsername(DatabaseReference usersRef,
                                        FirebaseUser user,
                                        int idx) {
        String candidate = "defaultfluxe" + idx;
        usersRef.orderByChild("username").equalTo(candidate)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snap) {
                        if (snap.exists()) {
                            // Ya existe ese username, probamos con el siguiente
                            recursiveCheckUsername(usersRef, user, idx+1);
                        } else {
                            // Username libre: guardamos al usuario
                            HashMap<Object,String> UserData = new HashMap<>();
                            UserData.put("id",              user.getUid());
                            UserData.put("username",        candidate);
                            UserData.put("firstName",       "");
                            UserData.put("lastName",        "");
                            UserData.put("secondName",      "");
                            UserData.put("email",           user.getEmail());
                            UserData.put("profile_picture", "");

                            usersRef.child(user.getUid())
                                    .setValue(UserData)
                                    .addOnCompleteListener(t -> {
                                        // Una vez guardado, vamos a Home
                                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e ->
                                            Toast.makeText(
                                                    LoginActivity.this,
                                                    getString(R.string.error_saving_user, e.getMessage()),
                                                    Toast.LENGTH_LONG
                                            ).show()
                                    );
                        }
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { }
                });
    }

    // Inicio de sesión con email/password
    private void userLogin(String email, String pwd) {
        progressDialog.setTitle("Signing in");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        firebaseAuth.signInWithEmailAndPassword(email, pwd)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        FirebaseUser u = firebaseAuth.getCurrentUser();
                        Toast.makeText(
                                LoginActivity.this,
                                getString(R.string.toast_welcome, email),
                                Toast.LENGTH_SHORT
                        ).show();
                        finish();
                    } else {
                        dialogNoSeason();
                    }
                })
                .addOnFailureListener(e -> {
                    progressDialog.dismiss();
                    Toast.makeText(LoginActivity.this,
                            e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    // Diálogo cuando falla el login
    private void dialogNoSeason(){
        Button ok_no_season;
        dialog.setContentView(R.layout.no_season);
        ok_no_season = dialog.findViewById(R.id.ok_no_season);
        ok_no_season.setOnClickListener(v -> dialog.dismiss());
        dialog.setCancelable(false);
        dialog.show();
    }

    // Acción de retroceso
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return super.onSupportNavigateUp();
    }
}
