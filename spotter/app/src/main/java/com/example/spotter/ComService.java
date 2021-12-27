package com.example.spotter;

import android.location.Location;

import org.json.JSONObject;

public class ComService {
    private static JSONObject object;
    private Location location;

    public ComService(Location location) {
        this.location = location;
    }

    public Location getLocation() {
        return location;
    }

    public void setLocation(Location location) {
        this.location = location;
    }

    public static void setAuth(JSONObject objectAuth) {
        object = objectAuth;
    }

    public static JSONObject getAuth() {
        return object;
    }
}
