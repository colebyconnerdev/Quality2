package com.ccdev.quality.Views;

import android.content.Context;
import android.support.v4.app.Fragment;

/**
 * Created by Coleby on 7/10/2016.
 */

public abstract class BackHandledFragment extends Fragment {

    protected BackHandlerInterface backHandlerInterface;
    public abstract boolean onBackPressed();

    public interface BackHandlerInterface {
        void setSelectedFragment(BackHandledFragment backHandledFragment);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        try {
            backHandlerInterface = (BackHandlerInterface) this;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() +
                    " must implement BackHandlerInterface");
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        backHandlerInterface.setSelectedFragment(this);
    }
}
