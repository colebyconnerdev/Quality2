package com.ccdev.quality;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.ccdev.quality.Views.BackHandledFragment;

/**
 * Created by Coleby on 7/10/2016.
 */

public class LoginFragment extends BackHandledFragment {

    // TODO LoginFragment

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

    }

    @Override
    public boolean onBackPressed() {

        return false;
    }
}