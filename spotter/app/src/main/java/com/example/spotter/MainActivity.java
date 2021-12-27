package com.example.spotter;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    String url = "http://167.71.5.55:8383";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if(new File(getFilesDir() + "/auth.txt").exists()) {
            Log.d("App check", "Файл есть");
            try {
                FileInputStream inStream = openFileInput("auth.txt");
                byte[] bytes = new byte[inStream.available()];
                inStream.read(bytes);
                String text = new String(bytes);
                inStream.close();
                JSONObject obj = new JSONObject(text);
                Log.d("App check", "Всё ок " + text);
                String id = obj.getString("id");
                String token = obj.getString("token");
                getInfo(id, token);
            } catch (IOException | JSONException e) {
                e.printStackTrace();
                Log.d("App check", "Прочитать файл не удалось");
            }
        }
        else {
            Log.d("App check", "Файла нет");
            startActivity(new Intent(MainActivity.this, Authentication.class));
            finish();
        }
    }

    private void getInfo(final String id, final String token) {
        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .url(url + "/getInfo")
                .addHeader("id", id)
                .addHeader("key", token)
                .post(RequestBody.create(new byte[0]))
                .build();
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, final Response response) {
                MainActivity.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (response.isSuccessful()) {
                            Sock.initSock(id);
                            try {
                                JSONObject infoUser = new JSONObject(response.body().string());
                                JSONObject object = new JSONObject();
                                object.put("id", id);
                                object.put("token", token);
                                object.put("login", infoUser.getString("login"));
                                object.put("avatar", infoUser.getString("avatar"));
                                new ComData(object);
                                startActivity(new Intent(MainActivity.this, MainHome.class));
                                finish();
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        else {
                            Log.d("App check", "Файла неверный");
                            new File(getFilesDir() + "/auth.txt").delete();
                            startActivity(new Intent(MainActivity.this, Authentication.class));
                            finish();
                        }
                    }
                });
            }
        });
    }
}