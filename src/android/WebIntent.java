package com.borismus.webintent;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.apache.cordova.CordovaActivity;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Intent;
import android.content.IntentFilter;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.net.Uri;
import android.text.Html;
import android.util.Log;
import android.os.Bundle;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaResourceApi;
import org.apache.cordova.PluginResult;

/**
 * WebIntent is a PhoneGap plugin that bridges Android intents and web
 * applications:
 * 
 * 1. web apps can spawn intents that call native Android applications. 2.
 * (after setting up correct intent filters for PhoneGap applications), Android
 * intents can be handled by PhoneGap web applications.
 * 
 * @author boris@borismus.com
 * 
 */
public class WebIntent extends CordovaPlugin {

    private CallbackContext onNewIntentCallbackContext = null;
    private CallbackContext onReceiveBroadcastCallbackContext = null;
    private BroadcastReceiver broadcastReceiver = null;

    //public boolean execute(String action, JSONArray args, String callbackId) {
    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) {
        Log.d("WebIntent", "execute action " + action);
        try {

            if (action.equals("startActivity")) {
                Log.d("WebIntent", "startActivity " + args.toString());
                if (args.length() != 1) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                // Parse the arguments
                final CordovaResourceApi resourceApi = webView.getResourceApi();
                JSONObject obj = args.getJSONObject(0);
                String intentAction = obj.has("action") ? obj.getString("action") : null;
                String type = obj.has("type") ? obj.getString("type") : null;
                String packageName = obj.has("packageName") ? obj.getString("packageName") : null;
                Uri uri = obj.has("url") ? resourceApi.remapUri(Uri.parse(obj.getString("url"))) : null;
                JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
                Map<String, String> extrasMap = new HashMap<String, String>();

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        String value = extras.getString(key);
                        extrasMap.put(key, value);
                    }
                }

                startActivity(intentAction, packageName, uri, type, extrasMap);
                //return new PluginResult(PluginResult.Status.OK);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                return true;

            } else if (action.equals("hasExtra")) {
                if (args.length() != 1) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                Intent i = ((CordovaActivity)this.cordova.getActivity()).getIntent();
                String extraName = args.getString(0);
                //return new PluginResult(PluginResult.Status.OK, i.hasExtra(extraName));
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, i.hasExtra(extraName)));
                return true;

            } else if (action.equals("getExtra")) {
                if (args.length() != 1) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                Intent i = ((CordovaActivity)this.cordova.getActivity()).getIntent();
                String extraName = args.getString(0);
                if (i.hasExtra(extraName)) {
                    //return new PluginResult(PluginResult.Status.OK, i.getStringExtra(extraName));
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, i.getStringExtra(extraName)));
                    return true;
                } else {
                    //return new PluginResult(PluginResult.Status.ERROR);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR));
                    return false;
                }
            } else if (action.equals("getUri")) {
                if (args.length() != 0) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                Intent i = ((CordovaActivity)this.cordova.getActivity()).getIntent();
                String uri = i.getDataString();
                //return new PluginResult(PluginResult.Status.OK, uri);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, uri));
                return true;
            } else if (action.equals("onNewIntent")) {
            	//save reference to the callback; will be called on "new intent" events
                this.onNewIntentCallbackContext = callbackContext;
        
                if (args.length() != 0) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                
                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(true); //re-use the callback on intent events
                callbackContext.sendPluginResult(result);
                return true;
            } else if (action.equals("registerBroadcastReceiver")) {
                if (args.length() != 1) {
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }
                if (WebIntent.this.broadcastReceiver != null) {
                   Log.d("WebIntent", "BroadcastReceiver already registered - unregister first");
                   ((CordovaActivity)this.cordova.getActivity()).unregisterReceiver(WebIntent.this.broadcastReceiver);
                   WebIntent.this.broadcastReceiver = null;
                   WebIntent.this.onReceiveBroadcastCallbackContext = null;
                }
                JSONObject obj = args.getJSONObject(0);
                String intentAction = obj.has("intentAction") ? obj.getString("intentAction") : null;
                Log.d("WebIntent", "registerBroadcastReceiver: registering for " + intentAction);
            	 // save reference to the callback; will be called on broadcast events
                this.onReceiveBroadcastCallbackContext = callbackContext;

                IntentFilter intentFilter = new IntentFilter(intentAction);
                WebIntent.this.broadcastReceiver = new BroadcastReceiver() {
                   @Override
                   public void onReceive(Context context, Intent intent) {
                      Log.d("WebIntent", "received " + intent + ", " + intent.getExtras());
                      JSONObject json = new JSONObject();
                      Bundle bundle = intent.getExtras();
                      Set<String> keys = bundle.keySet();
                      for (String key : keys) {
                         try {
                            json.put(key, bundle.get(key));
                         } catch(JSONException e) {
                            Log.e("WebIntent", "error converting bundle to JSON");
                         }
                      }
        	             PluginResult result = new PluginResult(PluginResult.Status.OK, json.toString());
                      result.setKeepCallback(true); 
                      WebIntent.this.onReceiveBroadcastCallbackContext.sendPluginResult(result);
                   }
                };
                ((CordovaActivity)this.cordova.getActivity()).registerReceiver(WebIntent.this.broadcastReceiver, intentFilter);
                PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                result.setKeepCallback(true); 
                callbackContext.sendPluginResult(result);
                return true;
            } else if (action.equals("unregisterBroadcastReceiver")) {
                if (WebIntent.this.broadcastReceiver != null) {
                   Log.d("WebIntent", "unregisterBroadcastReceiver");
                   ((CordovaActivity)this.cordova.getActivity()).unregisterReceiver(WebIntent.this.broadcastReceiver);
                   WebIntent.this.onReceiveBroadcastCallbackContext = null;
                   WebIntent.this.broadcastReceiver = null;
                }
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.NO_RESULT));
                return true;
            } else if (action.equals("sendBroadcast")) {
                if (args.length() != 1) {
                    //return new PluginResult(PluginResult.Status.INVALID_ACTION);
                    callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
                    return false;
                }

                // Parse the arguments
                JSONObject obj = args.getJSONObject(0);

                JSONObject extras = obj.has("extras") ? obj.getJSONObject("extras") : null;
                Map<String, String> extrasMap = new HashMap<String, String>();

                // Populate the extras if any exist
                if (extras != null) {
                    JSONArray extraNames = extras.names();
                    for (int i = 0; i < extraNames.length(); i++) {
                        String key = extraNames.getString(i);
                        String value = extras.getString(key);
                        extrasMap.put(key, value);
                    }
                }

                sendBroadcast(obj.getString("action"), extrasMap);
                //return new PluginResult(PluginResult.Status.OK);
                callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK));
                return true;
            }
            //return new PluginResult(PluginResult.Status.INVALID_ACTION);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.INVALID_ACTION));
            return false;
        } catch (JSONException e) {
            e.printStackTrace();
            String errorMessage=e.getMessage();
            //return new PluginResult(PluginResult.Status.JSON_EXCEPTION);
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.JSON_EXCEPTION,errorMessage));
            return false;
        }
    }

    @Override
    public void onNewIntent(Intent intent) {
    	 
        if (this.onNewIntentCallbackContext != null) {
        	PluginResult result = new PluginResult(PluginResult.Status.OK, intent.getDataString());
        	result.setKeepCallback(true);
         this.onNewIntentCallbackContext.sendPluginResult(result);
        }
    }

    void startActivity(String action, String packageName, Uri uri, String type, Map<String, String> extras) {

        Intent i = null;
        if (uri != null) {
            i = new Intent(action, uri);
        } else if (packageName != null) {
            i = ((CordovaActivity)this.cordova.getActivity()).getPackageManager().getLaunchIntentForPackage(packageName);
        } else {
            i = new Intent(action);
        }
        
        if (type != null && uri != null) {
            i.setDataAndType(uri, type); //Fix the crash problem with android 2.3.6
        } else {
            if (type != null) {
                i.setType(type);
            }
        }
        
        for (String key : extras.keySet()) {
            String value = extras.get(key);
            // If type is text html, the extra text must sent as HTML
            if (key.equals(Intent.EXTRA_TEXT) && type.equals("text/html")) {
                i.putExtra(key, Html.fromHtml(value));
            } else if (key.equals(Intent.EXTRA_STREAM)) {
                // allowes sharing of images as attachments.
                // value in this case should be a URI of a file
				final CordovaResourceApi resourceApi = webView.getResourceApi();
                i.putExtra(key, resourceApi.remapUri(Uri.parse(value)));
            } else if (key.equals(Intent.EXTRA_EMAIL)) {
                // allows to add the email address of the receiver
                i.putExtra(Intent.EXTRA_EMAIL, new String[] { value });
            } else {
                i.putExtra(key, value);
            }
        }
        ((CordovaActivity)this.cordova.getActivity()).startActivity(i);
    }

    void sendBroadcast(String action, Map<String, String> extras) {
        Intent intent = new Intent();
        intent.setAction(action);
        for (String key : extras.keySet()) {
            String value = extras.get(key);
            intent.putExtra(key, value);
        }

        ((CordovaActivity)this.cordova.getActivity()).sendBroadcast(intent);
    }
}
