package com.banc.sparkle_gateway.login;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.SpannableString;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.banc.sparkle_gateway.R;
import com.banc.util.Values;

import java.io.IOException;

import io.particle.android.sdk.cloud.ParticleCloud;
import io.particle.android.sdk.cloud.ParticleCloudException;
import io.particle.android.sdk.cloud.ParticleCloudSDK;
import io.particle.android.sdk.utils.Async;
import io.particle.android.sdk.utils.Toaster;

/**
 * Created by eely on 2/17/16.
 */
public class ParticleLoginActivity extends AppCompatActivity {

    private EditText mEmailTextView;
    private EditText mPasswordTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_particle_login);
        boolean showSkip = false;
        if (getIntent().hasExtra(Values.EXTRA_FROM_START)
                && getIntent().getBooleanExtra(Values.EXTRA_FROM_START, false)) {
            showSkip = true;
        }

        Toolbar toolbar = (Toolbar) findViewById(R.id.t_toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.particle_login);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        Button loginButton = (Button) findViewById(R.id.btn_login);
        loginButton.setOnClickListener(loginButtonClickListener());

        mEmailTextView = (EditText) findViewById(R.id.et_email);
        mPasswordTextView = (EditText) findViewById(R.id.et_password);

        TextView notNow = (TextView) findViewById(R.id.tv_skip);
        if (showSkip) {
            notNow.setVisibility(View.VISIBLE);
            String text = getString(R.string.not_now);
            SpannableString content = new SpannableString(text);
            content.setSpan(new UnderlineSpan(), 0, text.length(), 0);
            notNow.setText(content);
            notNow.setOnClickListener(skipButtonClickListener());
        } else {
            notNow.setVisibility(View.INVISIBLE);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private View.OnClickListener loginButtonClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Async.executeAsync(ParticleCloudSDK.getCloud(), new Async.ApiWork<ParticleCloud, Object>() {

                    @Override
                    public Object callApi(@NonNull ParticleCloud sparkCloud) throws ParticleCloudException, IOException {
                        ParticleCloudSDK.getCloud().logIn(
                                mEmailTextView.getText().toString(),
                                mPasswordTextView.getText().toString());
                        return 1;
                    }

                    @Override
                    public void onSuccess(@NonNull Object value) {
                        Log.d("Logging in", "succeeded");
                        Toaster.s(ParticleLoginActivity.this, "Logged in!");
                        try {
                            ParticleCloudSDK.getCloud().getDevices();
                        } catch (Exception ex) {
                            Log.d("ParticleLoginDisplay", "Received error when getting devices");
                            ex.printStackTrace();
                        }
                        finish();
                    }

                    @Override
                    public void onFailure(@NonNull ParticleCloudException e) {
                        Log.d("Logging in", "failed");
                        Toaster.s(ParticleLoginActivity.this, "Incorrect Username/Password");
                    }

                });
            }
        };
    }

    private View.OnClickListener skipButtonClickListener() {
        return new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        };
    }

}
