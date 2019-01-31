package com.onetrust.mobileConsentCollectionDemo;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import java.util.UUID;

import com.adobe.marketing.mobile.*;
import com.onetrust.mobileConsentCollection.otsdk_adobe;


public class mobileConsentCollectionDemo extends Application {

    private static final String TAG = "mobileConsentCollection";

    @Override
    public void onCreate() {
        super.onCreate();

        MobileCore.setApplication(this);

        try {

            // Create a random guid to use when identifying this datasubject.
            SharedPreferences settings =  this.getSharedPreferences("com.onetrust.consent.sdk", Context.MODE_PRIVATE);
            String RandomGUID = settings.getString("RandomGUID","");
            if (RandomGUID.isEmpty()){
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("RandomGUID", UUID.randomUUID().toString() );
                editor.commit();
            }

            // If you are creating your own method to udentify a datasubject, do it here.
            String SupplyOwn = settings.getString("SupplyOwn","");
            if (RandomGUID.isEmpty()){
                SharedPreferences.Editor editor = settings.edit();
                editor.putString("SupplyOwn", "AppSuppliedDatasubjectID" );
                editor.commit();
            }


            // init adobe stuff
            MobileCore.setLogLevel(LoggingMode.VERBOSE);
            UserProfile.registerExtension();
            Identity.registerExtension();
            Lifecycle.registerExtension();
            Signal.registerExtension();

            MobileCore.start(new AdobeCallback() {
                @Override
                public void call(Object o) {
                    MobileCore.configureWithAppID("launch-EN7551bc0548254078a00b56fcdc9660ac-development");
                    MobileCore.lifecycleStart(null);
                    registerMyExtension();
                }
            });



        } catch (Exception e) {
            //Log the exception
            Log.e(TAG, String.format("An error occurred in the Application onCreate method %s",  e.toString()));

        }
    }

    private void registerMyExtension() {

        MobileCore.setApplication(this);

        ExtensionErrorCallback<ExtensionError> errorCallback = new ExtensionErrorCallback<ExtensionError>() {
            @Override
            public void error(final ExtensionError extensionError) {
                Log.e(TAG, String.format("An error occurred while registering otsdk_adobe Extension %d %s", extensionError.getErrorCode(), extensionError.getErrorName()));
            }
        };

        if (!MobileCore.registerExtension(otsdk_adobe.class, errorCallback)) {
            Log.e(TAG, "Failed to register otsdk_adobe Extension");
        }
    }
}
