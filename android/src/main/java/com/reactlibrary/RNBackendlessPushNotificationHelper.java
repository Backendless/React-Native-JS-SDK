package com.reactlibrary;

import android.annotation.SuppressLint;
import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.support.annotation.RequiresApi;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.RemoteInput;
import android.util.Log;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.WritableArray;
import com.facebook.react.bridge.WritableMap;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.List;

import static com.reactlibrary.RNBackendlessHelper.LOG_TAG;

class RNBackendlessPushNotificationHelper {
    private Application appContext;
    private Resources resources;
    private String packageName;
    private NotificationManager notificationManager;
    private String backendlessAppId;

    RNBackendlessPushNotificationHelper(Application appContext) {
        this.appContext = appContext;
        this.resources = appContext.getResources();
        this.packageName = appContext.getPackageName();

        this.backendlessAppId = RNBackendlessStorage.getAppId(appContext);
        this.notificationManager = getNotificationManager(appContext);
    }

    static private NotificationManager getNotificationManager(Application appContext) {
        return (NotificationManager) appContext.getSystemService(Context.NOTIFICATION_SERVICE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    static WritableArray getNotifications(Application appContext) {
        WritableArray notifications = Arguments.createArray();
        NotificationManager notificationManager = getNotificationManager(appContext);
        StatusBarNotification[] statusBarNotifications = notificationManager.getActiveNotifications();

        for (StatusBarNotification statusBarNotification : statusBarNotifications) {
            Bundle notificationBundle = statusBarNotification.getNotification().extras.getBundle("notification");

            if (notificationBundle != null) {
                WritableMap notification = RNBackendlessPushNotificationMessage.fromBundleToJSObject(notificationBundle);

                notifications.pushMap(notification);
            }
        }

        return notifications;
    }

    static void cancelAllNotifications(Application appContext) {
        NotificationManager notificationManager = getNotificationManager(appContext);
        notificationManager.cancelAll();
    }

    static void cancelNotification(Application appContext, Integer notificationId) {
        Log.d(LOG_TAG, "Cancel Push Notification with id: " + notificationId);

        NotificationManager notificationManager = getNotificationManager(appContext);

        if (notificationId != null) {
            notificationManager.cancel(notificationId);
        }
    }

    static void deleteNotificationChannels(Application appContext) {
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Log.d(LOG_TAG, "Delete all the notification channels");

            NotificationManager notificationManager = getNotificationManager(appContext);
            List<NotificationChannel> notificationChannels = notificationManager.getNotificationChannels();

            for (NotificationChannel notificationChannel : notificationChannels) {
                notificationManager.deleteNotificationChannel(notificationChannel.getId());
            }
        }
    }

    static WritableMap parseNotificationActionBundle(Bundle bundle) {
        WritableMap notificationAction = Arguments.createMap();

        Bundle notificationBundle = bundle.getBundle("notification");
        String actionId = bundle.getString("actionId");
        String inlineReply = bundle.getString("inlineReply");

        if (notificationBundle != null) {
            notificationAction.putMap("notification", RNBackendlessPushNotificationMessage.fromBundleToJSObject(notificationBundle));
        }

        if (actionId != null) {
            notificationAction.putString("id", actionId);
        }

        if (inlineReply != null) {
            notificationAction.putString("inlineReply", inlineReply);
        }

        return notificationAction;
    }

    private Class getMainActivityClass() {
        Intent notificationIntent = appContext.getPackageManager().getLaunchIntentForPackage(packageName);

        if (notificationIntent == null) {
            return null;
        }

        String className = notificationIntent.getComponent().getClassName();

        try {
            return Class.forName(className);

        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationCompat.Builder createNotification(NotificationChannel notificationChannel) {
        return new NotificationCompat.Builder(appContext, notificationChannel.getId());
    }

    @SuppressLint("RestrictedApi")
    private NotificationCompat.Builder createNotification(RNBackendlessPushNotificationMessage pushMessage) {
        NotificationCompat.Builder notification = new NotificationCompat.Builder(appContext);

        setPriority(pushMessage, notification);
        setSound(pushMessage, notification);
        setVibrate(pushMessage, notification);

        return notification;
    }


    @SuppressLint("RestrictedApi")
    private void setPriority(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        Integer priority = pushMessage.getPriority();

        if (priority != null && priority > 0 && priority < 6) {
            priority = priority - 3;
        } else {
            priority = NotificationCompat.PRIORITY_DEFAULT;
        }

        notification.setPriority(priority);

    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setVibrate(RNBackendlessPushNotificationMessage pushMessage, NotificationChannel notificationChannel) {
        long[] vibrate = pushMessage.getVibrate();

        if (vibrate != null && vibrate.length > 0) {
            notificationChannel.enableVibration(true);
            notificationChannel.setVibrationPattern(vibrate);
        }
    }

    @SuppressLint("RestrictedApi")
    private void setVibrate(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        long[] vibrate = pushMessage.getVibrate();

        if (vibrate != null && vibrate.length > 0 && notification.getPriority() > NotificationCompat.PRIORITY_LOW) {
            notification.setVibrate(vibrate);
        }
    }


    private void setCancellations(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        if (pushMessage.getCancelOnTap() != null) {
            notification.setAutoCancel(pushMessage.getCancelOnTap());
        } else {
            notification.setAutoCancel(false);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (pushMessage.getCancelAfter() != null && pushMessage.getCancelAfter() != 0) {
                notification.setTimeoutAfter(pushMessage.getCancelAfter() * 1000);
            }
        }
    }

    void sendToNotificationCentre(RNBackendlessPushNotificationMessage pushMessage, Bundle pushMessageBundle) {
        Class mainActivityClass = getMainActivityClass();

        if (mainActivityClass == null) {
            Log.e(LOG_TAG, "No activity class found for the notification");
            return;
        }

        if (pushMessage.getId() == null) {
            Log.e(LOG_TAG, "Notification ID is missed");
            return;
        }

        if (pushMessage.getMessage() == null) {
            Log.d(LOG_TAG, "Notification Message is missed");
            return;
        }

        Integer notificationID = pushMessage.getId();

        NotificationCompat.Builder notification = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? createNotification(getOrCreateNotificationChannel(pushMessage))
                : createNotification(pushMessage);

        setContent(pushMessage, notification);
        setCancellations(pushMessage, notification);
        setSmallIcon(pushMessage, notification);
        setLargeIcon(pushMessage, notification);
        setAttachment(pushMessage, notification);
        setBadge(pushMessage, notification);
        setColor(pushMessage, notification);
        setContentIntent(notification, notificationID);
        setActions(pushMessage, notification, notificationID, pushMessageBundle);

        Bundle notificationBundle = new Bundle();
        notificationBundle.putBundle("notification", pushMessageBundle);
        notification.addExtras(notificationBundle);

        Intent intent = new Intent(appContext, RNBackendlessPushNotificationActionReceiver.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("notification", pushMessageBundle);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, notificationID, intent, 0);

        notification.setContentIntent(pendingIntent);

        notificationManager.notify(notificationID, notification.build());
    }

    private void setColor(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        Integer getColorCode = pushMessage.getColorCode();

        if (getColorCode != null) {
            notification.setColor(getColorCode | 0xFF000000);
        }
    }

    private void setContent(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        String message = pushMessage.getMessage();
        String title = pushMessage.getTitle();
        String subtitle = pushMessage.getSubtitle();

        notification.setContentText(message);

        if (title != null) {
            notification.setContentTitle(title);
        }

        if (subtitle != null) {
            notification.setSubText(subtitle);
        }
    }

    private void setContentIntent(NotificationCompat.Builder notification, int notificationID) {
        Intent notificationIntent = appContext.getPackageManager().getLaunchIntentForPackage(packageName);

        if (notificationIntent != null) {
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            PendingIntent contentIntent = PendingIntent.getActivity(appContext, notificationID * 3, notificationIntent, 0);
            notification.setContentIntent(contentIntent);
        }
    }

    private void setActions(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification, int notificationID, Bundle pushMessageBundle) {
        RNBackendlessPushNotificationAction[] actions = pushMessage.getActions();

        if (actions != null) {
            int i = 1;

            for (RNBackendlessPushNotificationAction action : actions) {
                Intent actionIntent = new Intent(appContext, RNBackendlessPushNotificationActionReceiver.class);

                actionIntent.setAction(appContext.getPackageName() + "." + action.getId());

                actionIntent.putExtra("actionId", action.getId());
                actionIntent.putExtra("notification", pushMessageBundle);

                actionIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(appContext, notificationID * 3 + i++, actionIntent, 0);

                NotificationCompat.Action.Builder actionBuilder = new NotificationCompat.Action.Builder(0, action.getTitle(), pendingIntent);

                if (action.getOptions() == 1) {
                    RemoteInput remoteInput = new RemoteInput.Builder("inline_reply").build();

                    actionBuilder
                            .setAllowGeneratedReplies(true)
                            .addRemoteInput(remoteInput);
                }

                notification.addAction(actionBuilder.build());
            }
        }
    }

    private void setAttachment(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        String attachmentUrl = pushMessage.getAttachmentUrl();

        if (attachmentUrl != null) {
            try {
                URL url = new URL(attachmentUrl);
                InputStream inputStream = (InputStream) url.getContent();
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

                if (bitmap != null) {
                    notification.setStyle(new NotificationCompat.BigPictureStyle().bigPicture(bitmap));
                } else {
                    Log.i(RNBackendlessPushNotificationHelper.class.getSimpleName(), "Cannot convert rich media for notification into bitmap.");
                }
            } catch (IOException e) {
                Log.e(RNBackendlessPushNotificationHelper.class.getSimpleName(), "Cannot receive rich media for notification.");
            }

        } else {
            String message = pushMessage.getMessage();

            if (message.length() > 35) {
                String title = pushMessage.getTitle();
                String subtitle = pushMessage.getSubtitle();

                NotificationCompat.BigTextStyle bigText = new NotificationCompat.BigTextStyle()
                        .setBigContentTitle(title)
                        .setSummaryText(subtitle)
                        .bigText(message);

                notification.setStyle(bigText);
            }
        }
    }

    private void setBadge(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        Integer badgeNumber = pushMessage.getBadgeNumber();
        Integer badge = pushMessage.getBadge();

        if (badgeNumber != null) {
            notification.setNumber(badgeNumber);
        }

        if (badge != null && (badge == NotificationCompat.BADGE_ICON_SMALL || badge == NotificationCompat.BADGE_ICON_LARGE)) {
            notification.setBadgeIconType(badge);
        } else {
            notification.setBadgeIconType(NotificationCompat.BADGE_ICON_NONE);
        }
    }

    private void setLargeIcon(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        String largeIcon = pushMessage.getLargeIcon();

        if (largeIcon != null) {
            if (largeIcon.startsWith("http")) {
                try {
                    InputStream is = (InputStream) new URL(largeIcon).getContent();
                    Bitmap bitmap = BitmapFactory.decodeStream(is);

                    if (bitmap != null)
                        notification.setLargeIcon(bitmap);
                    else
                        Log.i(RNBackendlessPushNotificationHelper.class.getSimpleName(), "Cannot convert Large Icon into bitmap.");
                } catch (IOException e) {
                    Log.e(RNBackendlessPushNotificationHelper.class.getSimpleName(), "Cannot receive bitmap for Large Icon.");
                }
            } else {
                int largeIconResource = appContext.getResources().getIdentifier(largeIcon, "drawable", appContext.getPackageName());

                if (largeIconResource != 0) {
                    Bitmap bitmap = BitmapFactory.decodeResource(appContext.getResources(), largeIconResource);
                    notification.setLargeIcon(bitmap);
                }
            }
        }
    }

    private void setSmallIcon(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        int smallIcon = 0;

        // try to get icon from template
        if (pushMessage.getIcon() != null) {
            smallIcon = appContext.getResources().getIdentifier(pushMessage.getIcon(), "mipmap", appContext.getPackageName());

            if (smallIcon == 0) {
                smallIcon = appContext.getResources().getIdentifier(pushMessage.getIcon(), "drawable", appContext.getPackageName());
            }
        }

        // try to get default icon
        if (smallIcon == 0) {
            smallIcon = appContext.getApplicationInfo().icon;

            if (smallIcon == 0) {
                smallIcon = android.R.drawable.sym_def_app_icon;
            }
        }

        notification.setSmallIcon(smallIcon);
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private NotificationChannel getOrCreateNotificationChannel(RNBackendlessPushNotificationMessage pushMessage) {
        final String templateName = pushMessage.getTemplateName();

        String channelName = templateName;

        if (templateName == null) {
            channelName = "Fallback";
        }

        final String channelId = backendlessAppId + "-" + channelName;

        NotificationChannel notificationChannel = notificationManager.getNotificationChannel(channelId);

        if (notificationChannel == null) {
            notificationChannel = new NotificationChannel(channelId, channelName, NotificationManager.IMPORTANCE_DEFAULT);

            setShowBadge(pushMessage, notificationChannel);
            setPriority(pushMessage, notificationChannel);
            setSound(pushMessage, notificationChannel);
            setLightsColor(pushMessage, notificationChannel);
            setVibrate(pushMessage, notificationChannel);

            notificationManager.createNotificationChannel(notificationChannel);
        }

        return notificationChannel;
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setShowBadge(RNBackendlessPushNotificationMessage pushMessage, NotificationChannel notificationChannel) {
        if (pushMessage.getShowBadge() != null) {
            notificationChannel.setShowBadge(pushMessage.getShowBadge());
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setLightsColor(RNBackendlessPushNotificationMessage pushMessage, NotificationChannel notificationChannel) {
        if (pushMessage.getLightsColor() != null) {
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(pushMessage.getLightsColor() | 0xFF000000);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setPriority(RNBackendlessPushNotificationMessage pushMessage, NotificationChannel notificationChannel) {
        Integer priority = pushMessage.getPriority();

        if (priority != null && priority > 0 && priority < 6) {
            notificationChannel.setImportance(priority);
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.O)
    private void setSound(RNBackendlessPushNotificationMessage pushMessage, NotificationChannel notificationChannel) {
        String sound = pushMessage.getSound();

        if (sound != null) {
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .build();

            notificationChannel.setSound(getSoundUri(sound), audioAttributes);
        }
    }

    @SuppressLint("RestrictedApi")
    private void setSound(RNBackendlessPushNotificationMessage pushMessage, NotificationCompat.Builder notification) {
        Integer priority = notification.getPriority();
        String sound = pushMessage.getSound();

        if (priority > NotificationCompat.PRIORITY_LOW) {
            notification.setSound(getSoundUri(sound), AudioManager.STREAM_NOTIFICATION);
        }
    }

    private Uri getSoundUri(String resource) {
        Uri soundUri;

        if (resource != null && !resource.isEmpty()) {
            int soundResource = resources.getIdentifier(resource, "raw", packageName);
            soundUri = Uri.parse(ContentResolver.SCHEME_ANDROID_RESOURCE + "://" + packageName + "/" + soundResource);
        } else
            soundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);

        return soundUri;
    }

}
