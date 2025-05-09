package com.tfg.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;
import com.tfg.R;
import com.tfg.models.Publication;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PublicationAdapter
        extends RecyclerView.Adapter<PublicationAdapter.ViewHolder> {

    private final Context ctx;
    private final List<Publication> pubs;

    public PublicationAdapter(Context ctx, List<Publication> pubs) {
        this.ctx  = ctx;
        this.pubs = pubs;
    }

    @NonNull @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx)
                .inflate(R.layout.item_publication, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder h, int pos) {
        Publication p = pubs.get(pos);

        // 1) Ponemos un texto provisional mientras carga:
        h.txtAuthor.setText("Loading…");

        // 2) Consulta Firebase para obtener el username
        String uid = p.getAuthorUid();
        if (uid != null && !uid.trim().isEmpty()) {
            DatabaseReference userRef = FirebaseDatabase
                    .getInstance()
                    .getReference("users")
                    .child(uid)
                    .child("username");
            userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snap) {
                    String name = snap.getValue(String.class);
                    h.txtAuthor.setText(name != null ? name : "Anonymous");
                }
                @Override
                public void onCancelled(@NonNull DatabaseError err) {
                    h.txtAuthor.setText("Anonymous");
                }
            });
        } else {
            h.txtAuthor.setText("Anonymous");
        }

        // 3) Fecha
        String time = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(p.getTimestamp()));
        h.txtTime.setText(time);

        // 4) Imagen si existe
        if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
            h.imgPost.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(p.getImageUrl())
                    .placeholder(R.drawable.login)
                    .into(h.imgPost);
        } else {
            h.imgPost.setVisibility(View.GONE);
        }

        // 5) Contenido
        h.txtContent.setText(p.getContent());
    }

    @Override public int getItemCount() {
        return pubs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView  txtAuthor, txtTime, txtContent;
        final ImageView imgPost;

        ViewHolder(@NonNull View v) {
            super(v);
            txtAuthor  = v.findViewById(R.id.txtAuthor);
            txtTime    = v.findViewById(R.id.txtTime);
            txtContent = v.findViewById(R.id.txtContent);
            imgPost    = v.findViewById(R.id.imgPost);
        }
    }
}
