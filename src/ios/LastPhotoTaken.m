/*
 Licensed to the Apache Software Foundation (ASF) under one
 or more contributor license agreements.  See the NOTICE file
 distributed with this work for additional information
 regarding copyright ownership.  The ASF licenses this file
 to you under the Apache License, Version 2.0 (the
 "License"); you may not use this file except in compliance
 with the License.  You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing,
 software distributed under the License is distributed on an
 "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 KIND, either express or implied.  See the License for the
 specific language governing permissions and limitations
 under the License.
 */

#import "LastPhotoTaken.h"
#import <AssetsLibrary/ALAssetsLibrary.h>
#import <AssetsLibrary/ALAssetsGroup.h>
#import <AssetsLibrary/ALAssetsFilter.h>
#import <AssetsLibrary/ALAssetRepresentation.h>

#define CDV_PHOTO_PREFIX @"cdv_photo_"

@implementation LastPhotoTaken

// Code is taken from:
// - http://stackoverflow.com/questions/8867496/get-last-image-from-photos-app
// - https://github.com/apache/cordova-plugin-camera/blob/master/src/ios/CDVCamera.m
-(void) getLastPhoto:(CDVInvokedUrlCommand *)command {

    NSInteger max = [[command.arguments objectAtIndex:0] integerValue];
    double startTimeTick = [[command.arguments objectAtIndex:1] doubleValue];
    NSDate *startTime = [NSDate dateWithTimeIntervalSince1970:startTimeTick];
    double endTimeTick = [[command.arguments objectAtIndex:2] doubleValue];
    NSDate *endTime = [NSDate dateWithTimeIntervalSince1970:endTimeTick];    
    
    ALAssetsLibrary *library = [[ALAssetsLibrary alloc] init];
    __block BOOL calledBack = NO;

    // Enumerate just the photos and videos group by using ALAssetsGroupSavedPhotos.
    [library enumerateGroupsWithTypes:ALAssetsGroupSavedPhotos usingBlock:^(ALAssetsGroup *group, BOOL *stop) {

        // The end of the enumeration is signaled by group == nil.
        if (group == nil) {
            // Note that enumerateGroupsWithTypes:usingBlock:failureBlock: is asynchronous.
            // So the only way to know when it's done is to wait util we get nil here.
            if (!calledBack) {
                CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_ERROR messageAsString:@"There are no photos."];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
            }
            return;
        }

        if ([group numberOfAssets] < 1) {
            return;
        };

        // Within the group enumeration block, filter to enumerate just photos.
        [group setAssetsFilter:[ALAssetsFilter allPhotos]];

        // Chooses the photo at the last index
        [group enumerateAssetsWithOptions:NSEnumerationReverse usingBlock:^(ALAsset *alAsset, NSUInteger index, BOOL *innerStop) {

            // The end of the enumeration is signaled by asset == nil.
            if (alAsset == nil) {
                return;
            }
            
            // If this is not what we want, just check the next one.
            NSDate * date = [alAsset valueForProperty:ALAssetPropertyDate];
            if ([date compare:startTime] != NSOrderedAscending || [date compare:endTime] != NSOrderedDescending) {
                return;
            }
            
            ALAssetRepresentation *representation = [alAsset defaultRepresentation];
            ALAssetOrientation orientation = [representation orientation];
            UIImage *latestPhoto = [UIImage imageWithCGImage:[representation fullResolutionImage] scale:[representation scale] orientation:(UIImageOrientation)orientation];
            latestPhoto = [self imageCorrectedForCaptureOrientation: latestPhoto];

            // Stop the enumerations
            *stop = YES; *innerStop = YES;
            NSData * jpegData = UIImageJPEGRepresentation(latestPhoto, 1.0);

            // write to temp directory and return URI
            // get the temp directory path
            NSString* docsPath = [NSTemporaryDirectory()stringByStandardizingPath];
            NSError* err = nil;
            NSFileManager* fileMgr = [[NSFileManager alloc] init]; // recommended by apple (vs [NSFileManager defaultManager]) to be threadsafe
            // generate unique file name
            NSString* filePath;

            int i = 1;
            do {
                filePath = [NSString stringWithFormat:@"%@/%@%03d.%@", docsPath, CDV_PHOTO_PREFIX, i++, @"jpg"];
            } while ([fileMgr fileExistsAtPath:filePath]);

            // save file
            if (![jpegData writeToFile:filePath options:NSAtomicWrite error:&err]) {
                CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[err localizedDescription]];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
                calledBack = YES;
            }
            else {
                NSMutableDictionary *resultDict = [NSMutableDictionary dictionaryWithCapacity:2];
                [resultDict setObject:[NSNumber numberWithDouble:[date timeIntervalSince1970] * 1000] forKey:@"timestamp"];
                [resultDict setObject:[[NSURL fileURLWithPath:filePath] absoluteString] forKey:@"path"];
                CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsDictionary:resultDict];
                [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
                calledBack = YES;
            }
        }];
    } failureBlock: ^(NSError *error) {
        CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_IO_EXCEPTION messageAsString:[error localizedDescription]];
        [self.commandDelegate sendPluginResult:result callbackId:command.callbackId];
        calledBack = YES;
    }];

}

// Actually rotate the image; otherwise it won't be rotated when we convert it to jpeg.
// See http://stackoverflow.com/questions/5427656/ios-uiimagepickercontroller-result-image-orientation-after-upload
// Code is taken from: https://github.com/apache/cordova-plugin-camera/blob/master/src/ios/CDVCamera.m
- (UIImage*)imageCorrectedForCaptureOrientation:(UIImage*)anImage
{
    float rotation_radians = 0;
    bool perpendicular = false;

    switch ([anImage imageOrientation]) {
        case UIImageOrientationUp :
            rotation_radians = 0.0;
            break;

        case UIImageOrientationDown:
            rotation_radians = M_PI; // don't be scared of radians, if you're reading this, you're good at math
            break;

        case UIImageOrientationRight:
            rotation_radians = M_PI_2;
            perpendicular = true;
            break;

        case UIImageOrientationLeft:
            rotation_radians = -M_PI_2;
            perpendicular = true;
            break;

        default:
            break;
    }

    UIGraphicsBeginImageContext(CGSizeMake(anImage.size.width, anImage.size.height));
    CGContextRef context = UIGraphicsGetCurrentContext();

    // Rotate around the center point
    CGContextTranslateCTM(context, anImage.size.width / 2, anImage.size.height / 2);
    CGContextRotateCTM(context, rotation_radians);

    CGContextScaleCTM(context, 1.0, -1.0);
    float width = perpendicular ? anImage.size.height : anImage.size.width;
    float height = perpendicular ? anImage.size.width : anImage.size.height;
    CGContextDrawImage(context, CGRectMake(-width / 2, -height / 2, width, height), [anImage CGImage]);

    // Move the origin back since the rotation might've change it (if its 90 degrees)
    if (perpendicular) {
        CGContextTranslateCTM(context, -anImage.size.height / 2, -anImage.size.width / 2);
    }

    UIImage* newImage = UIGraphicsGetImageFromCurrentImageContext();
    UIGraphicsEndImageContext();
    return newImage;
}

@end
