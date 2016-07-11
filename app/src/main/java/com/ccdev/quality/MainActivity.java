package com.ccdev.quality;

import android.icu.text.IDNA;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.os.Bundle;
import android.widget.Toast;

import com.ccdev.quality.Utils.ErrorStack;
import com.ccdev.quality.Utils.NetworkHelper;
import com.ccdev.quality.Utils.Prefs;
import com.ccdev.quality.Views.BackHandledFragment;

public class MainActivity extends FragmentActivity implements
        BackHandledFragment.BackHandlerInterface,
        SettingsFragment.OnSettingsListener,
        FoldersFragment.OnFoldersListener {

    private static final String TAG = "Quality.MainActivity";

    private FragmentManager mFragmentManager;
    private BackHandledFragment mFoldersFragment, mPhotoViewFragment,
            mLoginFragment, mSettingsFragment, mSelectedFragment;

    private Thread mGetFileTreeThread;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ErrorStack.setLogging(true);

        Prefs.getPrefs(this);
        System.setProperty("jcifs.smb.client.responseTimeout", "5000");

        mFoldersFragment = new FoldersFragment();
        mPhotoViewFragment = new PhotoViewFragment();
        mSettingsFragment = new SettingsFragment();
        mLoginFragment = new LoginFragment();

        mFragmentManager = getSupportFragmentManager();

        determineLandingPage();
    }

    @Override
    public void setSelectedFragment(BackHandledFragment backHandledFragment) {

        mSelectedFragment = backHandledFragment;
    }

    @Override
    public void onBackPressed() {

        if (mSelectedFragment == null || mSelectedFragment.onBackPressed()) {
            // TODO determine action
            //super.onBackPressed();
        }
    }

    @Override
    public void OnSettingsResult(int resultCode) {

        if (resultCode == SettingsFragment.RESULT_OK) {

            mFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content_frame, mFoldersFragment)
                    .commit();
        }
    }

    @Override
    public void OnFoldersError(int errorCode) {
        // TODO determine why FoldersFragment exited
        ErrorStack.add(TAG, "OnFolderError(): errorCode = " + errorCode);
        ErrorStack.dump();
        ErrorStack.flush();
    }

    @Override
    public void OnFoldersShowPhoto(String pathToFile) {

    }

    private void determineLandingPage() {

        if (Prefs.checkSettings(Prefs.SERVER_SETTINGS) != Prefs.RESULT_OK) {
            mFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content_frame, mSettingsFragment)
                    .commit();
            return;
        }

        if (Prefs.checkSettings(Prefs.USER_SETTINGS) != Prefs.RESULT_OK) {
            mFragmentManager
                    .beginTransaction()
                    .replace(R.id.main_content_frame, mLoginFragment)
                    .commit();
            return;
        }

        mGetFileTreeThread = new Thread(new Runnable() {
            @Override
            public void run() {
                if (NetworkHelper.getInitialFileTree()) {
                    mFragmentManager
                            .beginTransaction()
                            .replace(R.id.main_content_frame, mFoldersFragment)
                            .commit();
                } else {
                    generalError("Could not connect with given credentials.");
                    ErrorStack.add(TAG, "Could not connect with given credentials.");
                    ErrorStack.dump();
                    ErrorStack.flush();

                    mFragmentManager
                            .beginTransaction()
                            .replace(R.id.main_content_frame, mFoldersFragment)
                            .commit();
                }
            }
        });
        mGetFileTreeThread.start();
    }

    private void generalError(String message) {
        // TODO make dialog

        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }
}
