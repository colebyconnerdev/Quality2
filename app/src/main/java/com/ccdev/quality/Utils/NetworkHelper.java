package com.ccdev.quality.Utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;

/**
 * Created by Coleby on 7/10/2016.
 */

public class NetworkHelper {

    private static final String TAG = "Quality.NetworkHelper";
    private static final String FILES_FILTER = "(?i).*(.tif|.tiff|.gif|.jpeg|.jpg|.png)";
    public static final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy hh:mm aa");

    private static ArrayList<NetworkFile> mNetworkFiles;
    private static String mCurrentName;
    private static String mCurrentPath;

    private static class NetworkBitmap {
        private String pathToParentDir;
        private String pathToThumbsDir;
        private String pathToFile;
        private String pathToThumb;
        private String fileName;

        private NetworkBitmap(String pathToFile) {
            String[] split = pathToFile.split("/");

            this.pathToFile = pathToFile;
            fileName = split[split.length-1];
            pathToParentDir = pathToFile.replace(fileName, "");
            pathToThumbsDir = pathToParentDir + "thumbs/";
            pathToThumb = pathToThumbsDir + fileName;
        }

        public String getPathToParentDir() {
            return pathToParentDir;
        }

        public String getPathToThumbsDir() {
            return pathToThumbsDir;
        }

        public String getPathToFile() {
            return pathToFile;
        }

        public String getPathToThumb() {
            return  pathToThumb;
        }

        public String getFileName() {
            return fileName;
        }
    }

    public static class NetworkFile {

        private String fileName = "";
        private String pathToFile = "";
        private String details = "";
        private boolean isDirectory = true;

        private NetworkFile(String fileName, String pathToFile, boolean isDirectory) {
            this(fileName, "", pathToFile, isDirectory);
        }

        private NetworkFile(String fileName, String details, String pathToFile, boolean isDirectory) {
            this.fileName = fileName;
            this.details = details;
            this.pathToFile = pathToFile;
            this.isDirectory = isDirectory;
        }

        public String getFileName() {
            return fileName;
        }

        public String getPathToFile() {
            return pathToFile;
        }

        public String getDetails() {
            return details;
        }

        public boolean isDirectory() {
            return isDirectory;
        }
    }

    public static boolean isNetworkAvailable() {
        if (mNetworkFiles == null) return false;
        if (mCurrentName == null || mCurrentName.isEmpty()) return false;
        if (mCurrentPath == null || mCurrentPath.isEmpty()) return false;

        return true;
    }

    public static ArrayList<NetworkFile> getNetworkFiles() {
        return mNetworkFiles;
    }

    public static String getCurrentPath() {
        return mCurrentPath;
    }

    public static String getCurrentName() {
        return mCurrentName;
    }

    public static boolean getInitialFileTree() {
        if (Prefs.checkSettings(Prefs.BOTH_SETTINGS) != Prefs.RESULT_OK) {
            ErrorStack.add(TAG, "getFileTree(): Prefs.checkSettings(Prefs.BOTH_SETTINGS) failed.");
            ErrorStack.add(TAG, "^-- server settings: " + Prefs.checkSettings(Prefs.SERVER_SETTINGS));
            ErrorStack.add(TAG, "^-- user settings: " + Prefs.checkSettings(Prefs.USER_SETTINGS));

            return false;
        }

        String pathToDir = Prefs.getAuthString() + Prefs.getPathToRoot() + "/";

        return getFileTree(pathToDir);
    }

    public static boolean getFileTree(String pathToDir) {

        if (Prefs.checkSettings(Prefs.BOTH_SETTINGS) != Prefs.RESULT_OK) {
            ErrorStack.add(TAG, "getFileTree(): Prefs.checkSettings(Prefs.BOTH_SETTINGS) failed.");
            return false;
        }

        SmbFile smbRoot = null;
        try {
            smbRoot = new SmbFile(pathToDir);
        } catch (MalformedURLException e) {
            ErrorStack.add(TAG, "getFileTree(): error creating SmbFile:");
            ErrorStack.add(TAG, "^-- " + e.toString());
            return false;
        }

        try {
            if (!smbRoot.exists()) {
                ErrorStack.add(TAG, "getFileTree(): path does not exist:");
                ErrorStack.add(TAG, "^-- " + smbRoot.getPath());
                return false;
            }
        } catch (SmbException e) {
            ErrorStack.add(TAG, "getFileTree(): error checking SmbFile:");
            ErrorStack.add(TAG, "^-- " + e.toString());
            return false;
        }

        mCurrentName = smbRoot.getName();
        mCurrentPath = smbRoot.getPath();

        mNetworkFiles = new ArrayList<>();

        try {
            for(SmbFile smbFile : smbRoot.listFiles()) {
                if (smbFile.isHidden()) {
                    continue;
                }

                if (smbFile.isDirectory()) {
                    mNetworkFiles.add(new NetworkFile(
                            smbFile.getName(),
                            smbFile.getPath(),
                            true
                    ));
                }

                if (smbFile.isFile() && smbFile.getName().matches(FILES_FILTER)) {
                    mNetworkFiles.add(new NetworkFile(
                            smbFile.getName(),
                            smbFile.getPath(),
                            dateFormat.format(smbFile.getDate()),
                            false
                    ));
                }
            }
        } catch (SmbException e) {
            ErrorStack.add(TAG, "getFilesDir(): error listing files:");
            ErrorStack.add(TAG, "^-- " + e.toString());
        }

        return true;
    }

    public static Bitmap createOrGetThumbnail(String pathToFile) {

        NetworkBitmap networkBitmap = new NetworkBitmap(pathToFile);

        // TODO check thumbs dir - getThumbsDirFromNetwork(networkBitmap)
        // TODO create? exists - ^ result
        // TODO not created -> error

        // TODO download thumb - getThumbFromNetwork(networkBitmap)
        // TODO if created, upload thumb - ^ result
    }
}
