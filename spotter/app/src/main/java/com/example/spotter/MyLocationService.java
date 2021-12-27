package com.example.spotter;

import android.app.ActivityManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.greenrobot.eventbus.EventBus;
import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;


public class MyLocationService extends Service {

    private static final String CHANNEL_ID = "my_channel";

    private final IBinder mBinder = new LocalBinder();
    private static final float UPDATE_MIN_DISTANCE = 15;
    private static final long UPDATE_INTERVAL = 20000;
    private static final long FASTEST_INTERVAL = UPDATE_INTERVAL/2;
    private static final int NOTI_ID = 1223;
    private boolean mChangingConfiguration = false;
    private NotificationManager mNotificationManager;
    private LocationRequest locationRequest;
    private FusedLocationProviderClient fusedLocationProviderClient;
    LocationCallback locationCallback;
    private Handler mServiceHandler;
    private Location mLocation;
    private String id;
    private String token;
    String url = "http://167.71.5.55:8383";

    public MyLocationService() { }

    @Override
    public void onCreate() {
        Log.d("App", "onCreate()");
        try {
            this.id = ComService.getAuth().getString("id");
            this.token = ComService.getAuth().getString("token");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                super.onLocationResult(locationResult);
                onNewLocation(locationResult.getLastLocation());
            }
        };
        createLocationRequest();
        getLastLocation();
        HandlerThread handlerThread = new HandlerThread("SpotterDev");
        handlerThread.start();
        mServiceHandler = new Handler(handlerThread.getLooper());
        mNotificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT);
            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("App", "onStartCommand");
        try {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.myLooper());
            Log.d("App", "requestLocationUpdates()");
        } catch (SecurityException ex) {
            Log.e("App", "Lost location permission. Could not request it " + ex);
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        Log.d("App", "onConfigurationChanged");
        super.onConfigurationChanged(newConfig);
        mChangingConfiguration = true;
    }

    private void getLastLocation() {
        try {
            fusedLocationProviderClient.getLastLocation()
                    .addOnCompleteListener(new OnCompleteListener<Location>() {
                        @Override
                        public void onComplete(@NonNull Task<Location> task) {
                            if(task.isSuccessful() && task.getResult() != null)
                                mLocation = task.getResult();
                            else Log.e("App", "Failed to get location");
                        }
                    });
            Log.d("App", "getLastLocation()");
        } catch (SecurityException ex) {
            Log.e("App", "Lost location permission. "+ex);
        }
    }

    private void createLocationRequest() {
        Log.d("App", "createLocationRequest()");
        locationRequest = new LocationRequest();
        locationRequest.setSmallestDisplacement(UPDATE_MIN_DISTANCE);
        locationRequest.setInterval(UPDATE_INTERVAL);
        locationRequest.setFastestInterval(FASTEST_INTERVAL);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void onNewLocation(Location lastLocation) {
        Log.d("App", "onNewLocation");
        mLocation = lastLocation;
        Log.d("App", mLocation.getLatitude() + " "+ mLocation.getLongitude());
        EventBus.getDefault().postSticky(new ComService(mLocation));
        sendLocationToServer(mLocation);
        if(serviceIsRunningInForeGround(this)) {
            Log.d("App", "serviceIsRunningInForeGround");
            mNotificationManager.notify(NOTI_ID, getNotification());
        }
    }

    private Notification getNotification() {
        Log.d("App", "getNotification()");
        String text = getLocationText(mLocation);


        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setContentText(text)
                .setContentTitle(String.format("Location Updated: %1$s", DateFormat.getDateInstance().format(new Date())))
                .setOngoing(true)
                .setPriority(Notification.PRIORITY_HIGH)
                .setSmallIcon(R.mipmap.ic_launcher_custom_foreground)
                .setTicker(text)
                .setWhen(System.currentTimeMillis());

        if(Build.VERSION.SDK_INT >=Build.VERSION_CODES.O) {
            builder.setChannelId(CHANNEL_ID);
        }
        return builder.build();
    }

    private String getLocationText(Location mLocation) {
        return mLocation == null ? "Unknown Location, check GPS state" : new StringBuilder()
                .append(mLocation.getLongitude())
                .append("\n")
                .append(mLocation.getLatitude())
                .toString();
    }

    private boolean serviceIsRunningInForeGround(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for(ActivityManager.RunningServiceInfo service: manager.getRunningServices(Integer.MAX_VALUE))
            if(getClass().getName().equals(service.service.getClassName()))
                if(service.foreground)
                    return true;
        return false;
    }

    public class LocalBinder extends Binder {
        MyLocationService getService() {return MyLocationService.this;}
    }


    @Override
    public IBinder onBind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration = false;
        Log.d("App", "onBind");
        return mBinder;
    }

    @Override
    public void onRebind(Intent intent) {
        stopForeground(true);
        mChangingConfiguration = false;
        Log.d("App", "onRebind");
        super.onRebind(intent);
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if(!mChangingConfiguration) startForeground(NOTI_ID, getNotification());
        Log.d("App", "onUnbind");
        return true;
    }

    @Override
    public void onDestroy() {
        mServiceHandler.removeCallbacks(null);
        Log.d("App", "onDestroy()");
        super.onDestroy();
    }

    public void removeLocationUpdates() {
        try {
            Log.d("App", "removeLocationUpdates()");
            fusedLocationProviderClient.removeLocationUpdates(locationCallback);
            stopSelf();
        } catch (SecurityException ex) {
            Log.e("App", "Lost location permission. Could not remove updates. "+ex);
        }
    }


    public void sendLocationToServer(Location location) {
        if(location != null) {
            OkHttpClient client = new OkHttpClient();
            JSONObject object = new JSONObject();
            try {
                object.put("lat", location.getLatitude());
                object.put("long", location.getLongitude());
            } catch (JSONException ex) {
                Log.d("App", "Fail json: "+ex);
            }
            MediaType mediaType = MediaType.parse("application/json");
            RequestBody body = RequestBody.create(String.valueOf(object), mediaType);
            Request request = new Request.Builder()
                    .url(url + "/location.send")
                    .addHeader("id", id)
                    .addHeader("key", token)
                    .post(body)
                    .build();
            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(@NotNull Call call, @NotNull IOException e) {
                    Log.d("App", "fail send");
                }

                @Override
                public void onResponse(@NotNull Call call, @NotNull Response response) throws IOException {
                    Log.d("App", String.valueOf(response.code()));
                }
            });
        }
    }
}