import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import Backendless from 'backendless';

const NativeModule = NativeModules.RNBackendless;
const Emitter = new NativeEventEmitter(NativeModule);

const _initApp = Backendless.initApp;
const _registerDevice = Backendless.Messaging.registerDevice;

Backendless.initApp = function (appId, apiKey) {
  RNBackendless.NativeModule.setAppId(appId);

  return _initApp.apply(this, arguments)
};

Backendless.Messaging.registerDevice = function (deviceToken, channels, expiration) {
  if (Array.isArray(deviceToken)) {
    expiration = channels;
    channels = deviceToken;
    deviceToken = null
  }

  return Promise.resolve()
    .then(() => RNBackendless.NativeModule.getDeviceInfo())
    .then(device => {
      Backendless.setupDevice({
        uuid    : device.uuid,
        platform: Platform.OS,
        version : device.version,
      });

      return _registerDevice.call(this, deviceToken || device.token, channels, expiration)
    })
    .then(deviceRegistrationResult => {
      return Backendless.Messaging.getPushTemplates()
        .then(templates => NativeModule.setTemplates(templates))
        .then(() => deviceRegistrationResult)
    })
};

const RNBackendless = {
  NativeModule,
  Emitter,

  updatePushTemplates() {
    return Backendless.Messaging.getPushTemplates()
      .then(templates => {
        console.log('templates', templates);

        return NativeModule.setTemplates(templates).then(() => templates)
      })
  },

  onPushNotificationActionResponse(callback) {
    Emitter.addListener('didReceiveNotificationResponse', callback);
  }
};

export default RNBackendless

