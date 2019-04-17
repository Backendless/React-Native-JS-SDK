import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import Backendless from 'backendless';

const RNBackendless = NativeModules.RNBackendless;
const RNBackendlessEmitter = new NativeEventEmitter(RNBackendless);

const _initApp = Backendless.initApp;
const _registerDevice = Backendless.Messaging.registerDevice;
const _unregisterDevice = Backendless.Messaging.unregisterDevice;

Backendless.initApp = function (appId, apiKey) {
  RNBackendless.setAppId(appId);

  return _initApp.apply(this, arguments)
};

Backendless.Messaging.registerDevice = async function (deviceToken, channels, expiration) {
  if (Array.isArray(deviceToken)) {
    expiration = channels;
    channels = deviceToken;
    deviceToken = null
  }

  return Promise.resolve()
    .then(async () => {
      const device = await RNBackendless.registerDevice();

      Backendless.setupDevice({
        uuid    : device.uuid,
        version : device.version,
        platform: Platform.OS,
      });

      const deviceRegistration = await _registerDevice.call(this, deviceToken || device.token, channels, expiration);

      const pushTemplates = await Backendless.Messaging.getPushTemplates(Platform.OS);

      await RNBackendless.setTemplates(pushTemplates);

      return deviceRegistration
    })
};

Backendless.Messaging.unregisterDevice = async function (deviceUid) {
  return Promise.resolve()
    .then(async () => {
      Backendless.setupDevice({
        uuid    : deviceUid,
        version : device.version,
        platform: Platform.OS,
      });

      await RNBackendless.unregisterDevice();

      return _unregisterDevice.call(this)
    })
};

Backendless.Messaging.getInitialNotification = () => {
  return RNBackendless.getInitialNotification();
};

Backendless.Messaging.addPushNotificationListener = callback => {
  RNBackendlessEmitter.addListener('notification', callback);
};

Backendless.Messaging.removePushNotificationListener = callback => {
  RNBackendlessEmitter.removeListener('notification', callback);
};

Backendless.Messaging.addPushNotificationActionListener = callback => {
  RNBackendlessEmitter.addListener('notificationAction', callback);
};

Backendless.Messaging.removePushNotificationActionListener = callback => {
  RNBackendlessEmitter.removeListener('notificationAction', callback);
};

Backendless.Messaging.getAppBadgeNumber = () => {
  return RNBackendless.getAppBadgeNumber();
};

Backendless.Messaging.setAppBadgeNumber = value => {
  return RNBackendless.setAppBadgeNumber(value);
};

Backendless.Messaging.removeAllDeliveredNotifications = () => {
  return RNBackendless.removeAllDeliveredNotifications();
};

Backendless.Messaging.removeDeliveredNotifications = notificationIds => {
  return RNBackendless.removeDeliveredNotifications(notificationIds);
};

Backendless.Messaging.getDeliveredNotifications = () => {
  return RNBackendless.getDeliveredNotifications();
};


