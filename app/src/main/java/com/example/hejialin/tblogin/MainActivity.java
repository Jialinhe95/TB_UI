package com.example.hejialin.tblogin;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;


import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.Auth;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.auth.api.signin.GoogleSignInResult;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class MainActivity extends AppCompatActivity implements GoogleApiClient.OnConnectionFailedListener, View.OnClickListener {
    private static final String TAG = "SignInActivity";
    private static final int RC_SIGN_IN = 9001;
    private SignInButton googleSignInButton;
    private Button signOutButton;
    private TextView statusTextView;
    private GoogleApiClient mGoogleApiClient;
    private EditText emailEdit, passEdit;
    private Button forgetButton, signUpButton, emailSignInButton;
    private FirebaseAuth mAuth;
    //设置一个响应用户的登录状态变化的 AuthStateListener：
    private FirebaseAuth.AuthStateListener mAuthListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // [START config_signin]
        // Configure email sign in
        mAuth = FirebaseAuth.getInstance();

        // Configure Google Sign In
        // specify sign in scope
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        // [END config_signin]

        statusTextView = (TextView) findViewById(R.id.status_textview);

        emailEdit = (EditText) findViewById(R.id.editEmail);
        passEdit = (EditText) findViewById(R.id.editPass);
        forgetButton = (Button) findViewById(R.id.forget_password_button);
        signUpButton = (Button) findViewById(R.id.sign_up_button);
        emailSignInButton = (Button) findViewById(R.id.email_sign_in_button);

        // create a Google api client
        // when the user click a sign-in button, here we create a sign-in intent and the activity for it.
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .enableAutoManage(this /* FragmentActivity */, this /* OnConnectionFailedListener */)
                .addApi(Auth.GOOGLE_SIGN_IN_API, gso)
                .build();


        googleSignInButton = (SignInButton) findViewById(R.id.google_sign_in_button);
        googleSignInButton.setOnClickListener(this);

        signOutButton = (Button) findViewById(R.id.sign_out_button);
        signOutButton.setOnClickListener(this);

        forgetButton.setOnClickListener(this);
        signUpButton.setOnClickListener(this);
        emailSignInButton.setOnClickListener(this);

        // [START auth_state_listener]
        mAuthListener = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user = firebaseAuth.getCurrentUser();
                if (user != null) {
                    // User is signed in
                    Log.d(TAG, "onAuthStateChanged:signed_in:" + user.getUid());
                } else {
                    // User is signed out
                    Log.d(TAG, "onAuthStateChanged:signed_out");
                }
                // [START_EXCLUDE]
                updateUI(user);
                // [END_EXCLUDE]
            }
        };
        // [END auth_state_listener]

    }

    // [START on_start_add_listener]
    @Override
    public void onStart() {
        super.onStart();
        mAuth.addAuthStateListener(mAuthListener);
    }
    // [END on_start_add_listener]

    // [START on_stop_remove_listener]
    @Override
    public void onStop() {
        super.onStop();
        if (mAuthListener != null) {
            mAuth.removeAuthStateListener(mAuthListener);
        }
    }
    // [END on_stop_remove_listener]

    // 简单工厂模式
    @Override
    public void onClick(View v) {
        int i = v.getId();
        switch (i) {
            case R.id.google_sign_in_button:
                googleSignIn();
                break;
            case R.id.sign_out_button:
                signOut();
                break;
            case R.id.forget_password_button:
                forgetPass(emailEdit.getText().toString());
                break;
            case R.id.sign_up_button:
                signUp(emailEdit.getText().toString(),passEdit.getText().toString());
                break;
            case R.id.email_sign_in_button:
                emailSignIn(emailEdit.getText().toString(),passEdit.getText().toString());
                break;
        }
    }

    // [START google signin]
    private void googleSignIn() {
        Intent signInIntent = Auth.GoogleSignInApi.getSignInIntent(mGoogleApiClient);
        // Result returned from launching the Intent from GoogleSignInApi.getSignInIntent(...);
        startActivityForResult(signInIntent, RC_SIGN_IN);
        // and start an activity for it
    }
    // [END google signin]

    // [START onactivityresult]
    // get the user's data in the result
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            GoogleSignInResult result = Auth.GoogleSignInApi.getSignInResultFromIntent(data);
            // get signIn object from the data that come back from the intent
            handleSignInResult(result);
        }
    }
    // [END onactivityresult]

    private void handleSignInResult(GoogleSignInResult result) {
        Log.d(TAG, "handleSignInResult: " + result.isSuccess());
        if (result.isSuccess()) {
            // sign in successfully, show authenticated UI
            GoogleSignInAccount acct = result.getSignInAccount();
            statusTextView.setText("Hello Google Account User: " + acct.getDisplayName());
            firebaseAuthWithGoogle(acct);
        } else {
            //updateUI(null);
            statusTextView.setText("Google accounts sign in failed, please check the network access.");
        }
    }

    // [START auth_with_google]
    // 登录Google帐户成功后还要用signInWithCredential登录Firebase
    private void firebaseAuthWithGoogle(GoogleSignInAccount acct) {
        Log.d(TAG, "firebaseAuthWithGoogle:" + acct.getId());

        AuthCredential credential = GoogleAuthProvider.getCredential(acct.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        Log.d(TAG, "signInWithCredential:onComplete:" + task.isSuccessful());

                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithCredential", task.getException());
                            Toast.makeText(MainActivity.this, "Authentication failed.",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }
    // [END auth_with_google]

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        // An unresolvable error has occurred and Google APIs (including Sign-In) will not
        // be available.
        Log.d(TAG, "onConnectionFailed: " + connectionResult);
    }


    private void signOut() {
        // firebase sign out
        mAuth.signOut();

        // Google sign out
        Auth.GoogleSignInApi.signOut(mGoogleApiClient).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                //statusTextView.setText("Signed out.");
                updateUI(null);
            }
        });
    }

    private boolean validateForm(int type) {
        boolean valid = true;

        String email = emailEdit.getText().toString();
        if (TextUtils.isEmpty(email)) {
            emailEdit.setError("Required.");
            valid = false;
        } else {
            emailEdit.setError(null);
        }

        if(type == 1){ // sign up or sign in need to validate password
            String password = passEdit.getText().toString();
            if (TextUtils.isEmpty(password)) {
                passEdit.setError("Required.");
                valid = false;
            } else {
                passEdit.setError(null);
            }
        }


        return valid;
    }

    private void forgetPass(String emailAddress) {
        // forget password
        //通过sendPasswordResetEmail 方法向用户发送一封重设密码电子邮件。
        if (!validateForm(0)) { // only validate email address
            return;
        }
        mAuth.sendPasswordResetEmail(emailAddress)
                .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "Email sent.");
                            Toast.makeText(MainActivity.this, "Email sent successful",
                                    Toast.LENGTH_SHORT).show();
                        }else{
                            Toast.makeText(MainActivity.this, "Email has not been registered.",
                                    Toast.LENGTH_SHORT).show();
                            statusTextView.setText("Email has not been registered.");
                        }
                    }
                });
    }

    private void signUp(String email, String password){
        // sign up
        if (!validateForm(1)) { // validate email address and password
            return;
        }
        //通过  mAuth.createUserWithEmailAndPassword(email, password);方法来进行用户注册
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // If sign up fails, display a message to the user. If sign up succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(MainActivity.this, "Sign up failed, please check the network access.",
                                    Toast.LENGTH_SHORT).show();
                            // update status textview
                            statusTextView.setText("Sign up failed, please check the network access.");
                        }
                    }
                });
    }

    private void emailSignIn(String email, String password){
        // sign in with email
        if (!validateForm(1)) { // validate email address and password
            return;
        }
        //通过 signInWithEmailAndPassword(email, password);方法来进行身份认证
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(MainActivity.this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        // If sign in fails, display a message to the user. If sign in succeeds
                        // the auth state listener will be notified and logic to handle the
                        // signed in user can be handled in the listener.
                        if (!task.isSuccessful()) {
                            Log.w(TAG, "signInWithEmail:failed", task.getException());
                            Toast.makeText(MainActivity.this, "Sign in failed",
                                    Toast.LENGTH_SHORT).show();
                            // update status textview
                            statusTextView.setText("Sign in failed");
                        }

                    }
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            statusTextView.setText("Hello Email User: " + user.getEmail() );
            //statusTextView.setText(getString(R.string.hello_email_user, user.getEmail()));
            //string.xml <string name="hello_email_user">Email User: %s</string>

            findViewById(R.id.editPass).setVisibility(View.GONE);
            findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
        } else {
            statusTextView.setText("Signed Out.");
            findViewById(R.id.editPass).setVisibility(View.VISIBLE);
            findViewById(R.id.sign_out_button).setVisibility(View.GONE);
        }
    }

}
