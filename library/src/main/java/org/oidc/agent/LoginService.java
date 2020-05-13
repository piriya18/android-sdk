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

import androidx.annotation.NonNull;
import androidx.browser.customtabs.CustomTabsIntent;

import net.openid.appauth.AuthState;
import net.openid.appauth.AuthorizationException;
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
    private Context mContext;
    private OAuth2TokenResponse mOAuth2TokenResponse;
    private AuthorizationService mAuthorizationService;
    private static final String LOG_TAG = "LoginService";
    private AuthState authState;
    private static LoginService loginService;

    private LoginService(Context context) {
        mContext = context;
        if (mConfigManager == null) {
            mConfigManager = ConfigManager.getInstance(context);
        }
    }

    public static LoginService getInstance(@NonNull Context context) {
        if (loginService == null) {
            loginService = new LoginService(context);
        }
        return loginService;
    }

    public void doAuthorization(PendingIntent completionIntent, PendingIntent cancelIntent) {

        AuthorizationServiceConfiguration.fetchFromUrl(mConfigManager.getDiscoveryUri(), new AuthorizationServiceConfiguration.RetrieveConfigurationCallback() {
            @Override
            public void onFetchConfigurationCompleted(AuthorizationServiceConfiguration serviceConfiguration,
                    AuthorizationException ex) {
                if (ex != null) {
                    Log.w(LOG_TAG, "Failed to retrieve configuration for ", ex);
                } else {
                    Log.d(LOG_TAG, "configuration retrieved for " + ", proceeding");
                    doAuthorize(serviceConfiguration, mContext, completionIntent, cancelIntent);

                }
            }
        });
    }

    private void doAuthorize(AuthorizationServiceConfiguration configuration, Context context,
            PendingIntent completionIntent, PendingIntent cancelIntent) {

        authState = new AuthState(configuration);
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(configuration,
                mConfigManager.getClientId(), ResponseTypeValues.CODE,
                mConfigManager.getRedirectUri());
        builder.setScopes(mConfigManager.getScope());
        AuthorizationRequest request = builder.build();
        mAuthorizationService = new AuthorizationService(context);
        CustomTabsIntent.Builder intentBuilder = mAuthorizationService
                .createCustomTabsIntentBuilder(request.toUri());
        customTabIntent.set(intentBuilder.build());
        mAuthorizationService.performAuthorizationRequest(request, completionIntent, cancelIntent,
                customTabIntent.get());
        Log.d(LOG_TAG, "Handling authorization request for service provider :" + mConfigManager.getClientId());
    }

    public void handleAuthorization(Intent intent, TokenRequest.TokenRespCallback callback) {

        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        mOAuth2TokenResponse = new OAuth2TokenResponse();
        new TokenRequest(mAuthorizationService, mOAuth2TokenResponse, response, callback).execute();
        Log.d(LOG_TAG,
                "Handling token request for service provider :" + mConfigManager.getClientId());

    }

    public void logout(Context context) {

        StringBuffer url = new StringBuffer();
        url.append(mConfigManager.getLogoutUri());
        url.append("?id_token_hint=");
        url.append(mOAuth2TokenResponse.getIdToken());
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
