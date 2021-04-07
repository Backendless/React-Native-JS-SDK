
#import "RNBackendless.h"

#import <UserNotifications/UserNotifications.h>

#import <React/RCTBridge.h>
#import <React/RCTEventDispatcher.h>
#import <React/RCTUtils.h>

@implementation RNBackendless
{
    NSMutableDictionary *pendingPromisesGroups;
}

static NSString *backendlessAppId = nil;
static NSDictionary *initialNotificationAction = nil;

static NSString *const kRegisterUserNotificationSettings = @"BLRegisterUserNotificationSettings";
static NSString *const kRemoteNotificationsRegistered = @"BLRemoteNotificationsRegistered";
static NSString *const kRemoteNotificationRegistrationFailed = @"BLRemoteNotificationRegistrationFailed";
static NSString *const kRemoteNotificationReceived = @"BLRemoteNotificationReceived";
static NSString *const kRemoteNotificationAction = @"BLRemoteNotificationAction";

NSString *const gDeviceRegistrationPromisesGroup = @"deviceRegistrationPromisesGroup";

NSString *const pushTemplatesStorageKey = @"PushTemplates-";

NSString *const jsNotificationEvent = @"notification";
NSString *const jsNotificationActionEvent = @"notificationAction";

RCT_EXPORT_MODULE();

RCT_EXPORT_METHOD(setAppId:(NSString *)appId)
{
    backendlessAppId = appId;

    [self initNSNotificationCenter];
}

RCT_EXPORT_METHOD(registerDevice:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject)
{
    if ([self getPandingPromisesCount:gDeviceRegistrationPromisesGroup] > 0){
        reject(@"device_reg", @"Cannot call Backendless.Messaging.registerDevice twice before the first has returned.", nil);
        return;
    }

    [self addPandingPromise:gDeviceRegistrationPromisesGroup resolver:resolve rejector:reject];

    UIUserNotificationType types = UIUserNotificationTypeAlert | UIUserNotificationTypeBadge | UIUserNotificationTypeSound;;
    UIUserNotificationSettings *notificationSettings = [UIUserNotificationSettings settingsForTypes:types categories:nil];

    [RCTSharedApplication() registerUserNotificationSettings:notificationSettings];
}

RCT_EXPORT_METHOD(unregisterDevice:(RCTPromiseResolveBlock)resolve rejector:(__unused RCTPromiseRejectBlock)reject)
{
    NSString *deviceUID = [[[UIDevice currentDevice] identifierForVendor] UUIDString];

    [RCTSharedApplication() unregisterForRemoteNotifications];

    resolve(deviceUID);
}

RCT_EXPORT_METHOD(getInitialNotificationAction:(RCTPromiseResolveBlock)resolve
                  reject:(__unused RCTPromiseRejectBlock)reject)
{
    if (initialNotificationAction) {
        resolve(initialNotificationAction);
    } else {
        resolve((id)kCFNull);
    }
}

RCT_EXPORT_METHOD(setTemplates:(NSDictionary *)templates
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejector:(__unused RCTPromiseRejectBlock)reject)
{
    [RNBackendless writeToUserDefaults:templates withKey:pushTemplatesStorageKey];

    resolve(nil);
}

RCT_EXPORT_METHOD(setAppBadgeNumber:(NSInteger)number
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejector:(__unused RCTPromiseRejectBlock)reject)
{
    RCTSharedApplication().applicationIconBadgeNumber = number;

    resolve(nil);
}

RCT_EXPORT_METHOD(getAppBadgeNumber:(RCTPromiseResolveBlock)resolve rejector:(__unused RCTPromiseRejectBlock)reject)
{
    resolve(@(RCTSharedApplication().applicationIconBadgeNumber));
}

