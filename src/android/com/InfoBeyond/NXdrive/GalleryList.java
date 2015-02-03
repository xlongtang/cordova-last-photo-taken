/**
 * Retrieve the last picture by criteria
 */
package com.InfoBeyond.NXdrive;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.util.Log;

public class GalleryList extends CordovaPlugin {
	public static String TAG = "GalleryList";
    public static string ACTION = "getLastPhoto";

    final String[] projection = { MediaStore.Images.Media.DATA, MediaStore.Images.Media.DATE_TAKEN};
    final String orderBy = MediaStore.Images.Media.DATE_ADDED;
	 
	private CallbackContext callbackContext;

    public class Result {
        public double timestamp = 0.0;
        public string path = null;
        public JSONObject toJSONObject() throws JSONException {
            return new JSONObject(
                                  "{path:" + path +
                                  ",timestamp:" + timestamp + "}");
        }
    }
	 
	public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;
		if (action.equals(ACTION)) {
            // Expect three params: max, and a pair of time ticks
            int max = args.getInt(0);
            double startTimeTick = args.getFloat(1);
            double endTimeTick = args.getFloat(2);
            boolean found = false;
            Result searchResult = new Result();

            Cursor cursor =  managedQuery(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, orderBy);

            if (cursor.moveToFirst()) {
                int dataColumn = cursor.getColumnIndex(MediaStore.Images.Media.DATA);
                int dateColumn = cur.getColumnIndex(MediaStore.Images.Media.DATE_TAKEN);
                
                do {
                    // Do some math here ...
                    string url = cursor.getString(dataColumn);
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
    }
    return true;
}

