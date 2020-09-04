package com.reactlibrary;

import android.app.RemoteInput;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import static com.reactlibrary.RNBackendlessHelper.LOG_TAG;

public class RNBackendlessPushNotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

        Log.d(LOG_TAG, "RNBackendlessPushNotificationActionReceiver receive an action: " + intent.getExtras());

        Bundle actionBundle = new Bundle();

        actionBundle.putBundle("notification", intent.getBundleExtra("notification"));
        actionBundle.putString("actionId", intent.getStringExtra("actionId"));

        Bundle remoteInput = RemoteInput.getResultsFromIntent(intent);

        if (remoteInput != null) {
            String inlineReply = (String) remoteInput.getCharSequence("inline_reply");

            actionBundle.putString("inlineReply", inlineReply);
        }

        Intent startIntent = context
                .getPackageManager()
                .getLaunchIntentForPackage(context.getPackageName());

        if (startIntent != null) {
            startIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startIntent.putExtra("action", actionBundle);

            context.startActivity(startIntent);
        }

        RNBackendlessModule.sendNotificationActionEvent(actionBundle);
    }
}