RCT_EXPORT_METHOD(getNotifications:(RCTPromiseResolveBlock)resolve rejector:(__unused RCTPromiseRejectBlock)reject)
{
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center getDeliveredNotificationsWithCompletionHandler:^(NSArray<UNNotification *> *_Nonnull notifications) {
        NSMutableArray<NSDictionary *> *clientNotifications = [NSMutableArray new];

        for (UNNotification *notification in notifications) {
            [clientNotifications addObject:[RNBackendless prepareClientPushNotification:notification.request.content.userInfo]];
        }

        resolve(clientNotifications);
    }];
}

RCT_EXPORT_METHOD(cancelNotification:(NSString *)notificationId
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejector:(__unused RCTPromiseRejectBlock)reject)
{
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center removeDeliveredNotificationsWithIdentifiers:@[notificationId]];

    resolve(nil);
}

RCT_EXPORT_METHOD(cancelAllNotifications:(RCTPromiseResolveBlock)resolve rejector:(__unused RCTPromiseRejectBlock)reject)
{
    UNUserNotificationCenter *center = [UNUserNotificationCenter currentNotificationCenter];
    [center removeAllDeliveredNotifications];

    resolve(nil);
}

- (NSArray<NSString *> *)supportedEvents
{
    return @[jsNotificationEvent,
             jsNotificationActionEvent];
}

- (dispatch_queue_t)methodQueue
{
    return dispatch_get_main_queue();
}

- (void)initNSNotificationCenter
{
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRemoteNotificationsRegistered:)
                                                 name:kRemoteNotificationsRegistered
                                               object:nil];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRemoteNotificationRegistrationError:)
                                                 name:kRemoteNotificationRegistrationFailed
                                               object:nil];

    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRemoteNotificationReceived:)
                                                 name:kRemoteNotificationReceived
                                               object:nil];
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRemoteNotificationAction:)
                                                 name:kRemoteNotificationAction
                                               object:nil];
}

- (void)addPandingPromise:(NSString *)group resolver:(RCTPromiseResolveBlock)resolve rejector:(RCTPromiseRejectBlock)reject
{
    if(!pendingPromisesGroups){
        pendingPromisesGroups = [NSMutableDictionary dictionary];
    }

    if(!pendingPromisesGroups[group]){
        pendingPromisesGroups[group] = [NSMutableArray new];
    }

    [pendingPromisesGroups[group] addObject:@{@"reject": reject, @"resolve": resolve}];
}

- (NSUInteger)getPandingPromisesCount:(NSString *)group
{
    if(pendingPromisesGroups && pendingPromisesGroups[group]){
        NSArray *pendingPromisesGroup = pendingPromisesGroups[group];

        return pendingPromisesGroup.count;
    }

    return 0;
}

- (void)resolvePandingPromise:(NSString *)group data:(id)data
{
    if(pendingPromisesGroups && pendingPromisesGroups[group]){
        NSMutableArray *pendingPromises = [pendingPromisesGroups mutableArrayValueForKey:group];

        for (NSDictionary *pendingPromise in pendingPromises) {
            RCTPromiseResolveBlock resolve = pendingPromise[@"resolve"];

            resolve(data);
        }

        [pendingPromises removeAllObjects];
    }
}

- (void)rejectPandingPromise:(NSString *)group error:(NSError *)error
{
    if(pendingPromisesGroups && pendingPromisesGroups[group]){
        NSMutableArray *pendingPromises = [pendingPromisesGroups mutableArrayValueForKey:group];

        for (NSDictionary *pendingPromise in pendingPromises) {
            RCTPromiseRejectBlock reject = pendingPromise[@"reject"];

            reject(group, error.localizedDescription, error);
        }

        [pendingPromises removeAllObjects];
    }
}

- (void)handleRemoteNotificationAction:(NSNotification *)notification
{
    [self sendEventWithName:jsNotificationActionEvent body:notification.userInfo];
}

- (void)handleRemoteNotificationReceived:(NSNotification *)remoteNotification
{
    NSDictionary *notification = [RNBackendless prepareClientPushNotification:remoteNotification.userInfo];

    [self sendEventWithName:jsNotificationEvent body:notification];
}

