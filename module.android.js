import { NativeModules, DeviceEventEmitter } from 'react-native';

const RNBackendless = NativeModules.RNBackendless;

const RNBackendlessEmitter = {

  listeners: {},

  addListener(event, callback) {
    const subscription = DeviceEventEmitter.addListener(event, callback);

    this.listeners[event] = this.listeners[event] || [];
    this.listeners[event].push({
      remove: () => subscription.remove(),
      callback,
    })
  },

  removeListener(event, callback) {
    if (this.listeners[event]) {
      this.listeners[event] = this.listeners[event].filter(subscription => {
        if (subscription.callback !== callback) {
          return true
        }

        subscription.remove()
      })
    }
  }
};

module.exports = {
  RNBackendless,
  RNBackendlessEmitter,
};