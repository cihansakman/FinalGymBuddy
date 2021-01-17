package com.example.gymbuddy;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;

public class AddPostActivity extends AppCompatActivity {
    ActionBar actionBar;
    FirebaseAuth firebaseAuth;
    DatabaseReference userDbRef;

    //permissions constants
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    //image pick constants
    private static final int IMAGE_PICK_CAMERA_CODE= 300;
    private static final int IMAGE_PICK_GALLERY_CODE= 400;

    //permissions array
    String[] cameraPermissons ;
    String[] storagePermissons;

    //views
    EditText titleEt,descriptionEt;
    ImageView imageIv;
    Button uploadBtn;

    //user infos
    String name,email,uid,dp;

    //information of post which will be edited
    String editTitle, editDescription, editImage;

    //progress bar
    ProgressDialog pd;


    //image picked will be samed in this uri
    Uri image_rui=null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_post);


        actionBar = getSupportActionBar();
        actionBar.setTitle("Add New Post");
        //enable back button in actionbar
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setDisplayShowHomeEnabled(true);

        //init permissions arrays
        cameraPermissons = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissons= new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};

        pd = new ProgressDialog(this);

        firebaseAuth = FirebaseAuth.getInstance();
        checkUserStatus();

        //init views
        titleEt = findViewById(R.id.pTitleEt);
        descriptionEt = findViewById(R.id.pDescriptionEt);
        imageIv = findViewById(R.id.pImageIv);
        uploadBtn = findViewById(R.id.pUploadBtn);

        //get data through intent from previous activities' adapter
        Intent intent = getIntent();
        final String isUpdateKey = ""+intent.getStringExtra("key");
        final String editPostId = ""+intent.getStringExtra("editPostId");

        //validate if we came here to update came from AdapterPost
        if(isUpdateKey.equals("editPost")){
            //update
            actionBar.setTitle("Update Post");
            uploadBtn.setText("Update");
            loadPostData(editPostId);


        }else{
            //add
            actionBar.setTitle("Add New Post");
            uploadBtn.setText("Upload");
        }





        //necesarry info about user
        userDbRef = FirebaseDatabase.getInstance().getReference("UserInfos");
        Query query = userDbRef.orderByChild("email").equalTo(email);
        query.addValueEventListener(new ValueEventListener(){
        @Override
        public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
            for(DataSnapshot ds:dataSnapshot.getChildren()){
                name = ""+ ds.child("name").getValue();
                email = ""+ ds.child("email").getValue();
                dp = ""+ ds.child("profilepic").getValue();
            }
        }

        @Override
        public void onCancelled(@NonNull DatabaseError error) {

        }
    });

        actionBar.setSubtitle(email);




        //get image from camera/gallery on click
        imageIv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //show image pick dialog
                showImagePickDialog();
            }
        });

        //Button click listener
        uploadBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //get data(title, description) from EditTexts
                String title = titleEt.getText().toString();
                String description = descriptionEt.getText().toString();
                if(TextUtils.isEmpty(title)){
                    Toast.makeText(AddPostActivity.this,"Enter title...",Toast.LENGTH_SHORT).show();
                    return;
                }
                if(TextUtils.isEmpty(description)){
                    Toast.makeText(AddPostActivity.this,"Enter description...",Toast.LENGTH_SHORT).show();
                    return;
                }

                if(isUpdateKey.equals("editPost")){
                    beginUpdate(title, description, editPostId);

                }else{
                    uploadData(title,description);
                }

            }
        });
    }

    private void beginUpdate(String title, String description, String editPostId) {

    pd.setMessage("Updating Post....");
    pd.show();

    if(!editImage.equals("noImage")){
        //was with image
        updateWasWithImage(title, description, editPostId);

    }else if(imageIv.getDrawable() != null){
        //was without image, but now has image in imageView
        updateWithNowImage(title, description, editPostId);

    }else{
        //was without image, still no image
        updateWithoutImage(title, description, editPostId);
    }


    }

    private void updateWithoutImage(String title, String description, String editPostId) {
        HashMap<String, Object> hashMap = new HashMap<>();
        hashMap.put("uid",uid);
        hashMap.put("uName",name);
        hashMap.put("pLikes","0");
        hashMap.put("pComments","0");
        hashMap.put("uEmail",email);
        hashMap.put("uDp",dp);
        hashMap.put("pTitle",title);
        hashMap.put("pDescr",description);
        hashMap.put("pImage","noImage");

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        ref.child(editPostId).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this,"Updated",Toast.LENGTH_LONG).show();

            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this,""+e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void updateWithNowImage(final String title, final String description, final String editPostId) {
        String timeStamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/"+"post_"+timeStamp;

        //get image from imageView
        Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        //image compress
        bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
        byte[] data = baos.toByteArray();

        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
        ref.putBytes(data)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        //image uploaded get its url
                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        while(!uriTask.isSuccessful());

                        String downloadUri = uriTask.getResult().toString();

                        if(uriTask.isSuccessful()){
                            //url is recieved, upload to fb
                            HashMap<String, Object> hashMap = new HashMap<>();
                            hashMap.put("uid",uid);
                            hashMap.put("uName",name);
                            hashMap.put("pLikes","0");
                            hashMap.put("pComments","0");
                            hashMap.put("uEmail",email);
                            hashMap.put("uDp",dp);
                            hashMap.put("pTitle",title);
                            hashMap.put("pDescr",description);
                            hashMap.put("pImage",downloadUri);

                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                            ref.child(editPostId).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    pd.dismiss();
                                    Toast.makeText(AddPostActivity.this,"Updated",Toast.LENGTH_LONG).show();

                                }
                            }).addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    pd.dismiss();
                                    Toast.makeText(AddPostActivity.this,""+e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();

                                }
                            });
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //image not uploaded
                pd.dismiss();
                Toast.makeText(AddPostActivity.this,""+e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void updateWasWithImage(final String title, final String description, final String editPostId) {
        //delete previous image first
        StorageReference mPictureRef = FirebaseStorage.getInstance().getReferenceFromUrl(editImage);
        mPictureRef.delete()
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //image deleted, upload new image
                        String timeStamp = String.valueOf(System.currentTimeMillis());
                        String filePathAndName = "Posts/"+"post_"+timeStamp;

                        //get image from imageView
                        Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        //image compress
                        bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
                        byte[] data = baos.toByteArray();

                        StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
                        ref.putBytes(data)
                                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                                    @Override
                                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                                        //image uploaded get its url
                                        Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                                        while(!uriTask.isSuccessful());

                                        String downloadUri = uriTask.getResult().toString();

                                        if(uriTask.isSuccessful()){
                                            //url is recieved, upload to fb
                                            HashMap<String, Object> hashMap = new HashMap<>();
                                            hashMap.put("uid",uid);
                                            hashMap.put("uName",name);
                                            hashMap.put("pLikes","0");
                                            hashMap.put("pComments","0");
                                            hashMap.put("uEmail",email);
                                            hashMap.put("uDp",dp);
                                            hashMap.put("pTitle",title);
                                            hashMap.put("pDescr",description);
                                            hashMap.put("pImage",downloadUri);

                                            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                            ref.child(editPostId).updateChildren(hashMap).addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    pd.dismiss();
                                                    Toast.makeText(AddPostActivity.this,"Updated",Toast.LENGTH_LONG).show();


                                                }
                                            }).addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    pd.dismiss();
                                                    Toast.makeText(AddPostActivity.this,""+e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();

                                                }
                                            });
                                        }
                                    }
                                }).addOnFailureListener(new OnFailureListener() {
                            @Override
                            public void onFailure(@NonNull Exception e) {
                                //image not uploaded
                                pd.dismiss();
                                Toast.makeText(AddPostActivity.this,""+e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
                            }
                        });

                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                pd.dismiss();
                Toast.makeText(AddPostActivity.this,""+e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();
            }
        });

    }

    private void loadPostData(String editPostId) {
        DatabaseReference reference = FirebaseDatabase.getInstance().getReference("Posts");
        //get detail of post using id of post
        Query fquery = reference.orderByChild("pId").equalTo(editPostId);
        fquery.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    editTitle = ""+ds.child("pTitle").getValue();
                    editDescription = ""+ds.child("pDescr").getValue();
                    editImage = ""+ds.child("pImage").getValue();

                    //set data to views
                    titleEt.setText(editTitle);
                    descriptionEt.setText(editDescription);

                    //set image
                    if(!editImage.equals("noImage")){
                        try{
                            Picasso.get().load(editImage).into(imageIv);

                        }catch (Exception e){

                        }
                    }
                }

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void uploadData(final String title, final String description) {
        pd.setMessage("Publishing post....");
        pd.show();

        final String timestamp = String.valueOf(System.currentTimeMillis());
        String filePathAndName = "Posts/" + "post_" + timestamp;

        if(imageIv.getDrawable() != null){
            //get image from imageView
            Bitmap bitmap = ((BitmapDrawable)imageIv.getDrawable()).getBitmap();
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            //image compress
            bitmap.compress(Bitmap.CompressFormat.PNG,100,baos);
            byte[] data = baos.toByteArray();


            //post with image
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(filePathAndName);
            ref.putBytes(data)
                    .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            System.out.println("OnSuccess");
                            final Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();


                            uriTask.addOnCompleteListener(new OnCompleteListener<Uri>() {
                                @Override
                                public void onComplete(@NonNull Task<Uri> task) {
                                    if(uriTask.isSuccessful()){
                                        String downloadUri = uriTask.getResult().toString();

                                        //url is received upload post to firebase database
                                        HashMap<Object, String> hashMap = new HashMap<>();
                                        hashMap.put("uid",uid);
                                        hashMap.put("uName",name);
                                        hashMap.put("uEmail",email);
                                        hashMap.put("pLikes","0");
                                        hashMap.put("pComments","0");
                                        hashMap.put("uDp",dp);
                                        hashMap.put("pId",timestamp);
                                        hashMap.put("pTitle",title);
                                        hashMap.put("pDescr",description);
                                        hashMap.put("pImage",downloadUri);
                                        hashMap.put("pTime",timestamp);

                                        //path to store pathData
                                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                        ref.child(timestamp).setValue(hashMap)
                                                .addOnSuccessListener(new OnSuccessListener<Void>(){

                                                    @Override
                                                    public void onSuccess(Void aVoid) {
                                                        //added in database
                                                        pd.dismiss();
                                                        Toast.makeText(AddPostActivity.this,"Post published successfully",Toast.LENGTH_LONG).show();

                                                        //reset views
                                                        titleEt.setText("");
                                                        descriptionEt.setText("");
                                                        imageIv.setImageURI(null);
                                                        image_rui = null;


                                                    }
                                                })
                                                .addOnFailureListener(new OnFailureListener(){
                                                    @Override
                                                    public void onFailure(@NonNull Exception e){
                                                        //failed adding post
                                                        pd.dismiss();
                                                        Toast.makeText(AddPostActivity.this,""+e.getMessage(),Toast.LENGTH_LONG).show();


                                                    }
                                                });
                                    }

                                }
                            });

                            while(!uriTask.isSuccessful()){

                            }
                        }
                    }).addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e){
                            System.out.println("onFailure");
                            //failed uploading image
                            pd.dismiss();
                            Toast.makeText(AddPostActivity.this,""+e.getMessage(),Toast.LENGTH_LONG).show();
                        }
            });

        }else{
            //post without image
            System.out.println("postwithoutImage");
            HashMap<Object, String> hashMap = new HashMap<>();
            hashMap.put("uid",uid);
            hashMap.put("uName",name);
            hashMap.put("uEmail",email);
            hashMap.put("pLikes","0");
            hashMap.put("pComments","0");
            hashMap.put("uDp",dp);
            hashMap.put("pId",timestamp);
            hashMap.put("pTitle",title);
            hashMap.put("pDescr",description);
            hashMap.put("pImage","noImage");
            hashMap.put("pTime",timestamp);

            //path to store pathData
            DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
            ref.child(timestamp).setValue(hashMap)
                    .addOnSuccessListener(new OnSuccessListener<Void>(){

                        @Override
                        public void onSuccess(Void aVoid) {
                            //added in database
                            pd.dismiss();
                            Toast.makeText(AddPostActivity.this,"Post published successfully",Toast.LENGTH_SHORT).show();
                            titleEt.setText("");
                            descriptionEt.setText("");
                            imageIv.setImageURI(null);
                            image_rui = null;


                        }
                    })
                    .addOnFailureListener(new OnFailureListener(){
                        @Override
                        public void onFailure(@NonNull Exception e){
                            //failed adding post
                            pd.dismiss();
                            Toast.makeText(AddPostActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();


                        }
                    });


        }
    }

    private void showImagePickDialog() {
        //options(camre,gallery)
        String[] options = {"Camera","Gallery"};
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose image from");
        //set options at dialog
        builder.setItems(options,new DialogInterface.OnClickListener(){
            @Override
            public void onClick(DialogInterface dialog, int which){
                //item click handle
                if(which==0){
                    //cam clicked
                    if(!checkCameraPermission()){
                        requstCameraPermission();
                    }else{
                        pickFromCamera();
                    }

                }if(which==1){
                    //gallery clicked
                    if(!checkStoragePermission()){
                        requstStoragePermission();
                    }else{
                        pickFromGallery();
                    }

                }
            }
        });
        //create and show dialog
        builder.create().show();
    }

    private void pickFromGallery() {
    //image from gallery
    Intent intent = new Intent(Intent.ACTION_PICK);
    intent.setType("image/*");
    startActivityForResult(intent,IMAGE_PICK_GALLERY_CODE);
    }

    private void pickFromCamera() {
        //image from camera
        ContentValues cv = new ContentValues();
        cv.put(MediaStore.Images.Media.TITLE,"Temp Pick");
        cv.put(MediaStore.Images.Media.DESCRIPTION,"Temp Descr");
        image_rui = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,cv);

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT,image_rui);
        startActivityForResult(intent, IMAGE_PICK_CAMERA_CODE);


    }

    private boolean checkStoragePermission(){
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.WRITE_EXTERNAL_STORAGE) ==(PackageManager.PERMISSION_GRANTED);
    return result;
    }

    private void requstStoragePermission(){
        ActivityCompat.requestPermissions(this,storagePermissons,STORAGE_REQUEST_CODE);
    }


    private boolean checkCameraPermission(){
        boolean result = ContextCompat.checkSelfPermission(this,Manifest.permission.CAMERA) ==(PackageManager.PERMISSION_GRANTED);
        boolean result1 = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) ==(PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }

    private void requstCameraPermission(){
        ActivityCompat.requestPermissions(this,
                cameraPermissons,CAMERA_REQUEST_CODE);
    }





    @Override
    protected void onStart() {
        super.onStart();
        checkUserStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkUserStatus();
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); //previous activity;
        return super.onSupportNavigateUp();
    }
    private void checkUserStatus(){
        //Current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user!=null){
            //Stay logged in.
            email = user.getEmail();
            uid = user.getUid();


        }else{
            //User is not sign in we should go to signin activity
            Intent intent = new Intent(this,SignInActivity.class);
            startActivity(intent);
            finish();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }

    //handle permissions
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        switch(requestCode){
            case CAMERA_REQUEST_CODE:{
                if(grantResults.length > 0){
                    boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    boolean storageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                    if(cameraAccepted && storageAccepted){
                        pickFromCamera();
                    }
                    else{
                        Toast.makeText(this,"Camera & Storage both permissions are denied....",Toast.LENGTH_LONG).show();

                    }
                }else{


                }

            }
            break;
            case STORAGE_REQUEST_CODE:{
                if(grantResults.length > 0){
                    boolean storageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if(storageAccepted){
                        pickFromGallery();
                    }
                    else{
                        Toast.makeText(this,"Storage permission is denied....",Toast.LENGTH_LONG).show();

                    }
                }
                else{

                }

            }
            break;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //Called after picking image
        if(resultCode==RESULT_OK){
            if(requestCode == IMAGE_PICK_GALLERY_CODE){
                image_rui = data.getData();

                //set to imageView
                imageIv.setImageURI(image_rui);
            }
            else if(requestCode == IMAGE_PICK_CAMERA_CODE){
                imageIv.setImageURI(image_rui);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}

