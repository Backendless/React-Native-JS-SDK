package com.reactlibrary;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

public class RNBackendlessPushNotificationTemplate {
    private String name;
    private String contentTitle;
    private String summarySubText;

    private Integer badge;
    private Boolean showBadge;
    private Integer badgeNumber;

    private Integer priority;

    private Integer colorCode;
    private Integer lightsColor;

    private String icon;
    private String largeIcon;
    private String attachmentUrl;

    private String sound;
    private long[] vibrate;

    private Boolean cancelOnTap;
    private Integer cancelAfter;

    private RNBackendlessPushNotificationAction[] actions;

    private HashMap<String, String> customHeaders;

    private Integer contentAvailable;

    RNBackendlessPushNotificationTemplate() {
    }

    RNBackendlessPushNotificationTemplate(JSONObject jsonObject) {

        if (jsonObject.has("name") && !jsonObject.isNull("name")) {
            setName(jsonObject.optString("name"));
        }

        if (jsonObject.has("contentTitle") && !jsonObject.isNull("contentTitle")) {
            setContentTitle(jsonObject.optString("contentTitle"));
        }

        if (jsonObject.has("summarySubText") && !jsonObject.isNull("summarySubText")) {
            setSummarySubText(jsonObject.optString("summarySubText"));
        }

        if (jsonObject.has("badgeNumber") && !jsonObject.isNull("badgeNumber")) {
            setBadgeNumber(jsonObject.optInt("badgeNumber"));
        }

        if (jsonObject.has("priority") && !jsonObject.isNull("priority")) {
            setPriority(jsonObject.optInt("priority"));
        }

        if (jsonObject.has("colorCode") && !jsonObject.isNull("colorCode")) {
            setColorCode(jsonObject.optInt("colorCode"));
        }

        if (jsonObject.has("largeIcon") && !jsonObject.isNull("largeIcon")) {
            setLargeIcon(jsonObject.optString("largeIcon"));
        }

        if (jsonObject.has("icon") && !jsonObject.isNull("icon")) {
            setIcon(jsonObject.optString("icon"));
        }

        if (jsonObject.has("badge") && !jsonObject.isNull("badge")) {
            setBadge(jsonObject.optInt("badge"));
        }

        if (jsonObject.has("attachmentUrl") && !jsonObject.isNull("attachmentUrl")) {
            setAttachmentUrl(jsonObject.optString("attachmentUrl"));
        }

        if (jsonObject.has("showBadge") && !jsonObject.isNull("showBadge")) {
            setShowBadge(jsonObject.optBoolean("showBadge"));
        }

        if (jsonObject.has("sound") && !jsonObject.isNull("sound")) {
            setSound(jsonObject.optString("sound"));
        }

        if (jsonObject.has("cancelOnTap") && !jsonObject.isNull("cancelOnTap")) {
            setCancelOnTap(jsonObject.optBoolean("cancelOnTap"));
        }

        if (jsonObject.has("cancelAfter") && !jsonObject.isNull("cancelAfter")) {
            setCancelAfter(jsonObject.optInt("cancelAfter"));
        }

        if (jsonObject.has("lightsColor") && !jsonObject.isNull("lightsColor")) {
            setLightsColor(jsonObject.optInt("lightsColor"));
        }

        if (jsonObject.has("contentAvailable") && !jsonObject.isNull("contentAvailable")) {
            setContentAvailable(jsonObject.optInt("contentAvailable"));
        }

        if (jsonObject.has("vibrate") && !jsonObject.isNull("vibrate")) {
            JSONArray vibrateArray = jsonObject.optJSONArray("vibrate");

            long[] vibrate = new long[vibrateArray.length()];

            for (int i = 0; i < vibrateArray.length(); i++) {
                vibrate[i] = vibrateArray.optLong(i);
            }

            setVibrate(vibrate);
        }

        if (jsonObject.has("actions") && !jsonObject.isNull("actions")) {
            JSONArray actionsArray = jsonObject.optJSONArray("actions");

            RNBackendlessPushNotificationAction[] actions = new RNBackendlessPushNotificationAction[actionsArray.length()];

            for (int i = 0; i < actionsArray.length(); i++) {
                actions[i] = new RNBackendlessPushNotificationAction(actionsArray.optJSONObject(i));
            }

            setActions(actions);
        }

        if (jsonObject.has("customHeaders") && !jsonObject.isNull("customHeaders")) {
            JSONObject customHeadersObject = jsonObject.optJSONObject("customHeaders");

            HashMap<String, String> customHeadersMap = new HashMap<>();

            for (Iterator<String> it = customHeadersObject.keys(); it.hasNext(); ) {
                String key = it.next();
                String value = customHeadersObject.optString(key);

                customHeadersMap.put(key, value);
            }

            if (!customHeadersMap.isEmpty()) {
                setCustomHeaders(customHeadersMap);
            }
        }
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    String getContentTitle() {
        return contentTitle;
    }

    private void setContentTitle(String contentTitle) {
        this.contentTitle = contentTitle;
    }

    String getSummarySubText() {
        return summarySubText;
    }

    private void setSummarySubText(String summarySubText) {
        this.summarySubText = summarySubText;
    }

    HashMap<String, String> getCustomHeaders() {
        return customHeaders;
    }

    private void setCustomHeaders(HashMap<String, String> customHeaders) {
        this.customHeaders = customHeaders;
    }

    Integer getBadge() {
        return badge;
    }

    private void setBadge(Integer badge) {
        this.badge = badge;
    }

    Integer getBadgeNumber() {
        return badgeNumber;
    }

    private void setBadgeNumber(Integer badgeNumber) {
        this.badgeNumber = badgeNumber;
    }

    public String getIcon() {
        return icon;
    }

    public void setIcon(String icon) {
        this.icon = icon;
    }

    String getLargeIcon() {
        return largeIcon;
    }

    private void setLargeIcon(String largeIcon) {
        this.largeIcon = largeIcon;
    }

    Integer getColorCode() {
        return colorCode;
    }

    private void setColorCode(Integer colorCode) {
        this.colorCode = colorCode;
    }

    Boolean getCancelOnTap() {
        return cancelOnTap;
    }

    private void setCancelOnTap(Boolean cancelOnTap) {
        this.cancelOnTap = cancelOnTap;
    }

    Integer getCancelAfter() {
        return cancelAfter;
    }

    private void setCancelAfter(Integer cancelAfter) {
        this.cancelAfter = cancelAfter;
    }

    String getAttachmentUrl() {
        return attachmentUrl;
    }

    private void setAttachmentUrl(String attachmentUrl) {
        this.attachmentUrl = attachmentUrl;
    }

    Integer getContentAvailable() {
        return contentAvailable;
    }

    private void setContentAvailable(Integer contentAvailable) {
        this.contentAvailable = contentAvailable;
    }

    Boolean getShowBadge() {
        return showBadge;
    }

    private void setShowBadge(Boolean showBadge) {
        this.showBadge = showBadge;
    }

    Integer getPriority() {
        return priority;
    }

    private void setPriority(Integer priority) {
        this.priority = priority;
    }

    String getSound() {
        return sound;
    }

    private void setSound(String sound) {
        this.sound = sound;
    }

    Integer getLightsColor() {
        return lightsColor;
    }

    private void setLightsColor(Integer lightsColor) {
        this.lightsColor = lightsColor;
    }

    long[] getVibrate() {
        return vibrate;
    }

    private void setVibrate(long[] vibrate) {
        this.vibrate = vibrate;
    }

    public RNBackendlessPushNotificationAction[] getActions() {
        return actions;
    }

    public void setActions(RNBackendlessPushNotificationAction[] actions) {
        this.actions = actions;
    }

}
