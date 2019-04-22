import { NativeEventEmitter, NativeModules } from 'react-native';

const RNBackendless = NativeModules.RNBackendless;
const RNBackendlessEmitter = new NativeEventEmitter(RNBackendless);

module.exports = {
  RNBackendless,
  RNBackendlessEmitter,
};