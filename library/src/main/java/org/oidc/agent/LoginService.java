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

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.browser.customtabs.CustomTabsIntent;

import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ResponseTypeValues;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Handles the login process by making use of AppAuth library.
 */
public class LoginService {

    private final AtomicReference<CustomTabsIntent> customTabIntent = new AtomicReference<>();
    private ConfigManager mConfigManager;
    private OAuth2TokenResponse mOAuth2TokenResponse;
    private AuthorizationService mAuthorizationService;
    private static final String LOG_TAG = "LoginService";

    public void doAuthorization(Context context, ConfigManager configManager,
            PendingIntent completionIntent, PendingIntent cancelIntent) {

        this.mConfigManager = configManager;
        AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                configManager.getAuthEndpointUri(), configManager.getTokenEndpointUri());
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                serviceConfiguration, configManager.getClientId(), ResponseTypeValues.CODE,
                configManager.getRedirectUri());
        builder.setScopes(configManager.getScope());
        AuthorizationRequest request = builder.build();
        mAuthorizationService = new AuthorizationService(context);
        CustomTabsIntent.Builder intentBuilder = mAuthorizationService
                .createCustomTabsIntentBuilder(request.toUri());
        customTabIntent.set(intentBuilder.build());
        mAuthorizationService.performAuthorizationRequest(request, completionIntent, cancelIntent,
                customTabIntent.get());
        Log.d(LOG_TAG, "Handling authorization request for service provider :" + configManager
                .getClientId());

    }

    public void handleAuthorization(Intent intent, TokenRequest.TokenRespCallback callback) {

        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        mOAuth2TokenResponse = new OAuth2TokenResponse();
        new TokenRequest(mAuthorizationService, response, callback).execute();
        Log.d(LOG_TAG,
                "Handling token request for service provider :" + mConfigManager.getClientId());

    }

    public void logout(Context context, String idToken) {

        StringBuffer url = new StringBuffer();
        url.append(mConfigManager.getLogoutEndpointUri());
        url.append("?id_token_hint=");
        url.append(idToken);
        url.append("&post_logout_redirect_uri=");
        url.append(mConfigManager.getRedirectUri());
        Log.d(LOG_TAG,
                "Handling logout request for service provider :" + mConfigManager.getClientId());
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.setFlags(
                Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK
                        | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        customTabsIntent.launchUrl(context, Uri.parse(url.toString()));
    }

    public OAuth2TokenResponse getTokenResponse() {
        return mOAuth2TokenResponse;
    }

    public void dispose() {
        if (mAuthorizationService != null) {
            mAuthorizationService.dispose();
        }
    }
}
