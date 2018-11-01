import { NativeEventEmitter, NativeModules, Platform } from 'react-native';
import DeviceInfo from 'react-native-device-info';
import Backendless from 'backendless';

const _initBackendlessApp = Backendless.initApp;

Backendless.initApp = (...args) => {
  Backendless.setupDevice({
    uuid    : DeviceInfo.getUniqueID(),
    platform: Platform.OS,
    version : DeviceInfo.getSystemVersion()
  });

  _initBackendlessApp(...args)
};

const NativeModule = NativeModules.RNBackendless;
const Emitter = new NativeEventEmitter(NativeModule);

const RNBackendless = {
  updatePushTemplates() {
    return Backendless.Messaging.getPushTemplates()
      .then(templates => {
        return NativeModule.setTemplates(templates).then(() => templates)
      })
  },

  onPushNotificationActionResponse(callback) {
    Emitter.addListener('didReceiveNotificationResponse', callback);
  }
};

export default RNBackendless

