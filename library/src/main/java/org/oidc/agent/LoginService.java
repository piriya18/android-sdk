package org.oidc.sample;

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



public class LoginService {

    private final AtomicReference<CustomTabsIntent> customTabIntent = new AtomicReference<>();

    private ConfigManager configManager;
    private OAuth2TokenResponse oAuth2TokenResponse;
    private AuthorizationService authorizationService;
    private String idToken;


    public void doAuthorization(Context context, ConfigManager configManager, PendingIntent completionIntent, PendingIntent cancelIntent) {

        this.configManager = configManager;
        AuthorizationServiceConfiguration serviceConfiguration = new AuthorizationServiceConfiguration(
                configManager.getAuthEndpointUri(), configManager.getTokenEndpointUri());
        AuthorizationRequest.Builder builder = new AuthorizationRequest.Builder(
                serviceConfiguration,
                configManager.getClientId(),
                ResponseTypeValues.CODE,
                configManager.getRedirectUri()
        );
        builder.setScopes(configManager.getScope());
        AuthorizationRequest request = builder.build();
        authorizationService = new AuthorizationService(context);
        CustomTabsIntent.Builder intentBuilder = authorizationService.createCustomTabsIntentBuilder(request.toUri());

        customTabIntent.set(intentBuilder.build());

        authorizationService.performAuthorizationRequest(request, completionIntent, cancelIntent, customTabIntent.get());
    }

    public void handleAuthorization(Intent intent, TokenRequest.TokenRespCallback callback) throws InterruptedException {

        String clientSecret = configManager.getClientSecret();
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        oAuth2TokenResponse = new OAuth2TokenResponse();
        new TokenRequest(authorizationService, response,clientSecret, callback).execute();
    }

    public void logout(Context context, String idToken) {

        StringBuffer url = new StringBuffer();
        url.append(configManager.getLogoutEndpointUri());
        url.append("?id_token_hint=");
        url.append(idToken);
        url.append("&post_logout_redirect_uri=");
        url.append(configManager.getRedirectUri());

        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.intent.setFlags(Intent.FLAG_ACTIVITY_NO_HISTORY | Intent.FLAG_ACTIVITY_NEW_TASK
                | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        customTabsIntent.launchUrl(context, Uri.parse(url.toString()));
    }

    public OAuth2TokenResponse getTokenResponse() {
        return oAuth2TokenResponse;
    }

}
