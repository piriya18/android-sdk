
/*
 * Copyright (c) 2020, WSO2 Inc. (http://www.wso2.org) All Rights Reserved.
 *
 * WSO2 Inc. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package org.oidc.agent;

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
import org.oidc.agent.library.R;

/**
 * Reads and validates the configuration from res/raw/config.json file.
 */
public class ConfigManager {

    private static WeakReference<ConfigManager> sInstance = new WeakReference<>(null);
    private static int rawId;

    private final Context context;
    private final Resources resources;

    private JSONObject configJson;
    private String clientId;
    private String scope;
    private Uri redirectUri;
    private Uri logoutUri;
    private Uri dicoveryUri;
    private String issuerUri;
    private static final String DISCOVERY_ENDPOINT = "/oauth2/oidcdiscovery/.well-known/openid-configuration";
    private static final String OIDC_LOGOUT = "/oidc/logout";

    private static final String ISSUER_URI = "issuer_uri";

    private ConfigManager(Context context) {

        this.context = context;
        resources = context.getResources();
        readConfiguration(R.raw.oidc_config);
    }

    /**
     * Gives an instance of the ConfigManager class.
     *
     * @param context Context object with information about the current state of the application.
     * @return ConfigManager instance.
     */
    public static ConfigManager getInstance(Context context) {

        ConfigManager config = sInstance.get();
        if (config == null) {
            config = new ConfigManager(context);
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
     * Returns the dicovery endpoint URI derived from issuer uri specified in the res/raw/config
     * .json file.
     *
     * @return Token Endpoint URI.
     */
    @NonNull
    public Uri getDiscoveryUri() {

        String discoveryEnd = issuerUri + DISCOVERY_ENDPOINT;
        this.dicoveryUri = getRequiredUri(discoveryEnd);
        return dicoveryUri;
    }

    /**
     * Returns the logout endpoint URI specified in the res/raw/config.json file.
     *
     * @return LogoutRequest Endpoint URI.
     */
    @NonNull
    public Uri getLogoutUri() {

        String logoutEnd = issuerUri + OIDC_LOGOUT;
        this.logoutUri = getRequiredUri(logoutEnd);
        return this.logoutUri;
    }

    /**
     * Reads the configuration values.
     */
    private void readConfiguration(int rawid) {

        BufferedSource configSource = Okio.buffer(Okio.source(resources.openRawResource(rawid)));
        Buffer configData = new Buffer();

        try {
            configSource.readAll(configData);
            configJson = new JSONObject(configData.readString(Charset.forName("UTF-8")));
        } catch (IOException ex) {

        } catch (JSONException ex) {

        }
        clientId = getRequiredConfigString("client_id");
        scope = getRequiredConfigString("authorization_scope");
        redirectUri = getRequiredUri(getRequiredConfigString("redirect_uri"));
        issuerUri = getRequiredConfigString(ISSUER_URI);
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
            Log.e("ConfigManager",
                    propName + " is required but not specified in the configuration");
        }
        return value;
    }

    /**
     * return Config URI.
     *
     * @param uristr
     * @return Uri
     */
    private Uri getRequiredUri(String uristr) {

        Uri uri = null;
        try {
            uri = Uri.parse(uristr);
        } catch (Throwable ex) {
            Log.e("ConfigManager", uristr + "could not be parsed ");
        }
        return uri;
    }

}
