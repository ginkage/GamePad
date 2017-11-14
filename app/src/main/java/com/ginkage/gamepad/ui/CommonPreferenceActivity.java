package com.ginkage.gamepad.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.annotation.CallSuper;

public class CommonPreferenceActivity extends Activity
        implements PreferenceFragment.OnPreferenceStartFragmentCallback {
    @Override
    @CallSuper
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            Intent intent = getIntent();
            String initialFragment = intent.getStringExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT);
            Bundle initialArguments =
                    intent.getBundleExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS);
            if (initialFragment != null) {
                startPreferenceFragment(
                        Fragment.instantiate(this, initialFragment, initialArguments), false);
            }
        }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragment caller, Preference pref) {
        startActivity(onBuildStartFragmentIntent(pref.getFragment(), pref.getExtras(), 0));
        return true;
    }

    /**
     * Start a new fragment.
     *
     * @param fragment The fragment to start
     * @param push If true, the current fragment will be pushed onto the back stack. If false, the
     *     current fragment will be replaced.
     */
    public void startPreferenceFragment(Fragment fragment, boolean push) {
        if (push) {
            startActivity(
                    onBuildStartFragmentIntent(
                            fragment.getClass().getName(), fragment.getArguments(), 0));
        } else {
            getFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            transaction.replace(android.R.id.content, fragment);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commitAllowingStateLoss();
        }
    }

    /**
     * Build an Intent to launch a new activity showing the selected fragment. The default
     * implementation constructs an Intent that re-launches the current activity with the appropriate
     * arguments to display the fragment.
     *
     * @param fragmentName The name of the fragment to display.
     * @param args Optional arguments to supply to the fragment.
     * @param titleRes Optional resource ID of title to show for this item.
     * @return Returns an Intent that can be launched to display the given fragment.
     */
    public Intent onBuildStartFragmentIntent(String fragmentName, Bundle args, int titleRes) {
        return new Intent(Intent.ACTION_MAIN)
                .setClass(this, CommonPreferenceActivity.class)
                .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, fragmentName)
                .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_ARGUMENTS, args)
                .putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT_TITLE, titleRes)
                .putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);
    }
}
