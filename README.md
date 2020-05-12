# Start enable Authentication for Android App

## Register Application



| Field                 | Value         | 
| --------------------- | ------------- | 
| Service Provider Name | sample-app  |
| Description           | This is a mobile application  | 
| Call Back Url         | com.example.myapplication://oauth  | 

Enable following properties:
- PKCE Mandatory
- Allow authentication without the client secret


## Configure the Android SDK in your project

### Initializing the  SDK

#### Add the dependency 

1. Clone this project: https://github.com/piraveena/android-sdk.git

2. Build the library in your local maven. Run the following commands. Now the library will be available in your local .m2 cache. 
    - `./gradlew clean`
    - `./gradlew assembleRelease`
    - `./gradlew publishToMavenLocal `

3. Add `WSO2-SDK` dependency in `build.gradle` file.

```gradle
dependencies {
     implementation 'org.wso2.carbon.identity.sso:wso2is-oidc-sdk:0.0.1'
}

```

#### Add a URI Scheme   

You must add a redirect scheme to receive sign in results from the web browser.
 To do this, you must define a gradle manifest placeholder in your app's build.gradle:

```gradle
android.defaultConfig.manifestPlaceholders = [
       'appAuthRedirectScheme': 'com.example.myapplication'
]
```

#### Add RedirectActivity
```xml
<activity android:name="org.oidc.agent.RedirectActivity">
   <intent-filter>
       <action android:name="android.intent.action.VIEW" />
       <category android:name="android.intent.category.DEFAULT" />
       <category android:name="android.intent.category.BROWSABLE" />
       <data android:scheme="com.example.myapplication://oauth" />
   </intent-filter>
</activity>

```

#### Configuration


Create a `config.json` file in `res/raw/` directory and add the relevant configs. 
    - Add the client-id, client- secret and application-redirect-url of the application.
    - Update the {HOST_NAME}:{PORT} with the IS server's hostname and port respectively

```json
{
 "client_id": {client-id},
 "redirect_uri": "{application-redirect-url},
 "authorization_scope": "openid",
 "authorization_endpoint": "https://{HOST_NAME}:{PORT}/oauth2/authorize",
 "end_session_endpoint": "https://{HOST_NAME}:{PORT}/oidc/logout",
 "token_endpoint": "https://{HOST_NAME}:{PORT}/oauth2/token"
}
```

Example:
```json
{
 "client_id": "tkJfn9a8Yw2kfRfUSIrfvemcVjYa",
 "redirect_uri": "com.example.myapplication://oauth",
 "authorization_scope": "openid",
 "authorization_endpoint": "https://10.0.2.2:9443/oauth2/authorize",
 "end_session_endpoint": "https://10.0.2.2:9443/oidc/logout",
 "token_endpoint": "https://10.0.2.2:9443/oauth2/token",
 "userinfo_endpoint": "https://10.0.2.2:9443/oauth2/userinfo"
}
```
#### Add Util class

1. Add a Util class to initialize the objects.

    ```java
    
    import android.content.Context;
    
    import org.oidc.agent.ConfigManager;
    import org.oidc.agent.LoginService;
    
    public class Util {
    
       static ConfigManager configManager;
       static LoginService login;
    
       public static ConfigManager getConfigManager(Context context) {
           if(configManager == null) {
               configManager = ConfigManager.getInstance(context, R.raw.config);
           }
           return configManager;
    
       }
    
       public static LoginService getLogin(){
           if(login == null) {
               login = new LoginService();
           }
           return login;
       }
    }
    ```
2. Initiate login and configManager object from your Activity.

    ```loginservice = Util.getLogin();
       configManager = Util.getConfigManager(this);
    ```



### Login

#### Get the authorization code with PKCE.


```java

private void doAuthorization(Context context) {

   Intent completionIntent = new Intent(context, UserInfoActivity.class);
   Intent cancelIntent = new Intent(context, LoginActivity.class);
   cancelIntent.putExtra("failed", true);
   cancelIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);

   loginService.doAuthorization(context, configManager, PendingIntent.getActivity(context, 0,
           completionIntent, 0),  PendingIntent.getActivity(context, 0, cancelIntent, 0));
}
```
- You can add this `doAuthorization(Context context)` method inside a Activity class when a user clicks the login button. 

```java
    findViewById(R.id.login).setOnClickListener(v ->
                   doAuthorization(this)
           );
```

#### Get the accesstoken and idtoken.

 You can add this `handleAuthorizationResponse(Intent intent)` method inside a Activity when there is a successfull
 authentication response comes from the IDP. 
- In the authorization request, you need to create a Intent for successfull request and redirect to this activity.

```java
    @Override
        protected void onStart() {  
            super.onStart();
            getConfigManager(this);
            handleAuthorizationResponse(getIntent());
        }
``` 
```java
   
    private void handleAuthorizationResponse(Intent intent) {
    
       loginService.handleAuthorization(intent, new TokenRequest.TokenRespCallback() {
           @Override
           public void onTokenRequestCompleted(OAuth2TokenResponse oAuth2TokenResponse) {
               readUserInfo(oAuth2TokenResponse);
           }
       });
    }
```
-
### Logout

- Use the idToken obtained from the token response in the above flow to do the logout request.

    ```java  
    private void singleLogout(Context context, String idToken) {   
       loginService.logout(context, idToken);
       finish();
    }
    ```
  - You can call this logout method from an Activity when the user click the logout button.
  
  ```java
  findViewById(R.id.logout).setOnClickListener(v ->
                  singleLogout(this, idToken)
          ); 
  ```

### Read User Information

- Can read the user information from idToken
- Add this dependency in you gradle file `com.googlecode.json-simple:json-simple:1.1` to import `org.json.simple.*` library.

    ```java
    private void readUserInfo(String idToken){

        try {
            JSONParser parser = new JSONParser();
            String[] split = idToken.split("\\.");
            String decodeIDToken = new String(Base64.decode(split[1], Base64.URL_SAFE),"UTF-8");
            JSONObject json = (JSONObject) parser.parse(decodeIDToken);
            String userName = (String) json.get("sub");
            String email = (String) json.get("email");

        } catch (Exception e) {
            //handle the exception.
        }
    }
```