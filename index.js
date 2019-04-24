import { NativeEventEmitter, NativeModules, Platform, DeviceEventEmitter } from 'react-native';
import Backendless from 'backendless';

const { RNBackendless, RNBackendlessEmitter } = require('./module');

const _initApp = Backendless.initApp;
const _registerDevice = Backendless.Messaging.registerDevice;
const _unregisterDevice = Backendless.Messaging.unregisterDevice;

function voidResolver(){}

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
      const device = await RNBackendless.unregisterDevice();

      Backendless.setupDevice({
        uuid    : device.uuid,
        version : device.version,
        platform: Platform.OS,
      });

      return _unregisterDevice.call(this)
    })
};

Backendless.Messaging.getInitialNotificationAction = () => {
  return RNBackendless.getInitialNotificationAction();
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
  return RNBackendless.setAppBadgeNumber(value).then(voidResolver);
};

Backendless.Messaging.getNotifications = () => {
  return RNBackendless.getNotifications();
};

Backendless.Messaging.cancelNotification = notificationId => {
  return RNBackendless.cancelNotification(notificationId).then(voidResolver);
};

Backendless.Messaging.cancelAllNotifications = () => {
  return RNBackendless.cancelAllNotifications().then(voidResolver);
};

