package com.example.gymbuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
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
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
//author : Mehmet Cihan Sakman

public class PostDetailActivity extends AppCompatActivity {

    //to get detail of user and post
    String  hisUid, myUid, myEmail, myName, myDp,
            postId, pLikes, hisDp, hisName, pImage;

    boolean mProcessComment = false;
    boolean mProcessLike = false;


    //prog bar
    ProgressDialog pd;

    //views
    ImageView uPictureIv, pImageIv;
    TextView uNameTv, pTimeTiv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
    ImageButton moreBtn;
    Button likeBtn, shareBtn;
    LinearLayout profileLayout;
    RecyclerView recyclerView;

    List<ModelComment>commentList;
    AdapterComments adapterComments;


    //add comment views
    EditText commentEt;
    ImageButton sendBtn;
    ImageView cAvatarIv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_post_detail);

        //Action Bar
        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Post Detail");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //get id of post using intent
        Intent intent = getIntent();
        postId = intent.getStringExtra("postId");

        //init views
        uPictureIv = findViewById(R.id.uPictureIv);
        pImageIv = findViewById(R.id.pImageIv);
        uNameTv = findViewById(R.id.uNameTv);
        pTimeTiv = findViewById(R.id.pTimeTv);
        pTitleTv = findViewById(R.id.pTitleTv);
        pDescriptionTv = findViewById(R.id.pDescriptionTv);
        pLikesTv = findViewById(R.id.pLikesTv);
        pCommentsTv = findViewById(R.id.pCommentsTv);
        moreBtn = findViewById(R.id.moreBtn);
        likeBtn = findViewById(R.id.likeBtn);
        shareBtn = findViewById(R.id.shareBtn);
        profileLayout = findViewById(R.id.profileLayout);
        recyclerView = findViewById(R.id.recyclerView);

        commentEt = findViewById(R.id.commentEt);
        sendBtn = findViewById(R.id.sendBtn);
        cAvatarIv = findViewById(R.id.cAvatarIv);


        loadPostInfo();

        checkUserStatus();

        loadUserInfo();

        setLikes();


        //set subtitle
        actionBar.setSubtitle("SignedIn as: "+myEmail);

        loadComments();

        //send comment button click
        sendBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                postComment();
            }
        });
        //like button click handle
        likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                likePost();
            }
        });
        //more button click handle
        moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMoreOptions();
            }
        });

        //share button click handle
        shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String pTitle = pTitleTv.getText().toString().trim();
                String pDescription = pDescriptionTv.getText().toString().trim();

                BitmapDrawable bitmapDrawable = (BitmapDrawable)pImageIv.getDrawable();
                if(bitmapDrawable == null){
                    //post without image
                    shareTextOnly(pTitle, pDescription);

                }else{
                    //post with image

                    //convert image to bitmap
                    Bitmap bitmap = bitmapDrawable.getBitmap();
                    shareImageAndText(pTitle,pDescription, bitmap);
                }
            }
        });

        pLikesTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(PostDetailActivity.this,PostLikedByActivity.class);
                intent.putExtra("postId",postId);
                startActivity(intent);
            }
        });
    }

    private void addToHisNotifications(String hisUid,String pId, String notification){
        String timestamp = ""+System.currentTimeMillis();

        HashMap<Object, String> hashMap = new HashMap<>();
        hashMap.put("pId",pId);
        hashMap.put("timestamp",timestamp);
        hashMap.put("pUid",hisUid);
        hashMap.put("notification", notification);
        hashMap.put("sUid",myUid);

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("UserInfos");
        ref.child(hisUid).child("Notifications").child(timestamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //added succesfully

                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed

                    }
                });
    }

    private void shareTextOnly(String pTitle, String pDescription) {
        //concatenate title and desc
        String shareBody  = pTitle + "\n"+pDescription;

        //share Intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.setType("text/plain");
        sIntent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here"); //in case you share via an email app
        sIntent.putExtra(Intent.EXTRA_TEXT,shareBody); //text to share
        startActivity(Intent.createChooser(sIntent,"Share Via")); //message to show in share dialog


    }

    private void shareImageAndText(String pTitle, String pDescription, Bitmap bitmap) {
        //concatenate title and desc
        String shareBody  = pTitle + "\n"+pDescription;

        //first we will save image in cache
        Uri uri = saveImageToShare(bitmap);

        //share intent
        Intent sIntent = new Intent(Intent.ACTION_SEND);
        sIntent.putExtra(Intent.EXTRA_STREAM,uri);
        sIntent.putExtra(Intent.EXTRA_TEXT,shareBody);
        sIntent.putExtra(Intent.EXTRA_SUBJECT,"Subject Here");
        sIntent.setType("image/png");
        startActivity(Intent.createChooser(sIntent,"Share Via"));


    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(getCacheDir(),"images");
        Uri uri = null;
        try{
            imageFolder.mkdirs(); //create if not exists
            File file = new File(imageFolder,"shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90,stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(this,"com.example.gymbuddy.fileprovider",file);



        }catch (Exception e){
            Toast.makeText(this, ""+e.getMessage(),Toast.LENGTH_SHORT).show();

        }
        return uri;
    }

    private void loadComments() {
        //layout for recycler view
        LinearLayoutManager layoutManager = new LinearLayoutManager(getApplicationContext());
        //set layout to recy
        recyclerView.setLayoutManager(layoutManager);

        //init
        commentList = new ArrayList<>();
        //path of the post to get coment
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                commentList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelComment modelComment = ds.getValue(ModelComment.class);

                    commentList.add(modelComment);

                    //pass myUid and postId as paramter of constr. of Comment Adapter

                    //setup adapter
                    adapterComments = new AdapterComments(getApplicationContext(),commentList,myUid, postId);
                    //set adapter
                    recyclerView.setAdapter(adapterComments);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showMoreOptions() {
        //popup menu having option Delete
        PopupMenu popupMenu = new PopupMenu(this, moreBtn, Gravity.END);

        //show delete option in only posts of user itself.
        if(hisUid.equals(myUid)){
            //add items in menu
            popupMenu.getMenu().add(Menu.NONE,0,0,"Delete");
            popupMenu.getMenu().add(Menu.NONE,1,0,"Edit");
        }

        //item click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if(id ==0){
                    //delete is clicked
                    beginDelete();
                }else if(id==1){
                    //Edit is clicked
                    //start AddPostActivity with key "editPost"
                    Intent intent = new Intent(PostDetailActivity.this,AddPostActivity.class);
                    intent.putExtra("key","editPost");
                    intent.putExtra("editPostId",postId);
                    startActivity(intent);
                }
                return false;
            }
        });
        //show menu
        popupMenu.show();
    }

    private void beginDelete() {
        //post can be with or without image

        if(pImage.equals("noImage")){
            //post without image
            deleteWithoutImage();
        }else{
            //post with image
            deleteWithImage();
        }
    }

    private void deleteWithImage() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting....");

        //First delete Image using url
        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //image deleted, now delete database

                Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
                fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot ds: snapshot.getChildren()){
                            ds.getRef().removeValue(); //remove values from firebase where pid matches

                        }
                        //deleted
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this,"Deleted successfully.",Toast.LENGTH_SHORT).show();

                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });


            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                //failed
                pd.dismiss();
                Toast.makeText(PostDetailActivity.this,""+e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void deleteWithoutImage() {
        final ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deleting....");

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(postId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds: snapshot.getChildren()){
                    ds.getRef().removeValue(); //remove values from firebase where pid matches

                }
                //deleted
                pd.dismiss();
                Toast.makeText(PostDetailActivity.this,"Deleted successfully.",Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void setLikes() {
        //when the details of post loading, also chech liked or not
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");

        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postId).hasChild(myUid)){
                    //user has liked, to indicate that the post is liked by signed in user
                    //change left icon of like button, change text
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked,0,0,0);
                    likeBtn.setText("Liked");

                }
                else {
                    //user has not liked
                    likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black,0,0,0);
                    likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void likePost() {
        //get total num of likes, if currently user signed in has not liked it before
        //increase value by 1, otherwise decrease by 1
        mProcessLike = true;
        //get id of the post
        final DatabaseReference likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        final DatabaseReference postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (mProcessLike){
                    if (snapshot.child(postId).hasChild(myUid)){
                        //already like so remove like
                        postsRef.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)-1));
                        likesRef.child(postId).child(myUid).removeValue();
                        mProcessLike = false;


                    }
                    else{
                        //not liked it, like it
                        postsRef.child(postId).child("pLikes").setValue(""+(Integer.parseInt(pLikes)+1));
                        likesRef.child(postId).child(myUid).setValue("Liked");//set any value
                        mProcessLike = false;

                        addToHisNotifications(""+hisUid,""+postId,"Liked Your Post");


                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void postComment() {
        pd = new ProgressDialog(this);
        pd.setMessage("Adding comment...");

        //get data from comment ET
        final String comment = commentEt.getText().toString().trim();
        //validate
        if (TextUtils.isEmpty(comment)){
            //no value is entered
            Toast.makeText(this,"Comment is empty...",Toast.LENGTH_SHORT).show();
            return;
        }
        String timeStamp = String.valueOf(System.currentTimeMillis());
        //each post have child named comments
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId).child("Comments");

        HashMap<String,Object> hashMap = new HashMap<>();
        //put info in hashmap
        hashMap.put("cId",timeStamp);
        hashMap.put("comment",comment);
        hashMap.put("timeStamp",timeStamp);
        hashMap.put("uid",myUid);
        hashMap.put("uEmail",myEmail);
        hashMap.put("uDp",myDp);
        hashMap.put("uName",myName);

        //put this data iin db
        ref.child(timeStamp).setValue(hashMap)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        //added
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this,"Comment added...",Toast.LENGTH_SHORT).show();
                        commentEt.setText("");
                        updateCommentCount();

                        addToHisNotifications(""+hisUid,""+postId,"Commented On Your Post");
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        //failed
                        pd.dismiss();
                        Toast.makeText(PostDetailActivity.this,""+e.getMessage(),Toast.LENGTH_SHORT).show();

                    }
                });
    }

    private void updateCommentCount() {
        //whenever user adds comment increase count
        mProcessComment = true;
        final DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts").child(postId);
        ref.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                String comments = ""+snapshot.child("pComments").getValue();
                int newCommentVal = Integer.parseInt(comments)+1;
                ref.child("pComments").setValue(""+newCommentVal);
                mProcessComment = false;
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadUserInfo() {
        //get current user indo
        Query myRef = FirebaseDatabase.getInstance().getReference("UserInfos");
        myRef.orderByChild("uid").equalTo(myUid).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot ds : snapshot.getChildren()) {
                    myName = ""+ds.child("name").getValue();
                    myDp = ""+ds.child("profilepic").getValue();

                    //set data
                    try {
                        //if image is received
                        Picasso.get().load(hisDp).placeholder(R.drawable.ic_image_person).into(uPictureIv);

                    }catch (Exception e){
                        Picasso.get().load(R.drawable.ic_image_person).into(uPictureIv);

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void loadPostInfo() {
        //get post using id  of post
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        Query query = ref.orderByChild("pId").equalTo(postId);
        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                //keep checking posts until get the required p
                for (DataSnapshot ds : snapshot.getChildren()){
                    //get data
                    String pTitle = ""+ds.child("pTitle").getValue();
                    String pDescr = ""+ds.child("pDescr").getValue();
                    pLikes = ""+ds.child("pLikes").getValue();
                    String pTimeStamp = ""+ds.child("pTime").getValue();
                    pImage = ""+ds.child("pImage").getValue();
                    hisDp = ""+ds.child("uDp").getValue();
                    hisName = ""+ds.child("uName").getValue();
                    hisUid = ""+ds.child("uid").getValue();
                    String uEmail = ""+ds.child("uEmail").getValue();
                    String commentCount = ""+ds.child("pComments").getValue();


                    //convert timestamp to dd/mm/yyyy hh:mm aa
                    Calendar calendar = Calendar.getInstance(Locale.getDefault());
                    calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
                    String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

                    //set dataa
                    pTitleTv.setText(pTitle);
                    pDescriptionTv.setText(pDescr);
                    pLikesTv.setText(pLikes+"Likes");
                    pTimeTiv.setText(pTime);
                    pCommentsTv.setText(commentCount+ "Comments");

                    uNameTv.setText(hisName);

                    // set image who posts
                    //if there is no image then hide imageView
                    if(pImage.equals("noImage")){
                        //hide imageView
                        pImageIv.setVisibility(View.GONE);

                    }
                    else{

                        //show imageView
                        pImageIv.setVisibility(View.VISIBLE);

                        try{
                            Picasso.get().load(pImage).into(pImageIv);

                        }catch (Exception e){

                        }

                    }

                    //set user image in comment
                    try {
                        Picasso.get().load(myDp).placeholder(R.drawable.ic_image_person).into(cAvatarIv);

                    }catch (Exception e){
                        Picasso.get().load(R.drawable.ic_image_person).into(cAvatarIv);

                    }

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void checkUserStatus(){
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user!=null){
            //user signed in
            myEmail = user.getEmail();
            myUid = user.getUid();

        }
        else {
            //not signed in
            startActivity(new Intent(this,SignInActivity.class));
            finish();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed();
        return super.onSupportNavigateUp();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main,menu);
        //hide some menu items
        menu.findItem(R.id.action_add_post).setVisible(false);
        menu.findItem(R.id.action_search).setVisible(false);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //get item id
        if(id == R.id.action_logout){
            FirebaseAuth.getInstance().signOut();
            checkUserStatus();
        }
        return super.onOptionsItemSelected(item);
    }
}