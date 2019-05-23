package software.amazon.freertos.demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.mobile.client.AWSMobileClient;
import com.amazonaws.mobile.client.Callback;
import com.amazonaws.mobile.client.SignInUIOptions;
import com.amazonaws.mobile.client.UserStateDetails;
import com.amazonaws.regions.Region;
import com.amazonaws.services.iot.AWSIotClient;
import com.amazonaws.services.iot.model.AttachPolicyRequest;

import java.util.concurrent.CountDownLatch;

public class AuthenticatorActivity extends AppCompatActivity {
    private final static String TAG = "AuthActivity";
    private HandlerThread handlerThread;
    private Handler handler;
    public static Intent newIntent(Context packageContext) {
        Intent intent = new Intent(packageContext, AuthenticatorActivity.class);
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_fragment);
        if ( handlerThread == null ) {
            handlerThread = new HandlerThread("SignInThread");
            handlerThread.start();
            handler = new Handler(handlerThread.getLooper());
        }
        final CountDownLatch latch = new CountDownLatch(1);
        AWSMobileClient.getInstance().initialize(getApplicationContext(), new Callback<UserStateDetails>() {

                @Override
                public void onResult(UserStateDetails userStateDetails) {
                    Log.i(TAG, "AWSMobileClient initialization onResult: " + userStateDetails.getUserState());
                    latch.countDown();
                }

                @Override
                public void onError(Exception e) {
                    Log.e(TAG, "Initialization error.", e);
                }
            }
        );
        Log.d(TAG, "waiting for AWSMobileClient Initialization");
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        handler.post(new Runnable() {
            @Override
            public void run() {
                if (AWSMobileClient.getInstance().isSignedIn()) {
                    signinsuccessful();
                    finish();
                } else {
                    AWSMobileClient.getInstance().showSignIn(
                        AuthenticatorActivity.this,
                        SignInUIOptions.builder()
                                .nextActivity(null)
                                .build(),
                        new Callback<UserStateDetails>() {
                            @Override
                            public void onResult(UserStateDetails result) {
                                Log.d(TAG, "onResult: " + result.getUserState());
                                switch (result.getUserState()) {
                                    case SIGNED_IN:
                                        Log.i(TAG, "logged in!");
                                        signinsuccessful();
                                        finish();
                                        break;
                                    case SIGNED_OUT:
                                        Log.i(TAG, "onResult: User did not choose to sign-in");
                                        break;
                                    default:
                                        AWSMobileClient.getInstance().signOut();
                                        break;
                                }
                            }

                            @Override
                            public void onError(Exception e) {
                                Log.e(TAG, "onError: ", e);
                            }
                        }
                    );
                }
            }
        });

    }

    private void signinsuccessful() {
        try {
            AWSCredentialsProvider credentialsProvider = AWSMobileClient.getInstance();
            AWSIotClient awsIotClient = new AWSIotClient(credentialsProvider);
            awsIotClient.setRegion(Region.getRegion(DemoConstants.AWS_IOT_REGION));

            AttachPolicyRequest attachPolicyRequest = new AttachPolicyRequest()
                    .withPolicyName(DemoConstants.AWS_IOT_POLICY_NAME)
                    .withTarget(AWSMobileClient.getInstance().getIdentityId());
            awsIotClient.attachPolicy(attachPolicyRequest);
            Log.i(TAG, "Iot policy attached successfully.");
        } catch (Exception e) {
            Log.e(TAG, "Exception caught: ", e);
        }
    }
}
