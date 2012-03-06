package com.mendhak.gpslogger.senders.gdocs;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import com.google.api.client.auth.oauth2.draft10.AccessTokenResponse;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAccessTokenRequest;
import com.google.api.client.googleapis.auth.oauth2.draft10.GoogleAuthorizationRequestUrl;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson.JacksonFactory;
import com.mendhak.gpslogger.common.Utilities;


public class GDocsHelper
{
    /** Value of the "Client ID" shown under "Client ID for installed applications". */
    //private static final String CLIENT_ID = "";

    /** Value of the "Client secret" shown under "Client ID for installed applications". */
    //private static final String CLIENT_SECRET = "";

    /** OAuth 2 scope to use */
    //https://docs.google.com/feeds/ gives full access to the user's documents
    private static final String SCOPE = "https://docs.google.com/feeds/";

    /** OAuth 2 redirect uri */
    private static final String REDIRECT_URI = "http://localhost";


    public static void SaveAccessToken(AccessTokenResponse accessTokenResponse, Context applicationContext)
    {
        //Store in preferences, we'll use it later.
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("GDOCS_ACCESS_TOKEN",accessTokenResponse.accessToken );
        editor.putLong("GDOCS_EXPIRES_IN", accessTokenResponse.expiresIn);
        editor.putString("GDOCS_REFRESH_TOKEN", accessTokenResponse.refreshToken);
        editor.putString("GDOCS_SCOPE", accessTokenResponse.scope);
        editor.commit();
    }

    public static void ClearAccessToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        SharedPreferences.Editor editor = prefs.edit();

        editor.remove("GDOCS_ACCESS_TOKEN");
        editor.remove("GDOCS_EXPIRES_IN");
        editor.remove("GDOCS_REFRESH_TOKEN");
        editor.remove("GDOCS_SCOPE");
        editor.commit();
    }

    public static AccessTokenResponse GetAccessToken(Context applicationContext)
    {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(applicationContext);
        AccessTokenResponse atr = new AccessTokenResponse();

        atr.accessToken = prefs.getString("GDOCS_ACCESS_TOKEN","");
        atr.expiresIn = prefs.getLong("GDOCS_EXPIRES_IN",0);
        atr.refreshToken = prefs.getString("GDOCS_REFRESH_TOKEN","");
        atr.scope =  prefs.getString("GDOCS_SCOPE","");

        if(atr.accessToken.length() == 0 || atr.refreshToken.length() == 0)
        {
            return null;
        }
        else
        {
            return atr;
        }

    }
    
    public static boolean IsLinked(Context applicationContext)
    {
        return (GetAccessToken(applicationContext) != null);
    }


    public static String GetAuthorizationRequestUrl(Context applicationContext)
    {
        return  new GoogleAuthorizationRequestUrl(GetClientID(applicationContext), REDIRECT_URI, SCOPE).build();
    }

    public static boolean IsSuccessfulRedirectUrl(String url)
    {
        return url.startsWith(REDIRECT_URI);
    }
    
    

    private static String GetClientID(Context applicationContext)
    {
        int RClientId = applicationContext.getResources().getIdentifier(
                    "gdocs_clientid", "string", applicationContext.getPackageName());
                            
                    
        return applicationContext.getString(RClientId);
    }
    
    private static String GetClientSecret(Context applicationContext)
    {

        int RClientSecret = applicationContext.getResources().getIdentifier(
                    "gdocs_clientsecret", "string", applicationContext.getPackageName());

        return applicationContext.getString(RClientSecret);
    }

    public static AccessTokenResponse GetAccessTokenResponse(String code, Context applicationContext)
    {

        try
        {

           return new GoogleAccessTokenRequest.GoogleAuthorizationCodeGrant(
                    new NetHttpTransport(),
                    new JacksonFactory(),
                    GetClientID(applicationContext),
                    GetClientSecret(applicationContext),
                    code,
                    REDIRECT_URI).execute();
        }
        catch (Exception e)
        {
            Utilities.LogError("GDocsHelper.GetAccessTokenResponse", e);
        }

        return null;
    }

    public static void SaveAccessTokenFromUrl(String url, Context applicationContext)
    {
        String code = extractCodeFromUrl(url);
        AccessTokenResponse accessTokenResponse =  GDocsHelper.GetAccessTokenResponse(code, applicationContext);
        GDocsHelper.SaveAccessToken(accessTokenResponse, applicationContext);
    }

    private static String extractCodeFromUrl(String url)
    {
        return url.substring(REDIRECT_URI.length() + 7, url.length());
    }
}
