package com.ccdev.quality.Utils;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import jcifs.smb.SmbException;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileInputStream;
import jcifs.smb.SmbFileOutputStream;

/**
 * Created by Coleby on 7/10/2016.
 */

public class NetworkHelper {

    private static final String TAG = "Quality.NetworkHelper";
    private static final String FILES_FILTER = "(?i).*(.tif|.tiff|.gif|.jpeg|.jpg|.png)";
    private static final int BUFFER_SIZE = 4096;
    private static final int THUMB_QUALITY = 100;
    private static final int THUMB_WIDTH = 100;
    private static final int THUMB_HEIGHT = 100;

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
            ErrorStack.add(TAG, "^-- " + smbRoot.getPath());
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
            ErrorStack.add(TAG, "^-- " + smbRoot.getPath());
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
                            dateFormat.format(smbFile.getDate()),
                            smbFile.getPath(),
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

    public static Bitmap getOrCreateThumbnail(String pathToFile) {
        SmbFile smbFile;
        boolean thumbsDirExists = false;
        boolean thumbnailExists = false;
        BufferedInputStream inputStream;
        BufferedOutputStream outputStream;
        Bitmap bitmap;

        NetworkBitmap networkBitmap = new NetworkBitmap(pathToFile);

        // check to see if parent directory exists

        try {
            smbFile = new SmbFile(networkBitmap.getPathToParentDir());
        } catch (MalformedURLException e) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): (parent dir) smbFile = new SmbFile().");
            ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToParentDir());
            ErrorStack.add(TAG, "^-- " + e.toString());
            return null;
        }

        try {
            if (!smbFile.exists()) {
                ErrorStack.add(TAG, "getOrCreateThumbnail(): parent dir does not exist.");
                ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToParentDir());
                return null;
            }
        } catch (SmbException e) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): (parent dir) smbFile.exists().");
            ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToParentDir());
            ErrorStack.add(TAG, "^-- " + e.toString());
            return null;
        }

        // check to see if thumbnail directory exists

        try {
            smbFile = new SmbFile(networkBitmap.getPathToThumbsDir());
        } catch (MalformedURLException e) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): (thumbnail dir) smbFile = new SmbFile().");
            ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToThumbsDir());
            ErrorStack.add(TAG, "^-- " + e.toString());
            return null;
        }

        try {
            thumbsDirExists = smbFile.exists();
        } catch (SmbException e) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): (thumbnail dir) smbFile.exists().");
            ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToThumbsDir());
            ErrorStack.add(TAG, "^-- " + e.toString());
            return null;
        }

        // if thumbnail directory does not exist attempt to create it

        try {
            if (!thumbsDirExists) {
                smbFile.mkdir();
                smbFile.setAttributes(SmbFile.ATTR_HIDDEN);
            }
        } catch (SmbException e) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): (thumbnail dir) smbFile.mkdir().");
            ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToThumbsDir());
            ErrorStack.add(TAG, "^-- " + e.toString());
            ErrorStack.dump();
            ErrorStack.flush();
            // don't return here, folder may have been created by other thread.
        }

        // check to see if thumbnail exists

        try {
            smbFile = new SmbFile(networkBitmap.getPathToThumb());
        } catch (MalformedURLException e) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): (thumbnail) smbFile = new SmbFile().");
            ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToThumb());
            ErrorStack.add(TAG, "^-- " + e.toString());
            return null;
        }

        try {
            thumbnailExists = smbFile.exists();
        } catch (SmbException e) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): (thumbnail) smbFile.exists().");
            ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToThumb());
            ErrorStack.add(TAG, "^-- " + e.toString());
            return null;
        }

        // create the appropriate input stream

        try {
            if (thumbnailExists) {
                inputStream = new BufferedInputStream(
                        new SmbFileInputStream(networkBitmap.getPathToThumb()), BUFFER_SIZE);
            } else {
                inputStream = new BufferedInputStream(
                        new SmbFileInputStream(networkBitmap.getPathToFile()), BUFFER_SIZE);
            }
        } catch (MalformedURLException e) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): inputStream = new BufferedInputStream().");
            ErrorStack.add(TAG, "^-- " + (thumbnailExists ?
                    networkBitmap.getPathToThumb() : networkBitmap.getPathToFile()));
            ErrorStack.add(TAG, "^-- " + e.toString());
            return null;
        } catch (UnknownHostException e ) {;
            ErrorStack.add(TAG, "getOrCreateThumbnail(): inputStream = new BufferedInputStream().");
            ErrorStack.add(TAG, "^-- " + (thumbnailExists ?
                    networkBitmap.getPathToThumb() : networkBitmap.getPathToFile()));
            ErrorStack.add(TAG, "^-- " + e.toString());
            return null;
        } catch (SmbException e) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): inputStream = new BufferedInputStream().");
            ErrorStack.add(TAG, "^-- " + (thumbnailExists ?
                    networkBitmap.getPathToThumb() : networkBitmap.getPathToFile()));
            ErrorStack.add(TAG, "^-- " + e.toString());
            return null;
        }

        // if thumbnail does not exist create options to scale full size photo

        BitmapFactory.Options options = new BitmapFactory.Options();

        if (!thumbnailExists) {
            options.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(inputStream, null, options);

            options.inJustDecodeBounds = false;
            options.inSampleSize = 1;

            final int height = options.outHeight;
            final int width = options.outWidth;

            if (height > THUMB_HEIGHT || width > THUMB_WIDTH) {

                final int halfWidth = width / 2;
                final int halfHeight = height / 2;

                while ((halfHeight / options.inSampleSize) > THUMB_HEIGHT
                        && (halfWidth / options.inSampleSize) > THUMB_WIDTH) {
                    options.inSampleSize *= 2;
                }

            }

            try {
                inputStream = new BufferedInputStream(new SmbFileInputStream(networkBitmap.getPathToFile()), BUFFER_SIZE);
            } catch (MalformedURLException e) {
                ErrorStack.add(TAG, "getOrCreateThumbnail(): inputStream = new BufferedInputStream().");
                ErrorStack.add(TAG, "^-- " + (thumbnailExists ?
                        networkBitmap.getPathToThumb() : networkBitmap.getPathToFile()));
                ErrorStack.add(TAG, "^-- " + e.toString());
                return null;
            } catch (UnknownHostException e ) {;
                ErrorStack.add(TAG, "getOrCreateThumbnail(): inputStream = new BufferedInputStream().");
                ErrorStack.add(TAG, "^-- " + (thumbnailExists ?
                        networkBitmap.getPathToThumb() : networkBitmap.getPathToFile()));
                ErrorStack.add(TAG, "^-- " + e.toString());
                return null;
            } catch (SmbException e) {
                ErrorStack.add(TAG, "getOrCreateThumbnail(): inputStream = new BufferedInputStream().");
                ErrorStack.add(TAG, "^-- " + (thumbnailExists ?
                        networkBitmap.getPathToThumb() : networkBitmap.getPathToFile()));
                ErrorStack.add(TAG, "^-- " + e.toString());
                return null;
            }
        }

        // download the thumbnail

        bitmap = BitmapFactory.decodeStream(inputStream, null, options);

        try {
            inputStream.close();
        } catch (IOException e) {
            // this exception does not need broadcast
        }

        if (bitmap == null) {
            ErrorStack.add(TAG, "getOrCreateThumbnail(): BitmapFactory.decodeStream() returned null.");
            return null;
        }

        // if thumbnail was created upload

        if (!thumbnailExists) {

            try {
                outputStream = new BufferedOutputStream(new SmbFileOutputStream(networkBitmap.getPathToThumb()));
            } catch (MalformedURLException e) {
                ErrorStack.add(TAG, "getOrCreateThumbnail(): outputStream = new BufferedOutputStream().");
                ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToThumb());
                ErrorStack.add(TAG, "^-- " + e.toString());
                return null;
            } catch (UnknownHostException e) {
                ;
                ErrorStack.add(TAG, "getOrCreateThumbnail(): outputStream = new BufferedOutputStream().");
                ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToThumb());
                ErrorStack.add(TAG, "^-- " + e.toString());
                return null;
            } catch (SmbException e) {
                ErrorStack.add(TAG, "getOrCreateThumbnail(): outputStream = new BufferedOutputStream().");
                ErrorStack.add(TAG, "^-- " + networkBitmap.getPathToThumb());
                ErrorStack.add(TAG, "^-- " + e.toString());
                return null;
            }

            bitmap.compress(Bitmap.CompressFormat.JPEG, THUMB_QUALITY, outputStream);
        }

        return bitmap;
    }

    public static boolean createNewFolder(String pathToDir) {
        SmbFile smbFile;

        try {
            smbFile = new SmbFile(pathToDir);
        } catch (MalformedURLException e) {
            Log.e(TAG, e.toString()); // TODO remove this
            return false;
        }

        try {
            if (smbFile.exists()) {
                Log.e(TAG, "already exists"); // TODO remove this
                return true;
            }
        } catch (SmbException e) {
            Log.e(TAG, e.toString()); // TODO remove this
            return false;
        }

        try {
            smbFile.mkdir();
        } catch (SmbException e) {
            Log.e(TAG, e.toString()); // TODO remove this
            return false;
        }

        return true;
    }
}
