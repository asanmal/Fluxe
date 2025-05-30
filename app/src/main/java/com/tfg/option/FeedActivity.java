package com.tfg.option;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.tfg.R;
import com.tfg.adapters.PublicationAdapter;
import com.tfg.models.Publication;
import java.util.ArrayList;
import java.util.List;

public class FeedActivity extends AppCompatActivity {
    private RecyclerView        rv;
    private PublicationAdapter  adapter;
    private List<Publication>   pubs   = new ArrayList<>();
    private DatabaseReference   pubsRef;

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        setContentView(R.layout.activity_feed);

        // ActionBar setup
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle(getString(R.string.post));
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        // RecyclerView + adapter
        rv = findViewById(R.id.recyclerView);
        rv.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PublicationAdapter(this, pubs);
        rv.setAdapter(adapter);

        // Listen for publications
        pubsRef = FirebaseDatabase.getInstance()
                .getReference("publications");
        pubsRef.orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        pubs.clear();
                        for (DataSnapshot child : snapshot.getChildren()) {
                            Publication p = child.getValue(Publication.class);
                            if (p != null) {
                                // asignar el key de Firebase como id
                                p.setId(child.getKey());
                                pubs.add(0, p);
                            }
                        }
                        adapter.notifyDataSetChanged();
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
