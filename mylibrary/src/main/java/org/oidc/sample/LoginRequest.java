package org.oidc.sample;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.MainThread;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationRequest;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.AuthorizationServiceConfiguration;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.ResponseTypeValues;
import net.openid.appauth.TokenResponse;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Semaphore;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;


public class LoginRequest {

    private final AtomicReference<CustomTabsIntent> customTabIntent = new AtomicReference<>();

    private ConfigManager configManager;
    private OAuth2TokenResponse oAuth2TokenResponse;
    private AuthorizationService authorizationService;


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

    @MainThread
    public void handleAuthorization(Intent intent) throws InterruptedException {

        String clientSecret = configManager.getClientSecret();
        AuthorizationResponse response = AuthorizationResponse.fromIntent(intent);
        Map<String, String> additionalParameters = new HashMap<>();
        additionalParameters.put("client_secret", clientSecret);

        ClientAuthentication clientAuthentication = new ClientSecretBasic(clientSecret);
        oAuth2TokenResponse = new OAuth2TokenResponse();

        Log.i("Tokenqqqq", "before callback");

        Semaphore semaphore = new Semaphore(0, true);

        authorizationService.performTokenRequest(response.createTokenExchangeRequest(additionalParameters),
                clientAuthentication,  new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                        Log.i("SEMAPHORE", "code1 " + semaphore.availablePermits());
                        Log.i("Tokenqqqq", "inside callback");
                        if (exception != null) {
                            Log.i("LOG_TAG", "Token Exchange failed", exception);
                        } else {
                            Log.i("Tokenqqqq", "#####********$$");
                            if (tokenResponse != null) {
                                handleCodeExchangeResponse(tokenResponse);

                                Log.i("LOG_TAG", String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                            }
                        }
                        Log.i("SEMAPHORE", "code2 " + semaphore.availablePermits());
                        semaphore.release();
                    }
                });
        Log.i("SEMAPHORE", "code3 " + semaphore.availablePermits());
        semaphore.acquire();
        Log.i("Tokenqqqq", "after callback");
        onDestroy();
    }

    private void handleCodeExchangeResponse(TokenResponse tokenResponse ) {

        Log.i("Tokenqqqq", "inside callback");
        oAuth2TokenResponse.setAccessToken(tokenResponse.accessToken);
        oAuth2TokenResponse.setIdToken(tokenResponse.idToken);
        Log.i("Token", oAuth2TokenResponse.getIdToken());

    }

    public void singleLogout(Context context, ConfigManager configManager, String idToken) {
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

    @MainThread
    public OAuth2TokenResponse getTokenResponse() {
        return oAuth2TokenResponse;
    }

    protected void onDestroy() {
       authorizationService.dispose();
       authorizationService = null;
    }

}
