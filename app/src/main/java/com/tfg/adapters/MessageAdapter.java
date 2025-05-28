package com.tfg.adapters;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.squareup.picasso.Picasso;
import com.tfg.R;
import com.tfg.models.Chat;

import java.util.List;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.VH> {

    public static final int MSG_TYPE_LEFT = 0;
    public static final int MSG_TYPE_RIGHT = 1;

    private Context mContext;
    private List<Chat> mChat;
    private String profile_picture;

    FirebaseUser firebaseUser;

    public MessageAdapter(Context mContext, List<Chat> mChat, String profile_picture) {
        this.mContext = mContext;
        this.mChat = mChat;
        this.profile_picture = profile_picture;
    }

    @NonNull
    @Override
    public MessageAdapter.VH onCreateViewHolder(@NonNull ViewGroup p, int i) {
        if (i == MSG_TYPE_RIGHT){
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.chat_item_right, p, false);
            return new MessageAdapter.VH(v);
        } else {
            View v = LayoutInflater.from(p.getContext())
                    .inflate(R.layout.chat_item_left, p, false);
            return new MessageAdapter.VH(v);
        }
    }

    @Override public void onBindViewHolder(@NonNull MessageAdapter.VH h, int pos) {

        Chat chat = mChat.get(pos);

        h.show_message.setText(chat.getMessage());

        String pic = profile_picture;
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

        if (pos == mChat.size()-1){
            if (chat.isIsseen()){
                h.txt_seen.setText(R.string.seen_message);
            } else {
                h.txt_seen.setText(R.string.delivered_message);
            }
        } else {
            h.txt_seen.setVisibility(View.GONE);
        }

    }

    @Override public int getItemCount() { return mChat.size(); }

    public class VH extends RecyclerView.ViewHolder {
        public ImageView avatar;
        public TextView show_message;
        public TextView txt_seen;

        VH(View v) {
            super(v);

            avatar   = v.findViewById(R.id.profile_picture);
            show_message = v.findViewById(R.id.show_message);
            txt_seen = v.findViewById(R.id.txt_seen);
        }
    }

    @Override
    public int getItemViewType(int position){
        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (mChat.get(position).getSender().equals(firebaseUser.getUid())){
            return MSG_TYPE_RIGHT;
        } else {
            return MSG_TYPE_LEFT;
        }
    }

}
