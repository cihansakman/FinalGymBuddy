package com.example.gymbuddy;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
//author : Osman Batuhan Şahin


public class NotificationsFragment extends Fragment {

    //recycler view
    RecyclerView notificationsRv;

    private FirebaseAuth firebaseAuth;

    private ArrayList<ModelNotification> notificationsList;
    private  AdapterNotification adapterNotification;

    public NotificationsFragment() {
        // Required empty public constructor
    }


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        //init recyclerview
        notificationsRv = view.findViewById(R.id.notificationsRv);

        notificationsRv.setHasFixedSize(true);
        notificationsRv.setLayoutManager(new LinearLayoutManager(getActivity()));


        firebaseAuth = FirebaseAuth.getInstance();
        
        getAllNotifications();
        
        return view;
    }

    private void getAllNotifications() {
        notificationsList = new ArrayList<>();

        DatabaseReference ref = FirebaseDatabase.getInstance().getReference("UserInfos");
        ref.child(firebaseAuth.getUid()).child("Notifications")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        notificationsList.clear();
                        for (DataSnapshot ds : snapshot.getChildren()){
                            //get data
                            ModelNotification model = ds.getValue(ModelNotification.class);
                            //add t list
                            notificationsList.add(model);

                        }
                        //adapter
                        adapterNotification = new AdapterNotification(getActivity(),notificationsList);
                        //set recycler view
                        notificationsRv.setAdapter(adapterNotification);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {

                    }
                });
    }
}