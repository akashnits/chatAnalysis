package com.cloudbanter.chatanalysis;

import android.content.Context;
import android.support.multidex.MultiDex;
import android.support.multidex.MultiDexApplication;

import co.chatsdk.contact.ContactBookModule;
import co.chatsdk.core.session.ChatSDK;
import co.chatsdk.core.session.Configuration;
import co.chatsdk.firebase.FirebaseModule;
import co.chatsdk.firebase.file_storage.FirebaseFileStorageModule;
import co.chatsdk.ui.manager.UserInterfaceModule;


public class ChatApplication extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();

        Context context = getApplicationContext();

        // Create a new configuration
        Configuration.Builder builder = new Configuration.Builder(context);

        // Perform any configuration steps (optional)
        builder.firebaseRootPath("prod");

        // Initialize the Chat SDK
        ChatSDK.initialize(builder.build());
        UserInterfaceModule.activate(context);

        // Activate the Firebase module
        FirebaseModule.activate();

        // File storage is needed for profile image upload and image messages
        FirebaseFileStorageModule.activate();

        ContactBookModule.activateForFirebase();
    }
}
