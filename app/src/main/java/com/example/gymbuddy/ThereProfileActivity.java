package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ImageView;
import androidx.appcompat.widget.SearchView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.MenuItemCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import com.squareup.picasso.Picasso;

import java.util.ArrayList;
import java.util.List;
//author : Mehmet Cihan Sakman

public class ThereProfileActivity extends AppCompatActivity {

    FirebaseAuth firebaseAuth;
    FirebaseUser user;

    //views from xml
    ImageView avatarIV;
    TextView nameTV, nickTV, bioTV;

    RecyclerView postsRecyclerView;

    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    String uid;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_there_profile);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);

        //Init views
        avatarIV = findViewById(R.id.avatarIV);
        nameTV = findViewById(R.id.nameTV);
        nickTV = findViewById(R.id.nickTV);
        bioTV = findViewById(R.id.bioTV);
        postsRecyclerView = findViewById(R.id.recyclerview_posts);

        firebaseAuth = FirebaseAuth.getInstance();

        //get uid of clicked user
        Intent intent = getIntent();
        uid = intent.getStringExtra("uid");


        Query query = FirebaseDatabase.getInstance().getReference("UserInfos").orderByChild("uid").equalTo(uid);
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
                        Picasso.get().load(image).into(avatarIV);
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

        postList = new ArrayList<>();

        checkUserStatus();
        loadHistPosts();
        

    }

    private void loadHistPosts() {
        //linear layout for recyclerview
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
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
                    adapterPosts = new AdapterPosts(ThereProfileActivity.this,postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ThereProfileActivity.this,""+error.getMessage(),Toast.LENGTH_SHORT).show();

            }
        });
    }
    private void searchHistPosts(final String searchQuery){
        //linear layout for recyclerview
        final LinearLayoutManager layoutManager = new LinearLayoutManager(ThereProfileActivity.this);
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
                    adapterPosts = new AdapterPosts(ThereProfileActivity.this,postList);
                    //set this adapter to recyclerview
                    postsRecyclerView.setAdapter(adapterPosts);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(ThereProfileActivity.this,""+error.getMessage(),Toast.LENGTH_SHORT).show();


            }
        });

    }

    private void checkUserStatus(){
        //Current user
        user = firebaseAuth.getCurrentUser();
        if(user!=null){
            //Stay logged in.
        }
        else{
            //User is not sign in we should go to signin activity
            Intent intent = new Intent(this,SignInActivity.class);
            startActivity(intent);
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
        //hide add post from this activity
        menu.findItem(R.id.action_add_post).setVisible(false);

        MenuItem item = menu.findItem(R.id.action_search);

        //searchview ot search user sğecific posts
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called wheen user press search button
                if (!TextUtils.isEmpty(s)){
                    //search
                    searchHistPosts(s);
                }
                else{
                    loadHistPosts();
                }
                return false;

            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user types any letter
                if (!TextUtils.isEmpty(s)){
                    //search
                    searchHistPosts(s);
                }
                else{
                    loadHistPosts();
                }
                return false;
            }
        });

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
}