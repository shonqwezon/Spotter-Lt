package com.example.spotter;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class RestorePass extends AppCompatActivity {
    String url = "http://167.71.5.55:8383";
    Button btnRestore;
    EditText EditLoginRestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_restore_pass);

        btnRestore = findViewById(R.id.btnRestorePass);
        btnRestore.setClickable(false);

        EditLoginRestore = findViewById(R.id.loginforrestore);
        EditLoginRestore.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});

        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
            startActivity(new Intent(RestorePass.this, Authentication.class));
            finish();
            }
        });


        TextWatcher watcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                final String loginRes = EditLoginRestore.getText().toString();
                if(!loginRes.isEmpty()) {
                    btnRestore.setTextColor(getResources().getColor(R.color.text_button_active));
                    btnRestore.setBackgroundResource(R.drawable.btn_sign_active);
                    btnRestore.setClickable(true);
                    btnRestore.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            OkHttpClient client = new OkHttpClient();
                            Request request = new Request.Builder()
                                    .url(url + "/restorePass")
                                    .addHeader("login", loginRes)
                                    .post(RequestBody.create(new byte[0]))
                                    .build();
                            client.newCall(request).enqueue(new Callback() {
                                @Override
                                public void onFailure(@NotNull Call call, @NotNull IOException e) { }

                                @Override
                                public void onResponse(@NotNull Call call, @NotNull final Response response) throws IOException {
                                    RestorePass.this.runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (response.isSuccessful()) {
                                                showAlertDialog("Приложение", "Новый пароль отправлен вам на электронную почту", true);
                                            }
                                            else if(response.code() == 404) {
                                                showAlertDialog("Ошибка!", "Данный пользователь не зарегестрирован", false);
                                            }

                                            else if(response.code() == 400) {
                                                showAlertDialog("Ошибка!", "Извените, ошибка на стороне сервера\nПовторите попытку позжеЫ ", false);
                                            }
                                        }
                                    });
                                }
                            });
                        }
                    });
                }
                else { btnRestore.setClickable(false); btnRestore.setBackgroundResource(R.drawable.btn_sgin_disable); btnRestore.setTextColor(getResources().getColor(R.color.text_button_notactive));}
            }

            @Override
            public void afterTextChanged(Editable s) { }
        };

        EditLoginRestore.addTextChangedListener(watcher);
    }

    @Override
    public void onBackPressed(){
        startActivity(new Intent(RestorePass.this, Authentication.class));
        finish();
    }

    private void showAlertDialog(String title, String message, final boolean mode) {
        AlertDialog.Builder error_window = new AlertDialog.Builder(RestorePass.this);
        error_window.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if(mode){
                            startActivity(new Intent(RestorePass.this, Authentication.class));
                            finish();
                        }
                    }
                })
                .setTitle(title).create().show();
    }
}