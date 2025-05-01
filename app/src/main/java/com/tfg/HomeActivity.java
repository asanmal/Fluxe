package com.tfg;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.tfg.option.MyDataActivity;


public class HomeActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference DATABASE;

    ImageView profile_picture;
    TextView usernameProfile;
    TextView emailProfile;
    TextView nameProfile;

    Button signoutBtn, aboutMeOption, newPostOption, postOption, userOption, chatsOption;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("Home");
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        firebaseDatabase = FirebaseDatabase.getInstance();
        DATABASE = firebaseDatabase.getReference("users");

        profile_picture = findViewById(R.id.profile_picture);
        usernameProfile = findViewById(R.id.usernameProfile);
        emailProfile = findViewById(R.id.emailProfile);
        nameProfile = findViewById(R.id.nameProfile);

        /*OPCIONES DE MENU*/
        aboutMeOption = findViewById(R.id.aboutMeOption);
        newPostOption = findViewById(R.id.newPostOption);
        postOption = findViewById(R.id.postOption);
        userOption = findViewById(R.id.userOption);
        chatsOption = findViewById(R.id.chatsOption);
        signoutBtn = findViewById(R.id.signoutBtn);

        aboutMeOption.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Metodo para abrir mis datos de usuario
                Intent intent = new Intent(HomeActivity.this, MyDataActivity.class);
                startActivity(intent);
                Toast.makeText(HomeActivity.this, "My Data", Toast.LENGTH_SHORT).show();
            }
        });

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

    //Metodo que permita verificar si el usuario ya nicio sesion antes
    private void verifyLogin(){
        if (firebaseUser != null){
            loadData();
            Toast.makeText(this, "Logged in successfully", Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        }
    }

    //Metodo para recuperar los datos que vamos a mostrar en el perfil del usuario
    private void loadData(){
        Query query = DATABASE.orderByChild("email").equalTo(firebaseUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Recorro los usuarios registardos en firebase, hasta encontrar el usuario actual
                for (DataSnapshot ds : snapshot.getChildren()){

                    //Obtenemos los valores
                    String username = "" + ds.child("username").getValue();
                    String email = "" + ds.child("email").getValue();
                    String firstname = "" + ds.child("firstname").getValue();
                    String profilePicture = "" + ds.child("profile_picture").getValue();

                    //Seteamos los datos
                    usernameProfile.setText(username);
                    emailProfile.setText(email);
                    nameProfile.setText(firstname);

                    //Gestionar la foto de perfil del usuario
                    try{
                        //Si existe imagen la cargamos
                        if (profilePicture != null && !profilePicture.trim().isEmpty()) {
                            Picasso.get()
                                    .load(profilePicture)
                                    .placeholder(R.drawable.login)
                                    .error(R.drawable.login)
                                    .into(profile_picture);
                        } else {
                            profile_picture.setImageResource(R.drawable.login);
                        }
                    } catch (Exception e) {
                        //Si el usuario no tiene imagen en la base de datos
                        Picasso.get().load(profilePicture).into(profile_picture);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    //Metodo para cerrar sesion
    private void signOut(){
        firebaseAuth.signOut();
        Toast.makeText(this, "Signed out successfully", Toast.LENGTH_SHORT).show();

        startActivity(new Intent(HomeActivity.this, MainActivity.class));
    }
}