package com.tfg.option;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.tfg.R;
import com.tfg.models.Publication;
import java.io.File;
import java.io.IOException;

public class CreatePublicationActivity extends AppCompatActivity {
    private static final int REQ_GAL = 100, REQ_CAM = 200, REQ_PERM = 300;
    private EditText content;
    private Button btnGal, btnCam, btnPub;
    private ImageView imgPrev;
    private Uri imageUri;
    private String camPath;
    private DatabaseReference pubsRef;
    private StorageReference imgRef;
    private FirebaseUser user;

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        setContentView(R.layout.activity_create_publication);
        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setTitle("Post");
            ab.setDisplayShowHomeEnabled(true);
            ab.setDisplayHomeAsUpEnabled(true);
        }

        user    = FirebaseAuth.getInstance().getCurrentUser();
        pubsRef = FirebaseDatabase.getInstance().getReference("publications");
        imgRef  = FirebaseStorage.getInstance().getReference("postImages");

        content = findViewById(R.id.editContent);
        btnGal  = findViewById(R.id.btnChooseImage);
        btnCam  = findViewById(R.id.btnTakePhoto);
        btnPub  = findViewById(R.id.btnPublish);
        imgPrev = findViewById(R.id.imgPreview);

        btnGal.setOnClickListener(v -> openGallery());
        btnCam.setOnClickListener(v -> checkPermAndOpenCam());
        btnPub.setOnClickListener(v -> publish());
    }

    private void openGallery() {
        startActivityForResult(new Intent(Intent.ACTION_GET_CONTENT).setType("image/*"), REQ_GAL);
    }

    private void checkPermAndOpenCam() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.CAMERA },
                    REQ_PERM
            );
        } else {
            openCamera();
        }
    }


    private void openCamera() {
        try {
            File f = File.createTempFile("IMG_" + System.currentTimeMillis(), ".jpg", getExternalCacheDir());
            camPath = f.getAbsolutePath();
            Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", f);
            startActivityForResult(new Intent(MediaStore.ACTION_IMAGE_CAPTURE).putExtra(MediaStore.EXTRA_OUTPUT, uri), REQ_CAM);
        } catch (IOException e) { e.printStackTrace(); }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQ_PERM) {
            if (grantResults.length>0 &&
                    grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this,
                        "Camera permission denied",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }


    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        super.onActivityResult(req, res, data);
        if (res == RESULT_OK && ((req == REQ_GAL && data != null && data.getData() != null) || req == REQ_CAM)) {
            imageUri = (req == REQ_GAL ? data.getData() : Uri.fromFile(new File(camPath)));
            imgPrev.setImageURI(imageUri);
            imgPrev.setVisibility(ImageView.VISIBLE);
        }
    }


    private String getExt(Uri uri) {
        ContentResolver cr = getContentResolver();
        return MimeTypeMap.getSingleton().getExtensionFromMimeType(cr.getType(uri));
    }

    private void publish() {
        String t = content.getText().toString().trim();
        if (t.isEmpty() && imageUri == null) {
            Toast.makeText(this, "Write something or choose or take a photo", Toast.LENGTH_SHORT).show();
            return;
        }
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Publishing...");
        pd.show();
        long ts = System.currentTimeMillis();
        String id = pubsRef.push().getKey();
        if (imageUri != null) {
            StorageReference ref = imgRef.child(ts + "." + getExt(imageUri));
            ref.putFile(imageUri)
                    .continueWithTask((Continuation<UploadTask.TaskSnapshot, Task<Uri>>) task -> {
                        if (!task.isSuccessful()) throw task.getException();
                        return ref.getDownloadUrl();
                    })
                    .addOnCompleteListener(task -> {
                        pd.dismiss();
                        if (task.isSuccessful() && task.getResult() != null)
                            savePublication(id, t, task.getResult().toString(), ts, pd);
                        else Toast.makeText(this, "Image upload failed", Toast.LENGTH_SHORT).show();
                    });
        } else {
            savePublication(id, t, null, ts, pd);
        }
    }

    private void savePublication(String id, String t, String img, long ts, ProgressDialog pd) {
        pubsRef.child(id).setValue(new Publication(id, user.getUid(), t, img, ts))
                .addOnCompleteListener(task -> {
                    pd.dismiss();
                    Toast.makeText(this, task.isSuccessful() ? "Published!" : "Publish failed", Toast.LENGTH_SHORT).show();
                    if (task.isSuccessful()) finish();
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return super.onSupportNavigateUp();
    }
}
