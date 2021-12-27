package com.example.spotter;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.text.InputFilter;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.navigation.NavigationView;
import com.makeramen.roundedimageview.RoundedImageView;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.squareup.picasso.Picasso;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MainHome extends FragmentActivity implements OnMapReadyCallback {
    static final int PERMISSION_REQUEST = 100;
    String[] PERMISSIONS = null;
    String url = "http://167.71.5.55:8383";
    String login = null;
    String id = null;
    String token = null;
    List<User1> userList1;
    List<User2> userList2;
    LocationsMap locationsMap = null;
    Timer timer = null;
    JSONArray jsonArray = null;

    static MyLocationService mService = null;
    boolean mBound = false;

    private final ServiceConnection mServiceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            MyLocationService.LocalBinder binder = (MyLocationService.LocalBinder) iBinder;
            mService = binder.getService();
            mBound = true;
            Log.d("App", "onServiceConnected");
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mService = null;
            mBound = false;
            Log.d("App", "onServiceDisconnected");
        }
    };


    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_home);
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_BACKGROUND_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        }
        else{
            PERMISSIONS = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION};
        }
        requestPermissions(PERMISSIONS, PERMISSION_REQUEST);
    }

    public void DefaultAct() {
        try {
            JSONObject objAuth = new JSONObject();
            objAuth.put("id", ComData.getId());
            objAuth.put("token", ComData.getToken());
            ComService.setAuth(objAuth);
            login = ComData.getLogin();
            Log.d("App main", ComData.getAvatar());
            id = ComData.getId();
            token = ComData.getToken();
        } catch (JSONException e) {
            e.printStackTrace();
        }

        startService(new Intent(getApplicationContext(), MyLocationService.class));
        usersLocationsListener();
        Sock.startListenerSock(MainHome.this);

        Log.d("App main", "Start default act");
        final DrawerLayout drawerLayout = findViewById(R.id.drawerLayout);
        NavigationView navigationView = findViewById(R.id.navigationView);
        navigationView.setItemIconTintList(null);

        findViewById(R.id.btnShowLocal).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(locationsMap != null) {
                    locationsMap.showMyLocal();
                }
            }
        });

        findViewById(R.id.btnShowLocal).setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                if(jsonArray != null) {
                    String[] object = new String[jsonArray.length()];
                    for (int i = 0; i < jsonArray.length(); i++) {
                        try {
                            JSONObject jsonObject = jsonArray.getJSONObject(i);
                            object[i] = jsonObject.getString("user");
                        } catch (JSONException e) {

                            e.printStackTrace();
                        }
                    }
                    AlertDialog.Builder searchUser = new AlertDialog.Builder(MainHome.this);
                    searchUser.setTitle("Пользователи");
                    searchUser.setItems(object, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if(locationsMap != null){
                                locationsMap.usersShowLocal(which);
                            }
                        }
                    });
                    searchUser.show();
                }
                else {
                    Toast toast =  Toast.makeText(MainHome.this, "Список пуст", Toast.LENGTH_SHORT);
                    TextView textView = toast.getView().findViewById(android.R.id.message);
                    textView.setTextSize(16);
                    textView.setGravity(Gravity.CENTER);
                    textView.setTextColor(getResources().getColor(R.color.reject));
                    toast.show();
                }
                return false;
            }
        });

        findViewById(R.id.imageMenu).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                drawerLayout.openDrawer(GravityCompat.START);
            }
        });
        drawerLayout.addDrawerListener(new DrawerLayout.SimpleDrawerListener() {
            @Override
            public void onDrawerOpened(View drawerView) {
            super.onDrawerOpened(drawerView);
            Log.d("App main", "Open navigation");
            if(!ComData.getAvatar().equals("null")) {Picasso.get().load(url + "/" + ComData.getAvatar()).into((RoundedImageView) findViewById(R.id.imageProfile));}
            TextView userId = findViewById(R.id.userId);
            userId.setText(login);

            findViewById(R.id.add_user).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Log.d("App main", "Добавление юзера");
                    final AlertDialog addUser = new AlertDialog.Builder(MainHome.this).create();
                    View addUserWindow = LayoutInflater.from(MainHome.this).inflate(R.layout.adduser, null);

                    final MaterialEditText editAddUser = addUserWindow.findViewById(R.id.userNameField);
                    editAddUser.setFilters(new InputFilter[]{new InputFilter.LengthFilter(15)});

                    addUserWindow.findViewById(R.id.buttonAddUser).setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if(!editAddUser.getText().toString().isEmpty()){
                                if(!editAddUser.getText().toString().equals(login)){
                                    JSONObject object = new JSONObject();
                                    try {
                                        object.put("IDmain", login);
                                        object.put("IDsub", editAddUser.getText().toString());
                                    } catch (JSONException e) {
                                        e.printStackTrace();
                                    }
                                    Sock.reqPermission(MainHome.this, MainHome.this, addUser, object);
                                }
                                else {
                                    Toast toast =  Toast.makeText(MainHome.this, "Так не работает", Toast.LENGTH_SHORT);
                                    TextView textView = toast.getView().findViewById(android.R.id.message);
                                    textView.setTextSize(16);
                                    textView.setGravity(Gravity.CENTER);
                                    textView.setTextColor(getResources().getColor(R.color.reject));
                                    toast.show();
                                }
                            }
                            else {
                                Toast toast =  Toast.makeText(MainHome.this, "Введите ID", Toast.LENGTH_SHORT);
                                TextView textView = toast.getView().findViewById(android.R.id.message);
                                textView.setTextSize(16);
                                textView.setGravity(Gravity.CENTER);
                                textView.setTextColor(getResources().getColor(R.color.reject));
                                toast.show();
                            }
                        }
                    });
                    addUser.setView(addUserWindow);
                    addUser.show();
                }
            });
            }
        });

            findViewById(R.id.uTrack).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    OkHttpClient client = new OkHttpClient();
                    Request request = new Request.Builder()
                            .url(url + "/getUsersSub")
                            .addHeader("id", id)
                            .addHeader("key", token)
                            .addHeader("login", login)
                            .post(RequestBody.create(new byte[0]))
                            .build();
                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            call.cancel();
                        }

                        @Override
                        public void onResponse(Call call, final Response response) {
                            MainHome.this.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    if (response.isSuccessful()) {
                                        try {
                                            userList1 = new ArrayList<>();
                                            String res = response.body().string();
                                            String res1 = res.substring(res.indexOf("[")+1, res.lastIndexOf("]"));
                                            String[] users = res1.split(",");
                                            for(String element : users) {
                                                Log.d("App", element);
                                                JSONObject user = new JSONObject(element);
                                                userList1.add(new User1(user.getString("user_sub")));
                                            }
                                            final AlertDialog.Builder addUser = new AlertDialog.Builder(MainHome.this);
                                            addUser.setTitle("Пользователи");
                                            addUser.setAdapter(new MyListAdapter1(MainHome.this, R.layout.list_item_1, userList1, login), null);
                                            addUser.show();
                                        } catch (IOException | JSONException e) {
                                            e.printStackTrace();
                                            Toast toast =  Toast.makeText(MainHome.this, "Список пуст", Toast.LENGTH_SHORT);
                                            TextView textView = toast.getView().findViewById(android.R.id.message);
                                            textView.setTextSize(16);
                                            textView.setGravity(Gravity.CENTER);
                                            textView.setTextColor(getResources().getColor(R.color.reject));
                                            toast.show();
                                        }
                                    }
                                }
                            });
                        }
                    });
                }
            });

        findViewById(R.id.uBeingTracked).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url + "/getUsersMain")
                        .addHeader("id", id)
                        .addHeader("key", token)
                        .addHeader("login", login)
                        .post(RequestBody.create(new byte[0]))
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        call.cancel();
                    }

                    @Override
                    public void onResponse(Call call, final Response response) {
                        MainHome.this.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (response.isSuccessful()) {
                                    try {
                                        userList2 = new ArrayList<>();
                                        String res = response.body().string();
                                        String res1 = res.substring(res.indexOf("[")+1, res.lastIndexOf("]"));
                                        String[] users = res1.split(",");
                                        for(String element : users) {
                                            Log.d("App", element);
                                            JSONObject user = new JSONObject(element);
                                            userList2.add(new User2(user.getString("user_main")));
                                        }
                                        final AlertDialog.Builder addUser = new AlertDialog.Builder(MainHome.this);
                                        addUser.setTitle("Пользователи");
                                        addUser.setAdapter(new MyListAdapter2(MainHome.this, R.layout.list_item_2, userList2, login), null);
                                        addUser.show();
                                    } catch (IOException | JSONException e) {
                                        e.printStackTrace();
                                        Toast toast =  Toast.makeText(MainHome.this, "Список пуст", Toast.LENGTH_SHORT);
                                        TextView textView = toast.getView().findViewById(android.R.id.message);
                                        textView.setTextSize(16);
                                        textView.setGravity(Gravity.CENTER);
                                        textView.setTextColor(getResources().getColor(R.color.reject));
                                        toast.show();
                                    }
                                }
                            }
                        });
                    }
                });
            }
        });

        findViewById(R.id.profile).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainHome.this, Profile.class));
            }
        });

        findViewById(R.id.settings).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(MainHome.this, Settings.class));
            }
        });
    }

    public static void removeService() {
        mService.removeLocationUpdates();
    }

    @Override
    public void onMapReady(final GoogleMap googleMap) {
        locationsMap = new LocationsMap(googleMap, MainHome.this);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST) {
            if (hasAllPermissionsGranted(grantResults)) {
                Log.d("App", "ALL GOOD");
                bindService(new Intent(MainHome.this, MyLocationService.class), mServiceConnection, Context.BIND_AUTO_CREATE);
                DefaultAct();

            } else {
                Log.d("App", "BAD");
                AlertDialog.Builder error_window = new AlertDialog.Builder(MainHome.this);
                error_window.setMessage("Необходимо предоставить оба разрешения приложению!\nВ противном случае приложение будет некорректно работать")
                        .setCancelable(false)
                        .setPositiveButton("ОК", new DialogInterface.OnClickListener() {
                            @RequiresApi(api = Build.VERSION_CODES.M)
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                                requestPermissions(PERMISSIONS, PERMISSION_REQUEST);
                            }
                        })
                        .setTitle("Ошибка!").create().show();
            }
        } else {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    private void usersLocationsListener() {
        Log.d("App","Start usersLocationsListener()");
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                OkHttpClient client = new OkHttpClient();
                Request request = new Request.Builder()
                        .url(url + "/location.get")
                        .addHeader("id", id)
                        .addHeader("key", token)
                        .addHeader("login", login)
                        .get()
                        .build();
                client.newCall(request).enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        call.cancel();
                    }

                    @Override
                    public void onResponse(Call call, final Response response) {
                        if(response.isSuccessful()) {
                            if(locationsMap != null) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        try {
                                            String res = response.body().string();
                                            jsonArray = new JSONArray(res);
                                            locationsMap.usersLocal(jsonArray);
                                        } catch (JSONException | IOException e) {
                                            e.printStackTrace();
                                        }
                                    }
                                });
                            }
                        }
                    }
                });
            }
        }, 0, 25000);
    }

    public boolean hasAllPermissionsGranted(@NonNull int[] grantResults) {
        for (int grantResult : grantResults) {
            Log.d("App", String.valueOf(grantResult));
            if (grantResult == PackageManager.PERMISSION_DENIED) {
                return false;
            }
        }
        return true;
    }

    public void showRoot(final String userTrack) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Log.d("Emit main", "Started showRoot");
                final LinearLayout part1 = findViewById(R.id.part1);
                final LinearLayout part2 = findViewById(R.id.part2);
                TextView textView = findViewById(R.id.userTrack);
                Button btn1 = findViewById(R.id.btn1);
                Button btn2 = findViewById(R.id.btn2);

                final ConstraintLayout showRootWindow = findViewById(R.id.showRootWindow);
                if (part1.getVisibility() == View.GONE && part2.getVisibility() == View.GONE) {
                    textView.setText(userTrack);
                    part1.setVisibility(View.VISIBLE);
                    part2.setVisibility(View.VISIBLE);
                    final Animation show = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.show_root);
                    showRootWindow.startAnimation(show);

                    btn1.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            try {
                                JSONObject object = new JSONObject();
                                object.put("IDmain", userTrack);
                                object.put("IDsub", login);
                                Sock.responseTo(object);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                            final Animation show = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.hide_root);
                            show.setAnimationListener(new Animation.AnimationListener() {

                                @Override
                                public void onAnimationStart(Animation animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    part1.setVisibility(View.GONE);
                                    part2.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {
                                }

                            });
                            showRootWindow.startAnimation(show);
                        }
                    });

                    btn2.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            final Animation show = AnimationUtils.loadAnimation(getApplicationContext(), R.anim.hide_root);
                            show.setAnimationListener(new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {
                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    part1.setVisibility(View.GONE);
                                    part2.setVisibility(View.GONE);
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {
                                }
                            });
                            showRootWindow.startAnimation(show);
                        }
                    });
                }
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        Log.d("App", "onStart()");
        EventBus.getDefault().register(this);
        if(id != null && token != null && login != null) {
            usersLocationsListener();
        }
    }

    @Override
    protected void onStop() {
        Log.d("App", "onStop()");
        if(mBound) {
            Log.d("App", "onStop() mBound true");
            unbindService(mServiceConnection);
            mBound = false;
        }
        EventBus.getDefault().unregister(this);
        if(timer != null) {
            Log.d("App","Stop usersLocationsListener()");
            timer.cancel();
        }
        super.onStop();
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    public void onListenLocation(ComService event) {
        if(event != null && locationsMap != null) {
            locationsMap.myLocal(event.getLocation().getLatitude(), event.getLocation().getLongitude());
        }
    }
}
