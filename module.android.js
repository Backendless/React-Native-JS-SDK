import { NativeModules, DeviceEventEmitter } from 'react-native';

const RNBackendless = NativeModules.RNBackendless;

const RNBackendlessEmitter = {

  listeners: {},

  addListener(event, callback) {
    this.listeners[event] = this.listeners[event] || [];
    this.listeners[event].push({
      remove: DeviceEventEmitter.addListener(event, callback),
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