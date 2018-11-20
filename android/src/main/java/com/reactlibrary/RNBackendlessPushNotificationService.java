package com.reactlibrary;

import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContext;
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
                ReactInstanceManager mReactInstanceManager = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
                ReactContext context = mReactInstanceManager.getCurrentReactContext();

                if (context != null) {
                    handleRemotePushNotification((ReactApplicationContext) context, bundle);

                } else {
                    mReactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                        public void onReactContextInitialized(ReactContext context) {
                            handleRemotePushNotification((ReactApplicationContext) context, bundle);
                        }
                    });

                    if (!mReactInstanceManager.hasStartedCreatingInitialContext()) {
                        mReactInstanceManager.createReactContextInBackground();
                    }
                }
            }
        }).start();
    }

    private JSONObject parseJSON(String dataString) {
        try {
            return new JSONObject(dataString);
        } catch (Exception e) {
            e.printStackTrace();

            return null;
        }
    }

    private RNBackendlessPushNotificationTemplate getTemplate(Bundle bundle) {
        String immediatePushString = bundle.getString(RNBackendlessPushNotificationMessage.IMMEDIATE_PUSH_KEY);
        String templateName = bundle.getString(RNBackendlessPushNotificationMessage.TEMPLATE_NAME_KEY);

        if (immediatePushString != null) {
            JSONObject immediatePushData = parseJSON(immediatePushString);

            if (immediatePushData != null) {
                return new RNBackendlessPushNotificationTemplate(immediatePushData);
            } else {
                Log.e(LOG_TAG, "Can not parse \"" + RNBackendlessPushNotificationMessage.IMMEDIATE_PUSH_KEY + "\": " + immediatePushString);
            }
        }

        if (templateName != null) {
            return RNBackendlessPushNotificationTemplates.getTemplate(templateName);
        }

        return new RNBackendlessPushNotificationTemplate();
    }

    private void handleRemotePushNotification(ReactApplicationContext context, Bundle bundle) {
        RNBackendlessPushNotificationTemplate template = getTemplate(bundle);
        RNBackendlessPushNotificationMessage pushMessage = new RNBackendlessPushNotificationMessage(template, bundle);

        if (template.getContentAvailable() == null || template.getContentAvailable() != 1) {
            Log.i(LOG_TAG, "Send Push Notification to Notification Center: " + bundle);

            provideNotificationHelper(context);

            try {
                pushNotificationHelper.sendToNotificationCentre(pushMessage);
            } catch (Exception e) {
                Log.e(LOG_TAG, "Failed to send push notification", e);
            }
        }

        RNBackendlessModule.sendNotificationEvent(pushMessage.toJSObject());
    }

    private void provideNotificationHelper(ReactApplicationContext context) {
        if (pushNotificationHelper == null) {
            Application applicationContext = (Application) context.getApplicationContext();

            pushNotificationHelper = new RNBackendlessPushNotificationHelper(applicationContext);
        }
    }

}
