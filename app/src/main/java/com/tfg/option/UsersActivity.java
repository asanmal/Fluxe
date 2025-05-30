package com.tfg.option;

import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.tfg.R;
import com.tfg.adapters.UserAdapter;
import com.tfg.models.User;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SearchView searchView;

    // Referencia a followers para cargar seguidores/siguiendo
    private DatabaseReference followRef;

    // Usuario actual
    private FirebaseUser current;

    // Listas de datos
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filtered = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        // Configuracion ActionBar con flecha Up
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // Inicializar referencias Firebase
        current = FirebaseAuth.getInstance().getCurrentUser();
        followRef = FirebaseDatabase.getInstance().getReference("followers");

        // Referencias a vistas
        searchView = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recyclerView);

        // FORZAR SearchView siempre expandido y con botón submit
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);

        // RecyclerView y Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        UserAdapter adapter = new UserAdapter(filtered, false);
        recyclerView.setAdapter(adapter);

        // Listener del evento de SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Filtrar al pulsar la lupa de submit
                filter(query);
                adapter.notifyDataSetChanged();
                // Oculta teclado
                searchView.clearFocus();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String text) {
                // Filtrar en tiempo real al escribir
                filter(text);
                adapter.notifyDataSetChanged();
                return true;
            }
        });

        // Condicional por si son seguidores, siguiendo o todos
        String showMode = getIntent().getStringExtra("show");
        if (showMode != null && showMode.equals(getString(R.string.show_followers))) {
            // Sololo seguidores
            if (ab != null) ab.setTitle(getString(R.string.followers_title));
            loadFollowers(() -> updateList(adapter));
        } else if (showMode != null && showMode.equals(getString(R.string.show_following))) {
            // Solo usuarios seguidos
            if (ab != null) ab.setTitle(getString(R.string.following_title));
            loadFollowing(() -> updateList(adapter));
        } else {
            // Por defecto todos los usuarios
            if (ab != null) ab.setTitle(getString(R.string.users));
            loadUsers(() -> updateList(adapter));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    // Actualizar el adapter con el contenido de allUsers
    private void updateList(UserAdapter adapter) {
        filtered.clear();
        filtered.addAll(allUsers);
        adapter.notifyDataSetChanged();
    }

    //Carga todos los usuarios en allUsers, luego callback
    private void loadUsers(Runnable onLoaded) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                allUsers.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u == null || current == null) continue;

                    String uid = u.getId();
                    String myUid = current.getUid();
                    // uid no sea null y comparamos invirtiendo equals
                    if (uid != null && !myUid.equals(uid)) {
                        allUsers.add(u);
                    }
                }
                onLoaded.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError err) { }
        });
    }

    // Carga solo los usuarios que siguen al usuario actual
    private void loadFollowers(Runnable onLoaded) {
        if (current == null) return;
        followRef.child(current.getUid()).child("followers")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        List<String> ids = new ArrayList<>();
                        for (DataSnapshot ds : snap.getChildren()) {
                            ids.add(ds.getKey());
                        }
                        fetchUsersByIds(ids, onLoaded);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { }
                });
    }

    //Carga solo los usuarios a los que sigue el usuario actual
    private void loadFollowing(Runnable onLoaded) {
        if (current == null) return;
        followRef.child(current.getUid()).child("following")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        List<String> ids = new ArrayList<>();
                        for (DataSnapshot ds : snap.getChildren()) {
                            ids.add(ds.getKey());
                        }
                        fetchUsersByIds(ids, onLoaded);
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) { }
                });
    }

    //Con un listado de IDs, filtra el nodo users por esos IDs
    private void fetchUsersByIds(List<String> ids, Runnable onLoaded) {
        DatabaseReference usersRef = FirebaseDatabase.getInstance().getReference("users");
        usersRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                allUsers.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u != null && ids.contains(u.getId())) {
                        allUsers.add(u);
                    }
                }
                onLoaded.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
    }

    // Filtro allUsers por username conteniendo query
    private void filter(String query) {
        filtered.clear();
        if (TextUtils.isEmpty(query)) {
            filtered.addAll(allUsers);
        } else {
            String lower = query.toLowerCase();
            for (User u : allUsers) {
                if (u.getUsername().toLowerCase().contains(lower)) {
                    filtered.add(u);
                }
            }
        }
    }
}