package com.reactlibrary;

import android.app.Application;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.InstanceIdResult;

import org.json.JSONException;
import org.json.JSONObject;

import static com.reactlibrary.RNBackendlessHelper.LOG_TAG;

public class RNBackendlessModule extends ReactContextBaseJavaModule {
    static private ReactApplicationContext reactContext;

    RNBackendlessModule(ReactApplicationContext context) {
        super(context);

        reactContext = context;

        JSONObject storedTemplates = RNBackendlessHelper.getPushTemplates(reactContext);

        RNBackendlessPushNotificationTemplates.restoreTemplates(storedTemplates);
    }

    private static void sendEvent(String eventName, @Nullable WritableMap params) {
        Log.v(LOG_TAG, "Send \"" + eventName + "\" event to JS Code, params: " + params);

        reactContext
                .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                .emit(eventName, params);
    }

    static void sendNotificationEvent(WritableMap notification) {
        sendEvent("notification", notification);
    }

    static void sendNotificationActionEvent(WritableMap action) {
        sendEvent("notificationAction", action);
    }

    @Override
    public String getName() {
        return "RNBackendless";
    }

    @ReactMethod
    public void cancelNotification(Integer notificationId, Promise promise) {
        try {
            Application appContext = (Application) reactContext.getApplicationContext();
            RNBackendlessPushNotificationHelper.cancelPushNotification(appContext, notificationId);

            WritableMap result = Arguments.createMap();

            result.putDouble("notificationId", notificationId);

            promise.resolve("Push Notification with id: \"" + notificationId + "\" has been canceled.");

        } catch (Exception e) {
            promise.reject("Can not cancel Push Notification with id: " + notificationId, e);
        }
    }

    @ReactMethod
    public void getDeviceInfo(final Promise promise) {
        FirebaseInstanceId firebaseInstanceId = FirebaseInstanceId.getInstance();
        Task<InstanceIdResult> instanceIdTast = firebaseInstanceId.getInstanceId();

        instanceIdTast.addOnCompleteListener(new OnCompleteListener<InstanceIdResult>() {
            @Override
            public void onComplete(@NonNull Task<InstanceIdResult> task) {
                try {
                    if (!task.isSuccessful()) {
                        throw new Exception(task.getException());
                    }

                    InstanceIdResult instanceIdResult = task.getResult();

                    if (instanceIdResult == null) {
                        throw new Exception("Firebase Instance is null");
                    }

                    final WritableMap result = Arguments.createMap();

                    result.putString("version", Build.VERSION.RELEASE);
                    result.putString("uuid", instanceIdResult.getId());
                    result.putString("token", instanceIdResult.getToken());

                    promise.resolve(result);

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Can not get FCM Token:", e);

                    promise.reject(new Throwable("Can not get FCM Token: " + e.getMessage()));
                }

            }
        });
    }


    @ReactMethod
    public void setAppId(String appId) {
        RNBackendlessHelper.setAppId(reactContext, appId);
    }

    @ReactMethod
    public void setTemplates(ReadableMap templates, Promise promise) {
        JSONObject templatesJSON = null;

        try {
            templatesJSON = RNBackendlessHelper.convertMapToJson(templates);
        } catch (JSONException e) {
            Log.e(LOG_TAG, "Can not parse \"pushTemplates\": " + e.getMessage(), e.getCause());
            promise.reject("Can not parse \"pushTemplates\": " + e.getMessage(), e.getCause());
        }

        if (templatesJSON != null) {
            Log.v(LOG_TAG, "templates: " + templatesJSON);

            RNBackendlessHelper.setPushTemplates(reactContext, templatesJSON);

            RNBackendlessPushNotificationTemplates.restoreTemplates(templatesJSON);

            Application appContext = (Application) reactContext.getApplicationContext();
            RNBackendlessPushNotificationHelper.deleteNotificationChannels(appContext);

            promise.resolve(null);
        }
    }
}