package com.tfg.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tfg.CommentsActivity;
import com.tfg.R;
import com.tfg.models.Publication;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PublicationAdapter extends RecyclerView.Adapter<PublicationAdapter.ViewHolder> {
    private final Context           ctx;
    private final List<Publication> pubs;
    private final String            currentUid;
    private final DatabaseReference baseRef;

    public PublicationAdapter(Context ctx, List<Publication> pubs) {
        this.ctx        = ctx;
        this.pubs       = pubs;
        FirebaseUser u  = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUid = u != null ? u.getUid() : "";
        this.baseRef    = FirebaseDatabase.getInstance()
                .getReference("publications");
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

        // Username dinámico
        h.txtAuthor.setText("Loading…");
        if (p.getAuthorUid() != null && !p.getAuthorUid().isEmpty()) {
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(p.getAuthorUid())
                    .child("username")
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot snap) {
                            String name = snap.getValue(String.class);
                            h.txtAuthor.setText(name != null ? name : "Anonymous");
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {
                            h.txtAuthor.setText("Anonymous");
                        }
                    });
        }

        // Timestamp
        String time = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(p.getTimestamp()));
        h.txtTime.setText(time);

        // Imagen
        if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
            h.imgPost.setVisibility(View.VISIBLE);
            Picasso.get()
                    .load(p.getImageUrl())
                    .placeholder(R.drawable.login)
                    .into(h.imgPost);
        } else {
            h.imgPost.setVisibility(View.GONE);
        }

        // Contenido
        h.txtContent.setText(p.getContent());

        // Likes
        DatabaseReference likesRef = baseRef.child(p.getId()).child("likes");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                h.tvLikeCount.setText(String.valueOf(snap.getChildrenCount()));
                boolean liked = snap.hasChild(currentUid);
                h.ivLike.setImageResource(
                        liked ? R.drawable.ic_heart_filled
                                : R.drawable.ic_heart_outline
                );
            }
            @Override public void onCancelled(@NonNull DatabaseError e) {
                Toast.makeText(ctx, "Couldn't load likes", Toast.LENGTH_SHORT).show();
            }
        });
        h.ivLike.setOnClickListener(v ->
                likesRef.runTransaction(new Transaction.Handler() {
                    @NonNull @Override
                    public Transaction.Result doTransaction(@NonNull MutableData md) {
                        if (md.hasChild(currentUid)) md.child(currentUid).setValue(null);
                        else                           md.child(currentUid).setValue(true);
                        return Transaction.success(md);
                    }
                    @Override
                    public void onComplete(DatabaseError err, boolean committed, DataSnapshot d) {
                        if (err != null)
                            Toast.makeText(ctx, "Like failed", Toast.LENGTH_SHORT).show();
                    }
                })
        );

        // Comentarios & contador
        DatabaseReference commentsRef = FirebaseDatabase.getInstance()
                .getReference("comments")
                .child(p.getId());
        commentsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override public void onDataChange(@NonNull DataSnapshot snap) {
                h.tvCommentCount.setText(String.valueOf(snap.getChildrenCount()));
            }
            @Override public void onCancelled(@NonNull DatabaseError e) { }
        });
        h.ivComment.setOnClickListener(v -> {
            Intent i = new Intent(ctx, CommentsActivity.class);
            i.putExtra("postId", p.getId());
            ctx.startActivity(i);
        });
    }

    @Override public int getItemCount() {
        return pubs.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        final TextView  txtAuthor, txtTime, txtContent, tvLikeCount, tvCommentCount;
        final ImageView imgPost, ivLike, ivComment;

        ViewHolder(@NonNull View v) {
            super(v);
            txtAuthor      = v.findViewById(R.id.txtAuthor);
            txtTime        = v.findViewById(R.id.txtTime);
            imgPost        = v.findViewById(R.id.imgPost);
            txtContent     = v.findViewById(R.id.txtContent);
            ivLike         = v.findViewById(R.id.ivLike);
            tvLikeCount    = v.findViewById(R.id.tvLikeCount);
            ivComment      = v.findViewById(R.id.ivComment);
            tvCommentCount = v.findViewById(R.id.tvCommentCount);
        }
    }
}
