package com.tfg;

import android.app.Dialog;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;

public class LoginActivity extends AppCompatActivity {

    EditText emailLogin, pwdLogin;
    Button registerLogin, googleLogin;

    private GoogleSignInClient mGoogleSignInClient;
    private final static int RC_SIGN_IN = 123;

    private FirebaseAuth firebaseAuth;
    private ProgressDialog progressDialog;
    Dialog dialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
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

        firebaseAuth = FirebaseAuth.getInstance();
        progressDialog = new ProgressDialog(LoginActivity.this);
        dialog = new Dialog(LoginActivity.this);

        //Creamos la solicitud de inicio para google
        createRequest();

        //Asignamos evento al boton de ingresar
        registerLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
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
            }
        });

        //Asignamos evento al boton de ingresar con google
        googleLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signIn();
            }
        });
    }

    //Metodo para crear una solicitud de inicio con google
    private void createRequest() {
        //Configuracion de inicio de Google token + email
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id)) //Token para autenticacion con firebase
                .requestEmail()
                .build();

        //Crea el cliente de inicio de sesion con Google usando las opciones configuradas
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }

    //Metodo para crear la pantalla de google
    private void signIn(){
        Intent signIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signIntent, RC_SIGN_IN);
    }

    //Metodo que recibe el resultado de la actividad lanzada con startActivityForResult
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        //Resultado devuelto al iniciar con GoogleSignInAPi
        if (requestCode == RC_SIGN_IN){
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);

            try{
                //Inicio de sesion valido usando firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                //Ejecucion metodo de login con google
                firebaseAuthentication(account);
            } catch (ApiException e){
                Toast.makeText(this, e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    //Metodo para autenticarse en google con firebase
    private void firebaseAuthentication(GoogleSignInAccount account){
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        firebaseAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()){
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            if (task.getResult().getAdditionalUserInfo().isNewUser()){

                                String uid = user.getUid();
                                String username = user.getDisplayName();
                                String email = user.getEmail();

                                //Pasamos los parametros para registrar al nuevo usuario
                                //Hashmap con los datos de usuario para pasarlo a la bbdd
                                HashMap<Object, String> UserData = new HashMap<>();

                                UserData.put("id", uid);
                                UserData.put("username", username);
                                //UserData.put("firstName", first_Name);
                                //UserData.put("lastName", last_Name);
                                //UserData.put("secondName", second_Name);
                                UserData.put("email", email);
                                //UserData.put("password", password);
                                UserData.put("profile_picure", "");

                                //HomeActivity una instancia de la bbdd
                                FirebaseDatabase database = FirebaseDatabase.getInstance("https://fluxe-a2d2d-default-rtdb.europe-west1.firebasedatabase.app");

                                //Creo la bbdd
                                DatabaseReference reference = database.getReference("users");
                                reference.child(uid).setValue(UserData);
                            }

                            //Iniciamos el activity para que nos lleve al inicio
                            startActivity(new Intent(LoginActivity.this, HomeActivity.class));
                        } else {
                            dialogNoSeason();
                        }
                    }
                });
    }

    //Metodo para iniciar sesion el usuario
    private void userLogin(String email, String pwd) {
        progressDialog.setTitle("Signing in");
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        firebaseAuth.signInWithEmailAndPassword(email, pwd)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        //Condicional por si se inicia sesion bien
                        if (task.isSuccessful()) {
                            progressDialog.dismiss(); //Cerrando progress
                            FirebaseUser user = firebaseAuth.getCurrentUser();

                            //Cuando iniciemos sesion nos lleve al inicio
                            startActivity(new Intent( LoginActivity.this, HomeActivity.class));
                            assert  user != null;
                            Toast.makeText(LoginActivity.this, "Welcome to Fluxe " + user.getEmail(), Toast.LENGTH_SHORT).show();
                            finish();
                        } else {
                            progressDialog.dismiss();
                            dialogNoSeason();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(LoginActivity.this, e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Creamos el metodo del dialogo personalizado cuando el usuario no pueda iniciar sesion
    private void dialogNoSeason(){
        Button ok_no_season;

        dialog.setContentView(R.layout.no_season);
        ok_no_season = dialog.findViewById(R.id.ok_no_season);

        //Accion al presionar en ok para cerrar el cuadro de dialogo
        ok_no_season.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.setCancelable(false);
        dialog.show();
    }

    //Accion de retroceso
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return super.onSupportNavigateUp();
    }
}