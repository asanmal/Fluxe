package com.tfg;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;   // IMPORTANTE
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.tfg.models.User;

import java.util.ArrayList;
import java.util.List;

public class UsersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SearchView searchView;  // Asegúrate de usar el widget de AppCompat

    // Lista completa y lista filtrada
    private final List<User> allUsers = new ArrayList<>();
    private final List<User> filtered = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        // Configura ActionBar con flecha Up
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("Users");
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // Referencias a vistas
        searchView   = findViewById(R.id.search_view);
        recyclerView = findViewById(R.id.recyclerView);

        // FORZAR SearchView siempre expandido y con botón submit
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);

        // RecyclerView + Adapter
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        UserAdapter adapter = new UserAdapter(filtered);
        recyclerView.setAdapter(adapter);

        // Listener del SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                // Filtrar al pulsar la lupa de submit
                filter(query);
                adapter.notifyDataSetChanged();
                searchView.clearFocus();  // Oculta teclado
                return true;              // Consumimos el evento
            }
            @Override
            public boolean onQueryTextChange(String text) {
                // Filtrar en tiempo real al escribir
                filter(text);
                adapter.notifyDataSetChanged();
                return true;              // Consumimos el evento
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

    /** Filtra allUsers por username conteniendo `query` */
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

    /** Adapter interno usando la lista `filtered` */
    private class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
        private final List<User> list;
        UserAdapter(List<User> list) { this.list = list; }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup p, int i) {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.item_user, p, false);
            return new VH(v);
        }
        @Override public void onBindViewHolder(@NonNull VH h, int pos) {
            User u = list.get(pos);
            h.username.setText(u.getUsername());

            String pic = u.getProfile_picture();
            if (TextUtils.isEmpty(pic)) {
                h.avatar.setImageResource(R.drawable.login);
            } else if (pic.startsWith("http")) {
                Picasso.get().load(pic)
                        .placeholder(R.drawable.login).error(R.drawable.login)
                        .fit().centerCrop()
                        .into(h.avatar);
            } else {
                StorageReference ref = FirebaseStorage.getInstance()
                        .getReference(pic);
                ref.getDownloadUrl()
                        .addOnSuccessListener(uri -> Picasso.get().load(uri)
                                .placeholder(R.drawable.login)
                                .error(R.drawable.login)
                                .fit().centerCrop()
                                .into(h.avatar))
                        .addOnFailureListener(e -> h.avatar.setImageResource(R.drawable.login));
            }
        }
        @Override public int getItemCount() { return list.size(); }

        class VH extends RecyclerView.ViewHolder {
            final ImageView avatar;
            final TextView username;
            VH(View v) {
                super(v);
                avatar   = v.findViewById(R.id.profile_picture);
                username = v.findViewById(R.id.username);
            }
        }
    }
}
