package com.ccdev.quality.Utils;

import android.util.Log;

import java.util.ArrayList;
import java.util.Stack;

/**
 * Created by Coleby on 7/10/2016.
 */

public class ErrorStack {

    private static boolean mLogging = false;
    private static ArrayList<StackItem> mStack = new ArrayList<>();

    private static class StackItem {
        private String tag;
        private String errorMessage;

        private StackItem(String tag, String errorMessage) {
            this.tag = tag;
            this.errorMessage = errorMessage;
        }
    }

    public static void add(String tag, String errorMessage) {

        if (!mLogging) return;

        mStack.add(new StackItem(tag, errorMessage));
    }

    public static void dump() {

        if (!mLogging) return;

        for (StackItem item : mStack) {
            Log.d(item.tag, item.errorMessage);
        }
    }

    public static void flush() {

        if (!mLogging) return;

        mStack.clear();
    }

    public static void setLogging(boolean logging) {
        mLogging = logging;
    }
}
