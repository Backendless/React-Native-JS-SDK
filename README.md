## Patch for using Backendless JS-SDK inside React Native App.

Actually Backendless JS-SDK is totally adapted for working in React Native applications, 
you can use almost the entire Backendless API, 
but also there are some features which require Native iOS/Android implementation.

__Additional Backendless features__:
- Device setup inside `Backendless.appInit(...)`
- Push Notification Templates

### Push Notification Templates

#### Install base modules for Device Registration and for receiving Push Notifications
- [react-native-device-info](https://www.npmjs.com/package/react-native-device-info)  
- [react-native-push-notification](https://www.npmjs.com/package/react-native-push-notification)

````bash
npm i react-native-device-info react-native-push-notification -S
````
 
#### Install Backendless modules
````bash
npm i backendless backendless-react-native -S
````

#### Link Native Modules
````bash
react-native link
````

#### Setup Native projects
- [for iOS](./doc/ios/guide.md)
- [for Android](./doc/ios/guide.md)

## Usage

#### Import and init Backendless JS-SDK 

```javascript

import { Platform } from 'react-native';
import Backendless from 'backendless';

const APP_ID = 'YOUR_APP_ID';
const APP_KEY = Platform.select({
  ios    : 'YOUR_IOS_API_KEY',
  android: 'YOUR_ANDROID_API_KEY'
});

Backendless.initApp(APP_ID, APP_KEY);
```

#### Register Device
Use `react-native-push-notification` package for getting `onRegister` callback for device register in Backendless
```javascript
import PushNotification from 'react-native-push-notification';
import Backendless from 'backendless';

    PushNotification.configure({
      onRegister: device => {
        Backendless.Messaging.registerDevice(device.token, ['channel-name'])
          .then(() => console.log('Device is registered'))
          .catch(error => console.error(error));
      },      
    ...
```

#### Update Backendless Push Notification Templates
Use `react-native-push-notification` for that
```javascript
import RNBackendless from 'backendless-react-native';

    RNBackendless.updatePushTemplates()
        .then(() => console.log('Push Templates are update in Native Module'))
        .catch(error => console.error(error));

```

#### Receive Push Notification
Use `react-native-push-notification` for that
```javascript
import PushNotification from 'react-native-push-notification';

    PushNotification.configure({
      onNotification: (notification) => {
          console.log('NOTIFICATION:', notification);
        },     
    ...
```

#### Subscribe on Push Notification Action Response
Use `react-native-push-notification` for that
```javascript
import RNBackendless from 'backendless-react-native';

   RNBackendless.onPushNotificationActionResponse(actionResponse => {
      console.log('Push Notification Action Response:', actionResponse)
    })
```

  
  