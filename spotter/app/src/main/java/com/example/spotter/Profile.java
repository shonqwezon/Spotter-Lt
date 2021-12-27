package com.example.spotter;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.makeramen.roundedimageview.RoundedImageView;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class Profile extends AppCompatActivity {
    private static final int GALLERY_REQUEST = 1;
    String url = "http://167.71.5.55:8383";
    String id = null;
    String token = null;

    Button btnChangePass;
    MaterialEditText oldPass, newPass1, newPass2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        btnChangePass = findViewById(R.id.btnChangePass);
        btnChangePass.setClickable(false);

        oldPass = findViewById(R.id.oldPass);
        newPass1 = findViewById(R.id.newPass1);
        newPass2 = findViewById(R.id.newPass2);

        id = ComData.getId();
        token = ComData.getToken();
        Log.d("Settings", ComData.getAvatar());
        if(!ComData.getAvatar().equals("null")) {
            Picasso.get().load(url + "/" + ComData.getAvatar()).into((RoundedImageView) findViewById(R.id.imageProfileSettings));
        }

        findViewById(R.id.downloadImage).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d("App main", "Выбор фотки");
                Intent photoPickerIntent = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(photoPickerIntent, GALLERY_REQUEST);
            }
        });

        findViewById(R.id.buttonBack).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
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

        newPass1.addTextChangedListener(watcher);
        newPass2.addTextChangedListener(watcher);
        oldPass.addTextChangedListener(watcher);
    }


    private void switchPosButton() {
        final String oldPassBtn = oldPass.getText().toString();
        final String newPass1Btn = newPass1.getText().toString();
        final String newPass2Btn = newPass2.getText().toString();
        if(!oldPassBtn.isEmpty() && !newPass1Btn.isEmpty() && !newPass2Btn.isEmpty()) {
            btnChangePass.setTextColor(getResources().getColor(R.color.text_button_active));
            btnChangePass.setBackgroundResource(R.drawable.btn_sign_active);
            btnChangePass.setClickable(true);
            btnChangePass.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if(!newPass1Btn.equals(newPass2Btn)) {
                        Log.d("Settings", newPass1Btn + " " + newPass2Btn);
                        Toast toast =  Toast.makeText(Profile.this, "Пароли не совпадают!", Toast.LENGTH_SHORT);
                        TextView textView = toast.getView().findViewById(android.R.id.message);
                        textView.setTextSize(16);
                        textView.setGravity(Gravity.CENTER);
                        textView.setTextColor(getResources().getColor(R.color.reject));
                        toast.show();
                    }
                    else {
                        OkHttpClient client = new OkHttpClient();
                        RequestBody formBody = new FormBody.Builder()
                                .addEncoded("oldPass", oldPassBtn)
                                .addEncoded("newPass", newPass2Btn)
                                .build();
                        Request request = new Request.Builder()
                                .url(url + "/changePass")
                                .addHeader("id", id)
                                .addHeader("key", token)
                                .post(formBody)
                                .build();
                        client.newCall(request).enqueue(new Callback() {
                            @Override
                            public void onFailure(@NotNull Call call, @NotNull IOException e) { }

                            @Override
                            public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                                if(response.code() == 401) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast toast =  Toast.makeText(Profile.this, "Текущий пароль неверный!", Toast.LENGTH_SHORT);
                                            TextView textView = toast.getView().findViewById(android.R.id.message);
                                            textView.setTextSize(16);
                                            textView.setGravity(Gravity.CENTER);
                                            textView.setTextColor(getResources().getColor(R.color.reject));
                                            toast.show();
                                        }
                                    });
                                }
                                else if(response.isSuccessful()){
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            MainHome.removeService();
                                            Toast toast =  Toast.makeText(Profile.this, "Вы успешно изменили пароль!", Toast.LENGTH_SHORT);
                                            TextView textView = toast.getView().findViewById(android.R.id.message);
                                            textView.setTextSize(16);
                                            textView.setGravity(Gravity.CENTER);
                                            textView.setTextColor(getResources().getColor(R.color.accept));
                                            toast.show();
                                        }
                                    });
                                }
                            }
                        });
                    }
                }
            });
        }
        else { btnChangePass.setClickable(false); btnChangePass.setBackgroundResource(R.drawable.btn_sgin_disable); btnChangePass.setTextColor(getResources().getColor(R.color.text_button_notactive));}
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == GALLERY_REQUEST && resultCode == RESULT_OK && null != data) {
            Uri selectedImage = data.getData();
            String[] filePathColumn = { MediaStore.Images.Media.DATA };
            Cursor cursor = getContentResolver().query(selectedImage,filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);
            cursor.close();
            RoundedImageView imageView = findViewById(R.id.imageProfileSettings);
            imageView.setImageBitmap(BitmapFactory.decodeFile(picturePath));
            File fileSelected = new File(picturePath);
            if(fileSelected.exists()) {
                Log.d("App", "FileSelected is exist");
                OkHttpClient client = new OkHttpClient();
                RequestBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
                        .addFormDataPart("avatar", fileSelected.getName(),
                                RequestBody.create(MediaType.parse("image/*"), fileSelected))
                        .build();
                Request request = new Request.Builder()
                        .url(url + "/avatar")
                        .post(requestBody)
                        .addHeader("id", id)
                        .addHeader("key", token)
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(@NotNull Call call, @NotNull IOException e) {
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                Toast toast =  Toast.makeText(Profile.this, "Ошибка!\nПовторите попытку позже!", Toast.LENGTH_SHORT);
                                TextView textView = toast.getView().findViewById(android.R.id.message);
                                textView.setTextSize(16);
                                textView.setGravity(Gravity.CENTER);
                                textView.setTextColor(getResources().getColor(R.color.reject));
                                toast.show();
                            }
                        });
                    }

                    @Override
                    public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                        if(response.isSuccessful()) {
                            ComData.setAvatar(response.body().string());
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast toast =  Toast.makeText(Profile.this, "Вы успешно установили аватар", Toast.LENGTH_SHORT);
                                    TextView textView = toast.getView().findViewById(android.R.id.message);
                                    textView.setTextSize(16);
                                    textView.setGravity(Gravity.CENTER);
                                    textView.setTextColor(getResources().getColor(R.color.accept));
                                    toast.show();
                                }
                            });
                        }
                        else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast toast =  Toast.makeText(Profile.this, "Ошибка!\nПовторите попытку позже!", Toast.LENGTH_SHORT);
                                    TextView textView = toast.getView().findViewById(android.R.id.message);
                                    textView.setTextSize(16);
                                    textView.setGravity(Gravity.CENTER);
                                    textView.setTextColor(getResources().getColor(R.color.reject));
                                    toast.show();
                                }
                            });
                        }
                    }
                });
            }

            else Log.d("App", "FileSelected is not exist");
        }
    }

}