package com.tfg;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

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

    private static final int PERMISSION_REQUEST_POST_NOTIFICATIONS = 100;
    private static final String CHANNEL_ID = "follow_notifications";

    FirebaseAuth firebaseAuth;
    FirebaseUser firebaseUser;

    FirebaseDatabase firebaseDatabase;
    DatabaseReference DATABASE;

    ImageView profile_picture;
    TextView usernameTxt, emailTxt;
    TextView date, usernameProfile, emailProfile;
    Button signoutBtn, aboutMeOption, newPostOption, postOption, userOption, chatsOption, btnTestPing;

    // Follow contador y ref
    TextView tvFollowersCount, tvFollowingCount;
    DatabaseReference followRef;

    // Ignorar notificaciones iniciales
    private int ignoreFollowerEvents = 0;
    private ChildEventListener followersListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        // Crear canal de notificación
        createNotificationChannel();
        // Solicitar permiso POST_NOTIFICATIONS en Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        PERMISSION_REQUEST_POST_NOTIFICATIONS);
            }
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(getString(R.string.title_home));
        }

        // Inicializar Firebase
        firebaseAuth = FirebaseAuth.getInstance();
        firebaseUser = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        DATABASE = firebaseDatabase.getReference("users");

        // Bind views
        profile_picture = findViewById(R.id.profile_picture);
        date = findViewById(R.id.date);
        usernameProfile = findViewById(R.id.usernameProfile);
        emailProfile = findViewById(R.id.emailProfile);
        usernameTxt = findViewById(R.id.usernameTxt);
        emailTxt = findViewById(R.id.emailTxt);
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
        String sDate = new SimpleDateFormat("MMMM d, yyyy", Locale.ENGLISH).format(new Date());
        date.setText(sDate);

        // Lectura inicial de seguidores para contador y notificaciones
        followRef.child(firebaseUser.getUid()).child("followers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        long count = ds.getChildrenCount();
                        ignoreFollowerEvents = (int) count;
                        tvFollowersCount.setText(
                                getString(R.string.followers_count, count)
                        );
                        // Empezar a escuchar cambios en followers
                        attachFollowersListeners();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // Opción de mis datos
        aboutMeOption.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, MyDataActivity.class));
            Toast.makeText(this, getString(R.string.toast_my_data), Toast.LENGTH_SHORT).show();
        });
        // Opción de todos los usuarios
        userOption.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, UsersActivity.class));
            Toast.makeText(this, getString(R.string.toast_users), Toast.LENGTH_SHORT).show();
        });
        // Cerrar sesión
        signoutBtn.setOnClickListener(v -> signOut());
        // Chats
        chatsOption.setOnClickListener(v -> {
            Intent intent = new Intent(HomeActivity.this, ChatOption.class);
            intent.putExtra("id", firebaseUser.getUid());
            startActivity(intent);
        });
        // New Post
        newPostOption.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, CreatePublicationActivity.class));
            Toast.makeText(this, getString(R.string.toast_create_post), Toast.LENGTH_SHORT).show();
        });
        // Feed
        postOption.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, FeedActivity.class));
            Toast.makeText(this, getString(R.string.toast_feed), Toast.LENGTH_SHORT).show();
        });
        // Lista followers/following
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
        btnTestPing.setOnClickListener(v -> {
            startActivity(new Intent(HomeActivity.this, com.tfg.view.PingActivity.class));
        });
    }

    // Manejar permisos
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Adjunta listeners para recuento y notificaciones
    private void attachFollowersListeners() {
        // Actualizar contador en tiempo real
        followRef.child(firebaseUser.getUid()).child("followers")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        long count = ds.getChildrenCount();
                        tvFollowersCount.setText(
                                getString(R.string.followers_count, count)
                        );
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // 2) Notificar solo nuevas inserciones
        followersListener = followRef.child(firebaseUser.getUid())
                .child("followers")
                .addChildEventListener(new ChildEventListener() {
                    @Override public void onChildAdded(@NonNull DataSnapshot snapshot, String prev) {
                        if (ignoreFollowerEvents-- > 0) return;
                        showFollowNotification(snapshot.getKey());
                    }
                    @Override public void onChildChanged(@NonNull DataSnapshot ds, String s) {}
                    @Override public void onChildRemoved(@NonNull DataSnapshot ds) {}
                    @Override public void onChildMoved(@NonNull DataSnapshot ds, String s) {}
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // Mostrar notificación de nuevo seguidor
    private void showFollowNotification(String newFollowerUid) {
        boolean canNotify = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            canNotify = ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS)
                    == PackageManager.PERMISSION_GRANTED;
        }
        if (!canNotify) return;

        FirebaseDatabase.getInstance()
                .getReference("users")
                .child(newFollowerUid)
                .child("username")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        String who = ds.getValue(String.class);
                        String title = getString(R.string.notification_title);
                        String text = getString(
                                R.string.notification_text,
                                who != null ? who : "Someone");
                        NotificationCompat.Builder builder = new NotificationCompat.Builder(
                                HomeActivity.this, CHANNEL_ID)
                                .setSmallIcon(R.drawable.ic_notification)
                                .setContentTitle(title)
                                .setContentText(text)
                                .setAutoCancel(true);
                        NotificationManagerCompat.from(HomeActivity.this)
                                .notify(newFollowerUid.hashCode(), builder.build());
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    // Crear canal de notificacion
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return;
        CharSequence name = getString(R.string.channel_name);
        String description = getString(R.string.channel_description);
        int importance = NotificationManager.IMPORTANCE_DEFAULT;
        NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    // Metodo para cambiar la fuente
    private void changeFont() {
        Typeface tf = Typeface.createFromAsset(getAssets(), "fuente/sans_ligera.ttf");
        date.setTypeface(tf);
        usernameProfile.setTypeface(tf);
        emailProfile.setTypeface(tf);
        usernameTxt.setTypeface(tf);
        emailTxt.setTypeface(tf);
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
    protected void onStart() {
        super.onStart();
        verifyLogin();
    }

    // Verifica login
    private void verifyLogin() {
        if (firebaseUser != null) {
            loadData();
            Toast.makeText(
                    this,
                    getString(R.string.toast_logged_in),
                    Toast.LENGTH_SHORT
            ).show();
        } else {
            startActivity(
                    new Intent(this, MainActivity.class)
            );
            finish();
        }
    }

    // Carga datos de perfil
    private void loadData() {
        Query query = DATABASE.orderByChild("email")
                .equalTo(firebaseUser.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                for (DataSnapshot ds : snap.getChildren()) {
                    String username = ds.child("username").getValue(String.class);
                    String email = ds.child("email").getValue(String.class);
                    String pic = ds.child("profile_picture").getValue(String.class);
                    usernameProfile.setText(username);
                    emailProfile.setText(email);
                    try {
                        if (pic != null && !pic.trim().isEmpty()) {
                            Picasso.get()
                                    .load(pic)
                                    .placeholder(R.drawable.login)
                                    .error(R.drawable.login)
                                    .into(profile_picture);
                        } else {
                            profile_picture.setImageResource(R.drawable.login);
                        }
                    } catch (Exception e) {
                        Picasso.get().load(R.drawable.login)
                                .into(profile_picture);
                    }
                }
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {}
        });

        // Actualizar contadores iniciales (aunque ya lo hace ValueEventListener)
        followRef.child(firebaseUser.getUid())
                .child("following")
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
    private void signOut() {
        firebaseAuth.signOut();
        Toast.makeText(
                this,
                getString(R.string.toast_signed_out),
                Toast.LENGTH_SHORT
        ).show();
        startActivity(
                new Intent(this, MainActivity.class)
                        .setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        );
    }

    // Estado online/offline
    private void status(String status) {
        DatabaseReference ref = FirebaseDatabase.getInstance()
                .getReference("users")
                .child(firebaseUser.getUid());
        ref.updateChildren(new HashMap<String,Object>() {{
            put("status", status);
        }});
    }

    @Override
    protected void onResume() {
        super.onResume();
        status("online");
    }

    @Override
    protected void onPause() {
        super.onPause();
        status("offline");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (followersListener != null) {
            followRef.child(firebaseUser.getUid())
                    .child("followers")
                    .removeEventListener(followersListener);
        }
    }
}