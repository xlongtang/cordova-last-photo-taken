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
            ALAssetRepresentation *representation = [alAsset defaultRepresentation];
            ALAssetOrientation orientation = [representation orientation];
            UIImage *latestPhoto = [UIImage imageWithCGImage:[representation fullResolutionImage] scale:[representation scale] orientation:(UIImageOrientation)orientation];

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
                CDVPluginResult* result = [CDVPluginResult resultWithStatus:CDVCommandStatus_OK messageAsString:[[NSURL fileURLWithPath:filePath] absoluteString]];
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

@end
