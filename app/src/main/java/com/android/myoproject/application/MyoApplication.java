package com.android.myoproject.application;

import android.app.Application;

import com.squareup.otto.Bus;

public class MyoApplication extends Application {

    public static Bus bus;

    public static enum MusicField {
        TRACK, ARTIST, ALBUM
    }

    @Override
    public void onCreate() {
        super.onCreate();

        bus = new Bus();
    }

    public static Bus getBus() {
        return bus;
    }
}
