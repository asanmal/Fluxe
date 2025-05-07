package com.tfg.adapters;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.tfg.MessageActivity;
import com.tfg.R;
import com.tfg.models.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {
    private final List<User> list;
    public UserAdapter(List<User> list) { this.list = list; }

    @NonNull
    @Override
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

        h.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(v.getContext(), MessageActivity.class);
                intent.putExtra("id", u.getId());
                v.getContext().startActivity(intent);
            }
        });
    }
    @Override public int getItemCount() { return list.size(); }

    public class VH extends RecyclerView.ViewHolder {
        final ImageView avatar;
        final TextView username;
        VH(View v) {
            super(v);
            avatar   = v.findViewById(R.id.profile_picture);
            username = v.findViewById(R.id.username);
        }
    }
}
