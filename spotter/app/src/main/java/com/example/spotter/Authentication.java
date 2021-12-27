package com.example.spotter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Authentication extends AppCompatActivity {

    String url = "http://167.71.5.55:8383";
    Button btnSignIn, btnGoSignUp;
    EditText EditLoginAuth, EditPassAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_authentication);

        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignIn.setClickable(false);
        btnGoSignUp = findViewById(R.id.btnGoSignUp);

        EditLoginAuth = findViewById(R.id.EditLoginAuth);
        EditPassAuth = findViewById(R.id.EditPassAuth);
        EditLoginAuth.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
        EditPassAuth.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});

        btnGoSignUp.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(Authentication.this, Registry.class));
            }
        });

        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                switchPosButton();
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        EditLoginAuth.addTextChangedListener(watcher);
        EditPassAuth.addTextChangedListener(watcher);
    }

    private void switchPosButton() {
        String loginBtn = EditLoginAuth.getText().toString();
        String passBtn = EditPassAuth.getText().toString();
        if(!loginBtn.isEmpty() && !passBtn.isEmpty()) {
            btnSignIn.setTextColor(getResources().getColor(R.color.text_button_active));
            btnSignIn.setBackgroundResource(R.drawable.btn_sign_active);
            btnSignIn.setClickable(true);
            btnSignIn.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String login = EditLoginAuth.getText().toString();
                    final String pass = EditPassAuth.getText().toString();
                    auth(login, pass);
                }
            });
        }
        else { btnSignIn.setClickable(false); btnSignIn.setBackgroundResource(R.drawable.btn_sgin_disable); btnSignIn.setTextColor(getResources().getColor(R.color.text_button_notactive));}
    }

    private void auth(String login, String pass)  {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .addEncoded("login", login)
                .addEncoded("pass", pass)
                .build();
        Request request = new Request.Builder()
                .url(url + "/auth")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                final String myResponseBody = response.body().string();
                Authentication.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("App", String.valueOf(response.code()));
                        Log.d("App", (myResponseBody));

                        if(response.isSuccessful()) {
                            try {
                                new File(getFilesDir() + "/auth.txt").createNewFile();
                                FileOutputStream fos = openFileOutput("auth.txt", MODE_PRIVATE);
                                fos.write(myResponseBody.getBytes());
                                fos.flush();
                                fos.close();
                                Log.d("App", getFilesDir() + "/auth.txt");
                                JSONObject obj = new JSONObject(myResponseBody);
                                Sock.initSock(obj.getString("id"));
                                Log.d("App", "Good");
                                startActivity(new Intent(Authentication.this, MainActivity.class));
                                finish();
                            } catch (IOException | JSONException e) {
                                e.printStackTrace();
                                Log.d("App", "Запись не удалась");
                            }
                        }
                        else {
                            AlertDialog.Builder error_window = new AlertDialog.Builder(Authentication.this);
                            error_window.setMessage("Пароль или логин неверный")
                                    .setCancelable(false)
                                    .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .setNeutralButton("Не помню пароль", new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            startActivity(new Intent(Authentication.this, RestorePass.class));
                                        }
                                    })
                                    .setTitle("Ошибка").create().show();
                        }
                    }
                });
            }
        });
    }
}