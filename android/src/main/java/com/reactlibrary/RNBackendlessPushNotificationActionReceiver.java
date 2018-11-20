package com.reactlibrary;

import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableMap;

import static com.reactlibrary.RNBackendlessHelper.LOG_TAG;

public class RNBackendlessPushNotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();

        Log.v(LOG_TAG, "RNBackendlessPushNotificationActionReceiver receive an action: " + action);

        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

        WritableMap jsObject = Arguments.createMap();

        jsObject.putString("title", intent.getStringExtra("actionTitle"));
        jsObject.putString("id", intent.getStringExtra("actionId"));
        jsObject.putMap("notification", Arguments.makeNativeMap(intent.getBundleExtra("notification")));

        if (remoteInput != null) {
            String reply = (String) remoteInput.getCharSequence("inline_reply");

            jsObject.putString("inlineReplay", reply);
        }

        RNBackendlessModule.sendNotificationActionEvent(jsObject);
    }
}