- (void)handleRemoteNotificationsRegistered:(NSNotification *)notification
{

    NSString *deviceUID = [[[UIDevice currentDevice] identifierForVendor] UUIDString];
    NSString *deviceVersion = [[UIDevice currentDevice] systemVersion];
    NSString *deviceToken = notification.userInfo[@"deviceToken"];

    NSDictionary *device = @{
                             @"uuid" : deviceUID,
                             @"version" : deviceVersion,
                             @"token" : deviceToken
                             };

    [self resolvePandingPromise:gDeviceRegistrationPromisesGroup data:device];
}

- (void)handleRemoteNotificationRegistrationError:(NSNotification *)notification
{
    NSError *_deviceRegistrationError = notification.userInfo[@"error"];

    [self rejectPandingPromise:gDeviceRegistrationPromisesGroup error:_deviceRegistrationError];
}

+(void)writeToUserDefaults:(NSDictionary *)dictionary withKey:(NSString *)key {
    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
    NSString *appGroup = [self getAppGroup];

    if (appGroup) {
        userDefaults = [[NSUserDefaults alloc] initWithSuiteName:appGroup];
    }

    [userDefaults setObject:[NSKeyedArchiver archivedDataWithRootObject:dictionary] forKey:key];
    [userDefaults synchronize];
}

+(NSDictionary *)readFromUserDefaultsWithKey:(NSString *)key {
    NSUserDefaults *userDefaults = [NSUserDefaults standardUserDefaults];
    NSString *appGroup = [self getAppGroup];

    if (appGroup) {
        userDefaults = [[NSUserDefaults alloc] initWithSuiteName:appGroup];
    }

    NSData *data = [userDefaults objectForKey:key];

    return [[NSDictionary alloc] initWithDictionary:[NSKeyedUnarchiver unarchiveObjectWithData:data]];
}

+(NSString *)getAppGroup {
    NSString *projectName = [[NSBundle mainBundle] objectForInfoDictionaryKey:@"CFBundleName"];
    NSString *path = [[NSBundle mainBundle] pathForResource:projectName ofType:@"entitlements"];
    NSDictionary *dict = [NSDictionary dictionaryWithContentsOfFile:path];
    NSArray<NSString *> *appGroups = [dict objectForKey:@"com.apple.security.application-groups"];
    NSPredicate *predicate = [NSPredicate predicateWithFormat:@"SELF contains[c] 'BackendlessPushTemplates'"];
    NSString *appGroup = [[appGroups filteredArrayUsingPredicate:predicate] firstObject];
    return appGroup;
}

+ (void)didRegisterUserNotificationSettings:(__unused UIUserNotificationSettings *)notificationSettings
{
    if ([UIApplication instancesRespondToSelector:@selector(registerForRemoteNotifications)]) {
        [RCTSharedApplication() registerForRemoteNotifications];
        [[NSNotificationCenter defaultCenter] postNotificationName:kRegisterUserNotificationSettings
                                                            object:self
                                                          userInfo:@{@"notificationSettings": notificationSettings}];
    }
}

+ (void)didRegisterForRemoteNotificationsWithDeviceToken:(NSData *)deviceToken
{
    NSString *token = @"";
    if (@available(iOS 13, *)) {
        NSUInteger length = deviceToken.length;
        if (length == 0) {
            token = nil;
        }
        const unsigned char *buffer = deviceToken.bytes;
        NSMutableString *hexString  = [NSMutableString stringWithCapacity:(length * 2)];
        for (int i = 0; i < length; ++i) {
            [hexString appendFormat:@"%02x", buffer[i]];
        }
        token = [hexString copy];
    } else {
        token = [[deviceToken description] stringByTrimmingCharactersInSet: [NSCharacterSet characterSetWithCharactersInString:@"<>"]];
        token = [token stringByReplacingOccurrencesOfString:@" " withString:@""];
    }

    [[NSNotificationCenter defaultCenter] postNotificationName:kRemoteNotificationsRegistered
                                                        object:self
                                                      userInfo:@{@"deviceToken" : token}];
}

