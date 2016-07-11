package com.ccdev.quality.Utils;

import android.content.Context;

import com.ccdev.quality.Ignore.AuthSettings;
import com.securepreferences.SecurePreferences;

/**
 * Created by Coleby on 7/10/2016.
 */

public class Prefs {

    private static final String PREFS_FILE_NAME = "quality_prefs";
    private static final String PREFS_DOMAIN = "domain";
    private static final String PREFS_SERVER_IP = "server_ip";
    private static final String PREFS_PATH_TO_ROOT = "path_to_root";
    private static final String PREFS_USERNAME = "username";
    private static final String PREFS_PASSWORD = "password";
    private static final String PREFS_REMEMBER_ME = "remember_me";

    public static final int SERVER_SETTINGS = 0;
    public static final int USER_SETTINGS = 1;
    public static final int BOTH_SETTINGS = 2;

    public static int RESULT_OK = 0;
    public static int RESULT_MISSING_DOMAIN = -1;
    public static int RESULT_MISSING_SERVER_IP = -2;
    public static int RESULT_MISSING_PATH_TO_ROOT = -3;
    public static int RESULT_MISSING_USERNAME = -4;
    public static int RESULT_MISSING_PASSWORD = -5;

    private static SecurePreferences securePreferences;
    private static String domain;
    private static String serverIp;
    private static String pathToRoot;
    private static String username;
    private static String password;
    private static boolean rememberMe;

    public static void getPrefs(Context context) {

        securePreferences = new SecurePreferences(context, AuthSettings.SECRET_KEY, PREFS_FILE_NAME);
        domain = securePreferences.getString(PREFS_DOMAIN, "");
        serverIp = securePreferences.getString(PREFS_SERVER_IP, "");
        pathToRoot = securePreferences.getString(PREFS_PATH_TO_ROOT, "");
        username = securePreferences.getString(PREFS_USERNAME, "");
        password = securePreferences.getString(PREFS_PASSWORD, "");
        rememberMe = securePreferences.getBoolean(PREFS_REMEMBER_ME, false);
    }

    public static void reset() {

        domain = securePreferences.getString(PREFS_DOMAIN, "");
        serverIp = securePreferences.getString(PREFS_SERVER_IP, "");
        pathToRoot = securePreferences.getString(PREFS_PATH_TO_ROOT, "");
        username = securePreferences.getString(PREFS_USERNAME, "");
        password = securePreferences.getString(PREFS_PASSWORD, "");
        rememberMe = securePreferences.getBoolean(PREFS_REMEMBER_ME, false);
    }

    public static int checkSettings(int which) {

        // TODO format validation

        switch (which) {
            case SERVER_SETTINGS:
                if (domain == null || domain.isEmpty()) return RESULT_MISSING_DOMAIN;
                if (serverIp == null || serverIp.isEmpty()) return RESULT_MISSING_SERVER_IP;
                if (pathToRoot == null || pathToRoot.isEmpty()) return RESULT_MISSING_PATH_TO_ROOT;
                break;
            case USER_SETTINGS:
                if (username == null || username.isEmpty()) return RESULT_MISSING_USERNAME;
                if (password == null || password.isEmpty()) return RESULT_MISSING_PASSWORD;
                break;
            case BOTH_SETTINGS:
                if (domain == null || domain.isEmpty()) return RESULT_MISSING_DOMAIN;
                if (serverIp == null || serverIp.isEmpty()) return RESULT_MISSING_SERVER_IP;
                if (pathToRoot == null || pathToRoot.isEmpty()) return RESULT_MISSING_PATH_TO_ROOT;
                break;
        }

        return RESULT_OK;
    }

    public static void commit(int which) {

        switch (which) {
            case SERVER_SETTINGS:
                securePreferences.edit()
                        .putString(PREFS_DOMAIN, domain)
                        .putString(PREFS_SERVER_IP, serverIp)
                        .putString(PREFS_PATH_TO_ROOT, pathToRoot)
                        .apply();
                break;
            case USER_SETTINGS:
                securePreferences.edit()
                        .putString(PREFS_USERNAME, username)
                        .putString(PREFS_PASSWORD, password)
                        .putBoolean(PREFS_REMEMBER_ME, rememberMe)
                        .apply();
                break;
            case BOTH_SETTINGS:
                securePreferences.edit()
                        .putString(PREFS_DOMAIN, domain)
                        .putString(PREFS_SERVER_IP, serverIp)
                        .putString(PREFS_PATH_TO_ROOT, pathToRoot)
                        .putString(PREFS_USERNAME, username)
                        .putString(PREFS_PASSWORD, password)
                        .putBoolean(PREFS_REMEMBER_ME, rememberMe)
                        .apply();
                break;
        }
    }

    public static String getAuthString() {
        return String.format("smb://%s;%s:%s@%s/", domain, username, password, serverIp);
    }

    public static String getServerIp() {
        return serverIp;
    }

    public static String getPathToRoot() {
        return pathToRoot;
    }

    public static String getDomain() {
        return domain;
    }

    public static String getUsername() {
        return username;
    }

    public static String getPassword() {
        return password;
    }

    public static boolean getRememberMe() {
        return rememberMe;
    }

    public static void setServerIp(String value) {
        serverIp = value;
    }

    public static void setPathToRoot(String value) {
        pathToRoot = value;
    }

    public static void setDomain(String value) {
        domain = value;
    }

    public static void setUsername(String value) {
        username = value;
    }

    public static void setPassword(String value) {
        password = value;
    }

    public static void setRememberMe(boolean value) {
        rememberMe = value;
    }
}
