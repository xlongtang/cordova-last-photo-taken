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

import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

public class LastPhotoTaken extends CordovaPlugin {
	public static String TAG = "LastPhotoTaken";
    public static String ACTION = "getLastPhoto";

    final String[] projection = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
    final String orderBy = MediaStore.Images.Media.DATE_ADDED;
	 
	private CallbackContext callbackContext = null;
	private Context context = null;
	
    public class Result {
        public long timestamp = 0;
        public String path = null;
        public int totalImages = 0;
        public int newImages = 0;
        public int waitingTobeUploaded = 0;
        public JSONObject toJSONObject() throws JSONException {
            return new JSONObject(
                                  "{path:" + JSONObject.quote(path) +
                                  ",timestamp:" + timestamp + 
                                  ",totalImages:" + totalImages + 
                                  ",newImages:" + newImages + 
                                  ",waitingImages" + waitingTobeUploaded + 
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
            // Expect three params: max, and a pair of time ticks
			// TODO: max is supposed to be used when we return a list of images rather than a single image...
			@SuppressWarnings("unused")
            int max = args.getInt(0);
			
            double startTimeTick = args.getDouble(1);
            double endTimeTick = args.getDouble(2);
            double scanStartTimeTick = args.getDouble(3);
            boolean found = false;
            Result searchResult = new Result();            

            IImageList imageList = ImageManager.makeImageList(context.getContentResolver(), 
            		MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageManager.SORT_DESCENDING);
            
            searchResult.totalImages = imageList.getCount();
            
            for(int i = 0; i < imageList.getCount(); i++)
            {
            	IImage image = imageList.getImageAt(i);
            	long timestamp = image.getDateTaken();
            	// Count new images
            	if (timestamp > scanStartTimeTick) 
            	{
            		searchResult.newImages++;
            		continue;
            	}
            	// Skip those pictures between scanStartTimeTick and startTimeTick
            	if (timestamp >= startTimeTick) {
            		continue;
            	}
            	// Once we pass endTimeTick, we can stop right now
            	if (endTimeTick >= timestamp)
            	{
            		break;
            	}
            	// The first time we reach here, the image accessible is what we are interested 
            	// in. 
            	if (!found)
            	{
            		searchResult.timestamp = timestamp;
            		searchResult.path = image.fullSizeImageUri().toString();
            		found = true;
            	} 
            	else 
            	{
            		// Count images waiting to be scanned next
            		searchResult.waitingTobeUploaded++;            		
            	}
            } 
            
            // Return
            if (found) {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, searchResult.toJSONObject()));
            } else {
                this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "There are no photos."));
            }
        } else {
            this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.IO_EXCEPTION, "Something wrong."));
        }
		return true;
    }
}

