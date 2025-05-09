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

    // Lista completa y lista filtrada
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filtered = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(com.tfg.R.layout.activity_users);

        // Configuracion ActionBar con flecha Up
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("Users");
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // Referencias a vistas
        searchView   = findViewById(com.tfg.R.id.search_view);
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

        // Carga usuarios y al terminar llena ambas listas y refresca
        loadUsers(() -> {
            filtered.clear();
            filtered.addAll(allUsers);
            adapter.notifyDataSetChanged();
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }

    /** Carga todos los usuarios en allUsers, luego callback */
    private void loadUsers(Runnable onLoaded) {
        FirebaseUser current = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("users");
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                allUsers.clear();
                for (DataSnapshot ds : snap.getChildren()) {
                    User u = ds.getValue(User.class);
                    if (u != null && current != null &&
                            !u.getId().equals(current.getUid())) {
                        allUsers.add(u);
                    }
                }
                onLoaded.run();
            }
            @Override public void onCancelled(@NonNull DatabaseError err) { }
        });
    }

    //Filtra allUsers por username conteniendo query
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
