package com.onetrust.mobileConsentCollection;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.adobe.marketing.mobile.Event;
import com.adobe.marketing.mobile.Extension;
import com.adobe.marketing.mobile.ExtensionApi;
import com.adobe.marketing.mobile.ExtensionError;
import com.adobe.marketing.mobile.ExtensionErrorCallback;
import com.adobe.marketing.mobile.ExtensionUnexpectedError;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import com.android.volley.AuthFailureError;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class otsdk_adobe extends Extension implements ExtensionErrorCallback<ExtensionError>{

    private static final String TAG = "mobileConsentCollection";

    private RequestQueue m_RequestQueue;
    private int m_lastWebRequestStatusCode;
    private Context m_context;

    public otsdk_adobe(ExtensionApi extensionApi) {
        super(extensionApi);
        m_context = getAdobeContext();
        m_RequestQueue = Volley.newRequestQueue(m_context);
        initExtension();
    }

    private void initExtension() {
        // Register an Event Listener for hub sharedState events
        getApi().registerEventListener("com.adobe.eventType.hub", "com.adobe.eventSource.sharedState", otsdk_adobe_listener.class, this);
    }


    @Override
    protected String getName() {
        return "com.OneTrust.OTSDK_Adobe";
    }

    @Override
    protected String getVersion() {
        return "1.0.0";
    }

    @Override
    protected void onUnregistered() {
        super.onUnregistered();
    }

    @Override
    protected void onUnexpectedError(ExtensionUnexpectedError extensionUnexpectedError) {
        super.onUnexpectedError(extensionUnexpectedError);
    }

    @Override
    public void error(ExtensionError extensionError) {
        // something went wrong...
        Log.e(TAG, String.format("An error occurred in the otsdk_adobe extension %d %s", extensionError.getErrorCode(), extensionError.getErrorName()));
    }



    void handleAdobeEvent(final Event event) {

        Map<String, Object> thisEventData = event.getEventData();
        String stateOwner = thisEventData.get("stateowner").toString();

        if (stateOwner != null) {
            if (stateOwner.equalsIgnoreCase("com.adobe.module.configuration")) {
                Map<String, Object> configurationSharedState = this.getApi().getSharedEventState("com.adobe.module.configuration", event, new ExtensionErrorCallback<ExtensionError>() {
                    @Override
                    public void error(ExtensionError extensionError) {
                        Log.e(TAG, String.format("An error occurred in the MyExtension %d %s", extensionError.getErrorCode(), extensionError.getErrorName()));
                    }
                });

                if (configurationSharedState !=null){

                    String privacySetting = configurationSharedState.get("global.privacy").toString();
                    Map<String, Object> OneTrustKeys = (Map)configurationSharedState.get("collectionPointKeys");
                    //Log.e(TAG, String.format("privacySetting %s", privacySetting));
                    //Log.e(TAG, String.format("OneTrustKeys %s", OneTrustKeys.toString()));


                    // if this privacySetting value is different from what we alredy have, then send it to OneTrust
                    SharedPreferences settings =  m_context.getSharedPreferences("com.onetrust.consent.sdk", Context.MODE_PRIVATE);
                    String savedPrivacySetting = settings.getString("currentPrivacySetting","");

                    if (!privacySetting.equalsIgnoreCase(savedPrivacySetting)){
                        SharedPreferences.Editor editor = settings.edit();
                        editor.putString("currentPrivacySetting", privacySetting);
                        editor.commit();

                        Log.e(TAG, String.format("new privacySetting %s", privacySetting));

                        saveDataToOneTrust(OneTrustKeys,privacySetting);

                    }

                }

            }
        }

    }


    public void saveDataToOneTrust(Map<String, Object> OneTrustKeys,  String privacySetting){

        String CP_Endpoint = OneTrustKeys.get("Endpoint").toString();
        String CP_APIToken = OneTrustKeys.get("Token").toString();
        String CP_PurposeID = OneTrustKeys.get("PurposeId").toString();
        String CP_IdentifierType = OneTrustKeys.get("DataSubjectIDType").toString();
        String IdentifierValue = "";

        SharedPreferences diskStorage =  m_context.getSharedPreferences("com.onetrust.consent.sdk", Context.MODE_PRIVATE);

        if (CP_IdentifierType.equalsIgnoreCase("Random GUID")){
            IdentifierValue = diskStorage.getString("RandomGUID","UNKNOWN");
        }else{
            IdentifierValue = diskStorage.getString("SupplyOwn","UNKNOWN");
        }

        try {

            JSONObject purposeObject = new JSONObject();
            purposeObject.put("Id", CP_PurposeID);

            JSONArray purposesArray = new JSONArray();
            purposesArray.put(purposeObject);

            JSONObject customPayload = new JSONObject();
            customPayload.put("Adobe Launch Privacy Setting", privacySetting);


            JSONObject data2Send = new JSONObject();
            data2Send.put("identifier", IdentifierValue);
            data2Send.put("requestInformation", CP_APIToken);
            data2Send.put("test", false);
            data2Send.put("purposes", purposesArray);
            data2Send.put("customPayload", customPayload);

            final String requestBody = data2Send.toString();


            StringRequest stringRequest = new StringRequest(Request.Method.POST, CP_Endpoint, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {

                    if (m_lastWebRequestStatusCode == 200){
                        Log.i(TAG, "Saved to OneTrust Successfully.");
                        return;

                    }else{
                        // did not get success code
                        Log.e(TAG, "An error saving to OneTrust! ");
                        return;
                    }

                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e(TAG, String.format("An error saving to OneTrust! - %s", error.toString()));
                    return;
                }
            }) {

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    m_lastWebRequestStatusCode = response.statusCode;
                    return super.parseNetworkResponse(response);
                }

                @Override
                public Map<String, String> getHeaders() throws AuthFailureError {
                    HashMap<String, String> headers = new HashMap<String, String>();
                    headers.put("Content-Type", "application/json");
                    headers.put("cache-control", "no-cache");
                    return headers;
                }

                @Override
                public byte[] getBody() {
                    try {
                        return requestBody.getBytes("utf-8");

                    } catch (UnsupportedEncodingException e) {
                        Log.e(TAG, String.format("An error saving to OneTrust - getBody(! - %s", e.toString()));
                        return null;
                    }

                }

            };

            stringRequest.setRetryPolicy(new DefaultRetryPolicy(
                    120000,
                    DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                    DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));


            m_RequestQueue.add(stringRequest);


        } catch (JSONException e) {
            e.printStackTrace();
            return;

        } catch (Exception e) {
            e.printStackTrace();
            return;

        }


    }


    private Context getAdobeContext() {

        Context context = null;

        try {
            Class cls = Class.forName("com.adobe.marketing.mobile.App");
            Field appContext = cls.getDeclaredField("appContext");
            appContext.setAccessible(true);

            context = (Context)appContext.get(null);

        } catch (Exception e) {
            Log.e(TAG, "Unable to get Context", e);
        }

        return context;
    }

}
