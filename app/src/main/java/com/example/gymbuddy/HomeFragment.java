package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import androidx.appcompat.widget.SearchView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
//author : Osman Batuhan Şahin

public class HomeFragment extends Fragment {

    FirebaseAuth firebaseAuth;

    RecyclerView recyclerView;
    List<ModelPost> postList;
    AdapterPosts adapterPosts;
    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        //inits
        firebaseAuth = FirebaseAuth.getInstance();

        //recyler view and its proporties
        recyclerView = view.findViewById(R.id.postRecylerview);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getActivity());

        //show newest post first
        layoutManager.setStackFromEnd(true);
        layoutManager.setReverseLayout(true);
        //set layout to recylcerview
        recyclerView.setLayoutManager(layoutManager);


        //init post ist
        postList = new ArrayList<>();

        loadPosts();
        return view;
    }

    private void loadPosts() {
        //path of all posts
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //get all data from this ref
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelPost modelPost = ds.getValue(ModelPost.class);

                    postList.add(modelPost);

                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(),postList);
                    //set adapter to recyclerview
                    recyclerView.setAdapter(adapterPosts);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //in case of error
                //Toast.makeText(getActivity(),""+error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });

    }

    private void searchPosts(final String searchQuery){
        //path of all posts
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("Posts");
        //get all data from this ref
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                postList.clear();
                for(DataSnapshot ds: dataSnapshot.getChildren()){
                    ModelPost modelPost = ds.getValue(ModelPost.class);


                    if(modelPost.getpTitle().toLowerCase().contains(searchQuery.toLowerCase()) || modelPost.getpDescr().toLowerCase().contains(searchQuery.toLowerCase())){
                        postList.add(modelPost);
                    }

                    postList.add(modelPost);

                    //adapter
                    adapterPosts = new AdapterPosts(getActivity(),postList);
                    //set adapter to recyclerview
                    recyclerView.setAdapter(adapterPosts);

                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                //in case of error
                Toast.makeText(getActivity(),""+error.getMessage(),Toast.LENGTH_LONG).show();
            }
        });
    }

    private void checkUserStatus(){
        //Current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user!=null){
            //Stay logged in.
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
        //inflating menu
        inflater.inflate(R.menu.menu_main,menu);

        //searchview to seearch posts by post title
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        //search Listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called when user press search button
                if(!TextUtils.isEmpty(s)){
                    searchPosts(s);
                }else{
                    loadPosts();
                }

                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called as and when user press any letter
                if(!TextUtils.isEmpty(s)){
                    searchPosts(s);
                }else{
                    loadPosts();
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