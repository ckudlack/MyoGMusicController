package com.android.myoproject.application;

import android.app.Application;

import com.squareup.otto.Bus;

public class MyoApplication extends Application {

    public static Bus bus;

    @Override
    public void onCreate() {
        super.onCreate();

        bus = new Bus();
    }
}
