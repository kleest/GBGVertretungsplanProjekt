/*
 * Copyright (c) 2014 Steffen Klee
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.stkl.gbgvertretungsplan.authenticator;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.content.ContentResolver;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;

import de.stkl.gbgvertretungsplan.BuildConfig;
import de.stkl.gbgvertretungsplan.R;
import de.stkl.gbgvertretungsplan.Util;
import de.stkl.gbgvertretungsplan.errorreporting.ErrorReporter;
import de.stkl.gbgvertretungsplan.fragments.Eula;
import de.stkl.gbgvertretungsplan.networkcommunication.CommunicationInterface;
import de.stkl.gbgvertretungsplan.sync.SyncAdapter;

import static de.stkl.gbgvertretungsplan.sync.SyncAdapter.tryLogin;

/**
 * Created by Steffen Klee on 16.12.13.
 */

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class AuthenticatorActivity extends AccountAuthenticatorActivity {
    private static final String LOG_TAG = "AuthenticatorActivity";

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // Values for email and password at the time of the login attempt.
    private String mUsername;
    private String mPassword;

    // UI references.
    private EditText mUsernameView;
    private EditText mPasswordView;
    private View mLoginFormView;
    private View mLoginStatusView;
    private TextView mLoginStatusMessageView;
    private AccountManager mAccountManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_authenticator);

        // Set up the login form.
        //mUsername = getIntent().getStringExtra(EXTRA_EMAIL);
        mUsernameView = (EditText) findViewById(R.id.username);
        //mUsernameView.setText(mUsername);

        mPasswordView = (EditText) findViewById(R.id.password);
        // if the enter key is pressed or the IME action is triggered
        mPasswordView.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int id, KeyEvent keyEvent) {
                if (id == R.id.login || id == EditorInfo.IME_NULL) {
                    attemptLogin();
                    return true;
                }
                return false;
            }
        });

        mLoginFormView = findViewById(R.id.login_form);
        mLoginStatusView = findViewById(R.id.login_status);
        mLoginStatusMessageView = (TextView) findViewById(R.id.login_status_message);
        // if the login button is clicked
        findViewById(R.id.sign_in_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                attemptLogin();
            }
        });

        mAccountManager = AccountManager.get(this);

        // tos
        new Eula(this).show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.authenticator, menu);
        return true;
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        // Reset errors.
        mUsernameView.setError(null);
        mPasswordView.setError(null);

        // Store values at the time of the login attempt.
        mUsername = mUsernameView.getText().toString();
        mPassword = mPasswordView.getText().toString();

        boolean cancel = false;
        View focusView = null;

        // Check for a valid password.
        if (TextUtils.isEmpty(mPassword)) {
            mPasswordView.setError(getString(R.string.error_field_required));
            focusView = mPasswordView;
            cancel = true;
        // TODO password format check
        } else if (mPassword.length() < 4) {
            mPasswordView.setError(getString(R.string.error_invalid_password));
            focusView = mPasswordView;
            cancel = true;
        }

        // Check for a valid username
        // TODO define allowed characters (no special chars?)
        if (TextUtils.isEmpty(mUsername)) {
            mUsernameView.setError(getString(R.string.error_field_required));
            focusView = mUsernameView;
            cancel = true;
        }/* else if (!mUsername.contains("@")) {
            mUsernameView.setError(getString(R.string.error_invalid_username));
            focusView = mUsernameView;
            cancel = true;
        }*/

        if (cancel) {
            // There was an error; don't attempt login and focus the first
            // form field with an error.
            focusView.requestFocus();
        } else {
            // Show a progress spinner, and kick off a background task to
            // perform the user login attempt.
            mLoginStatusMessageView.setText(R.string.login_progress_signing_in);
            mPasswordView.setError(null);
            showProgress(true);
            mAuthTask = new UserLoginTask();
            mAuthTask.execute((Void) null);
        }
    }

    /**
     * Shows the progress UI and hides the login form.
     */
    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR2)
    private void showProgress(final boolean show) {
        // On Honeycomb MR2 we have the ViewPropertyAnimator APIs, which allow
        // for very easy animations. If available, use these APIs to fade-in
        // the progress spinner.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB_MR2) {
            int shortAnimTime = getResources().getInteger(android.R.integer.config_shortAnimTime);

            mLoginStatusView.setVisibility(View.VISIBLE);
            mLoginStatusView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 1 : 0)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
                        }
                    });

            mLoginFormView.setVisibility(View.VISIBLE);
            mLoginFormView.animate()
                    .setDuration(shortAnimTime)
                    .alpha(show ? 0 : 1)
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
                        }
                    });
        } else {
            // The ViewPropertyAnimator APIs are not available, so simply show
            // and hide the relevant UI components.
            mLoginStatusView.setVisibility(show ? View.VISIBLE : View.GONE);
            mLoginFormView.setVisibility(show ? View.GONE : View.VISIBLE);
        }
    }

    // called after login procedure has finished (successfully completed)
    private void loginFinished() {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "loginFinished()");
        // create account object
        final Account acc = new Account(mUsername, getString(R.string.account_type));
        // create user data that is associated with the account
        Bundle userData = new Bundle();
        // add the account
        mAccountManager.addAccountExplicitly(acc, mPassword, userData);

        // pass return values
        final Intent intent = new Intent();
        intent.putExtra(AccountManager.KEY_ACCOUNT_NAME, mUsername)
            .putExtra(AccountManager.KEY_ACCOUNT_TYPE, de.stkl.gbgvertretungsplan.values.Account.ACCOUNT_TYPE);
        setAccountAuthenticatorResult(intent.getExtras());
        setResult(RESULT_OK, intent);
        finish();
    }

    /**
     * Represents an asynchronous login task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, Integer> {
        @Override
        protected Integer doInBackground(Void... params) {
            try {
                if (tryLogin(mUsername, mPassword))
                    return 1;
                else
                    return 0;
            } catch (CommunicationInterface.ParsingException e) {
                ErrorReporter.reportError(e, getApplicationContext());
            } catch (CommunicationInterface.CommunicationException e) {
                ErrorReporter.reportError(e, getApplicationContext());
            } catch (IOException e) {
                return -1;
            }

            return -1;
        }

        @Override
        protected void onPostExecute(final Integer success) {
            mAuthTask = null;
            showProgress(false);

            // register new account if login attempt has been successful
            if (success == 1) {
                loginFinished();
            } else if (success == 0) {
                mPasswordView.setError(getString(R.string.error_incorrect_password));
                mPasswordView.requestFocus();
            } else if (success == -1) {
                mPasswordView.setError(getString(R.string.status_error));
                mPasswordView.requestFocus();
            }
        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
            showProgress(false);
        }
    }
}
