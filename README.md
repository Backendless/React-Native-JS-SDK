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

const APP_KEY = Platform.select({
  ios    : 'YOUR_IOS_API_KEY',
  android: 'YOUR_ANDROID_API_KEY'
});

Backendless.initApp(APP_ID, APP_KEY);
````

#### Register Device

````
...
constructor(props) {
  super(props);
 
  Backendless.Messaging.registerDevice(['default'])
    .then(r => console.log('registerDevice:', r))
    .catch(e => console.log('registerDevice:', e));
}
...
````

#### Receive Push Notifications

````
    Backendless.Messaging.addPushNotificationListener(notification => {
        console.log('NOTIFICATION:', notification);
    })     
````

#### Subscribe on Push Notification Action Response

````
    Backendless.Messaging.addPushNotificationActionListener(action => {
        console.log('NOTIFICATION_ACTION:', action);
    })     
````
  
  