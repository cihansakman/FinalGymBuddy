package com.example.gymbuddy;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.squareup.picasso.Picasso;

import java.util.List;

public class AdapterUsers extends RecyclerView.Adapter<AdapterUsers.MyHolder>{

    Context context;
    List<ModelUsers> usersList;
    //constructor


    public AdapterUsers(Context context, List<ModelUsers> usersList) {
        this.context = context;
        this.usersList = usersList;
    }

    @NonNull
    @Override
    public MyHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        //inflate layout(row_search)
        View view = LayoutInflater.from(context).inflate(R.layout.row_search, viewGroup,false);

        return new MyHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MyHolder holder, int i) {
        //get data
        final String hisUID = usersList.get(i).getUid();
        String userImage = usersList.get(i).getImageLink();
        String userName = usersList.get(i).getName();
        final String userNick = usersList.get(i).getNickname();

        //set data
        holder.mNameTV.setText(userName);
        holder.mNickTV.setText(userNick);

        try {
            Picasso.get().load(userImage).placeholder(R.drawable.ic_image_person)
                    .into(holder.mAvatarIV);
        }
        catch (Exception e){

        }
        //handle item click
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                //show dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setItems(new String[]{"Profile"}, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        Intent intent = new Intent(context, ThereProfileActivity.class);
                        intent.putExtra("uid",hisUID);
                        context.startActivity(intent);
                    }
                });
                builder.create().show();

            }
        });
        

    }

    @Override
    public int getItemCount() {
        return usersList.size();
    }

    //view holder class
    class MyHolder extends RecyclerView.ViewHolder {

        ImageView mAvatarIV;
        TextView mNameTV, mNickTV;

        public MyHolder(@NonNull View itemView) {
            super(itemView);

            //init views
            mAvatarIV = itemView.findViewById(R.id.avatarIV);
            mNameTV = itemView.findViewById(R.id.nameTV);
            mNickTV = itemView.findViewById(R.id.nickTV);


        }
    }
}
