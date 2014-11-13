package com.android.myoproject;

import android.app.Application;

import com.squareup.otto.Bus;

import api.api.impl.GoogleMusicAPI;


public class MyoApplication extends Application {

    public static GoogleMusicAPI API = new GoogleMusicAPI();
    public static Bus bus;

    public static enum MusicField {
        TRACK, ARTIST, ALBUM
    }

    @Override
    public void onCreate() {
        super.onCreate();

        bus = new Bus();
    }

    public static GoogleMusicAPI getAPI() {
        return API;
    }

    public static Bus getBus() {
        return bus;
    }
}
