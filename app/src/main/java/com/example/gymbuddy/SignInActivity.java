package com.example.gymbuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
//author : Mehmet Cihan Sakman

public class SignInActivity extends AppCompatActivity {

    //views
    EditText txtEmail;
    EditText txtPass;
    Button signInBtninSignIn;
    Button signUpBtninSignIn;
    TextView forgottenPass;

    //Text Input Layouts
    TextInputLayout email_til;
    TextInputLayout password_til;

    //Firebase
    private FirebaseAuth firebaseAuth;

    //Progress dialog
    ProgressDialog pd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);

        //init views
        txtEmail = findViewById(R.id.editTextEmailSignIn);
        txtPass = findViewById(R.id.editTextPasswordSignIn);
        signInBtninSignIn = findViewById(R.id.signInButtonSignIn);
        signUpBtninSignIn = findViewById(R.id.signUpButtonSignIn);
        forgottenPass = findViewById(R.id.alreadyHaveAccount);

        //Til init
        email_til = findViewById(R.id.til_email);
        password_til = findViewById(R.id.til_password);

        //Init progress dialog
        pd = new ProgressDialog(this);
        pd.setMessage("Logging in.....");


        //FireBase init
        firebaseAuth = FirebaseAuth.getInstance();

        //If user is already logged in we'll not ask for user info again.
        FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
        if(firebaseUser!=null){
            Intent intent = new Intent(SignInActivity.this, DashboardActivity.class);
            startActivity(intent);
            finish();
        }


        //Action Bar and it's title
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Sign In");


        signInBtninSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = txtEmail.getText().toString().trim();
                String password = txtPass.getText().toString().trim();

                if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()){
                    email_til.setError("Invalid email.");
                    txtEmail.setFocusable(true);
                }else if(password.length()<6){
                    password_til.setError("Password length should be at least 6 character.");
                    txtPass.setFocusable(true);
                }else{
                    //show progress dialog
                    pd.show();
                    firebaseAuth.signInWithEmailAndPassword(email,password).addOnSuccessListener(new OnSuccessListener<AuthResult>() {
                        @Override
                        //If everything is fine successfully logged in.
                        public void onSuccess(AuthResult authResult) {
                            //dismiss pd
                            pd.dismiss();
                            Intent intent = new Intent(SignInActivity.this, DashboardActivity.class);
                            Toast.makeText(SignInActivity.this,"Successful Login..",Toast.LENGTH_LONG).show();
                            startActivity(intent);
                            finish();
                        }
                        //If there is an error won't log in.
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            //dismiss pd
                            pd.dismiss();
                            Toast.makeText(SignInActivity.this,e.getLocalizedMessage().toString(),Toast.LENGTH_LONG).show();
                        }
                    });
                }

            }
        });

        signUpBtninSignIn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(SignInActivity.this, SignUpActivity.class);
                startActivity(intent);
                finish();

            }
        });

        forgottenPass.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
               showForgottenPassword();
            }
        });


    }

    private void showForgottenPassword() {
        //Alert Dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Forgotten Password");
        //set linear layout
        LinearLayout linearLayout = new LinearLayout(this);

        //views to set in dialog
        final EditText emailTxt = new EditText(this);
        emailTxt.setHint("Email");
        emailTxt.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        emailTxt.setMinEms(16);

        linearLayout.addView(emailTxt);
        linearLayout.setPadding(10,10,10,10);

        builder.setView(linearLayout);


        //buttons
        builder.setPositiveButton("Recover Password", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //input email
                String email = emailTxt.getText().toString().trim();
                beginRecovery(email);
            }
        });
        //buttons cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //dismiss dialog.
                pd.dismiss();
            }
        });

        //show dialog
        builder.create().show();


    }

    private void beginRecovery(String email) {
        pd.setMessage("Sending email....");
        pd.show();
        firebaseAuth.sendPasswordResetEmail(email).
                addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if(task.isSuccessful()){
                    pd.dismiss();
                    Toast.makeText(SignInActivity.this,"Email sent to your mail address!",Toast.LENGTH_LONG).show();
                }else{
                    pd.dismiss();
                    Toast.makeText(SignInActivity.this,"Failed!",Toast.LENGTH_LONG).show();
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(SignInActivity.this,""+e.getLocalizedMessage().toString(),Toast.LENGTH_LONG).show();
            }
        });



    }

}