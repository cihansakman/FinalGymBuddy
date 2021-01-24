package com.example.gymbuddy;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuItemCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

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

public class SearchFragment extends Fragment {

    RecyclerView recyclerView;
    AdapterUsers adapterUsers;
    List<ModelUsers> usersList;
    FirebaseAuth firebaseAuth;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_search, container, false);

        //init recyclerview
        recyclerView =view.findViewById(R.id.search_recyclerView);
        //set properties
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        //init user list
        usersList = new ArrayList<>();
        //inits
        //firebaseAuth = FirebaseAuth.getInstance();

        //get all users
        getAllUsers();

        return view;
    }

    private void getAllUsers() {
        //get current user
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //get path of db named "Userinfos"
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("UserInfos");
        // get all data from path
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelUsers modelUsers = ds.getValue(ModelUsers.class);
                    //get all users except currently signed in user
                    if(!modelUsers.getUid().equals(fUser.getUid())){
                        usersList.add(modelUsers);
                        System.out.println("LALALAL"+modelUsers.getProfilepic());
                        System.out.println("LOLOLO"+modelUsers.getName());
                        System.out.println("LOLOLO"+modelUsers.getNickname());
                    }
                    //adapter
                    adapterUsers = new AdapterUsers(getActivity(), usersList);
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

            }
        });
    }

    private void searchUsers(final String query) {
        //get current user
        final FirebaseUser fUser = FirebaseAuth.getInstance().getCurrentUser();
        //get path of db named "Userinfos"
        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("UserInfos");
        // get all data from path
        ref.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                usersList.clear();
                for (DataSnapshot ds: snapshot.getChildren()){
                    ModelUsers modelUsers = ds.getValue(ModelUsers.class);
                    //get all searched users except currently signed in user
                    if(!modelUsers.getUid().equals(fUser.getUid())){
                        if (modelUsers.getName().toLowerCase().contains(query.toLowerCase()) ||
                                modelUsers.getNickname().toLowerCase().contains(query.toLowerCase())){
                            usersList.add(modelUsers);


                        }
                    }
                    //adapter
                    adapterUsers = new AdapterUsers(getActivity(), usersList);
                    //refresh adapter
                    adapterUsers.notifyDataSetChanged();
                    //set adapter to recycler view
                    recyclerView.setAdapter(adapterUsers);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {

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
        inflater.inflate(R.menu.menu_main,menu);

        //Hide addpost icon
        menu.findItem(R.id.action_add_post).setVisible(false);


        //Search view
        MenuItem item = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(item);

        //search listener
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String s) {
                //called when user press search button from keyboard
                //if search query is not empty then search
                if (!TextUtils.isEmpty(s.trim())){
                    //search it
                    searchUsers(s);

                }
                else {
                    //get all users
                }
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                //called whenever user press any letter
                //if search query is not empty then search
                if (!TextUtils.isEmpty(s.trim())){
                    //search it
                    searchUsers(s);

                }
                else {
                    //get all users
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
        return super.onOptionsItemSelected(item);
    }
}