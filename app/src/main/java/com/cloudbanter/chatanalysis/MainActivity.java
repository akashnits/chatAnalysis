package com.cloudbanter.chatanalysis;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;

import co.chatsdk.ui.manager.InterfaceManager;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        InterfaceManager.shared().a.startLoginActivity(this, true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.v("resume", "resumed");
    }
}
