package com.example.spotter;

import androidx.annotation.NonNull;
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

import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Registry extends AppCompatActivity {

    String url = "http://167.71.5.55:8383";
    Button btnSignUp;
    EditText EditLoginReg, EditPassReg, EditEmailReg;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_registry);

        btnSignUp = findViewById(R.id.btnSignUp);
        btnSignUp.setClickable(false);

        EditEmailReg = findViewById(R.id.EditEmailReg);
        EditLoginReg = findViewById(R.id.EditLoginReg);
        EditPassReg = findViewById(R.id.EditPassReg);
        EditLoginReg.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});
        EditPassReg.setFilters(new InputFilter[]{new InputFilter.LengthFilter(30)});
        EditEmailReg.setFilters(new InputFilter[]{new InputFilter.LengthFilter(50)});

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

        EditLoginReg.addTextChangedListener(watcher);
        EditPassReg.addTextChangedListener(watcher);
        EditEmailReg.addTextChangedListener(watcher);
    }

    private void switchPosButton() {
        String loginBtn = EditLoginReg.getText().toString();
        String passBtn = EditPassReg.getText().toString();
        String emailBtn = EditEmailReg.getText().toString();
        if(!loginBtn.isEmpty() && !passBtn.isEmpty() && !emailBtn.isEmpty()) {
            btnSignUp.setTextColor(getResources().getColor(R.color.text_button_active));
            btnSignUp.setBackgroundResource(R.drawable.btn_sign_active);
            btnSignUp.setClickable(true);
            btnSignUp.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    final String login = EditLoginReg.getText().toString();
                    final String pass = EditPassReg.getText().toString();
                    final String email = EditEmailReg.getText().toString();
                    reg(login, pass, email);
                }
            });
        }
        else { btnSignUp.setClickable(false); btnSignUp.setBackgroundResource(R.drawable.btn_sgin_disable); btnSignUp.setTextColor(getResources().getColor(R.color.text_button_notactive));}
    }

    @Override
    public void onBackPressed(){
        startActivity(new Intent(Registry.this, Authentication.class));
        finish();
    }

    private void showAlertDialog(String title, String message, final boolean mode) {
        AlertDialog.Builder error_window = new AlertDialog.Builder(Registry.this);
        error_window.setMessage(message)
                .setCancelable(false)
                .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                        if(mode){
                            startActivity(new Intent(Registry.this, Authentication.class));
                            finish();
                        }
                    }
                })
                .setTitle(title).create().show();
    }

    private void reg(String login, String pass, String email) {
        OkHttpClient client = new OkHttpClient();
        RequestBody formBody = new FormBody.Builder()
                .addEncoded("email", email)
                .addEncoded("login", login)
                .addEncoded("pass", pass)
                .build();
        Request request = new Request.Builder()
                .url(url + "/reg")
                .post(formBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                call.cancel();
            }

            @Override
            public void onResponse(Call call, final Response response) throws IOException {
                Registry.this.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("App reg", String.valueOf(response.code()));
                        if (response.code() == 200) {
                            showAlertDialog("Приложение", "Вы успешно зарегистрировались", true);
                        } else {
                            try {
                                String myResponseBody = response.body().string();
                                Log.d("App reg", "Registry " + myResponseBody);
                                if (myResponseBody.equals("authentication_email_key")){showAlertDialog("Ошибка", "Данный электронный адрес уже зарегистрирован", false);}
                                if(myResponseBody.equals("authentication_login_key")){showAlertDialog("Ошибка", "Данный логин уже зарегистрирован", false);}
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                });
            }
        });
    }
}