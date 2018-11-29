package com.amazon.aws.freertosandroid;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;
import com.amazonaws.mobile.auth.core.IdentityManager;
import com.amazonaws.mobile.auth.core.SignInStateChangeListener;
import com.amazonaws.mobile.auth.ui.SignInUI;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.AWSStartupHandler;
import com.amazonaws.mobile.client.AWSStartupResult;
import com.amazonaws.regions.Region;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPrincipalPolicyRequest;

public class AuthenticatorActivity extends AppCompatActivity {


    private final static String TAG = "AuthenticatorActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);

        // Add a call to initialize AWSMobileClient
        AWSMobileClient.getInstance().initialize(this, new AWSStartupHandler(){
            @Override
            public void onComplete(AWSStartupResult awsStartupResult) {
                Log.i(TAG, "AWSMobileClient is instantiated and you are connected to AWS!");
            }
        }).execute();

        // Sign-in listener
        IdentityManager.getDefaultIdentityManager().addSignInStateChangeListener(new SignInStateChangeListener() {
            @Override
            public void onUserSignedIn() {
                Log.i(TAG, "User Signed In");
                try {
                    AWSCredentialsProvider credentialsProvider =
                            AWSMobileClient.getInstance().getCredentialsProvider();
                    AWSIotClient awsIotClient = new AWSIotClient(credentialsProvider);
                    awsIotClient.setRegion(Region.getRegion(DemoConstants.AWS_IOT_REGION));
                    final CognitoCachingCredentialsProvider cognitoCachingCredentialsProvider =
                        new CognitoCachingCredentialsProvider(getApplicationContext(),
                                DemoConstants.COGNITO_POOL_ID, DemoConstants.COGNITO_REGION);
                    String principalId = cognitoCachingCredentialsProvider.getIdentityId();

                    AttachPrincipalPolicyRequest policyAttachRequest =
                        new AttachPrincipalPolicyRequest()
                                .withPolicyName(DemoConstants.AWS_IOT_POLICY_NAME)
                                .withPrincipal(principalId);

                    awsIotClient.attachPrincipalPolicy(policyAttachRequest);
                    Log.i(TAG, "Iot policy attached successfully.");
                } catch (Exception e) {
                    Log.e(TAG, "Exception caught: ", e);
                }
            }

            // Sign-out listener
            @Override
            public void onUserSignedOut() {
                Log.i(TAG, "User Signed Out");
                showSignIn();
            }
        });

        showSignIn();
    }

    /*
     * Display the AWS SDK sign-in/sign-up UI
     */
    private void showSignIn() {

        Log.d(TAG, "showSignInActivity");

        SignInUI signin = (SignInUI) AWSMobileClient.getInstance()
                .getClient(AuthenticatorActivity.this, SignInUI.class);
        signin.login(AuthenticatorActivity.this, DeviceScanActivity.class).execute();
    }
}
