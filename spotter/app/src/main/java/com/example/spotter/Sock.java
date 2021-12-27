package com.example.spotter;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.os.Build;
import android.util.Log;
import android.view.Gravity;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.RequiresApi;

import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.Ack;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;

import org.json.JSONObject;

import java.net.URISyntaxException;


public class Sock {
    private static Socket socket;
    public static Thread thread = null;

    public static void startListenerSock(final MainHome act) {
        Log.d("Socket", "Start thread of listen");
        thread  = new Thread(new Runnable() {
            @Override
            public void run() {
                socket.on("message", new Emitter.Listener() {
                    @Override
                    public void call(Object... args) {
                        Log.d("Socket", args[0].toString());
                        act.showRoot(args[0].toString());
                    }
                });
            }
        });
        thread.start();
    }

    public static void initSock(String userId) {
        Log.d("Socket", "init socket");
        try {
            IO.Options options = new IO.Options();
            options.query = "IDofUser="+userId;
            socket = IO.socket("http://167.71.5.55:8383/", options);
            Log.d("Socket", "OK");
        } catch (URISyntaxException e) {
            e.printStackTrace();
            Log.d("Socket", "FAIL");
        }

        socket.connect();
    }

    public static void reqPermission(final Context context, final MainHome act, final AlertDialog addUser, JSONObject object) {
        Log.d("Socket", "socket.emit");
        socket.emit("rootID", object, new Ack() {
            @Override
            public void call(final Object... args) {
                act.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("Socket", args[0].toString());
                        if(args[0].toString().equals("OK")) {
                            Toast toast =  Toast.makeText(context, "Вы успешно подали заявку!\nОжидайте ответа", Toast.LENGTH_SHORT);
                            TextView textView = toast.getView().findViewById(android.R.id.message);
                            textView.setTextSize(16);
                            textView.setGravity(Gravity.CENTER);
                            textView.setTextColor(act.getResources().getColor(R.color.accept));
                            toast.show();
                            addUser.cancel();
                        }
                        else if(args[0].toString().equals("error_found")){
                            Toast toast =  Toast.makeText(context, "Данный пользователь\nне зарегистрирован", Toast.LENGTH_SHORT);
                            TextView textView = toast.getView().findViewById(android.R.id.message);
                            textView.setTextSize(16);
                            textView.setGravity(Gravity.CENTER);
                            textView.setTextColor(act.getResources().getColor(R.color.reject));
                            toast.show();
                        }
                        else if(args[0].toString().equals("error_exist")){
                            Toast toast =  Toast.makeText(context, "Данный пользователь\nуже добавлен в список", Toast.LENGTH_SHORT);
                            TextView textView = toast.getView().findViewById(android.R.id.message);
                            textView.setTextSize(16);
                            textView.setGravity(Gravity.CENTER);
                            textView.setTextColor(act.getResources().getColor(R.color.reject));
                            toast.show();
                        }
                    }
                });
            }
        });
    }

    public static void responseTo(JSONObject object) {
        socket.emit("responseTo", object);
    }

    public static void deleteUsers(JSONObject object) {
        socket.emit("deletePermiss", object);
    }
}
