package com.example.spotter;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class LocationsMap {
    private GoogleMap mMap;
    private Context context;
    private Marker marker;
    private Marker[] markers = new Marker[100];
    Bitmap markerIconRed, markerIconBlue;
    Double latitude, longitude;


    public LocationsMap(GoogleMap googleMap, Context context) {
        this.mMap = googleMap;
        this.context = context;

        mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(60, 57)));


        BitmapDrawable bitmapDrawableRed = (BitmapDrawable) context.getResources().getDrawable(R.drawable.pin_red);
        Bitmap bitmapRed = bitmapDrawableRed.getBitmap();
        markerIconRed = Bitmap.createScaledBitmap(bitmapRed, 78, 78, false);

        BitmapDrawable bitmapDrawableBlue = (BitmapDrawable) context.getResources().getDrawable(R.drawable.pin_blue);
        Bitmap bitmapBlue = bitmapDrawableBlue.getBitmap();
        markerIconBlue = Bitmap.createScaledBitmap(bitmapBlue, 78, 78, false);
    }

    public void myLocal(Double latitude, Double longitude) {
        this.latitude = latitude;
        this.longitude = longitude;
        if (marker == null) {
            LatLng myLatLng = new LatLng(latitude, longitude);
            mMap.moveCamera(CameraUpdateFactory.newLatLng(myLatLng));
            mMap.animateCamera(CameraUpdateFactory.zoomTo(12), 2000, null);
            marker = mMap.addMarker(new MarkerOptions().position(myLatLng).title("Вы").icon(BitmapDescriptorFactory.fromBitmap(markerIconRed)));
        } else {
            marker.remove();
            LatLng myLatLng = new LatLng(latitude, longitude);
            marker = mMap.addMarker(new MarkerOptions().position(myLatLng).title("Вы").icon(BitmapDescriptorFactory.fromBitmap(markerIconRed)));
        }
        marker.showInfoWindow();
    }

    public void showMyLocal() {
        if(latitude != null && longitude != null){
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latitude, longitude), 17), 2000, null);
        }
    }

    public void usersLocal(JSONArray jsonArray) throws JSONException {
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String obj = jsonObject.getString("datetime");
            String time = obj.substring(0, obj.indexOf("T")) + " " + obj.substring(obj.indexOf("T")+1, obj.indexOf("."));
            if (markers[i] == null) {
                LatLng myLatLng = new LatLng(jsonObject.getDouble("lat"), jsonObject.getDouble("long"));
                markers[i] = mMap.addMarker(new MarkerOptions().position(myLatLng).title(jsonObject.getString("user")).snippet(time).icon(BitmapDescriptorFactory.fromBitmap(markerIconBlue)));
            } else {
                markers[i].remove();
                LatLng myLatLng = new LatLng(jsonObject.getDouble("lat"), jsonObject.getDouble("long"));
                markers[i] = mMap.addMarker(new MarkerOptions().position(myLatLng).title(jsonObject.getString("user")).snippet(time).icon(BitmapDescriptorFactory.fromBitmap(markerIconBlue)));
            }
            markers[i].showInfoWindow();
        }
    }

    public void usersShowLocal(int i) {
        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(markers[i].getPosition(), 17), 2000, null);
    }
}