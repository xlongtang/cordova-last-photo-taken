<!---
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
-->

# com.ning.last-photo-taken

This plugin provides an API for taking pictures and for choosing images from
the system's image library. It only works in iOS - if someone can submit a pull
request to make it work in Android, that would be appreciated.

    cordova plugin add https://github.com/JonathanAquino/cordova-last-photo-taken.git


## navigator.LastPhotoTaken.getLastPhoto

Retrieves the last photo taken by the user. The onSuccess callback is passed
the URI for the image file. The onError callback is passed an error message.
Works on iOS only.

    navigator.LastPhotoTaken.getLastPhoto(onSuccess, onError);

### Description

The `getLastPhoto` function retrieves the last photo taken by the user, at its
original size, and at its current orientation. The user may be prompted to give
access to photos on the device. The URI returned is for a temporary jpeg file
created in NSTemporaryDirectory.

### Supported Platforms

- iOS

### Example

If the device is running iOS, retrieve the URI for the last photo taken:

    // May need to install the device plugin:
    // https://github.com/apache/cordova-plugin-device/blob/master/doc/index.md
    if (device.platform === 'iOS') {
      navigator.LastPhotoTaken.getLastPhoto(function (imageUri) {
        alert('imageUri: ' + imageUri);
      }, function (errorMessage) {
        alert(errorMessage);
      });
    }
