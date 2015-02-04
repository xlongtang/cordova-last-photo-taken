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
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.content.ContentProvider;
import android.database.Cursor;
import android.provider.MediaStore;

public class LastPhotoTaken extends CordovaPlugin {
	public static String TAG = "LastPhotoTaken";
    public static String ACTION = "getLastPhoto";

    final String[] projection = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
    final String orderBy = MediaStore.Images.Media.DATE_ADDED;
	 
	private CallbackContext callbackContext = null;
	private Context context = null;
	
    public class Result {
        public double timestamp = 0.0;
        public String path = null;
        public JSONObject toJSONObject() throws JSONException {
            return new JSONObject(
                                  "{path:" + path +
                                  ",timestamp:" + timestamp + "}");
        }
    }
    
    /**
     * Ctor
     */
    public LastPhotoTaken() {
    	super();
    	this.context = this.cordova.getActivity().getApplicationContext();
    }
	 
	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
		if (action.equals(ACTION)) {
            // Expect three params: max, and a pair of time ticks
            int max = args.getInt(0);
            double startTimeTick = args.getDouble(1);
            double endTimeTick = args.getDouble(2);
            boolean found = false;
            Result searchResult = new Result();
           
            Cursor cursor =  MediaStore.Images.Media.query(context.getContentResolver(),
            		MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, orderBy);

            if (cursor.moveToFirst()) {
                int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                
                do {
                    // Do some math here ...
                    String url = cursor.getString(dataColumn);
                    // COnvert it into a path
                    searchResult.path = url;
                    // timeStamp = cursor.getDouble(dateColumn);
                    found = true;
                    break;
                } while (cursor.moveToNext());
            }
            cursor.close();

            // Return
            if (found) {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, searchResult.toJSONObject()));
            } else {
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, "There are no photos."));
            }
        } else {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.IO_EXCEPTION, "Something wrong."));
        }
		return true;
    }
}

