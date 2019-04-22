package com.reactlibrary;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.bridge.WritableMap;
import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONObject;

import java.util.Map;

import static com.reactlibrary.RNBackendlessHelper.LOG_TAG;

public class RNBackendlessPushNotificationService extends FirebaseMessagingService {
    static RNBackendlessPushNotificationHelper pushNotificationHelper;

    @Override
    public void onMessageReceived(RemoteMessage message) {
        Map<String, String> pushData = message.getData();

        final Bundle bundle = new Bundle();

        for (Map.Entry<String, String> entry : pushData.entrySet()) {
            bundle.putString(entry.getKey(), entry.getValue());
        }

        Log.i(LOG_TAG, "Received a new Push Notification Message: " + bundle);

        new Thread(new Runnable() {
            public void run() {
                handleRemotePushNotification(getApplication(), bundle);
            }
        }).start();
    }

    private RNBackendlessPushNotificationTemplate getTemplate(Application context, Bundle bundle) {
        String immediatePushString = bundle.getString(RNBackendlessPushNotificationMessage.IMMEDIATE_PUSH_KEY);
        String templateName = bundle.getString(RNBackendlessPushNotificationMessage.TEMPLATE_NAME_KEY);

        RNBackendlessPushNotificationTemplate template = null;

        if (immediatePushString != null) {
            JSONObject immediatePushData = RNBackendlessHelper.parseJSON(immediatePushString);

            if (immediatePushData != null) {
                template = new RNBackendlessPushNotificationTemplate(immediatePushData);

                Log.d(LOG_TAG, "built Immediate Push Notification Template: " + template);
            } else {
                Log.e(LOG_TAG, "Can not parse \"" + RNBackendlessPushNotificationMessage.IMMEDIATE_PUSH_KEY + "\": " + immediatePushString);
            }
        }

        if (template == null && templateName != null) {
            template = RNBackendlessStorage.getTemplate(context, templateName);

            Log.d(LOG_TAG, "got stored Push Notification Template: " + template);
        }

        if (template == null) {
            template = new RNBackendlessPushNotificationTemplate();

            Log.d(LOG_TAG, "create empty Push Notification Template: " + template);
        }

        return template;
    }

    private void handleRemotePushNotification(Application context, Bundle bundle) {
        Log.d(LOG_TAG, "Handle Remote Push Notification: " + bundle);

        RNBackendlessPushNotificationTemplate template = getTemplate(context, bundle);

        Log.d(LOG_TAG, "Push notification template: " + template);

        RNBackendlessPushNotificationMessage pushMessage = new RNBackendlessPushNotificationMessage(template, bundle);
        Bundle pushMessageBundle = pushMessage.toBundle();

        WritableMap notification = null;

        if (template.getContentAvailable() == null || template.getContentAvailable() != 1) {
            Log.i(LOG_TAG, "Send Push Notification to Notification Center: " + bundle);

            provideNotificationHelper(context);

            try {
                pushNotificationHelper.sendToNotificationCentre(pushMessage, pushMessageBundle);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to display push notification ", e);
            }

            notification = RNBackendlessPushNotificationMessage.fromBundleToJSObject(pushMessageBundle);
        }

        if (notification != null) {
            RNBackendlessModule.sendNotificationEvent(notification);
        }
    }

    private void provideNotificationHelper(Application context) {
        if (pushNotificationHelper == null) {
            pushNotificationHelper = new RNBackendlessPushNotificationHelper(context);
        }
    }

}
