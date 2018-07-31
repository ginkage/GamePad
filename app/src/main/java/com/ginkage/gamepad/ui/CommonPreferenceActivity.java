package com.ginkage.gamepad.ui;

import android.content.Intent;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.support.annotation.CallSuper;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.Preference;
import android.support.v7.preference.PreferenceFragmentCompat;
import android.text.TextUtils;

public class CommonPreferenceActivity extends AppCompatActivity
    implements PreferenceFragmentCompat.OnPreferenceStartFragmentCallback {
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
    protected void onStart() {
        super.onStart();

        Fragment fragment = getSupportFragmentManager().findFragmentById(android.R.id.content);
        if (fragment instanceof PreferenceFragmentCompat) {
            CharSequence title =
                    ((PreferenceFragmentCompat) fragment).getPreferenceScreen().getTitle();
            if (!TextUtils.isEmpty(title) && !TextUtils.equals(title, getTitle())) {
                setTitle(title);
            }
      }
    }

    @Override
    public boolean onPreferenceStartFragment(PreferenceFragmentCompat caller, Preference pref) {
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
            getSupportFragmentManager()
                    .popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(android.R.id.content, fragment);
            transaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            transaction.commitAllowingStateLoss();
        }
    }

    /**
     * Build an Intent to launch a new activity showing the selected fragment. The default
     * implementation constructs an Intent that re-launches the current activity with the
     * appropriate arguments to display the fragment.
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