+ (void)didFailToRegisterForRemoteNotificationsWithError:(NSError *)error
{
    [[NSNotificationCenter defaultCenter] postNotificationName:kRemoteNotificationRegistrationFailed
                                                        object:self
                                                      userInfo:@{@"error": error}];
}

+ (NSDictionary *)getPushTemplates
{
    return [self readFromUserDefaultsWithKey:pushTemplatesStorageKey];
}

+ (void)didReceiveRemoteNotification:(NSDictionary *)notification
{
    [[NSNotificationCenter defaultCenter] postNotificationName:kRemoteNotificationReceived
                                                        object:self
                                                      userInfo:notification];
}

+ (void)didReceiveNotificationResponse:(UNTextInputNotificationResponse *)response
{
    UNNotificationContent *requestContent = response.notification.request.content;
    NSString *actionIdentifier  = response.actionIdentifier;
    NSMutableDictionary *notification = [self prepareClientPushNotification:requestContent.userInfo];

    NSMutableDictionary *action = [NSMutableDictionary new];

    if([actionIdentifier isEqualToString:@"com.apple.UNNotificationDefaultActionIdentifier"]){
        [action setObject:[NSNull new] forKey:@"id"];
    } else {
        [action setObject:actionIdentifier ?:[NSNull new] forKey:@"id"];
    }

    if ([response isKindOfClass:UNTextInputNotificationResponse.class]) {
        [action setObject:response.userText forKey:@"inlineReply"];
    }

    [action setObject:notification forKey:@"notification"];

    if(!backendlessAppId){
        initialNotificationAction = action;
    }

    [[NSNotificationCenter defaultCenter] postNotificationName:kRemoteNotificationAction
                                                        object:self
                                                      userInfo:action];
}

+ (NSMutableDictionary *)prepareClientPushNotification:(NSDictionary * _Nonnull)sourceNotification
{
    NSMutableDictionary *notification = [NSMutableDictionary dictionaryWithDictionary:sourceNotification];

    if ([notification valueForKey:@"attachment-url"]) {
        [notification setObject:[notification valueForKey:@"attachment-url"] forKey:@"attachmentUrl"];
        [notification removeObjectForKey:@"attachment-url"];
    }

    NSDictionary *aps = notification[@"aps"];

    if (aps) {
        if ([aps valueForKey:@"badge"]) {
            [notification setObject:[aps valueForKey:@"badge"] ?:[NSNull new] forKey:@"badge"];
        }

        if ([aps valueForKey:@"alert"]) {
            NSDictionary *alert = aps[@"alert"];

            for (NSString *fieldName in alert) {
                [notification setObject:[alert valueForKey:fieldName] ?:[NSNull new] forKey:fieldName];
            }
        }

        [notification removeObjectForKey:@"aps"];
    }

    if ([notification valueForKey:@"body"]) {
        [notification setObject:[notification valueForKey:@"body"] ?:[NSNull new] forKey:@"message"];

        [notification removeObjectForKey:@"body"];
    }

    return notification;
}

