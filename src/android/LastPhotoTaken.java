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
        public JSONObject toJSONObject() throws JSONException {
            return new JSONObject(
                                  "{path:" + JSONObject.quote(path) +
                                  ",timestamp:" + timestamp + "}");
        }
    }
    
    @Override 
    public void initialize(CordovaInterface cordova, CordovaWebView webView)
    {
    	super.initialize(cordova, webView);    	
    	this.context = this.cordova.getActivity().getApplicationContext();
    }
    
    /*
    // TODO: Change public to protected
    private Uri contentUri(Uri baseUri, long id) {
        // TODO: avoid using exception for most cases
        try {
            // does our uri already have an id (single image query)?
            // if so just return it
            long existingId = ContentUris.parseId(baseUri);
            if (existingId != id) Log.e(TAG, "id mismatch");
            return baseUri;
        } catch (NumberFormatException ex) {
            // otherwise tack on the id
            return ContentUris.withAppendedId(baseUri, id);
        }
    } */
    

    @Override
	public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
		if (action.equals(ACTION)) {
            // Expect three params: max, and a pair of time ticks
            int max = args.getInt(0);
            double startTimeTick = args.getDouble(1);
            double endTimeTick = args.getDouble(2);
            boolean found = false;
            Result searchResult = new Result();
           
            /*
            Uri baseUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

            Cursor cursor =  MediaStore.Images.Media.query(context.getContentResolver(),
            		baseUri, projection, null, null, orderBy);

            if (cursor.moveToFirst()) {
                int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int dateColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                
                do {
                    // Do some math here ...
                    String url = cursor.getString(dataColumn);
                    // COnvert it into a path
                    searchResult.path = url;
                    searchResult.timestamp = cursor.getLong(dateColumn); 
                    found = true;
                    break;
                } while (cursor.moveToNext());
            }
            cursor.close();
            */
            

            IImageList imageList = ImageManager.makeImageList(context.getContentResolver(), 
            		MediaStore.Images.Media.EXTERNAL_CONTENT_URI, ImageManager.SORT_DESCENDING);
            
            for(int i = 0; i < imageList.getCount(); i++)
            {
            	IImage image = imageList.getImageAt(i);
            	long timestamp = image.getDateTaken();
            	if (timestamp >= startTimeTick || endTimeTick >= timestamp) 
            	{
            		continue;
            	}
            	// Found one
            	searchResult.timestamp = timestamp;
            	searchResult.path = image.fullSizeImageUri().toString();
            	found = true;
            	break;
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

