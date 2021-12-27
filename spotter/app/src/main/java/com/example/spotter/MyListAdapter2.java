package com.example.spotter;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.List;

public class MyListAdapter2 extends ArrayAdapter<User2> {
    List<User2> userList;
    Context context;
    int resource;
    String loginSub;

    //constructor initializing the values
    public MyListAdapter2(Context context, int resource, List<User2> userList, String loginSub) {
        super(context, resource, userList);
        this.context = context;
        this.resource = resource;
        this.userList = userList;
        this.loginSub = loginSub;
    }

    //this will return the ListView Item as a View
    @NonNull
    @Override
    public View getView(final int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        //we need to get the view of the xml for our list item
        //And for this we need a layoutinflater
        LayoutInflater layoutInflater = LayoutInflater.from(context);

        //getting the view
        @SuppressLint("ViewHolder") View view = layoutInflater.inflate(resource, null, false);

        //getting the view elements of the list from the view
        TextView textViewName = view.findViewById(R.id.userName2);
        ImageView buttonDelete = view.findViewById(R.id.buttonDelete2);

        //getting the hero of the specified position
        final User2 user = userList.get(position);

        //adding values to the list item
        textViewName.setText(user.getName());

        //adding a click listener to the button to remove item from the list
        buttonDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                removeUser(position, user.getName());
            }
        });

        //finally returning the view
        return view;
    }

    //this method will remove the item from the list
    private void removeUser(final int position, final String login) {
        //Creating an alert dialog to confirm the deletion
        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
        alertDialog.setMessage("Вы точно хотите запретить данному пользователю отслеживать вас?")

                //if the response is positive in the alert
                .setPositiveButton("Да", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        JSONObject objPermiss = new JSONObject();
                        try {
                            objPermiss.put("IDmain", login);
                            objPermiss.put("IDsub", loginSub);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        Sock.deleteUsers(objPermiss);
                        Log.d("MyListAdapter2 remove", login);
                        userList.remove(position);
                        notifyDataSetChanged();
                        dialogInterface.cancel();
                    }
                })

                //if response is negative nothing is being done
                .setNegativeButton("Нет", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.cancel();
                    }
                }).create().show();
    }
}