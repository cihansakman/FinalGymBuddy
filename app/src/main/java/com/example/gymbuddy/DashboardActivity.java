package com.example.gymbuddy;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentTransaction;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
//author : Osman Batuhan Şahin

public class DashboardActivity extends AppCompatActivity {
    FirebaseAuth firebaseAuth;
    ActionBar actionBar;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        //Action Bar and it's title
        actionBar = getSupportActionBar();
        actionBar.setTitle("Profile");

        //inits
        firebaseAuth = FirebaseAuth.getInstance();

        FirebaseUser user = firebaseAuth.getCurrentUser();

        //bottom navigation
        BottomNavigationView navigationView = findViewById(R.id.navigation);
        navigationView.setOnNavigationItemSelectedListener(selectedListener);

        //For now just take the email, then we will update it as username.
        //profileTextView.setText(user.getEmail());

        // home fragment transaction (default on start)
        actionBar.setTitle("Home");
        HomeFragment fragment1 = new HomeFragment();
        FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
        ft1.replace(R.id.content, fragment1,"");
        ft1.commit();
    }

    private BottomNavigationView.OnNavigationItemSelectedListener selectedListener = new BottomNavigationView.OnNavigationItemSelectedListener() {
        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            //handling item clicks
            switch (item.getItemId()){
                case R.id.nav_home:
                    // home fragment transaction
                    actionBar.setTitle("Home");
                    HomeFragment fragment1 = new HomeFragment();
                    FragmentTransaction ft1 = getSupportFragmentManager().beginTransaction();
                    ft1.replace(R.id.content, fragment1,"");
                    ft1.commit();
                    return true;
                case R.id.nav_search:
                    // search fragment transaction
                    actionBar.setTitle("Search");
                    SearchFragment fragment2 = new SearchFragment();
                    FragmentTransaction ft2 = getSupportFragmentManager().beginTransaction();
                    ft2.replace(R.id.content, fragment2,"");
                    ft2.commit();
                    return true;
                case R.id.nav_profile:
                    // profile fragment transaction
                    actionBar.setTitle("Profile");
                    ProfileFragment fragment3 = new ProfileFragment();
                    FragmentTransaction ft3= getSupportFragmentManager().beginTransaction();
                    ft3.replace(R.id.content, fragment3,"");
                    ft3.commit();
                    return true;
                case R.id.nav_notification:
                    // profile fragment transaction
                    actionBar.setTitle("Notifications");
                    NotificationsFragment fragment4 = new NotificationsFragment();
                    FragmentTransaction ft4= getSupportFragmentManager().beginTransaction();
                    ft4.replace(R.id.content, fragment4,"");
                    ft4.commit();
                    return true;
            }
            return false;
        }
    };


    private void checkUserStatus(){
        //Current user
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if(user!=null){
            //Stay logged in.
        }else{
            //User is not sign in we should go to signin activity
            Intent intent = new Intent(DashboardActivity.this,SignInActivity.class);
            startActivity(intent);
            finish();
        }
    }

    /*@Override
    protected void onStart() {
        checkUserStatus();
        super.onStart();
    }*/


}