#if (TARGET_OS_IOS || TARGET_OS_SIMULATOR) && !TARGET_OS_TV && ! TARGET_OS_WATCH
+(void)processMutableContent:(UNNotificationRequest *_Nonnull)request withContentHandler:(void(^_Nonnull)(UNNotificationContent *_Nonnull))contentHandler NS_AVAILABLE_IOS(10_0)
{

    if ([request.content.userInfo valueForKey:@"ios_immediate_push"]) {
        request = [self prepareRequestWithIosImmediatePush:request];
    }

    if ([request.content.userInfo valueForKey:@"template_name"]) {
        request = [self prepareRequestWithTemplate:request];
    }

    UNMutableNotificationContent *bestAttemptContent = [request.content mutableCopy];
    if ([request.content.userInfo valueForKey:@"attachment-url"]) {
        NSString *urlString = [request.content.userInfo valueForKey:@"attachment-url"];
        NSURL *fileUrl = [NSURL URLWithString:urlString];
        [[[NSURLSession sharedSession] downloadTaskWithURL:fileUrl
                                         completionHandler:^(NSURL *location, NSURLResponse *response, NSError *error) {
                                             if (location) {
                                                 NSString *tmpDirectory = NSTemporaryDirectory();
                                                 NSString *tmpFile = [[@"file://" stringByAppendingString:tmpDirectory] stringByAppendingString:fileUrl.lastPathComponent];
                                                 NSURL *tmpUrl = [NSURL URLWithString:tmpFile];
                                                 [[NSFileManager defaultManager] moveItemAtURL:location toURL:tmpUrl error:nil];
                                                 UNNotificationAttachment *attachment = [UNNotificationAttachment attachmentWithIdentifier:@"" URL:tmpUrl options:nil error:nil];
                                                 if (attachment) {
                                                     bestAttemptContent.attachments = @[attachment];
                                                 }
                                             }
                                             contentHandler(bestAttemptContent);
                                         }] resume];
    } else {
        contentHandler(bestAttemptContent);
    }
}

+(UNNotificationRequest *)prepareRequestWithIosImmediatePush:(UNNotificationRequest *)request
{
    NSString *JSONString = [request.content.userInfo valueForKey:@"ios_immediate_push"];
    NSDictionary *pushTemplate = [self dictionaryFromJson:JSONString];
    return [self createRequestFromTemplate:[self dictionaryWithoutNulls:pushTemplate] request:request];
}

+(UNNotificationRequest *)prepareRequestWithTemplate:(UNNotificationRequest *)request
{
    NSString *templateName = [request.content.userInfo valueForKey:@"template_name"];
    NSDictionary *pushTemplates = [self getPushTemplates];
    NSDictionary *pushTemplate = [pushTemplates valueForKey:templateName];

    return [self createRequestFromTemplate:[self dictionaryWithoutNulls:pushTemplate] request:request];
}

+(NSDictionary *)dictionaryFromJson:(NSString *)JSONString
{
    NSMutableDictionary *dictionary = [NSMutableDictionary new];
    NSError *error;
    NSData *JSONData = [JSONString dataUsingEncoding:NSUTF8StringEncoding];
    NSDictionary *JSONDictionary = [NSJSONSerialization JSONObjectWithData:JSONData options:0 error:&error];
    for (NSString *fieldName in JSONDictionary) {
        if (![fieldName isEqualToString:@"___jsonclass"] && ![fieldName isEqualToString:@"__meta"]) {
            [dictionary setValue:[JSONDictionary valueForKey:fieldName] forKey:fieldName];
        }
    }
    return dictionary;
}

+(NSDictionary *)dictionaryWithoutNulls:(NSDictionary *)dictionary
{
    NSMutableDictionary *resultDictionary = [dictionary mutableCopy];
    NSArray *keysForNullValues = [resultDictionary allKeysForObject:[NSNull null]];
    [resultDictionary removeObjectsForKeys:keysForNullValues];
    return resultDictionary;
}

