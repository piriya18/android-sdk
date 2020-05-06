package com.example.mylibrary;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.lang.ref.WeakReference;
import java.nio.charset.Charset;

import okio.Buffer;
import okio.BufferedSource;
import okio.Okio;

/**
 * Reads and validates the configuration from res/raw/config.json file.
 */
public class ConfigManager {

    private static WeakReference<ConfigManager> sInstance = new WeakReference<>(null);

    private final Context context;
    private final Resources resources;

    private JSONObject configJson;
    private String clientId;
    private String clientSecret;
    private String scope;
    private Uri redirectUri;
    private Uri authEndpointUri;
    private Uri tokenEndpointUri;
    private Uri userInfoEndpointUri;
    private Uri logoutEndpointUri;

    private ConfigManager(Context context, int rawid) {

        this.context = context;
        resources = context.getResources();
        readConfiguration(rawid);
    }

    /**
     * Gives an instance of the ConfigManager class.
     *
     * @param context Context object with information about the current state of the application.
     * @return ConfigManager instance.
     */
    public static ConfigManager getInstance(Context context, int rawid) {

        ConfigManager config = sInstance.get();
        if (config == null) {
            config = new ConfigManager(context, rawid);
            sInstance = new WeakReference<>(config);
        }

        return config;
    }
    /**
     * Returns the client id specified in the res/raw/config.json file.
     *
     * @return Client ID.
     */
    @NonNull
    public String getClientId() {

        return clientId;
    }

    /**
     * Returns the authorization scope specified in the res/raw/config.json file.
     *
     * @return Authorization Scope.
     */
    @NonNull
    public String getScope() {

        return scope;
    }

    /**
     * Returns the redirect URI specified in the res/raw/config.json file.
     *
     * @return Redirect URI.
     */
    @NonNull
    public Uri getRedirectUri() {

        return redirectUri;
    }

    /**
     * Returns the authorization endpoint URI specified in the res/raw/config.json file.
     *
     * @return Authorization Endpoint URI.
     */
    @NonNull
    public Uri getAuthEndpointUri() {

        return authEndpointUri;
    }

    /**
     * Returns the token endpoint URI specified in the res/raw/config.json file.
     *
     * @return Token Endpoint URI.
     */
    @NonNull
    public Uri getTokenEndpointUri() {

        return tokenEndpointUri;
    }

    /**
     * Returns the user info endpoint URI specified in the res/raw/config.json file.
     *
     * @return User Info Endpoint URI.
     */
    @NonNull
    public Uri getUserInfoEndpointUri() {

        return userInfoEndpointUri;
    }

    /**
     * Returns the logout endpoint URI specified in the res/raw/config.json file.
     *
     * @return LogoutRequest Endpoint URI.
     */
    @NonNull
    public Uri getLogoutEndpointUri() {

        return logoutEndpointUri;
    }

    /**
     * Returns the client secret specified in the res/raw/config.json file.
     *
     * @return Client secret.
     */
    @NonNull
    public String getClientSecret() {

        return clientSecret;
    }

    /**
     * Reads the configuration values.
     *
     */
    private void readConfiguration(int rawid)  {

        BufferedSource configSource = Okio.buffer(Okio.source(resources.openRawResource(rawid)));
        Buffer configData = new Buffer();

        try {
            configSource.readAll(configData);
            configJson = new JSONObject(configData.readString(Charset.forName("UTF-8")));
        } catch (IOException ex) {

        } catch (JSONException ex) {

        }
        clientId = getRequiredConfigString("client_id");
        clientSecret = getRequiredConfigString("client_secret");
        scope = getRequiredConfigString("authorization_scope");
        redirectUri = getRequiredConfigUri("redirect_uri");
        authEndpointUri = getRequiredConfigUri("authorization_endpoint");
        tokenEndpointUri = getRequiredConfigUri("token_endpoint");
        userInfoEndpointUri = getRequiredConfigUri("userinfo_endpoint");
        logoutEndpointUri = getRequiredConfigUri("end_session_endpoint");
    }

    /**
     * Returns the Config String of the the particular property name.
     *
     * @param propName Property name.
     * @return Property value.
     */
    @NonNull
    private String getRequiredConfigString(String propName) {

        String value = configJson.optString(propName);

        if (value != null) {
            value = value.trim();
            if (TextUtils.isEmpty(value)) {
                value = null;
            }
        }

        if (value == null) {
            Log.e("ConfigManager", propName + " is required but not specified in the configuration");
        }

        return value;
    }

    /**
     * Returns the Config URI specified by the property name.
     *
     * @param propName Property name.
     * @return Config URI.
     */
    @NonNull
    private Uri getRequiredConfigUri(String propName) {

        String uriStr = getRequiredConfigString(propName);
        Uri uri = null;

        try {
            uri = Uri.parse(uriStr);
        } catch (Throwable ex) {
            Log.e("ConfigManager", propName + "could not be parsed ");
        }
        return uri;
    }
}
