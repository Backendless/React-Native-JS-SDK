package com.reactlibrary;

import android.util.Log;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import static com.reactlibrary.RNBackendlessHelper.LOG_TAG;

class RNBackendlessPushNotificationTemplates {
    private static HashMap<String, RNBackendlessPushNotificationTemplate> templates = new HashMap<>();

    static void restoreTemplates(JSONObject templatesJSONObject) {
        Iterator<String> iterator = templatesJSONObject.keys();

        while (iterator.hasNext()) {
            String templateName = iterator.next();

            try {
                JSONObject templateJSONObject = templatesJSONObject.getJSONObject(templateName);
                Log.i(LOG_TAG, "Parse stored Push Template JSON Object: " + templateJSONObject);

                RNBackendlessPushNotificationTemplate template = new RNBackendlessPushNotificationTemplate(templateJSONObject);

                templates.put(templateName, template);

            } catch (Exception e) {
                Log.e(LOG_TAG, "Can not initialize Push Notification Template: " + e.getMessage(), e.getCause());
            }
        }
    }

    static RNBackendlessPushNotificationTemplate getTemplate(String templateName) {
        return templates.get(templateName);
    }
}
