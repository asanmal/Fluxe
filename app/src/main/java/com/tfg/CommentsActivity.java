package com.tfg;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.tfg.adapters.CommentAdapter;
import com.tfg.models.Comment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class CommentsActivity extends AppCompatActivity {
    private TextView            tvCommentsHeader;
    private RecyclerView        rv;
    private EditText            editComment;
    private Button              btnPost;
    private CommentAdapter      adapter;
    private List<Comment>       comments = new ArrayList<>();
    private DatabaseReference   commentsRef;
    private FirebaseUser        user;
    private String              postId;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_comments);

        // ActionBar
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("Comments");
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        tvCommentsHeader = findViewById(R.id.tvCommentsHeader);
        rv          = findViewById(R.id.rvComments);
        editComment = findViewById(R.id.editComment);
        btnPost     = findViewById(R.id.btnPostComment);

        user    = FirebaseAuth.getInstance().getCurrentUser();
        postId  = getIntent().getStringExtra("postId");
        commentsRef = FirebaseDatabase.getInstance()
                .getReference("comments")
                .child(postId);

        adapter = new CommentAdapter(this, comments);
        rv.setLayoutManager(new LinearLayoutManager(this));
        rv.setAdapter(adapter);

        // load & count comments
        commentsRef.orderByChild("timestamp")
                .addValueEventListener(new ValueEventListener() {
                    @Override public void onDataChange(@NonNull DataSnapshot snap) {
                        comments.clear();
                        for (DataSnapshot c : snap.getChildren()) {
                            Comment cm = c.getValue(Comment.class);
                            if (cm != null) {
                                cm.setId(c.getKey());
                                comments.add(cm);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        tvCommentsHeader.setText(
                                getString(R.string.comment_count, comments.size())
                        );
                    }
                    @Override public void onCancelled(@NonNull DatabaseError e) {}
                });

        // post
        btnPost.setOnClickListener(v -> {
            String text = editComment.getText().toString().trim();
            if (TextUtils.isEmpty(text)) return;
            String id = commentsRef.push().getKey();
            long ts    = System.currentTimeMillis();
            Comment cm = new Comment(id, user.getUid(), text, ts);
            commentsRef.child(id).setValue(cm);
            editComment.setText("");
        });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
