package com.example.gymbuddy;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.Guideline;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;

import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.squareup.picasso.Picasso;

import java.security.Key;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static com.google.firebase.storage.FirebaseStorage.getInstance;
//author : Osman Batuhan Şahin

public class ProfileFragment extends Fragment {

    //firebase
    FirebaseAuth firebaseAuth;
    FirebaseUser user;
    FirebaseDatabase firebaseDatabase;
    DatabaseReference databaseReference;

    //Storage
    StorageReference storageReference;
    //path of profile pic stored
    String storagePath = "Users_Profile_Imgs/";

    //views from xml
    ImageView avatarIV;
    TextView nameTV, nickTV, bioTV;
    FloatingActionButton fab;
    RecyclerView postsRecyclerView;

    //progress dialog
    ProgressDialog pd;

    //permissions constraints
    private static final int CAMERA_REQUEST_CODE = 100;
    private static final int STORAGE_REQUEST_CODE = 200;
    private static final int IMAGE_PICK_GALLERY_CODE = 300;
    private static final int IMAGE_PICK_CAMERA_CODE = 400;

    //arrays pf permissions to be requested
    String cameraPermissions[];
    String storagePermissions[];

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    //uri of picked image
    Uri image_uri;
    //for checking photo type
    String profileOrCoverPhoto;

    public ProfileFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        //Init firebase
        firebaseAuth = FirebaseAuth.getInstance();
        user = firebaseAuth.getCurrentUser();
        firebaseDatabase = FirebaseDatabase.getInstance();
        databaseReference = firebaseDatabase.getReference("UserInfos");
        storageReference = getInstance().getReference(); //firabase store ref

