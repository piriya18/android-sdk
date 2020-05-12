package org.oidc.agent;

import android.app.Activity;
import android.os.Bundle;

import net.openid.appauth.AuthorizationManagementActivity;

public class RedirectActivity extends Activity {

    @Override
    public void onCreate(Bundle savedInstanceBundle) {
        super.onCreate(savedInstanceBundle);

        // while this does not appear to be achieving much, handling the redirect in this way
        // ensures that we can remove the browser tab from the back stack. See the documentation
        // on AuthorizationManagementActivity for more details.
        startActivity(AuthorizationManagementActivity.createResponseHandlingIntent(
                this, getIntent().getData()));
        finish();
    }

}