+(UNNotificationRequest *)createRequestFromTemplate:(NSDictionary *)pushTemplate request:(UNNotificationRequest *)request
{
    UNMutableNotificationContent *content = [UNMutableNotificationContent new];
    NSMutableDictionary *userInfo = [NSMutableDictionary new];

    // check if silent
    NSInteger contentAvailable = [[pushTemplate valueForKey:@"contentAvailable"] integerValue];
    if (contentAvailable != 1) {
        content.body = [[[request.content.userInfo valueForKey:@"aps"] valueForKey:@"alert"] valueForKey:@"body"];
        content.title = request.content.title ?: [pushTemplate valueForKey:@"alertTitle"];
        content.subtitle = request.content.subtitle ?: [pushTemplate valueForKey:@"alertSubtitle"];

        content.badge = [pushTemplate valueForKey:@"badge"] ?: request.content.badge ;

        content.sound = [pushTemplate valueForKey:@"sound"]
        ? [UNNotificationSound soundNamed:[pushTemplate valueForKey:@"sound"]]
        : [UNNotificationSound defaultSound];

        if ([pushTemplate valueForKey:@"attachmentUrl"]) {
            [userInfo setObject:[pushTemplate valueForKey:@"attachmentUrl"] forKey:@"attachment-url"];
        }

        content.categoryIdentifier = [self setActions:[pushTemplate valueForKey:@"actions"]];
    }

    [userInfo setObject:request.identifier forKey:@"id"];

    [userInfo setObject:content.body ?:[NSNull new] forKey:@"body"];
    [userInfo setObject:content.title ?:[NSNull new] forKey:@"title"];
    [userInfo setObject:content.subtitle ?:[NSNull new] forKey:@"subtitle"];
    [userInfo setObject:content.badge ?:[NSNull new] forKey:@"badge"];

    [userInfo setObject:[pushTemplate valueForKey:@"name"] ?:[NSNull new] forKey:@"templateName"];
    [userInfo setObject:[pushTemplate valueForKey:@"contentAvailable"] ?:[NSNull new] forKey:@"contentAvailable"];
    [userInfo setObject:[pushTemplate valueForKey:@"mutableContent"] ?:[NSNull new] forKey:@"mutableContent"];
    [userInfo setObject:[pushTemplate valueForKey:@"sound"] ?:[NSNull new] forKey:@"sound"];
    [userInfo setObject:[pushTemplate valueForKey:@"customHeaders"] ?:[NSDictionary new] forKey:@"customHeaders"];

    content.userInfo = userInfo;

    UNTimeIntervalNotificationTrigger *trigger = [UNTimeIntervalNotificationTrigger
                                                  triggerWithTimeInterval:0.1
                                                  repeats:NO];

    return [UNNotificationRequest requestWithIdentifier:@"request" content:content trigger:trigger];
}

+(NSString *)setActions:(NSArray *)actions
{
    NSMutableArray *categoryActions = [NSMutableArray new];

    for (NSDictionary *action in actions) {
        NSString *actionId = [action valueForKey:@"id"];
        NSString *actionTitle = [action valueForKey:@"title"];
        NSNumber *actionOptions = [action valueForKey:@"options"];
        UNNotificationActionOptions options = [actionOptions integerValue];

        if ([[action valueForKey:@"inlineReply"] isEqual:@YES]) {
            NSString *textInputPlaceholder = @"Input text here...";
            if (![[action valueForKey:@"textInputPlaceholder"] isKindOfClass:[NSNull class]]) {
                if ([[action valueForKey:@"textInputPlaceholder"] length] > 0) {
                    textInputPlaceholder = [action valueForKey:@"textInputPlaceholder"];
                }
            }
            NSString *inputButtonTitle = @"Send";
            if (![[action valueForKey:@"inputButtonTitle"] isKindOfClass:[NSNull class]]) {
                if ([[action valueForKey:@"inputButtonTitle"] length] > 0) {
                    inputButtonTitle = [action valueForKey:@"inputButtonTitle"];
                }
            }
            [categoryActions addObject:[UNTextInputNotificationAction actionWithIdentifier:actionId title:actionTitle options:options textInputButtonTitle:inputButtonTitle textInputPlaceholder:textInputPlaceholder]];
        }
        else {
            [categoryActions addObject:[UNNotificationAction actionWithIdentifier:actionId title:actionTitle options:options]];
        }
    }
    NSString *categoryId = @"buttonActionsTemplate";
    UNNotificationCategory *category = [UNNotificationCategory categoryWithIdentifier:categoryId actions:categoryActions intentIdentifiers:@[] options:UNNotificationCategoryOptionNone];
    [UNUserNotificationCenter.currentNotificationCenter setNotificationCategories:[NSSet setWithObject:category]];
    return categoryId;
}

#endif

@end

