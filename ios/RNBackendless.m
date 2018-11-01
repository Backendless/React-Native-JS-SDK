
#import "RNBackendless.h"
#import "React/RCTEventEmitter.h"

#define PUSH_TEMPLATES_USER_DEFAULTS @"iOSPushTemplates"
#define RECEIVE_NOTIFICATION_RESPONSE @"ReceiveNotificationResponse"
#define ATTACHMENT_URL_HEADER_KEY @"attachment-url"

@implementation RNBackendless

NSString* userDefaultsSuiteName = @"group.com.backendless.PushTemplates";

RCT_EXPORT_MODULE();

+ (NSUserDefaults *) getUserDefaults{
    return [[NSUserDefaults alloc] initWithSuiteName:userDefaultsSuiteName];
}

- (dispatch_queue_t)methodQueue {
    return dispatch_get_main_queue();
}

- (NSArray<NSString *> *)supportedEvents {
    return @[@"eventFromNative",
             @"didReceiveNotificationResponse"];
}

- (void)startObserving
{
    [[NSNotificationCenter defaultCenter] addObserver:self
                                             selector:@selector(handleRemoteNotificationsRegistered:)
                                                 name:RECEIVE_NOTIFICATION_RESPONSE
                                               object:nil];
}

- (void)stopObserving
{
    [[NSNotificationCenter defaultCenter] removeObserver:self];
}

- (void)handleRemoteNotificationsRegistered:(NSNotification *)notification
{
    [self sendEventWithName:@"didReceiveNotificationResponse" body:notification.userInfo];
}

+ (void)didReceiveNotificationResponse:(UNNotificationResponse *)response
{
    UNNotificationContent *requestContent = response.notification.request.content;
    NSString *actionIdentifier  = response.actionIdentifier;
    NSMutableDictionary *customHeaders = [NSMutableDictionary dictionaryWithDictionary:requestContent.userInfo];
    NSString *attachmentUrl = [customHeaders valueForKey:ATTACHMENT_URL_HEADER_KEY];
    
    if (attachmentUrl) {
        [customHeaders removeObjectForKey:ATTACHMENT_URL_HEADER_KEY];
    }
    
    NSDictionary *data = @{
                           @"body" : requestContent.body,
                           @"title" : requestContent.title,
                           @"subtitle" : requestContent.subtitle,
                           @"badge" : requestContent.badge,
                           @"headers" : customHeaders,
                           @"attachmentUrl" : attachmentUrl,
                           @"actionIdentifier" : actionIdentifier
                           };
    
    [[NSNotificationCenter defaultCenter] postNotificationName:RECEIVE_NOTIFICATION_RESPONSE
                                                        object:self
                                                      userInfo:data ];
}

RCT_EXPORT_METHOD(getTemplates:(RCTPromiseResolveBlock)resolve
                  rejector:(__unused RCTPromiseRejectBlock)reject)
{
    NSUserDefaults *userDefaults = [RNBackendless getUserDefaults];
    NSData *data = [userDefaults objectForKey:PUSH_TEMPLATES_USER_DEFAULTS];
    NSDictionary *result = [[NSDictionary alloc] initWithDictionary:[NSKeyedUnarchiver unarchiveObjectWithData:data]];
    
    resolve(result);
}

RCT_EXPORT_METHOD(setTemplates:(NSDictionary *)templates
                  resolver:(RCTPromiseResolveBlock)resolve
                  rejector:(__unused RCTPromiseRejectBlock)reject)
{
    NSUserDefaults *userDefaults = [RNBackendless getUserDefaults];
    
    [userDefaults setObject:[NSKeyedArchiver archivedDataWithRootObject:templates] forKey:PUSH_TEMPLATES_USER_DEFAULTS];
    [userDefaults synchronize];
    
    resolve([NSNull null]);
}

