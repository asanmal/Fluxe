package com.tfg;

import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.squareup.picasso.Picasso;
import com.tfg.option.ChatOption;
import com.tfg.option.CreatePublicationActivity;
import com.tfg.option.FeedActivity;
import com.tfg.option.MyDataActivity;
import com.tfg.option.UsersActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

public class HomeActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference DATABASE;

    ImageView profile_picture;
    TextView usernameTxt, emailTxt;
    TextView date, usernameProfile, emailProfile;
    //TextView  nameProfile;

    Button signoutBtn, aboutMeOption, newPostOption, postOption, userOption, chatsOption, btnTestPing;

    //Follow contador y ref
    TextView tvFollowersCount, tvFollowingCount;
    DatabaseReference followRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle(getString(R.string.title_home));
        }

        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        DATABASE = firebaseDatabase.getReference("users");

        profile_picture = findViewById(R.id.profile_picture);
        date = findViewById(R.id.date);
        usernameProfile = findViewById(R.id.usernameProfile);
        emailProfile = findViewById(R.id.emailProfile);
        //nameProfile = findViewById(R.id.nameProfile);

        usernameTxt = findViewById(R.id.usernameTxt);
        emailTxt = findViewById(R.id.emailTxt);
        //nameTxt = findViewById(R.id.nameTxt);

        /* OPCIONES DE MENU */
        aboutMeOption = findViewById(R.id.aboutMeOption);
        newPostOption = findViewById(R.id.newPostOption);
        postOption = findViewById(R.id.postOption);
        userOption = findViewById(R.id.userOption);
        chatsOption = findViewById(R.id.chatsOption);
        signoutBtn = findViewById(R.id.signoutBtn);
        btnTestPing = findViewById(R.id.btnTestPing);

        // Follow bindear contadores
        tvFollowersCount = findViewById(R.id.tvFollowersCount);
        tvFollowingCount = findViewById(R.id.tvFollowingCount);
        followRef = FirebaseDatabase.getInstance().getReference("followers");

        // Cambiar fuente
        changeFont();

        // Fecha actual
        Date dates = new Date();
        SimpleDateFormat dateC = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH);
        String sDate = dateC.format(dates);
        date.setText(sDate);

        // Opción de mis datos
        aboutMeOption.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MyDataActivity.class));
            Toast.makeText(HomeActivity.this,
                    getString(R.string.toast_my_data),
                    Toast.LENGTH_SHORT).show();
        });

        // Opcion de todos los usuarios
        userOption.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, UsersActivity.class));
            Toast.makeText(HomeActivity.this,
                    getString(R.string.toast_users),
                    Toast.LENGTH_SHORT).show();
        });

        // Opción para cerrar sesion
        signoutBtn.setOnClickListener(v -> signOut());

        // Opción de chats
        chatsOption.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ChatOption.class);
            intent.putExtra("id", firebaseUser.getUid());
            startActivity(intent);
        });

        // Opción New Post
        newPostOption.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, CreatePublicationActivity.class));
            Toast.makeText(HomeActivity.this,
                    getString(R.string.toast_create_post),
                    Toast.LENGTH_SHORT).show();
        });

        // Opción Feed
        postOption.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FeedActivity.class));
            Toast.makeText(HomeActivity.this,
                    getString(R.string.toast_feed),
                    Toast.LENGTH_SHORT).show();
        });

        // Click en contadores para ver lista
        tvFollowersCount.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, UsersActivity.class);
            i.putExtra("show", "followers");
            startActivity(i);
        });
        tvFollowingCount.setOnClickListener(v -> {
            Intent i = new Intent(HomeActivity.this, UsersActivity.class);
            i.putExtra("show", "following");
            startActivity(i);
        });

        findViewById(R.id.btnTestPing).setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, com.tfg.view.PingActivity.class));
        });
    }

    // Metodo para cambiar la fuente
    private void changeFont(){
        Typeface tf = Typeface.createFromAsset(getAssets(), "fuente/sans_ligera.ttf");
        date.setTypeface(tf);
        usernameProfile.setTypeface(tf);
        emailProfile.setTypeface(tf);
        //nameProfile.setTypeface(tf);
        usernameTxt.setTypeface(tf);
        emailTxt.setTypeface(tf);
        //nameTxt.setTypeface(tf);
        signoutBtn.setTypeface(tf);
        aboutMeOption.setTypeface(tf);
        newPostOption.setTypeface(tf);
        postOption.setTypeface(tf);
        userOption.setTypeface(tf);
        chatsOption.setTypeface(tf);
        tvFollowersCount.setTypeface(tf);
        tvFollowingCount.setTypeface(tf);
        btnTestPing.setTypeface(tf);
    }

    @Override
    protected void onStart(){
        super.onStart();
        verifyLogin();
    }

    // Verifica si hay un usuario logueado
    private void verifyLogin(){
        if (firebaseUser != null){
            loadData();
            Toast.makeText(this,
                    getString(R.string.toast_logged_in),
                    Toast.LENGTH_SHORT).show();
        } else {
            startActivity(new Intent(HomeActivity.this, MainActivity.class));
            finish();
        }
    }

    // Carga datos de perfil
    private void loadData(){
        Query query = DATABASE.orderByChild("email").equalTo(firebaseUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()){
                    String username = ds.child("username").getValue(String.class);
                    String email = ds.child("email").getValue(String.class);
                    String firstname = ds.child("firstName").getValue(String.class);
                    String profilePicture = ds.child("profile_picture").getValue(String.class);

                   //nameProfile.setText(firstname != null ? firstname : "");
                    usernameProfile.setText(username);
                    emailProfile.setText(email);

                    try {
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
                        Picasso.get().load(R.drawable.login).into(profile_picture);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });

        // Contar followers
        followRef.child(firebaseUser.getUid()).child("followers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        int cnt = (int) ds.getChildrenCount();
                        tvFollowersCount.setText(
                                getString(R.string.followers_count, cnt)
                        );
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
        // Contar following
        followRef.child(firebaseUser.getUid()).child("following")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        int cnt = (int) ds.getChildrenCount();
                        tvFollowingCount.setText(
                                getString(R.string.following_count, cnt)
                        );
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // Cierra sesion
    private void signOut(){
        firebaseAuth.signOut();
        Toast.makeText(this,
                getString(R.string.toast_signed_out),
                Toast.LENGTH_SHORT).show();
        startActivity(new Intent(HomeActivity.this, MainActivity.class)
                .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP));
    }

    // Estado online/offline
    private void status(String status){
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(firebaseUser.getUid());
        ref.updateChildren(new HashMap<String,Object>() {{ put("status", status); }});
    }

    @Override
    protected void onResume(){
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause(){
        super.onPause();
        status("offline");
    }
}
