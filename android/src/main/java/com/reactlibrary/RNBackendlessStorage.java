package com.reactlibrary;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import static com.reactlibrary.RNBackendlessHelper.LOG_TAG;

class RNBackendlessStorage {
    final static private String SHARED_PREFERENCES_KEY = "RNBackendlessPreferences";
    final static private String PUSH_TEMPLATES_KEY = "PushTemplates";
    final static private String APP_ID_KEY = "AppId";

    private static HashMap<String, RNBackendlessPushNotificationTemplate> pushTemplates;

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_KEY, Context.MODE_PRIVATE);
    }

    static String getAppId(Context context) {
        return getSharedPreferences(context).getString(APP_ID_KEY, "");
    }

    static void setAppId(Context context, String appId) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(APP_ID_KEY, appId);
        editor.apply();
    }

    private static void restorePushTemplates(Context context) {
        String str = getSharedPreferences(context).getString(PUSH_TEMPLATES_KEY, "");
        JSONObject templates;

        try {
            templates = new JSONObject(str);
        } catch (JSONException e) {
            Log.e(LOG_TAG, e.getMessage(), e.getCause());

            templates = new JSONObject();
        }

        setPushTemplates(context, templates);
    }

    static void setPushTemplates(Context context, JSONObject templates) {
        Log.i(LOG_TAG, "Set Push Templates: " + templates);

        Iterator<String> iterator = templates.keys();

        pushTemplates = new HashMap<>();

        while (iterator.hasNext()) {
            String templateName = iterator.next();

            try {
                Log.i(LOG_TAG, "Initialize Push Notification Template: " + templates);

                pushTemplates.put(templateName, new RNBackendlessPushNotificationTemplate(templates.getJSONObject(templateName)));

            } catch (Exception e) {
                Log.e(LOG_TAG, "Can not initialize Push Notification Template: " + e.getMessage(), e.getCause());
            }
        }

        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putString(PUSH_TEMPLATES_KEY, templates.toString());
        editor.apply();
    }

    static RNBackendlessPushNotificationTemplate getTemplate(Context context, String templateName) {
        if (pushTemplates == null) {
            restorePushTemplates(context);
        }

        return pushTemplates.get(templateName);
    }

}
