package gateway;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.EditText;

import com.banc.gateway.R;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

/**
 * Created by eely on 2/17/16.
 */
public class ParticleLoginDisplay extends Activity {
    private static final String TAG = "Particle";
    private String email;
    private String password;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_particle_login);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();  // Always call the superclass method first
    }

    public void loginButtonPressed(View view) {
        // Do something in response to button
        EditText emailTextView = (EditText)findViewById(R.id.emailTextField);
        EditText passwordTextView = (EditText)findViewById(R.id.passwordTextField);
        email = emailTextView.getText().toString();
        password = passwordTextView.getText().toString();

        Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {
            @Override
            public Integer callApi(ParticleCloud sparkCloud) throws ParticleCloudException, IOException {
                ParticleCloudSDK.getCloud().logIn(email, password);
                return 1;

            }

            @Override
            public void onSuccess(Object value) {
                // Log.d("Logging in", "succeeded");
                Toaster.s(ParticleLoginDisplay.this, "Logged in!");
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_OK, returnIntent);
                finish();
            }

            @Override
            public void onFailure(ParticleCloudException e) {
                // Log.d("Logging in", "failed");
                Toaster.s(ParticleLoginDisplay.this, "Incorrect Username/Password");
                Intent returnIntent = new Intent();
                setResult(Activity.RESULT_CANCELED, returnIntent);
                finish();
            }
        });



//        try {
////            ParticleCloudSDK.getCloud().logIn(emailTextView.getText().toString(), passwordTextView.getText().toString());
//            ParticleCloudSDK.getCloud().logIn("eric.j.ely@gmail.com", "lacrosse22");
//        }
//        catch (Exception ex)
//        {
//            Log.e("Error", "Login error: ", ex);
//            ex.printStackTrace();
//        }
//        if (ParticleCloudSDK.getCloud().isLoggedIn()) {
//            Toaster.s(this, "Logged in!");
//        } else {
//            Toaster.s(this, "Incorrect Username/Password!");
//        }
    }
}
