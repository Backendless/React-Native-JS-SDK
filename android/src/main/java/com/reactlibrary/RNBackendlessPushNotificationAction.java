package com.reactlibrary;

import org.json.JSONObject;

public class RNBackendlessPushNotificationAction {
    private String id;
    private String title;
    private Integer options;

    RNBackendlessPushNotificationAction(JSONObject jsonObject) {
        if (jsonObject.has("id")) {
            setId(jsonObject.optString("id"));
        }

        if (jsonObject.has("title")) {
            setTitle(jsonObject.optString("title"));
        }

        if (jsonObject.has("options")) {
            setOptions(jsonObject.optInt("options"));
        }
    }

    public String getId() {
        return this.id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return this.title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    Integer getOptions() {
        return this.options;
    }

    private void setOptions(Integer options) {
        this.options = options;
    }

}
