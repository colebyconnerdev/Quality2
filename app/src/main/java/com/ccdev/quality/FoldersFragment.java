package com.ccdev.quality;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.ccdev.quality.Utils.ErrorStack;
import com.ccdev.quality.Utils.NetworkHelper;
import com.ccdev.quality.Utils.Prefs;
import com.ccdev.quality.Views.BackHandledFragment;

import java.sql.BatchUpdateException;
import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by Coleby on 7/10/2016.
 */

public class FoldersFragment extends BackHandledFragment {

    // private static
    private static final String TAG = "Quality.FoldersFragment";

    // public static
    public static int RESULT_NETWORK_NOT_AVAILABLE = -1;
    public static int RESULT_PREFS_NOT_AVAILABLE = -2;

    // private
    private Stack<String> mPathsStack = new Stack<>();

    private TextView mHeader;
    private HorizontalScrollView mBreadCrumbsScroll;
    private LinearLayout mBreadCrumbsLayout;
    private ScrollView mFilesListScroll;
    private LinearLayout mFilesListLayout;
    private Button mNewPhoto, mNewScan, mNewFolder;

    // threads

    private OnFoldersListener mCallback;
    public interface OnFoldersListener {
        void OnFoldersError(int errorCode);
        void OnFoldersShowPhoto(String pathToFile);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_folders, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (!NetworkHelper.isNetworkAvailable()) {
            ErrorStack.add(TAG, "onActivityCreated(): network variables empty or null.");
            mCallback.OnFoldersError(RESULT_NETWORK_NOT_AVAILABLE);
            return;
        }

        if (Prefs.checkSettings(Prefs.BOTH_SETTINGS) != Prefs.RESULT_OK) {
            ErrorStack.add(TAG, "onActivityCreated(): preferences are not available.");
            mCallback.OnFoldersError(RESULT_PREFS_NOT_AVAILABLE);
            return;
        }

        mHeader = (TextView) getView().findViewById(R.id.folders_header);
        mBreadCrumbsScroll = (HorizontalScrollView) getView().findViewById(R.id.folders_breadCrumbsScroll);
        mBreadCrumbsLayout = (LinearLayout) getView().findViewById(R.id.folders_breadCrumbsLayout);
        mFilesListScroll = (ScrollView) getView().findViewById(R.id.folders_filesScroll);
        mFilesListLayout = (LinearLayout) getView().findViewById(R.id.folders_filesLayout);
        mNewFolder = (Button) getView().findViewById(R.id.folders_newFolder);
        mNewScan = (Button) getView().findViewById(R.id.folders_newScan);
        mNewFolder =(Button) getView().findViewById(R.id.folders_newPhoto);

        mNewFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO newFolder
            }
        });

        mNewScan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO newScan
            }
        });

        mNewFolder.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // TODO newPhoto
            }
        });

        updateFiles(NetworkHelper.getNetworkFiles());

        updateHeader(NetworkHelper.getCurrentName());

        updateBreadCrumbs(NetworkHelper.getCurrentPath());
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            mCallback = (OnFoldersListener) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString()
                    + " must implement OnFoldersListener");
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public boolean onBackPressed() {
        // TODO grab returned thread
        goBackThreaded();
        return true;
    }

    private Thread goBackThreaded() {
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                goBack();
            }
        });
        thread.start();

        return thread;
    }

    private void goBack() {
        if (mPathsStack != null && mPathsStack.size() > 1) {
            getFileTreeThreaded(mPathsStack.get(mPathsStack.size() - 2));
        }
    }

    private void updateHeaderOnUiThread(final String currentPath) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateHeader(currentPath);
            }
        });
    }

    private void updateHeader(String currentPath) {
        String header = currentPath;
        header = header.substring(0, header.length() - 1);
        mHeader.setText(header);
    }

    private void updateBreadCrumbsOnUiThread(final String currentPath) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateBreadCrumbs(currentPath);
            }
        });
    }

    private void updateBreadCrumbs(String currentPath) {

        if (mPathsStack.isEmpty()) {
            String[] rootSplit = Prefs.getPathToRoot().split("/");
            String name = rootSplit[rootSplit.length - 1];;

            mPathsStack.add(currentPath);

            View rootCrumb = getBreadCrumbView(name, currentPath);
            mBreadCrumbsLayout.addView(rootCrumb);

            return;
        }

        String lastPath = mPathsStack.peek();

        String[] currentSplit = currentPath.replace(Prefs.getAuthString(), "").split("/");
        String name = currentSplit[currentSplit.length - 1];

        if (currentPath.length() == lastPath.length()) {

            return;
        } else if (currentPath.length() > lastPath.length()) {

            mPathsStack.add(currentPath);

            View newCrumb = getBreadCrumbView(name, currentPath);
            mBreadCrumbsLayout.addView(newCrumb);

            return;
        }

        int mark = 0;
        for (int i = 0; i < mBreadCrumbsLayout.getChildCount(); i++) {
            if (((TextView) mBreadCrumbsLayout.getChildAt(i)).getText().equals(name)) {
                mark = i;
                break;
            }
        }

        for (int i = mBreadCrumbsLayout.getChildCount() - 1; i > mark; i--) {
            mPathsStack.pop();
            mBreadCrumbsLayout.removeViewAt(i);
        }
    }

    private View getBreadCrumbView(String name, String path) {

        TextView newView = new TextView(getContext());
        newView.setText(name);
        newView.setBackgroundResource(R.drawable.breadcrumbs);
        newView.setTag(path);
        newView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO grab returned thread
                getFileTreeThreaded(((String) v.getTag()));
            }
        });

        return newView;
    }

    private void updateFilesOnUiThread(final ArrayList<NetworkHelper.NetworkFile> files) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                updateFiles(files);
            }
        });
    }

    private void updateFiles(ArrayList<NetworkHelper.NetworkFile> files) {

        mFilesListLayout.removeAllViews();

        for (NetworkHelper.NetworkFile file : files) {
            View newFile = getFileView(file);
            mFilesListLayout.addView(newFile);
        }
    }

    private View getFileView(final NetworkHelper.NetworkFile file) {

        ConstraintLayout itemsLayout = (ConstraintLayout) LayoutInflater.from(getContext())
                .inflate(R.layout.item_folders, null);

        ImageView thumbnailView = (ImageView) itemsLayout.findViewById(R.id.item_folders_thumbnail);
        ProgressBar loadingView = (ProgressBar) itemsLayout.findViewById(R.id.item_folders_loading);
        TextView nameView = (TextView) itemsLayout.findViewById(R.id.item_folders_fileName);
        TextView detailsView = (TextView) itemsLayout.findViewById(R.id.item_folders_fileDetials);
        Button editButton = (Button) itemsLayout.findViewById(R.id.item_folders_button);

        String fileName = file.getFileName();
        fileName = fileName.substring(0, fileName.length() - 1);
        nameView.setText(fileName);
        detailsView.setText(file.getDetails());

        if (file.isDirectory()) {
            loadingView.setVisibility(View.GONE);
            thumbnailView.setBackgroundResource(R.drawable.item_folder);
        } else {
            // TODO grab returned thread
            getOrCreateThumbnailThreaded(file.getPathToFile(), thumbnailView, loadingView);
        }

        editButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO button action
            }
        });

        itemsLayout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (file.isDirectory()) {
                    // TODO grab returned thread
                    getFileTreeThreaded(file.getPathToFile());
                } else {
                    mCallback.OnFoldersShowPhoto(file.getPathToFile());
                }
            }
        });

        return itemsLayout;
    }

    private Thread getFileTreeThreaded(final String pathToFile) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                getFileTree(pathToFile);
            }
        });
        thread.start();

        return thread;
    }

    private void getFileTree(String pathToFile) {

        if (NetworkHelper.getFileTree(pathToFile)) {
            updateBreadCrumbsOnUiThread(pathToFile);
            updateFilesOnUiThread(NetworkHelper.getNetworkFiles());
            updateHeaderOnUiThread(NetworkHelper.getCurrentName());
        } else {
            generalError("Problems getting file tree.");
            ErrorStack.add(TAG, "getFileTree(): problem getting file tree.");
            ErrorStack.add(TAG, "^-- " + pathToFile);
            ErrorStack.dump();
            ErrorStack.flush();
        }
    }

    private Thread getOrCreateThumbnailThreaded(
            final String pathToFile, final ImageView thumbnailView, final ProgressBar loadingView) {

        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                Bitmap bitmap = NetworkHelper.getOrCreateThumbnail(pathToFile);
                if (bitmap != null) {
                    setThumbnailOnUiThread(bitmap, thumbnailView, loadingView);
                } else {
                    ErrorStack.dump();
                    ErrorStack.flush();
                    bitmap = BitmapFactory.decodeResource(getResources(), R.drawable.question_mark);
                    setThumbnailOnUiThread(bitmap, thumbnailView, loadingView);
                }
            }
        });
        thread.start();

        return thread;
    }

    private void setThumbnailOnUiThread(
            final Bitmap bitmap, final ImageView thumbnailView, final ProgressBar loadingView) {
        getActivity().runOnUiThread(new Runnable() {
            @Override
            public void run() {
                thumbnailView.setImageBitmap(bitmap);
                loadingView.setVisibility(View.INVISIBLE);
                thumbnailView.setVisibility(View.VISIBLE);
            }
        });
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
}
