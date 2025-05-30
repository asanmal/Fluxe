package com.tfg.adapters;

import android.content.Intent;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.tfg.option.MessageActivity;
import com.tfg.R;
import com.tfg.models.Chat;
import com.tfg.models.User;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.VH> {

    private final List<User> list;
    private boolean isChat;
    String theLastMessage;

    //Tabla de seguidores
    private FirebaseUser me = FirebaseAuth.getInstance().getCurrentUser();
    private DatabaseReference followRef =
            FirebaseDatabase.getInstance()
                    .getReference("followers");

    public UserAdapter(List<User> list, boolean isChat) { this.list = list; this.isChat = isChat;}

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

        if (isChat){
            lastMessage(u.getId(), h.last_msg);
        } else {
            h.last_msg.setVisibility(View.GONE);
        }

        if (isChat){
            if (u.getStatus().equals("online")){
                h.img_on.setVisibility(View.VISIBLE);
                h.img_off.setVisibility(View.GONE);
            } else {
                h.img_on.setVisibility(View.GONE);
                h.img_off.setVisibility(View.VISIBLE);
            }
        } else {
            h.img_on.setVisibility(View.GONE);
            h.img_off.setVisibility(View.GONE);
        }

        // comprobar si ya sigo a u.getId()
        followRef.child(me.getUid()).child("following")
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot ds) {
                        boolean following = ds.hasChild(u.getId());
                        h.btnFollow.setText(
                                following
                                        ? h.itemView.getContext().getString(R.string.unfollow)
                                        : h.itemView.getContext().getString(R.string.follow)
                        );
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // manejar click follow/unfollow
        h.btnFollow.setOnClickListener(v -> {
            DatabaseReference myFollowing =
                    followRef.child(me.getUid()).child("following");
            DatabaseReference theirFollowers =
                    followRef.child(u.getId()).child("followers");
            myFollowing.child(u.getId()).addListenerForSingleValueEvent(
                    new ValueEventListener() {
                        @Override public void onDataChange(@NonNull DataSnapshot ds) {
                            if (ds.exists()) {
                                // ya sigo y dejo de seguir
                                myFollowing.child(u.getId()).removeValue();
                                theirFollowers.child(me.getUid()).removeValue();
                                h.btnFollow.setText(
                                        v.getContext().getString(R.string.follow)
                                );
                            } else {
                                // no sigo y comienzo a seguir
                                myFollowing.child(u.getId()).setValue(true);
                                theirFollowers.child(me.getUid()).setValue(true);
                                h.btnFollow.setText(
                                        v.getContext().getString(R.string.unfollow)
                                );
                            }
                        }
                        @Override public void onCancelled(@NonNull DatabaseError e) {}
                    });
        });

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
        public ImageView avatar;
        public TextView username;
        private ImageView img_on;
        private ImageView img_off;
        private TextView last_msg;
        public Button btnFollow;

        VH(View v) {
            super(v);
            avatar   = v.findViewById(R.id.profile_picture);
            username = v.findViewById(R.id.username);
            img_on = v.findViewById(R.id.img_on);
            img_off = v.findViewById(R.id.img_off);
            last_msg = v.findViewById(R.id.last_msg);
            btnFollow = v.findViewById(R.id.btnFollow);
        }
    }

    //Comprobar el ultimo mensaje
    private void lastMessage(String userId, TextView last_msg){
        theLastMessage = "default";
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("chats");

        reference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                for (DataSnapshot snapshot : dataSnapshot.getChildren()){
                    Chat chat = snapshot.getValue(Chat.class);
                    if (chat.getReceiver().equals(firebaseUser.getUid()) && chat.getSender().equals(userId) ||
                        chat.getReceiver().equals(userId) && chat.getSender().equals(firebaseUser.getUid())){
                        theLastMessage = chat.getMessage();
                    }
                }

                switch (theLastMessage){
                    case "default":
                        last_msg.setText("No Message");
                        break;
                    default:
                        last_msg.setText(theLastMessage);
                        break;
                }
                theLastMessage = "default";
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
