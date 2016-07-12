package com.ccdev.quality;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Toast;

import com.ccdev.quality.Ignore.AuthSettings;
import com.ccdev.quality.Utils.ErrorStack;
import com.ccdev.quality.Utils.NetworkHelper;
import com.ccdev.quality.Utils.Prefs;
import com.ccdev.quality.Views.BackHandledFragment;

/**
 * Created by Coleby on 7/10/2016.
 */

public class SettingsFragment extends BackHandledFragment {

    // TODO for testing only
    private static final boolean USE_AUTH_SETTINGS = true;

    private static final String TAG = "Quality.SettingsFragment";

    public static final int RESULT_OK = 0;
    public static final int RESULT_CANCEL = -1;

    private EditText mServer, mRoot, mDomain, mUsername, mPassword;
    private CheckBox mRememberMe;
    private Button mCancel, mConfirm;

    private Thread mGetFileTreeThread;

    private OnSettingsListener mCallback;
    public interface OnSettingsListener {
        void OnSettingsResult(int resultCode);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        // TODO add admin password

        mDomain = (EditText) getView().findViewById(R.id.settings_domain);
        mServer = (EditText) getView().findViewById(R.id.settings_server);
        mRoot = (EditText) getView().findViewById(R.id.settings_root);
        mUsername = (EditText) getView().findViewById(R.id.settings_username);
        mPassword = (EditText) getView().findViewById(R.id.settings_password);
        mRememberMe = (CheckBox) getView().findViewById(R.id.settings_rememberMe);
        mConfirm = (Button) getView().findViewById(R.id.settings_confirm);
        mCancel = (Button) getView().findViewById(R.id.settings_cancel);

        if (Prefs.checkSettings(Prefs.SERVER_SETTINGS) == Prefs.RESULT_OK) {
            mDomain.setText(Prefs.getDomain());
            mServer.setText(Prefs.getServerIp());
            mRoot.setText(Prefs.getPathToRoot());
        }

        if (Prefs.checkSettings(Prefs.USER_SETTINGS) == Prefs.RESULT_OK && Prefs.getRememberMe()) {
            mUsername.setText(Prefs.getUsername());
            mPassword.setText(Prefs.getPassword());
            mRememberMe.setChecked(Prefs.getRememberMe());
        }

        if (USE_AUTH_SETTINGS) {
            mDomain.setText(AuthSettings.DOMAIN);
            mServer.setText(AuthSettings.SERVER);
            mRoot.setText(AuthSettings.ROOT);
            mUsername.setText(AuthSettings.USERNAME);
            mPassword.setText(AuthSettings.PASSWORD);
        }

        mConfirm.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onConfirm();
            }
        });

        mCancel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onCancel();
            }
        });
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (OnSettingsListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnSettingsListener");
        }
    }

    @Override
    public boolean onBackPressed() {

        // let parent handle it
        return false;
    }

    private void onConfirm() {

        String domain = mDomain.getText().toString();
        if (domain.isEmpty()) {
            Prefs.reset();
            missingFieldError(mDomain, "Domain cannot be blank.");
            return;
        } else if (false) {
            // TODO check format
            return;
        } else {
            Prefs.setDomain(domain);
        }

        String serverIp = mServer.getText().toString();
        if (serverIp.isEmpty()) {
            Prefs.reset();
            missingFieldError(mServer, "Server IP cannot be blank.");
            return;
        } else if (false) {
            // TODO check format
            return;
        } else {
            Prefs.setServerIp(serverIp);
        }

        String pathToRoot = mRoot.getText().toString();
        if (pathToRoot.isEmpty()) {
            Prefs.reset();
            missingFieldError(mRoot, "Path to root cannot be blank.");
            return;
        } else if (false) {
            // TODO check format
            return;
        } else {
            Prefs.setPathToRoot(pathToRoot);
        }

        String username = mUsername.getText().toString();
        if (username.isEmpty()) {
            Prefs.reset();
            missingFieldError(mUsername, "Username cannot be blank.");
            return;
        } else if (false) {
            // TODO check format
            return;
        } else {
            Prefs.setUsername(username);
        }

        String password = mPassword.getText().toString();
        if (password.isEmpty()) {
            Prefs.reset();
            missingFieldError(mPassword, "Password cannot be blank.");
            return;
        } else if (false) {
            // TODO check format
            return;
        } else {
            Prefs.setPassword(password);
        }

        boolean rememberMe = mRememberMe.isChecked();
        Prefs.setRememberMe(rememberMe);

        mGetFileTreeThread = new Thread(new Runnable() {
            @Override
            public void run() {

                if (NetworkHelper.getInitialFileTree()) {
                    Prefs.commit();
                    mCallback.OnSettingsResult(RESULT_OK);
                } else {
                    Prefs.reset();
                    generalError("Could not connect with given credentials.");
                    ErrorStack.dump();
                    ErrorStack.flush();
                }
            }
        });
        mGetFileTreeThread.start();
    }

    private void onCancel() {
        mCallback.OnSettingsResult(RESULT_CANCEL);
    }


    private void generalError(final String message) {
        // TODO make dialog
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void missingFieldError(final EditText editText, final String message) {
        // TODO make dialog
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {

                editText.requestFocus();
                editText.selectAll();

                Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            }
        });
    }
}