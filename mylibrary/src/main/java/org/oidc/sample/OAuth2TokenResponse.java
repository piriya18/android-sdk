package org.oidc.sample;

public class OAuth2TokenResponse {

    private String tokenType;

    private String accessToken;

    private Long accessTokenExpirationTime;

    private String idToken;

    private String refreshToken;

    private String scope;


    OAuth2TokenResponse() { }

    public void setIdToken(String idToken) {
        this.idToken = idToken;
    }

    public void setAccessToken(String accessToken) {
        this.accessToken = accessToken;
    }

    public void setTokenType(String tokenType) {
        this.tokenType = tokenType;
    }

    public String getIdToken() {
        return idToken;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }
}
