package com.android.myoproject.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.android.myoproject.R;
import com.android.myoproject.background.MusicControllerService;

public class StartServiceActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_start_service);

        startService(new Intent(this, MusicControllerService.class));
    }

}
