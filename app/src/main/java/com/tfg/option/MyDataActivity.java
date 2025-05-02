package com.tfg.option;

import android.graphics.Typeface;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;
import com.tfg.R;

public class MyDataActivity extends AppCompatActivity {

    ImageView dataImg;
    TextView dataUsername, dataFirstName, dataLastName, dataSecondName, dataEmail, dataPwd;
    TextView dataTxt, dataUsernameTxt, dataFirstNameTxt, dataLastNameTxt, dataSecondNameTxt, dataEmailTxt, dataPwdTxt;
    Button updateBtn, updatePwdBtn;

    FirebaseAuth firebaseAuth;
    FirebaseUser user;

    DatabaseReference DATABASE;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_data);

        ActionBar actionBar = getSupportActionBar();
        assert  actionBar!=null;
        actionBar.setTitle("My Data");
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
        dataPwd = findViewById(R.id.dataPwd);
        updateBtn = findViewById(R.id.updateBtn);
        updatePwdBtn = findViewById(R.id.updatePwdBtn);

        //Datos del textView
        dataUsernameTxt = findViewById(R.id.dataUsernameTxt);
        dataFirstNameTxt = findViewById(R.id.dataFirstNameTxt);
        dataLastNameTxt = findViewById(R.id.dataLastNameTxt);
        dataSecondNameTxt = findViewById(R.id.dataSecondNameTxt);
        dataEmailTxt = findViewById(R.id.dataEmailTxt);
        dataPwdTxt = findViewById(R.id.dataPwdTxt);

        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();

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
                    String password = "" + snapshot.child("password").getValue();
                    String profilePicture = "" + snapshot.child("profile_picture").getValue();

                    //Seteamos los datos en los textview
                    dataUsername.setText(username);
                    dataFirstName.setText(firstName);
                    dataLastName.setText(lastName);
                    dataSecondName.setText(secondName);
                    dataEmail.setText(email);
                    dataPwd.setText(password);

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
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

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
        dataPwdTxt.setTypeface(tf);

        dataUsername.setTypeface(tf);
        dataFirstName.setTypeface(tf);
        dataLastName.setTypeface(tf);
        dataSecondName.setTypeface(tf);
        dataEmail.setTypeface(tf);
        dataPwd.setTypeface(tf);
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