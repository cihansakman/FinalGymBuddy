package com.example.gymbuddy;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.provider.ContactsContract;
import android.text.format.DateFormat;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.RecyclerView;


import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
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
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Queue;

public class AdapterPosts extends RecyclerView.Adapter<AdapterPosts.MyHolder> {
    //zip
    Context context;
    List<ModelPost> postList;

    String myUid;

    private DatabaseReference likesRef; //for likes
    private DatabaseReference postsRef; //for posts

    boolean mProcessLike = false;

    public AdapterPosts(Context context, List<ModelPost> postList) {
        this.context = context;
        this.postList = postList;
        myUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        likesRef = FirebaseDatabase.getInstance().getReference().child("Likes");
        postsRef = FirebaseDatabase.getInstance().getReference().child("Posts");


    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //inflate layout row_post.html
        View view = LayoutInflater.from(context).inflate(R.layout.row_posts, viewGroup, false);
        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull final MyHolder myHolder, final int i) {
        //get data
        final String uid = postList.get(i).getUid();
        String uEmail = postList.get(i).getuEmail();
        String uName = postList.get(i).getuName();
        String uDp = postList.get(i).getuDp();
        final String pId = postList.get(i).getpId();
        final String pTitle = postList.get(i).getpTitle();
        final String pDescription = postList.get(i).getpDescr();
        final String pImage = postList.get(i).getpImage();
        String pTimeStamp = postList.get(i).getpTime();
        String pLikes = postList.get(i).getpLikes();//contains total likes a post
        String pComments = postList.get(i).getpComments();//contains total likes a post


        //convert timestamp to dd/mm/yyyy hh:mm aa
        Calendar calendar = Calendar.getInstance(Locale.getDefault());
        calendar.setTimeInMillis(Long.parseLong(pTimeStamp));
        String pTime = DateFormat.format("dd/MM/yyyy hh:mm aa",calendar).toString();

        //set data
        myHolder.uNameTv.setText(uName);
        myHolder.pTimeTv.setText(pTime);
        myHolder.pTitleTv.setText(pTitle);
        myHolder.pDescriptionTv.setText(pDescription);
        myHolder.pLikesTv.setText(pLikes+" Likes");//eg. 10 likes
        myHolder.pCommentsTv.setText(pComments+" Comments");//eg. 10 likes
        //set likes for each post
        setLikes(myHolder, pId);
        
        //set user dp
        try{
            Picasso.get().load(uDp).placeholder(R.drawable.ic_image_person).into(myHolder.uPictureTv);

        }catch (Exception e){

        }

        //set post image
        //if there is no image then hide imageView
        if(pImage.equals("noImage")){
            //hide imageView
            myHolder.pImageIv.setVisibility(View.GONE);

        }
        else{

            //show imageView
            myHolder.pImageIv.setVisibility(View.VISIBLE);

            try{
                Picasso.get().load(pImage).into(myHolder.pImageIv);

            }catch (Exception e){

            }

        }


        //handle button clicks
        myHolder.moreBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showMoreOptions(myHolder.moreBtn, uid, myUid, pId, pImage);
            }
        });

        myHolder.likeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //get total num of likes, if currently user signed in has not liked it before
                //increase value by 1, otherwise decrease by 1
                final int pLikes = Integer.parseInt(postList.get(i).getpLikes());
                mProcessLike = true;
                //get id of the post
                final String postIde = postList.get(i).getpId();
                likesRef.addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (mProcessLike){
                            if (snapshot.child(postIde).hasChild(myUid)){
                                //already like so remove like
                                postsRef.child(postIde).child("pLikes").setValue(""+(pLikes-1));
                                likesRef.child(postIde).child(myUid).removeValue();
                                mProcessLike = false;
                            }
                            else{
                                //not liked it, like it
                                postsRef.child(postIde).child("pLikes").setValue(""+(pLikes+1));
                                likesRef.child(postIde).child(myUid).setValue("Liked");//set any value
                                mProcessLike = false;
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
            }
        });

        myHolder.commentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //start postDetailAct
                Intent intent = new Intent(context, PostDetailActivity.class);
                intent.putExtra("postId",pId);//will get detail of post using id of post
                context.startActivity(intent);

            }
        });

        myHolder.shareBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //handle with posts with image and without image
                //get image from imageView
                BitmapDrawable bitmapDrawable = (BitmapDrawable)myHolder.pImageIv.getDrawable();
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
        myHolder.profileLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //click to go thereProfileact with uid
                Intent intent = new Intent(context, ThereProfileActivity.class);
                intent.putExtra("uid",uid);
                context.startActivity(intent);

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
        context.startActivity(Intent.createChooser(sIntent,"Share Via")); //message to show in share dialog


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
        context.startActivity(Intent.createChooser(sIntent,"Share Via"));


    }

    private Uri saveImageToShare(Bitmap bitmap) {
        File imageFolder = new File(context.getCacheDir(),"images");
        Uri uri = null;
        try{
            imageFolder.mkdirs(); //create if not exists
            File file = new File(imageFolder,"shared_image.png");

            FileOutputStream stream = new FileOutputStream(file);
            bitmap.compress(Bitmap.CompressFormat.PNG, 90,stream);
            stream.flush();
            stream.close();
            uri = FileProvider.getUriForFile(context,"com.example.gymbuddy.fileprovider",file);



        }catch (Exception e){
            Toast.makeText(context, ""+e.getMessage(),Toast.LENGTH_SHORT).show();

        }
        return uri;
    }




    private void setLikes(final MyHolder holder, final String postKey) {
        likesRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.child(postKey).hasChild(myUid)){
                    //user has liked, to indicate that the post is liked by signed in user
                    //change left icon of like button, change text
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_liked,0,0,0);
                    holder.likeBtn.setText("Liked");

                }
                else {
                    //user has not liked
                    holder.likeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_like_black,0,0,0);
                    holder.likeBtn.setText("Like");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void showMoreOptions(ImageButton moreBtn, String uid, String myUid, final String pId, final String pImage) {
        //popup menu having option Delete
        PopupMenu popupMenu = new PopupMenu(context, moreBtn, Gravity.END);

        //show delete option in only posts of user itself.
        if(uid.equals(myUid)){
            //add items in menu
            popupMenu.getMenu().add(Menu.NONE,0,0,"Delete");
            popupMenu.getMenu().add(Menu.NONE,1,0,"Edit");
        }
        popupMenu.getMenu().add(Menu.NONE,2,0,"View Detail");


        //item click listener
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                int id = menuItem.getItemId();
                if(id ==0){
                    //delete is clicked
                    beginDelete(pId, pImage);
                }else if(id==1){
                    //Edit is clicked
                    //start AddPostActivity with key "editPost"
                    Intent intent = new Intent(context,AddPostActivity.class);
                    intent.putExtra("key","editPost");
                    intent.putExtra("editPostId",pId);
                    context.startActivity(intent);

                }
                else if (id==2){
                    //start postdetailact
                    Intent intent = new Intent(context, PostDetailActivity.class);
                    intent.putExtra("postId",pId);//will get detail of post using id of post
                    context.startActivity(intent);

                }
                return false;
            }
        });
        //show menu
        popupMenu.show();

    }

    private void beginDelete(String pId, String pImage) {
        //post can be with or without image

        if(pImage.equals("noImage")){
            //post without image
            deleteWithoutImage(pId);
        }else{
            //post with image
            deleteWithImage(pId,pImage);
        }

    }
    private void deleteWithImage(final String pId, String pImage) {
        //progress bar
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting....");

        //First delete Image using url
        StorageReference picRef = FirebaseStorage.getInstance().getReferenceFromUrl(pImage);
        picRef.delete().addOnSuccessListener(new OnSuccessListener<Void>() {
            @Override
            public void onSuccess(Void aVoid) {
                //image deleted, now delete database

                Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
                fquery.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        for(DataSnapshot ds: snapshot.getChildren()){
                            ds.getRef().removeValue(); //remove values from firebase where pid matches

                        }
                        //deleted
                        pd.dismiss();
                        Toast.makeText(context,"Deleted successfully.",Toast.LENGTH_SHORT).show();

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
                Toast.makeText(context,""+e.getLocalizedMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }

    private void deleteWithoutImage(String pId) {
        //progress bar
        final ProgressDialog pd = new ProgressDialog(context);
        pd.setMessage("Deleting....");

        Query fquery = FirebaseDatabase.getInstance().getReference("Posts").orderByChild("pId").equalTo(pId);
        fquery.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for(DataSnapshot ds: snapshot.getChildren()){
                    ds.getRef().removeValue(); //remove values from firebase where pid matches

                }
                //deleted
                pd.dismiss();
                Toast.makeText(context,"Deleted successfully.",Toast.LENGTH_SHORT).show();

            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });


    }





    @Override
    public int getItemCount() {
        return postList.size();
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder{

        //views from row_post.htm
        ImageView uPictureTv, pImageIv;
        TextView uNameTv, pTimeTv, pTitleTv, pDescriptionTv, pLikesTv, pCommentsTv;
        ImageButton moreBtn;
        Button likeBtn, commentBtn, shareBtn;
        LinearLayout profileLayout;
        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            uPictureTv = itemView.findViewById(R.id.uPictureIv);
            pImageIv = itemView.findViewById(R.id.pImageIv);
            uNameTv = itemView.findViewById(R.id.uNameTv);
            pTimeTv = itemView.findViewById(R.id.pTimeTv);
            pTitleTv = itemView.findViewById(R.id.pTitleTv);
            pDescriptionTv = itemView.findViewById(R.id.pDescriptionTv);
            pLikesTv = itemView.findViewById(R.id.pLikesTv);
            pCommentsTv = itemView.findViewById(R.id.pCommentsTv);
            moreBtn = itemView.findViewById(R.id.moreBtn);
            likeBtn = itemView.findViewById(R.id.likeBtn);
            commentBtn = itemView.findViewById(R.id.commentBtn);
            shareBtn = itemView.findViewById(R.id.shareBtn);
            profileLayout = itemView.findViewById(R.id.profileLayout);



        }
    }


}
