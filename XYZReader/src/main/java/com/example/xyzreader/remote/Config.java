package com.example.xyzreader.remote;

import android.util.Log;

import java.net.MalformedURLException;
import java.net.URL;

public class Config {
    public static final URL BASE_URL;
    private static String TAG = Config.class.toString();

    static {
        URL url = null;
        try {
            // Someone in class created this so we can use it instead of slow large file
            // https://s3-us-west-2.amazonaws.com/44d827ed-8529-42b0-98fb-eb9d2afd6d12/data/xyz-reader.json
            url = new URL("https://go.udacity.com/xyz-reader-json" );
        } catch (MalformedURLException ignored) {
            // TODO: throw a real error
            Log.e(TAG, "Please check your internet connection. " + ignored.getMessage());
        }

        BASE_URL = url;
    }
}
