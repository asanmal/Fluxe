package com.tfg.option;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.MimeTypeMap;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.StorageTask;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;
import com.tfg.R;
import com.tfg.change.ChangePasswordActivity;
import com.tfg.change.EditDataActivity;

import java.util.HashMap;

public class MyDataActivity extends AppCompatActivity {

    ImageView dataImg;
    TextView dataUsername, dataFirstName, dataLastName, dataSecondName, dataEmail;
    TextView dataTxt, dataUsernameTxt, dataFirstNameTxt, dataLastNameTxt, dataSecondNameTxt, dataEmailTxt;
    Button updateBtn, updatePwdBtn;

    FirebaseAuth firebaseAuth;
    FirebaseUser user;

    DatabaseReference DATABASE;

    StorageReference storageReference;
    private static final int IMAGE_REQUEST = 1;
    private Uri imageUri;
    private StorageTask uploadTask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_data);

        ActionBar actionBar = getSupportActionBar();
        assert  actionBar!=null;
        actionBar.setTitle(R.string.my_data);
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //Datos activity
        dataTxt = findViewById(R.id.dataTxt);
        dataImg = findViewById(R.id.dataImg);
        dataUsername = findViewById(R.id.dataUsername);
        dataFirstName = findViewById(R.id.dataFirstName);
        dataLastName = findViewById(R.id.dataLastName);
        dataSecondName = findViewById(R.id.dataSecondName);
        dataEmail = findViewById(R.id.dataEmail);
        updateBtn = findViewById(R.id.updateBtn);
        updatePwdBtn = findViewById(R.id.updatePwdBtn);

        //Datos del textView
        dataUsernameTxt = findViewById(R.id.dataUsernameTxt);
        dataFirstNameTxt = findViewById(R.id.dataFirstNameTxt);
        dataLastNameTxt = findViewById(R.id.dataLastNameTxt);
        dataSecondNameTxt = findViewById(R.id.dataSecondNameTxt);
        dataEmailTxt = findViewById(R.id.dataEmailTxt);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

        //Datos de storage para cambiar la imagen
        storageReference = FirebaseStorage.getInstance().getReference("uploads");

        DATABASE = FirebaseDatabase.getInstance().getReference("users");

        changeFont();

        /*Obtener los datos del usuario*/
        DATABASE.child(user.getUid()).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //Si el usuario existe
                if (snapshot.exists()){

                    //Obtenemos los valores de nuestra base de datos
                    String username = "" + snapshot.child("username").getValue();
                    String firstName = "" + snapshot.child("firstName").getValue();
                    String lastName = "" + snapshot.child("lastName").getValue();
                    String secondName = "" + snapshot.child("secondName").getValue();
                    String email = "" + snapshot.child("email").getValue();
                    String profilePicture = "" + snapshot.child("profile_picture").getValue();

                    //Seteamos los datos en los textview
                    dataUsername.setText(username);
                    dataFirstName.setText(firstName);
                    dataLastName.setText(lastName);
                    dataSecondName.setText(secondName);
                    dataEmail.setText(email);

                    //Setear los datos de la imagen
                    try {
                        Picasso.get()
                                .load(profilePicture)
                                .placeholder(R.drawable.login)
                                .error(R.drawable.login)
                                .into(dataImg);
                    } catch (Exception e){
                        //Si no existe se pone por defecto
                        Picasso.get().load(R.drawable.login).into(dataImg);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) { }
        });

        updatePwdBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Mandarnos a la pantalla de cambio de contraseña
                startActivity(new Intent(MyDataActivity.this, ChangePasswordActivity.class));
            }
        });

        updateBtn.setOnClickListener(v -> {
            // Lanza la pantalla de edición de datos
            Intent intent = new Intent(MyDataActivity.this, EditDataActivity.class);
            startActivity(intent);
        });

        //Metodo para el evento de la imagen
        dataImg.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openImage();
            }
        });
    }

    private void openImage(){
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(intent, IMAGE_REQUEST);
    }

    private String getFileExtension(Uri uri){
        ContentResolver contentResolver = getContentResolver();
        MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
        return mimeTypeMap.getExtensionFromMimeType(contentResolver.getType(uri));
    }

    private void uploadImage(){
        final ProgressDialog pd = new ProgressDialog(MyDataActivity.this);
        pd.setMessage(getString(R.string.pub_uploading));
        pd.show();

        if (imageUri != null){
            String extension = getFileExtension(imageUri);
            String fileName = System.currentTimeMillis() + "." + extension;
            final StorageReference fileReference = storageReference.child(fileName);

            uploadTask = fileReference.putFile(imageUri);
            uploadTask.continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>(){
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()){
                        throw task.getException();
                    }
                    return fileReference.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    pd.dismiss();
                    if (task.isSuccessful()){
                        Uri downloadUri = task.getResult();
                        String mUri = downloadUri.toString();

                        DATABASE = FirebaseDatabase.getInstance().getReference("users").child(user.getUid());
                        HashMap<String, Object> map = new HashMap<>();
                        map.put("profile_picture", mUri);
                        DATABASE.updateChildren(map);
                    } else {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.toast_failed), Toast.LENGTH_SHORT).show();
                    }
                }
            }).addOnFailureListener(new OnFailureListener() {
                @Override
                public void onFailure(@NonNull Exception e) {
                    pd.dismiss();
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.toast_error_fmt, e.getMessage()),
                            Toast.LENGTH_SHORT).show();
                }
            });
        } else {
            Toast.makeText(getApplicationContext(),
                    getString(R.string.toast_no_image_selected),
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data){
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_REQUEST && resultCode == RESULT_OK && data!=null && data.getData()!=null){
            imageUri = data.getData();

            if (uploadTask != null && uploadTask.isInProgress()){
                Toast.makeText(getApplicationContext(),
                        getString(R.string.toast_upload_in_progress),
                        Toast.LENGTH_SHORT).show();
            } else {
                uploadImage();
            }
        }
    }

    //Metodo para cambiar la fuente a las letras
    private void changeFont(){
        //Fuente de letra
        String locate = "fuente/sans_ligera.ttf";
        Typeface tf = Typeface.createFromAsset(MyDataActivity.this.getAssets(), locate);

        dataTxt.setTypeface(tf);
        dataUsernameTxt.setTypeface(tf);
        dataFirstNameTxt.setTypeface(tf);
        dataLastNameTxt.setTypeface(tf);
        dataSecondNameTxt.setTypeface(tf);
        dataEmailTxt.setTypeface(tf);

        dataUsername.setTypeface(tf);
        dataFirstName.setTypeface(tf);
        dataLastName.setTypeface(tf);
        dataSecondName.setTypeface(tf);
        dataEmail.setTypeface(tf);
        updateBtn.setTypeface(tf);
        updatePwdBtn.setTypeface(tf);
    }

    //Accion de retroceso
    @Override
    public boolean onSupportNavigateUp(){
        finish();
        return super.onSupportNavigateUp();
    }
}
