package com.tfg.change;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.tfg.LoginActivity;
import com.tfg.R;

import java.util.HashMap;

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
            actionBar.setTitle("Change Password");
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

        progressDialog = new ProgressDialog(ChangePasswordActivity.this);

        //Cambiar la fuent a las letras
        changeFont();

        //Consultar correo y contraseña del usuario actual
        Query query = users.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    //Traemos los valores
                    String email = "" + ds.child("email").getValue();

                    //Seteamos los datos en los textView
                    currentEmail.setText(email);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });


        //Evento para cambiar la password
        updatePwd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String beforePwd = currentPwdChg.getText().toString().trim();
                String afterPwd = newPwd.getText().toString().trim();

                //Condicionales para la nueva contraseña
                if (TextUtils.isEmpty(beforePwd)){
                    Toast.makeText(ChangePasswordActivity.this, "The current password field is empty", Toast.LENGTH_SHORT).show();
                }
                if (TextUtils.isEmpty(afterPwd)){
                    Toast.makeText(ChangePasswordActivity.this, "The new password field is empty", Toast.LENGTH_SHORT).show();
                }
                if (!afterPwd.equals("") && afterPwd.length() >= 8){
                    //LLamamos al metodo para cambiar la contraseña actual por la nueva
                    changePassword(beforePwd, afterPwd);
                } else {
                    newPwd.setError("The password must be longer than 8 characters");
                    newPwd.setFocusable(true);
                }
            }
        });
    }

    //Metodo para cambiar la contraseña
    private void changePassword(String beforePwd, String afterPwd) {
        progressDialog.show();
        progressDialog.setTitle("Updating");
        progressDialog.setMessage("Please wait!!");
        user = FirebaseAuth.getInstance().getCurrentUser();
        assert user != null;
        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(), beforePwd);
        user.reauthenticate(authCredential)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        user.updatePassword(afterPwd)
                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                    @Override
                                    public void onSuccess(Void unused) {
                                        progressDialog.dismiss();
                                        String value = newPwd.getText().toString().trim();
                                        HashMap<String, Object> result = new HashMap<>();
                                        result.put("password", value);

                                        //Actualizamos la nueva contraseña en firebase
                                        users.child(user.getUid()).updateChildren(result)
                                                .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                    @Override
                                                    public void onSuccess(Void unused) {
                                                        Toast.makeText(ChangePasswordActivity.this, "Password changed", Toast.LENGTH_SHORT).show();
                                                    }
                                                }).addOnFailureListener(new OnFailureListener() {
                                            @Override
                                            public void onFailure(@NonNull Exception e) {
                                                progressDialog.dismiss();
                                            }
                                        });
                                        //Luego se crierr la sesion
                                        firebaseAuth.signOut();
                                        startActivity(new Intent(ChangePasswordActivity.this, LoginActivity.class));
                                        finish();
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                                    @Override
                                    public void onFailure(@NonNull Exception e) {

                                        progressDialog.dismiss();
                                    }
                                });
                    }
                }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        progressDialog.dismiss();
                        Toast.makeText(ChangePasswordActivity.this, "The current password is incorrect", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    //Metodo para cambiar la  fuente
    private void changeFont(){
        //Fuente de letra
        String locate = "fuente/sans_ligera.ttf";
        Typeface tf = Typeface.createFromAsset(ChangePasswordActivity.this.getAssets(), locate);

        myCredentials.setTypeface(tf);
        currentEmail.setTypeface(tf);
        currentEmailTxt.setTypeface(tf);
        currentPwdChg.setTypeface(tf);
        newPwd.setTypeface(tf);
        updatePwd.setTypeface(tf);
    }

    //Accion de retroceso
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return super.onSupportNavigateUp();
    }
}