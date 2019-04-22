package com.reactlibrary;

import android.app.Activity;
import android.app.Application;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactApplicationContext;
import com.facebook.react.bridge.ReactContextBaseJavaModule;
import com.facebook.react.bridge.ReactMethod;
import com.facebook.react.bridge.Promise;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.WritableArray;
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
    static private Application applicationContext;

    private String deviceUid;
    private String deviceToken;

    RNBackendlessModule(ReactApplicationContext context) {
        super(context);

        reactContext = context;
        applicationContext = (Application) context.getApplicationContext();
    }

    private static void sendEvent(String eventName, @Nullable WritableMap params) {
        if (reactContext == null) {
            Log.v(LOG_TAG, "JS code is not running, ignore sending \"" + eventName + "\" event with params: " + params);

        } else {
            Log.v(LOG_TAG, "Send \"" + eventName + "\" event to JS Code, params: " + params);

            reactContext
                    .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                    .emit(eventName, params);
        }
    }

    static void sendNotificationEvent(WritableMap notification) {
        sendEvent("notification", notification);
    }

    static void sendNotificationActionEvent(Bundle notificationBundle) {
        WritableMap notification = RNBackendlessPushNotificationHelper.parseNotificationActionBundle(notificationBundle);

        sendEvent("notificationAction", notification);
    }

    @NonNull
    @Override
    public String getName() {
        return "RNBackendless";
    }

    private WritableMap getDevice() {
        if (deviceToken == null) {
            return null;
        }

        WritableMap device = Arguments.createMap();

        device.putString("version", Build.VERSION.RELEASE);
        device.putString("uuid", deviceUid);
        device.putString("token", deviceToken);

        return device;
    }

    @ReactMethod
    public void registerDevice(final Promise promise) {
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

                    deviceUid = instanceIdResult.getId();
                    deviceToken = instanceIdResult.getToken();

                    promise.resolve(getDevice());

                } catch (Exception e) {
                    Log.e(LOG_TAG, "Can not get FCM Token:", e);

                    promise.reject(new Throwable("Can not get FCM Token: " + e.getMessage()));
                }

            }
        });
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @ReactMethod
    public void getNotifications(final Promise promise) {
        WritableArray notifications = RNBackendlessPushNotificationHelper.getNotifications(applicationContext);

        Log.d(LOG_TAG, "Get Delivered Notifications: " + notifications);

        promise.resolve(notifications);
    }

    @ReactMethod
    public void cancelNotification(final Integer notificationId, final Promise promise) {
        RNBackendlessPushNotificationHelper.cancelNotification(applicationContext, notificationId);

        promise.resolve(null);
    }

    @ReactMethod
    public void cancelAllNotifications(final Promise promise) {
        RNBackendlessPushNotificationHelper.cancelAllNotifications(applicationContext);

        promise.resolve(null);
    }

    @ReactMethod
    public void unregisterDevice(final Promise promise) {
        WritableMap device = getDevice();

        if (device == null) {
            promise.reject(new Throwable("Device is not registered yet!"));
        } else {
            promise.resolve(device);
        }
    }

    @ReactMethod
    public void getInitialNotificationAction(Promise promise) {
        WritableMap notification = null;
        Activity activity = getCurrentActivity();

        if (activity != null) {
            Intent intent = activity.getIntent();

            if (intent.hasExtra("action")) {
                Bundle notificationBundle = intent.getBundleExtra("action");
                notification = RNBackendlessPushNotificationHelper.parseNotificationActionBundle(notificationBundle);
            }
        }

        promise.resolve(notification);
    }

    @ReactMethod
    public void setAppId(String appId) {
        RNBackendlessStorage.setAppId(applicationContext, appId);
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

            RNBackendlessStorage.setPushTemplates(applicationContext, templatesJSON);
            RNBackendlessPushNotificationHelper.deleteNotificationChannels(applicationContext);

            promise.resolve(null);
        }
    }
}