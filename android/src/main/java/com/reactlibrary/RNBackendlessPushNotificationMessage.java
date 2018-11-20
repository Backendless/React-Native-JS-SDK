package com.reactlibrary;

import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class RNBackendlessPushNotificationMessage {
    static final String IMMEDIATE_PUSH_KEY = "android_immediate_push";
    static final String TEMPLATE_NAME_KEY = "template_name";

    private static final String ID_KEY = "id";

    private static final String MESSAGE_KEY = "message";
    private static final String TITLE_KEY = "title";
    private static final String SUBTITLE_KEY = "subtitle";

    private static final String ANDROID_CONTENT_TITLE_KEY = "android-content-title";
    private static final String ANDROID_SUMMARY_SUBTEXT_KEY = "android-summary-subtext";

    private RNBackendlessPushNotificationTemplate template;

    private Bundle contentBundle = new Bundle();
    private HashMap<String, String> customHeaders = new HashMap<>();

    RNBackendlessPushNotificationMessage(RNBackendlessPushNotificationTemplate template, Bundle bundle) {
        this.template = template;

        if (template.getCustomHeaders() != null && !template.getCustomHeaders().isEmpty()) {
            customHeaders = template.getCustomHeaders();

            for (Map.Entry<String, String> header : customHeaders.entrySet()) {
                contentBundle.putString(header.getKey(), header.getValue());
            }
        }

        if (bundle.getString(ID_KEY) == null) {
            Random randomNumberGenerator = new Random(System.currentTimeMillis());

            contentBundle.putInt(ID_KEY, randomNumberGenerator.nextInt());
        }

        contentBundle.putString(MESSAGE_KEY, bundle.getString(MESSAGE_KEY));
        contentBundle.putString(TITLE_KEY, bundle.getString(ANDROID_CONTENT_TITLE_KEY, template.getContentTitle()));
        contentBundle.putString(SUBTITLE_KEY, bundle.getString(ANDROID_SUMMARY_SUBTEXT_KEY, template.getSummarySubText()));
    }

    Integer getId() {
        return contentBundle.getInt(ID_KEY);
    }

    String getMessage() {
        return contentBundle.getString(MESSAGE_KEY);
    }

    String getTitle() {
        return contentBundle.getString(TITLE_KEY);
    }

    String getSubtitle() {
        return contentBundle.getString(SUBTITLE_KEY);
    }

    Boolean getCancelOnTap() {
        return template.getCancelOnTap();
    }

    Integer getCancelAfter() {
        return template.getCancelAfter();
    }

    Integer getBadgeNumber() {
        return template.getBadgeNumber();
    }

    String getIcon() {
        return template.getIcon();
    }

    String getLargeIcon() {
        return template.getLargeIcon();
    }

    String getAttachmentUrl() {
        return template.getAttachmentUrl();
    }

    Integer getBadge() {
        return template.getBadge();
    }

    Integer getColorCode() {
        return template.getColorCode();
    }

    String getTemplateName() {
        return template.getName();
    }

    RNBackendlessPushNotificationAction[] getActions() {
        return template.getActions();
    }

    long[] getVibrate() {
        return template.getVibrate();
    }

    Integer getPriority() {
        return template.getPriority();
    }

    String getSound() {
        return template.getSound();
    }

    Boolean getShowBadge() {
        return template.getShowBadge();
    }

    Integer getLightsColor() {
        return template.getLightsColor();
    }

    WritableMap toJSObject() {
        WritableMap jsObject = Arguments.createMap();

        jsObject.putString("message", getMessage());
        jsObject.putString("title", getTitle());
        jsObject.putString("subtitle", getSubtitle());

        jsObject.putInt("id", getId());
        jsObject.putString("templateName", getTemplateName());

        jsObject.putInt("badgeType", getBadge());
        jsObject.putBoolean("showBadge", getShowBadge());
        jsObject.putInt("badgeNumber", getBadgeNumber());

        jsObject.putInt("priority", getPriority());

        jsObject.putInt("colorCode", getColorCode());
        jsObject.putInt("lightsColor", getLightsColor());

        jsObject.putString("icon", getIcon());
        jsObject.putString("largeIcon", getLargeIcon());
        jsObject.putString("attachmentUrl", getAttachmentUrl());

        jsObject.putString("sound", getSound());

        WritableArray vibrateArray = Arguments.createArray();

        for (long v : (long[]) getVibrate()) {
            vibrateArray.pushDouble(v);
        }

        jsObject.putArray("vibrate", vibrateArray);

        jsObject.putBoolean("cancelOnTap", getCancelOnTap());
        jsObject.putInt("cancelAfter", getCancelAfter());

        WritableArray actionsArray = Arguments.createArray();

        for (RNBackendlessPushNotificationAction v : getActions()) {
            Bundle action = new Bundle();
            action.putString("id", v.getId());
            action.putString("title", v.getTitle());
            action.putBoolean("inlineReplay", v.getOptions() == 1);

            actionsArray.pushMap(Arguments.makeNativeMap(action));
        }

        jsObject.putArray("actions", actionsArray);

        WritableMap customHeadersMap = Arguments.createMap();

        for (String key : customHeaders.keySet()) {
            customHeadersMap.putString(key, customHeaders.get(key));
        }

        jsObject.putMap("customHeaders", customHeadersMap);
        jsObject.putBoolean("contentAvailable", template.getContentAvailable() == 1);

        return jsObject;
    }

    public Bundle customHeadersToIntentBundle() {
        Bundle intentBundle = new Bundle();

        for (String key : customHeaders.keySet()) {
            intentBundle.putString(key, customHeaders.get(key));
        }

        return intentBundle;
    }
}
