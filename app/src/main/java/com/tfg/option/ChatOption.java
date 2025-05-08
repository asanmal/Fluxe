package com.tfg.option;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.R;
import com.tfg.adapters.UserAdapter;
import com.tfg.models.Chat;
import com.tfg.models.User;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChatOption extends AppCompatActivity {

    private RecyclerView recyclerView;
    private UserAdapter userAdapter;

    private List<User> mUsers;

    FirebaseUser firebaseUser;
    DatabaseReference DATABASE;

    private List<String> usersList;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat_option);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle("Chats");
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        usersList = new ArrayList<>();

        // Leemos todos los chats para extraer IDs de interlocutores
        DATABASE = FirebaseDatabase.getInstance().getReference("chats");
        DATABASE.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                usersList.clear();

                for (DataSnapshot snapchot : dataSnapshot.getChildren()){
                    Chat chat = snapchot.getValue(Chat.class);
                    if (chat == null || firebaseUser == null) continue;

                    // Si soy el emisor, recojo receptor
                    if (chat.getSender().equals(firebaseUser.getUid())){
                        usersList.add(chat.getReceiver());
                    }
                    // Si soy el receptor, recojo emisor
                    if (chat.getReceiver().equals(firebaseUser.getUid())){
                        usersList.add(chat.getSender());
                    }
                }

                // Una vez tengo todos los IDs, leo los perfiles de usuario
                readChats();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    private void readChats(){
        // Creamos un set para IDs únicos
        Set<String> uniqueIds = new HashSet<>(usersList);
        mUsers = new ArrayList<>();

        // Leemos todos los usuarios y solo añadimos los que estén en uniqueIds
        DATABASE = FirebaseDatabase.getInstance().getReference("users");
        DATABASE.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                mUsers.clear();

                // display 1 usuario del chats
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    User user = snapshot.getValue(User.class);
                    if (user == null) continue;

                    // Si este usuario está en la lista de interlocutores
                    if (uniqueIds.contains(user.getId())) {
                        // Verificar que no lo hayamos añadido ya
                        boolean alreadyAdded = false;
                        for (User u : mUsers) {
                            if (u.getId().equals(user.getId())) {
                                alreadyAdded = true;
                                break;
                            }
                        }
                        if (!alreadyAdded) {
                            mUsers.add(user);
                        }
                    }
                }

                // Cada vez que cambian los datos, inicializamos o notificamos al adapter
                if (userAdapter == null) {
                    userAdapter = new UserAdapter(mUsers, true);
                    recyclerView.setAdapter(userAdapter);
                } else {
                    userAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });
    }

    //Accion de retroceso
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return super.onSupportNavigateUp();
    }
}
