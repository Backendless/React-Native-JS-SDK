package com.reactlibrary;

import android.os.Bundle;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReadableMap;
import com.facebook.react.bridge.ReadableMapKeySetIterator;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
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

            for (Map.Entry<String, String> header : template.getCustomHeaders().entrySet()) {
                String key = header.getKey();
                String value = bundle.containsKey(key) ? bundle.getString(key) : header.getValue();

                customHeaders.put(key, value);
                contentBundle.putString(key, value);
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

    Bundle toBundle() {
        Bundle bundle = new Bundle();

        if (getId() != null) {
            bundle.putInt("id", getId());
        }

        if (getMessage() != null) {
            bundle.putString("message", getMessage());
        }

        if (getTitle() != null) {
            bundle.putString("title", getTitle());
        }

        if (getSubtitle() != null) {
            bundle.putString("subtitle", getSubtitle());
        }

        if (getTemplateName() != null) {
            bundle.putString("templateName", getTemplateName());
        }

        if (getBadge() != null) {
            bundle.putInt("badgeType", getBadge());
        }

        if (getShowBadge() != null) {
            bundle.putBoolean("showBadge", getShowBadge());
        }

        if (getBadgeNumber() != null) {
            bundle.putInt("badgeNumber", getBadgeNumber());
        }

        if (getPriority() != null) {
            bundle.putInt("priority", getPriority());
        }

        if (getColorCode() != null) {
            bundle.putInt("colorCode", getColorCode());
        }

        if (getLightsColor() != null) {
            bundle.putInt("lightsColor", getLightsColor());
        }

        if (getIcon() != null) {
            bundle.putString("icon", getIcon());
        }

        if (getLargeIcon() != null) {
            bundle.putString("largeIcon", getLargeIcon());
        }

        if (getAttachmentUrl() != null) {
            bundle.putString("attachmentUrl", getAttachmentUrl());
        }

        if (getSound() != null) {
            bundle.putString("sound", getSound());
        }

        if (getVibrate() != null) {
            bundle.putLongArray("vibrate", getVibrate());
        }

        if (getCancelOnTap() != null) {
            bundle.putBoolean("cancelOnTap", getCancelOnTap());
        }

        if (getCancelAfter() != null) {
            bundle.putInt("cancelAfter", getCancelAfter());
        }

        if (getActions() != null) {
            Bundle actionsList = new Bundle();

            for (RNBackendlessPushNotificationAction v : getActions()) {
                Bundle action = new Bundle();
                action.putString("id", v.getId());
                action.putString("title", v.getTitle());
                action.putBoolean("inlineReply", v.getOptions() == 1);

                actionsList.putBundle(v.getId(), action);
            }

            bundle.putBundle("actions", actionsList);
        }

        if (customHeaders != null) {
            Bundle customHeadersMap = new Bundle();

            for (String key : customHeaders.keySet()) {
                customHeadersMap.putString(key, customHeaders.get(key));
            }

            bundle.putBundle("customHeaders", customHeadersMap);
        }

        if (template.getContentAvailable() != null) {
            bundle.putBoolean("contentAvailable", template.getContentAvailable() == 1);
        }

        return bundle;
    }

    static WritableMap fromBundleToJSObject(Bundle bundle) {
        WritableMap pushMessage = Arguments.makeNativeMap(bundle);

        if (pushMessage.hasKey("actions")) {
            ReadableMap actionsMap = pushMessage.getMap("actions");

            if (actionsMap != null) {
                ReadableMapKeySetIterator iterator = actionsMap.keySetIterator();
                WritableArray actionsArray = Arguments.createArray();

                while (iterator.hasNextKey()) {
                    WritableMap action = Arguments.createMap();
                    action.merge(Objects.requireNonNull(actionsMap.getMap(iterator.nextKey())));
                    actionsArray.pushMap(action);
                }

                pushMessage.putArray("actions", actionsArray);
            }
        }

        return pushMessage;
    }
}