        //init arrays of perm
        cameraPermissions = new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE};
        storagePermissions = new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE};


        //Init views
        avatarIV = view.findViewById(R.id.avatarIV);
        nameTV = view.findViewById(R.id.nameTV);
        nickTV = view.findViewById(R.id.nickTV);
        bioTV = view.findViewById(R.id.bioTV);
        fab = view.findViewById(R.id.fab);
        postsRecyclerView = view.findViewById(R.id.recyclerview_posts);

        //init progress dialog
        pd = new ProgressDialog(getActivity());


        //Query
        Query query = databaseReference.orderByChild("email").equalTo(user.getEmail());
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {

                //check until get data
                for (DataSnapshot ds: snapshot.getChildren()){
                    //get data
                    String name = ""+ ds.child("name").getValue();
                    String nick = ""+ ds.child("nickname").getValue();
                    String image = ""+ ds.child("profilepic").getValue();
                    String bio = ""+ ds.child("bio").getValue();

                    //set data
                    nameTV.setText(name);
                    nickTV.setText(nick);
                    bioTV.setText(bio);
                    try {
                        //if image is received then set
                        Picasso.get().load(image).placeholder(R.drawable.ic_add_image).into(avatarIV);
                    }
                    catch (Exception e){
                        //set default
                        Picasso.get().load(R.drawable.ic_add_image).into(avatarIV);
                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
        //fab button clicked
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showEditProfileDialog();
            }
        });

        postList = new ArrayList<>();


        checkUserStatus();
        loadMyPosts();

        return view;
    }

    private void loadMyPosts() {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first(load from last)
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);

        //init posts list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load posts
        //uid of user saved as info of post we take post with uid
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    //add to list
                    postList.add(myPosts);

                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(),postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });

    }

    private void searchMyPosts(final String searchQuery) {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());
        //show newest post first(load from last)
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to recyclerview
        postsRecyclerView.setLayoutManager(layoutManager);

        //init posts list
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //query to load posts
        //uid of user saved as info of post we take post with uid
        Query query = ref.orderByChild("uid").equalTo(uid);
        //get all data from this ref
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                postList.clear();
                for (DataSnapshot ds : snapshot.getChildren()){
                    ModelPost myPosts = ds.getValue(ModelPost.class);

                    if (myPosts.getpTitle().toLowerCase().contains(searchQuery.toLowerCase())||
                    myPosts.getpDescr().toLowerCase().contains(searchQuery.toLowerCase())){
                        //add to list
                        postList.add(myPosts);
                    }


                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(),postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getActivity(),""+error.getMessage(),Toast.LENGTH_SHORT).show();


            }
        });

    }


    private boolean checkStoragePermission(){
        //check storage perm is enable or not return true if enabled
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);
        return result;
    }
    private void requestStoragePermission(){
        //reuest runtime storage permission
        requestPermissions(storagePermissions, STORAGE_REQUEST_CODE);
    }

    private boolean checkCameraPermission(){
        //check storage perm is enable or not return true if enabled
        boolean result = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.CAMERA)
                == (PackageManager.PERMISSION_GRANTED);

        boolean result1 = ContextCompat.checkSelfPermission(getActivity(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                == (PackageManager.PERMISSION_GRANTED);

        return result && result1;
    }
    private void requestCameraPermission(){
        //reuest runtime storage permission
        requestPermissions(cameraPermissions, CAMERA_REQUEST_CODE);
    }

    private void showEditProfileDialog() {
        //edit pp name etc.
        String options[] = {"Edit Profile Picture", "Edit Your Name","Edit Nickname", "Edit Your Bio","Change Password"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
       //set title
       builder.setTitle("Choose Action");
       //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog items clicks
                if (which == 0){
                    //pp
                    pd.setMessage("Updating Profile Picture");
                    profileOrCoverPhoto = "profilepic";
                    showImagePicDialog();
                }
                else if (which == 1){
                    //name
                    pd.setMessage("Updating Name");
                    //calling method name as param
                    showNamePhoneUpdateDialog("name");

                }
                else if (which == 2){
                    //nick
                    pd.setMessage("Updating Nickname");
                    showNamePhoneUpdateDialog("nickname");


                }
                else if (which == 3){
                    //bio
                    pd.setMessage("Updating Bio");
                    showNamePhoneUpdateDialog("bio");

                }
                else if (which == 4){
                    //bio
                    pd.setMessage("Changing Password");
                    showChangePasswordDialog();

                }
            }
        });
        //create and show dialog
        builder.create().show();

    }

    private void showChangePasswordDialog() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_update_password,null);
        final EditText passwordEt = view.findViewById(R.id.passwordEt);
        final EditText newpasswordEt = view.findViewById(R.id.newPasswordEt);
        final Button updatePasswordBtn = view.findViewById(R.id.updatePasswordBtn);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(view); //set view dialog

        final AlertDialog dialog = builder.create();
        dialog.show();

        updatePasswordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String oldPassword = passwordEt.getText().toString().trim();
                String newPassword = newpasswordEt.getText().toString().trim();
                if (TextUtils.isEmpty(oldPassword)){
                    Toast.makeText(getActivity(),"Enter your current password...",Toast.LENGTH_SHORT).show();
                    return;
                }
                if (newPassword.length()<6){
                    Toast.makeText(getActivity(),"Password length must at least 6 characters...",Toast.LENGTH_SHORT).show();
                    return;
                }
                dialog.dismiss();
                updatePassword(oldPassword,newPassword);
            }
        });
    }

    private void updatePassword(String oldPassword, final String newPassword) {
        pd.show();

        //get curent user
        final FirebaseUser user = firebaseAuth.getCurrentUser();

        //before change pass re-authenticate user
        AuthCredential authCredential = EmailAuthProvider.getCredential(user.getEmail(),oldPassword);
        user.reauthenticate(authCredential).addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //succesfullu authenticated, beggin
                user.updatePassword(newPassword)
                        .addOnSuccessListener(new OnSuccessListener<Void>() {
                            @Override
                            public void onSuccess(Void aVoid) {
                                //updated
                                pd.dismiss();
                                Toast.makeText(getActivity(),"Password Updated",Toast.LENGTH_SHORT).show();

                            }
                        }).addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed update pass, show reason
                        pd.dismiss();
                        Toast.makeText(getActivity(),""+e.getMessage(),Toast.LENGTH_SHORT).show();
                    }
                });
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //auth failed, show reason
                pd.dismiss();
                Toast.makeText(getActivity(),""+e.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });

    }

    private void showNamePhoneUpdateDialog(final String key) {
        //key param is name or nick or bio

        //custom dialog
        AlertDialog.Builder builder =new AlertDialog.Builder(getActivity());
        builder.setTitle("Update "+key);
        //set layout of dialog
        LinearLayout linearLayout = new LinearLayout(getActivity());
        linearLayout.setOrientation(LinearLayout.VERTICAL);
        linearLayout.setPadding(10,10,10,10);
        //add edit text
        final EditText editText = new EditText(getActivity());
        editText.setHint("Enter "+key);
        linearLayout.addView(editText);

        builder.setView(linearLayout);

        //add button to update
        builder.setPositiveButton("Update", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //input text from edittext
                final String value = editText.getText().toString().trim();
                //validate if uer has entered something or not
                if (!TextUtils.isEmpty(value)){
                    pd.show();
                    HashMap<String,Object> result = new HashMap<>();
                    result.put(key,value);

                    databaseReference.child(user.getUid()).updateChildren(result)
                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                @Override
                                public void onSuccess(Void aVoid) {
                                    //updated so dismiss progress
                                    pd.dismiss();
                                    Toast.makeText(getActivity(),"Updated",Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(new OnFailureListener() {
                                @Override
                                public void onFailure(@NonNull Exception e) {
                                    //failed show error
                                    pd.dismiss();
                                    Toast.makeText(getActivity(),""+e.getMessage(),Toast.LENGTH_SHORT).show();

                                }
                            });

                    //if user edit his name, change it from his posts
                    if (key.equals("name")){
                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                        Query query = ref.orderByChild("uid").equalTo(uid);
                        query.addValueEventListener(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds: snapshot.getChildren()){
                                    String child = ds.getKey();
                                    snapshot.getRef().child(child).child("uName").setValue(value);

                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                        //update name in current users comments on posts
                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                for (DataSnapshot ds: snapshot.getChildren()){
                                    String child = ds.getKey();
                                    if (snapshot.child(child).hasChild("Comments")){
                                        String child1 = ""+snapshot.child(child).getKey();
                                        Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments")
                                                .orderByChild("uid").equalTo(uid);
                                        child2.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot ds: snapshot.getChildren()){
                                                    String child = ds.getKey();
                                                    snapshot.getRef().child(child).child("uName").setValue(value);

                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });
                                    }
                                }
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {

                            }
                        });
                    }

                }
                else {
                    Toast.makeText(getActivity(),"Enter"+key,Toast.LENGTH_SHORT).show();

                }

            }
        });
        //add button to cancel
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();

            }
        });
        //create and show dialog
        builder.create().show();
    }

    private void showImagePicDialog() {
        //show dialog containing options camera and gallery
        String options[] = {"Camera", "Gallery"};
        //alert dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        //set title
        builder.setTitle("Pick Image From");
        //set items to dialog
        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                //handle dialog items clicks
                if (which == 0){
                    //camera
                    if (!checkCameraPermission()){
                        requestCameraPermission();
                    }
                    else {
                        pickFromCamera();
                    }
                }
                else if (which == 1){
                    //Gallery
                    if (!checkStoragePermission()){
                        requestStoragePermission();
                    }
                    else{
                        pickFromGallery();
                    }
                }

            }
        });
        //create and show dialog
        builder.create().show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //handle permission cases allow deny
        switch (requestCode){
            case CAMERA_REQUEST_CODE:{
                //picking from camera, checking permssions
               if (grantResults.length>0){
                   boolean cameraAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                   boolean writeStorageAccepted = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                   if (cameraAccepted && writeStorageAccepted){
                       //permission enabled
                       pickFromCamera();

                   }
                   else {
                       //denied
                       Toast.makeText(getActivity(), "Please Enable Camera Permission",Toast.LENGTH_SHORT).show();
                   }

               }
            }
            break;
            case STORAGE_REQUEST_CODE:{

                //picking from gallery, checking permssions
                if (grantResults.length>0){
                    boolean writeStorageAccepted = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                    if (writeStorageAccepted){
                        //permission enabled
                        pickFromGallery();

                    }
                    else {
                        //denied
                        Toast.makeText(getActivity(), "Please Enable Gallery Permission",Toast.LENGTH_SHORT).show();
                    }

                }

            }
            break;
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        //called after picking image
        if (resultCode == RESULT_OK){
            if (requestCode == IMAGE_PICK_GALLERY_CODE){
                image_uri = data.getData();

                uploadProfilePhoto(image_uri);
            }
            if (requestCode == IMAGE_PICK_CAMERA_CODE){
                uploadProfilePhoto(image_uri);
            }
        }

        super.onActivityResult(requestCode, resultCode, data);
    }

    private void uploadProfilePhoto(Uri uri) {
        //show progress
        pd.show();
        //path and name of image to firabase storage
        String filePathAndName = storagePath+""+profileOrCoverPhoto+"_"+user.getUid();
        StorageReference storageReference2nd = storageReference.child(filePathAndName);
        storageReference2nd.putFile(uri)
                .addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        // image uploaded
                        final Task<Uri> uriTask = taskSnapshot.getStorage().getDownloadUrl();
                        uriTask.addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (uriTask.isSuccessful()){

                                    final Uri downloadUri =uriTask.getResult();
                                    //image uploaded
                                    //update url in users database
                                    HashMap<String,Object> results = new HashMap<>();
                                    //first param=image or cover seceond this url will be saved as value
                                    results.put(profileOrCoverPhoto,downloadUri.toString());

                                    try {
                                        //if image is received then set
                                        Picasso.get().load(downloadUri).placeholder(R.drawable.ic_image_person).into(avatarIV);
                                    }
                                    catch (Exception e){
                                        //set default
                                        Picasso.get().load(R.drawable.ic_add_image).into(avatarIV);
                                    }

                                    databaseReference.child(user.getUid()).updateChildren(results)
                                            .addOnSuccessListener(new OnSuccessListener<Void>() {
                                                @Override
                                                public void onSuccess(Void aVoid) {
                                                    //url in db of user is added succesfully
                                                    pd.dismiss();
                                                    Toast.makeText(getActivity(),"Image Updated",Toast.LENGTH_SHORT).show();

                                                }
                                            })
                                            .addOnFailureListener(new OnFailureListener() {
                                                @Override
                                                public void onFailure(@NonNull Exception e) {
                                                    //error
                                                    pd.dismiss();
                                                    Toast.makeText(getActivity(),"Error updating Image",Toast.LENGTH_SHORT).show();

                                                }
                                            });

                                    //if user edit his name, change it from his posts
                                    if (profileOrCoverPhoto.equals("profilePic")){
                                        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
                                        Query query = ref.orderByChild("uid").equalTo(uid);
                                        query.addValueEventListener(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot ds: snapshot.getChildren()){
                                                    String child = ds.getKey();
                                                    snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());

                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });

                                        //update image in user comments
                                        ref.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                for (DataSnapshot ds: snapshot.getChildren()){
                                                    String child = ds.getKey();
                                                    if (snapshot.child(child).hasChild("Comments")){
                                                        String child1 = ""+snapshot.child(child).getKey();
                                                        Query child2 = FirebaseDatabase.getInstance().getReference("Posts").child(child1).child("Comments")
                                                                .orderByChild("uid").equalTo(uid);
                                                        child2.addValueEventListener(new ValueEventListener() {
                                                            @Override
                                                            public void onDataChange(@NonNull DataSnapshot snapshot) {
                                                                for (DataSnapshot ds: snapshot.getChildren()){
                                                                    String child = ds.getKey();
                                                                    snapshot.getRef().child(child).child("uDp").setValue(downloadUri.toString());

                                                                }
                                                            }

                                                            @Override
                                                            public void onCancelled(@NonNull DatabaseError error) {

                                                            }
                                                        });
                                                    }
                                                }
                                            }

                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {

                                            }
                                        });

                                    }

                                } //check if image is uploaded or not and url is received


                                else{
                                    System.out.println("else");
                                    //error
                                    pd.dismiss();
                                    Toast.makeText(getActivity(),"Some error occured",Toast.LENGTH_SHORT).show();
                                }
                            }
                        });
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //error
                        pd.dismiss();
                        Toast.makeText(getActivity(), e.getMessage(), Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void pickFromCamera() {
        //intent of picking image from device camera
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.TITLE, "Temp Pic");
        values.put(MediaStore.Images.Media.DESCRIPTION, "Temp Description");
        //put image uri
        image_uri = getActivity().getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

        //intent to start camera
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, image_uri);
        startActivityForResult(cameraIntent, IMAGE_PICK_CAMERA_CODE);

    }

    private void pickFromGallery() {
        //pick from gallery
        Intent galleryIntent = new Intent(Intent.ACTION_PICK);
        galleryIntent.setType("image/*");
        startActivityForResult(galleryIntent,IMAGE_PICK_GALLERY_CODE);
    }

    private void checkUserStatus(){
        //Current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user!=null){
            //Stay logged in.
            uid = user.getUid();
        }else{
            //User is not sign in we should go to signin activity
            Intent intent = new Intent(getActivity(),SignInActivity.class);
            startActivity(intent);
            getActivity().finish();
        }
    }


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        setHasOptionsMenu(true); //to show menu opt in fragment
        super.onCreate(savedInstanceState);
    }

    //Inflate options menu
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_main,menu);

        MenuItem item = menu.findItem(R.id.action_search);

        //searchview ot search user sğecific posts
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called wheen user press search button
                if (!TextUtils.isEmpty(s)){
                    //search
                    searchMyPosts(s);
                }
                else{
                    loadMyPosts();
                }
                return false;

            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user types any letter
                if (!TextUtils.isEmpty(s)){
                    //search
                    searchMyPosts(s);
                }
                else{
                    loadMyPosts();
                }
                return false;
            }
        });
        super.onCreateOptionsMenu(menu, inflater);
    }

    //When menu item clicks.
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if(id == R.id.action_logout){
            firebaseAuth.signOut();
            checkUserStatus();
        }
        if(id == R.id.action_add_post){
            startActivity(new Intent(getActivity(),AddPostActivity.class));

        }
        return super.onOptionsItemSelected(item);
    }
}