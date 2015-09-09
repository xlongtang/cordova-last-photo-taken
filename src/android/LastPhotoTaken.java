/*
 * Copyright (C) 2015  Xiaolong Tang
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

/**
 * Retrieve the last picture by criteria
 */
package com.infobeyond.nxdrive;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.provider.MediaStore;

import java.io.IOException;

public class LastPhotoTaken extends CordovaPlugin {
	public static String TAG = "LastPhotoTaken";
    public static String ACTION = "getLastPhoto";
	 
	private CallbackContext callbackContext = null;
	private Context context = null;

    public class Result {
        public long timestamp = 0;
        public String path = null;
        public int totalImages = 0;
        public int newImages = 0;
        public int waitingTobeUploaded = 0;
        public String filename = null;
        public long size = 0;
        public JSONObject toJSONObject() throws JSONException {
            return new JSONObject("{path:" + JSONObject.quote(path) +
                    ",timestamp:" + timestamp +
                    ",size:" + size +
                    ",filename:" + JSONObject.quote(filename) +
                    ",totalImages:" + totalImages +
                    ",newImages:" + newImages +
                    ",waitingImages:" + waitingTobeUploaded +
                    "}");
        }
    }
    
    @Override 
    public void initialize(CordovaInterface cordova, CordovaWebView webView)
    {
    	super.initialize(cordova, webView);    	
    	this.context = this.cordova.getActivity().getApplicationContext();
    }    

    @Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
		if (action.equals(ACTION)) {
            Helper helper = new Helper(args, callbackContext, context);
            cordova.getThreadPool().execute(helper);
        }
		return true;
    }

    public class Helper implements Runnable {
        private CallbackContext callbackContext = null;
        private Context context = null;

        int max;
        double startTimeTick;
        double endTimeTick;
        double scanStartTimeTick;
        boolean foundImage;
        boolean foundVideo;
        Result searchResult;

        private JSONArray args;

        public Helper(JSONArray args, CallbackContext callbackContext, Context context) throws JSONException {
            this.callbackContext = callbackContext;
            this.context = context;
            max = args.getInt(0);

            startTimeTick = args.getDouble(1);
            endTimeTick = args.getDouble(2);
            scanStartTimeTick = args.getDouble(3);
            foundImage = false;
            foundVideo = false;
            searchResult = new Result();
        }

        @Override
        public void run() {

            try {
                // Expect four parameters: max representing the upperbound of the number of files
                // which can be returned each time, and three time stamps
                // TODO: max is supposed to be used when we return a list of images rather than a single image...

                IImageList imageList = ImageManager.makeImageList(context.getContentResolver(),
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        ImageManager.SORT_DESCENDING);

                searchResult.totalImages = imageList.getCount();

                for (int i = 0; i < imageList.getCount(); i++) {
                    IImage image = imageList.getImageAt(i);
                    long timestamp = image.getDateTaken();
                    // Count new images
                    if (timestamp > scanStartTimeTick) {
                        searchResult.newImages++;
                        continue;
                    }
                    // Skip those pictures between scanStartTimeTick and startTimeTick
                    if (timestamp >= startTimeTick) {
                        continue;
                    }
                    // Once we pass endTimeTick, we can stop right now
                    if (endTimeTick >= timestamp) {
                        break;
                    }
                    // The first time we reach here, the image accessible is what we are interested
                    // in.
                    if (!foundImage) {
                        searchResult.timestamp = timestamp;
                        searchResult.path = image.fullSizeImageUri().toString();

                        // To determine the extension of a file name, we may
                        // map the MimeType to a known extension. Refer Apache Tika...
                        // A lightweight way is to extract the extension from the
                        // data path
                        String datapath = image.getDataPath();
                        searchResult.filename = datapath.substring(datapath.lastIndexOf("\\") + 1);
                        searchResult.size = image.getSize();
                        foundImage = true;
                    } else {
                        // Count images waiting to be scanned next
                        searchResult.waitingTobeUploaded++;
                    }
                }

                IImageList videoList = ImageManager.makeImageList(context.getContentResolver(),
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        ImageManager.SORT_DESCENDING);

                searchResult.totalImages += videoList.getCount();

                for (int i = 0; i < videoList.getCount(); i++) {
                    IImage image = videoList.getImageAt(i);
                    long timestamp = image.getDateTaken();
                    // Count new images
                    if (timestamp > scanStartTimeTick) {
                        searchResult.newImages++;
                        continue;
                    }
                    // Skip those pictures between scanStartTimeTick and startTimeTick
                    if (timestamp >= startTimeTick) {
                        continue;
                    }
                    // Once we pass endTimeTick, we can stop right now
                    if (endTimeTick >= timestamp) {
                        break;
                    }
                    // The first time we reach here, the image accessible is what we are interested
                    // in.
                    if (!foundVideo) {
                        if (!foundImage || searchResult.timestamp < timestamp) {
                            searchResult.timestamp = timestamp;
                            searchResult.path = image.fullSizeImageUri().toString();

                            // To determine the extension of a file name, we may
                            // map the MimeType to a known extension. Refer Apache Tika...
                            // A lightweight way is to extract the extension from the
                            // data path
                            String datapath = image.getDataPath();
                            searchResult.filename = datapath.substring(datapath.lastIndexOf("\\") + 1);
                            searchResult.size = image.getSize();
                            foundVideo = true;
                        } else {
                            searchResult.waitingTobeUploaded++;
                        }
                    } else {
                        // Count images waiting to be scanned next
                        searchResult.waitingTobeUploaded++;
                    }
                }

                // Return
                if (foundImage || foundVideo) {
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, searchResult.toJSONObject()));
                } else {
                    this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "There are no photos."));
                }
            } catch (JSONException e) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION, "Something wrong."));
            }
        }
    }
}

