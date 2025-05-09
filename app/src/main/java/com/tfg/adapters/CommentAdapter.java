package com.tfg.adapters;

import android.content.Context;
import android.widget.TextView;
import android.widget.Toast;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.tfg.R;
import com.tfg.models.Comment;
import com.google.firebase.database.*;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.VH> {
    private final Context       ctx;
    private final List<Comment> comments;

    public CommentAdapter(Context ctx, List<Comment> comments) {
        this.ctx      = ctx;
        this.comments = comments;
    }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(ctx)
                .inflate(R.layout.item_comment, parent, false);
        return new VH(v);
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Comment c = comments.get(pos);

        // username dinámico
        h.txtAuthor.setText("Loading…");
        if (c.getAuthorUid() != null && !c.getAuthorUid().isEmpty()) {
            FirebaseDatabase.getInstance()
                    .getReference("users")
                    .child(c.getAuthorUid())
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

        h.txtText.setText(c.getText());

        String t = new SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
                .format(new Date(c.getTimestamp()));
        h.txtTime.setText(t);
    }

    @Override public int getItemCount() {
        return comments.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final TextView txtAuthor, txtText, txtTime;
        VH(@NonNull View v) {
            super(v);
            txtAuthor = v.findViewById(R.id.txtCommentAuthor);
            txtText   = v.findViewById(R.id.txtCommentText);
            txtTime   = v.findViewById(R.id.txtCommentTime);
        }
    }
}
