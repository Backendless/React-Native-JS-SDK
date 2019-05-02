[![npm version](https://img.shields.io/npm/v/backendless-react-native.svg?style=flat)](https://www.npmjs.com/package/backendless-react-native)

## Patch for using Backendless JS-SDK inside React Native App.

Actually Backendless JS-SDK is totally adapted for working in React Native applications, 
you can use almost the entire Backendless API, 
but also there are some features which require Native iOS/Android implementation.

__Additional Backendless features__:

- Support Push Notification Templates
- Device Registration becomes more simpler `Backendless.Messaging.registerDevice(['channelName'])`
- Listeners for Push Notifications
    ````
    Backendless.Messaging.addPushNotificationListener(callback);
    Backendless.Messaging.removePushNotificationListener(callback);
    Backendless.Messaging.addPushNotificationActionListener(callback);
    Backendless.Messaging.removePushNotificationActionListener(callback);
    ````

### Install
````bash
npm i backendless backendless-react-native -S
````

#### Link Native Modules
````bash
react-native link
````

#### Setup Native projects
- [for iOS](./doc/ios/guide.md)
- [for Android](./doc/android/guide.md)

## Usage

#### Import and init Backendless JS-SDK 

````
import { Platform } from 'react-native';
import Backendless from 'backendless';
import 'backendless-react-native';

const APP_ID = 'YOUR_APP_ID';

const API_KEY = Platform.select({
  ios    : 'YOUR_IOS_API_KEY',
  android: 'YOUR_ANDROID_API_KEY'
});

Backendless.initApp(APP_ID, API_KEY);
````

Recommended to use corresponding Api Keys for each platform, but you can use any Api Key, for ex: JS_API_KEY or REST_API_KEY

````
Backendless.initApp(APP_ID, ANY_API_KEY);
````

### Features

#### Register Device

````
Backendless.Messaging.registerDevice(['default']).then(onSuccess).catch(onFail);
````

#### Unregister Device

````
Backendless.Messaging.unregisterDevice().then(onSuccess).catch(onFail);
````

#### Subscribe on Push Notifications

````
Backendless.Messaging.addPushNotificationListener(callback)
Backendless.Messaging.removePushNotificationListener(callback)

function callback(notification:Object){
// notification.message => "push message"
// notification.title => "push title"
// notification.subtitle => "push subtitle" 
// notification.sound => null
// notification.badge => 2
// notification.attachmentUrl => "https://backendlessappcontent.com/.../files/banner-4.jpg"
// notification.contentAvailable => 0
// notification.mutableContent => 1
// notification.customHeaders => { myHeaderKey: "myHeaderValue" }
// notification.templateName => "testName"
}
````

#### Subscribe on Push Notification Actions

````

Backendless.Messaging.addPushNotificationActionListener(callback)
Backendless.Messaging.removePushNotificationActionListener(callback)

function callback(action:Object){

// action.id => "action id" 
// action.inlineReply => "some text" 

// action.notification.message => "push message"
// action.notification.title => "push title"
// action.notification.subtitle => "push subtitle" 
// action.notification.sound => null
// action.notification.badge => 2
// action.notification.attachmentUrl => "https://backendlessappcontent.com/.../files/banner-4.jpg"
// action.notification.contentAvailable => 0
// action.notification.mutableContent => 1
// action.notification.customHeaders => { myHeaderKey: "myHeaderValue" }
// action.notification.templateName => "testName"
}
    
````
  
#### Get Initial Notification Action

````
Backendless.Messaging.getInitialNotificationAction().then(onSuccess).catch(onFail)

function onSuccess(action:Object){
// action.id => "action id" 
// action.inlineReply => "some text" 

// action.notification.message => "push message"
// action.notification.title => "push title"
// action.notification.subtitle => "push subtitle" 
// action.notification.sound => null
// action.notification.badge => 2
// action.notification.attachmentUrl => "https://backendlessappcontent.com/.../files/banner-4.jpg"
// action.notification.contentAvailable => 0
// action.notification.mutableContent => 1
// action.notification.customHeaders => { myHeaderKey: "myHeaderValue" }
// action.notification.templateName => "testName"
}

```` 

#### Get/Set Application Icon Badge Number (for iOS only)

````
Backendless.Messaging.getAppBadgeNumber().then(onGetSuccess).catch(onFail)

function onGetSuccess(badge:Number){
}

Backendless.Messaging.setAppBadgeNumber(badge:Number).then(onSetSuccess).catch(onFail)

function onSetSuccess(void){
}
````

#### Get Delivered Notifications 
````
Backendless.Messaging.getNotifications().then(onSuccess).catch(onFail)

function onSuccess(notifications:Array<Object>){
}
````

#### Cancel All Delivered Notifications
````
Backendless.Messaging.cancelAlldNotifications().then(onSuccess).catch(onFail)

function onSuccess(void){
}
````

#### Cancel Delivered Notification
````
Backendless.Messaging.cancelNotification(notification.id).then(onSuccess).catch(onFail)

function onSuccess(void){
}
````

 