#if (TARGET_OS_IOS || TARGET_OS_SIMULATOR) && !TARGET_OS_TV && ! TARGET_OS_WATCH
+(void)processMutableContent:(UNNotificationRequest *_Nonnull)request withContentHandler:(void(^_Nonnull)(UNNotificationContent *_Nonnull))contentHandler NS_AVAILABLE_IOS(10_0) {
    
    if ([request.content.userInfo valueForKey:@"ios_immediate_push"]) {
        request = [self prepareRequestWithIosImmediatePush:request];
    }
    
    if ([request.content.userInfo valueForKey:@"template_name"]) {
        request = [self prepareRequestWithTemplate:request];
    }
    
    UNMutableNotificationContent *bestAttemptContent = [request.content mutableCopy];
    if ([request.content.userInfo valueForKey:ATTACHMENT_URL_HEADER_KEY]) {
        NSString *urlString = [request.content.userInfo valueForKey:ATTACHMENT_URL_HEADER_KEY];
        NSURL *fileUrl = [NSURL URLWithString:urlString];
        [[[NSURLSession sharedSession] downloadTaskWithURL:fileUrl
                                         completionHandler:^(NSURL *location, NSURLResponse *response, NSError *error) {
                                             if (location) {
                                                 NSString *tmpDirectory = NSTemporaryDirectory();
                                                 NSString *tmpFile = [[@"file://" stringByAppendingString:tmpDirectory] stringByAppendingString:fileUrl.lastPathComponent];
                                                 NSURL *tmpUrl = [NSURL URLWithString:tmpFile];
                                                 BOOL success = [[NSFileManager defaultManager] moveItemAtURL:location toURL:tmpUrl error:nil];
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

+(UNNotificationRequest *)prepareRequestWithIosImmediatePush:(UNNotificationRequest *)request {
    NSString *JSONString = [request.content.userInfo valueForKey:@"ios_immediate_push"];
    NSDictionary *iosPushTemplate = [self dictionaryFromJson:JSONString];
    return [self createRequestFromTemplate:[self dictionaryWithoutNulls:iosPushTemplate] request:request];
}

+(NSDictionary *)dictionaryFromJson:(NSString *)JSONString {
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

+(UNNotificationRequest *)prepareRequestWithTemplate:(UNNotificationRequest *)request {
    NSString *templateName = [request.content.userInfo valueForKey:@"template_name"];
    NSDictionary *iosPushTemplates = [self readFromUserDefaultsWithKey];
    NSDictionary *iosPushTemplate = [iosPushTemplates valueForKey:templateName];
    return [self createRequestFromTemplate:[self dictionaryWithoutNulls:iosPushTemplate] request:request];
}

+(NSDictionary *)readFromUserDefaultsWithKey {
    NSUserDefaults *userDefaults = [RNBackendless getUserDefaults];
    
    NSData *data = [userDefaults objectForKey:PUSH_TEMPLATES_USER_DEFAULTS];
    return [[NSDictionary alloc] initWithDictionary:[NSKeyedUnarchiver unarchiveObjectWithData:data]];
}

+(NSDictionary *)dictionaryWithoutNulls:(NSDictionary *)dictionary {
    NSMutableDictionary *resultDictionary = [dictionary mutableCopy];
    NSArray *keysForNullValues = [resultDictionary allKeysForObject:[NSNull null]];
    [resultDictionary removeObjectsForKeys:keysForNullValues];
    return resultDictionary;
}

+(UNNotificationRequest *)createRequestFromTemplate:(NSDictionary *)iosPushTemplate request:(UNNotificationRequest *)request {
    UNMutableNotificationContent *content = [UNMutableNotificationContent new];
    NSMutableDictionary *userInfo = [NSMutableDictionary new];
    
    // check if silent
    NSInteger contentAvailable = [[iosPushTemplate valueForKey:@"contentAvailable"] integerValue];
    
    if (contentAvailable != 1) {
        content.body = [[[request.content.userInfo valueForKey:@"aps"] valueForKey:@"alert"] valueForKey:@"body"];
        content.title = request.content.title ?: [iosPushTemplate valueForKey:@"alertTitle"];
        content.subtitle = request.content.subtitle ?: [iosPushTemplate valueForKey:@"alertSubtitle"];
        
        content.badge = [iosPushTemplate valueForKey:@"badge"] ?: request.content.badge ;
        
        content.sound = [iosPushTemplate valueForKey:@"sound"]
        ? [UNNotificationSound soundNamed:[iosPushTemplate valueForKey:@"sound"]]
        : [UNNotificationSound defaultSound];
        
        if ([iosPushTemplate valueForKey:@"attachmentUrl"]) {
            [userInfo setObject:[iosPushTemplate valueForKey:@"attachmentUrl"] forKey:ATTACHMENT_URL_HEADER_KEY];
        }
        
        content.categoryIdentifier = [self setActions:[iosPushTemplate valueForKey:@"actions"]];
    }
    
    if ([iosPushTemplate valueForKey:@"customHeaders"]) {
        NSDictionary *customHeaders = [iosPushTemplate valueForKey:@"customHeaders"];
        
        for (NSString *headerKey in [customHeaders allKeys]) {
            [userInfo setObject:[customHeaders valueForKey:headerKey] forKey:headerKey];
        }
    }
    
    content.userInfo = userInfo;
    
    UNTimeIntervalNotificationTrigger *trigger = [UNTimeIntervalNotificationTrigger
                                                  triggerWithTimeInterval:0.1
                                                  repeats:NO];
    
    return [UNNotificationRequest requestWithIdentifier:@"request" content:content trigger:trigger];
}

+(NSString *)setActions:(NSArray *)actions {
    NSMutableArray *categoryActions = [NSMutableArray new];
    
    for (NSDictionary *action in actions) {
        NSString *actionId = [action valueForKey:@"id"];
        NSString *actionTitle = [action valueForKey:@"title"];
        NSNumber *actionOptions = [action valueForKey:@"options"];
        UNNotificationActionOptions options = [actionOptions integerValue];
        [categoryActions addObject:[UNNotificationAction actionWithIdentifier:actionId title:actionTitle options:options]];
    }
    NSString *categoryId = @"buttonActionsTemplate";
    UNNotificationCategory *category = [UNNotificationCategory categoryWithIdentifier:categoryId actions:categoryActions intentIdentifiers:@[] options:UNNotificationCategoryOptionNone];
    [UNUserNotificationCenter.currentNotificationCenter setNotificationCategories:[NSSet setWithObject:category]];
    return categoryId;
}

#endif

@end

