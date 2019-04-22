#import <UserNotifications/UserNotifications.h>
#import <React/RCTEventEmitter.h>

@interface RNBackendless : RCTEventEmitter

#if (TARGET_OS_IOS || TARGET_OS_SIMULATOR) && !TARGET_OS_TV && ! TARGET_OS_WATCH

+ (void)processMutableContent:(UNNotificationRequest *_Nonnull)request withContentHandler:(void(^_Nonnull)(UNNotificationContent *_Nonnull))contentHandler NS_AVAILABLE_IOS(10_0);

+ (void)didRegisterUserNotificationSettings:(UIUserNotificationSettings *_Nonnull)notificationSettings;
+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *_Nonnull)deviceToken;
+ (void)didFailToRegisterForRemoteNotificationsWithError:(NSError *_Nonnull)error;
+ (void)didReceiveRemoteNotification:(NSDictionary *_Nonnull)notification;
+ (void)didReceiveNotificationResponse:(UNNotificationResponse *_Nonnull)response;

#endif

@end
