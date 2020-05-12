package org.oidc.sample;

import android.os.AsyncTask;
import android.util.Log;

import androidx.annotation.Nullable;

import net.openid.appauth.AuthorizationException;
import net.openid.appauth.AuthorizationResponse;
import net.openid.appauth.AuthorizationService;
import net.openid.appauth.ClientAuthentication;
import net.openid.appauth.ClientSecretBasic;
import net.openid.appauth.NoClientAuthentication;
import net.openid.appauth.TokenResponse;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

public class TokenRequest extends AsyncTask<Void, Void, OAuth2TokenResponse> {

    AuthorizationService authorizationService;
    AuthorizationResponse response;
    String clientSecret;
    TokenRespCallback callback;

    TokenRequest(AuthorizationService authorizationService, AuthorizationResponse response, String clientSecret, TokenRespCallback callback ){
        this.authorizationService=authorizationService;
        this.response=response;
        this.clientSecret=clientSecret;
        this.callback = callback;
    }

    @Override
    protected OAuth2TokenResponse doInBackground(Void... voids) {

        OAuth2TokenResponse oAuth2TokenResponse = new OAuth2TokenResponse();
        authorizationService.performTokenRequest(response.createTokenExchangeRequest(),  new AuthorizationService.TokenResponseCallback() {
                    @Override
                    public void onTokenRequestCompleted(@Nullable TokenResponse tokenResponse, @Nullable AuthorizationException exception) {
                        if (exception != null) {
                            Log.i("LOG_TAG", "Token Exchange failed", exception);
                        } else {
                            if (tokenResponse != null) {
                                oAuth2TokenResponse.setAccessToken(tokenResponse.accessToken);
                                oAuth2TokenResponse.setIdToken(tokenResponse.idToken);
                                callback.onTokenRequestCompleted(oAuth2TokenResponse);
                                Log.i("LOG_TAG", String.format("Token Response [ Access Token: %s, ID Token: %s ]", tokenResponse.accessToken, tokenResponse.idToken));
                                authorizationService.dispose();
                            }
                        }
                    }
                });
        return oAuth2TokenResponse;
    }

//    @Override
//    protected void onPostExecute(OAuth2TokenResponse response) {
//        callback.onTokenRequestCompleted(response);
//    }

    public interface TokenRespCallback {

        void onTokenRequestCompleted(OAuth2TokenResponse oAuth2TokenResponse);
    }

}
