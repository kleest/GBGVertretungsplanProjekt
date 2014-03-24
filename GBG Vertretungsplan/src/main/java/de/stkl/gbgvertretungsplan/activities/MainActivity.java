/*
 * Copyright (c) 2014 Steffen Klee
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.stkl.gbgvertretungsplan.activities;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.widget.Toast;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import de.stkl.gbgvertretungsplan.BuildConfig;
import de.stkl.gbgvertretungsplan.R;
import de.stkl.gbgvertretungsplan.authenticator.Authenticator;
import de.stkl.gbgvertretungsplan.fragments.AboutFragment;
import de.stkl.gbgvertretungsplan.fragments.Eula;
import de.stkl.gbgvertretungsplan.fragments.MainFragment;
import de.stkl.gbgvertretungsplan.fragments.NavigationDrawerFragment;
import de.stkl.gbgvertretungsplan.fragments.PlaceholderFragment;
import de.stkl.gbgvertretungsplan.sync.Storage;
import de.stkl.gbgvertretungsplan.values.Account;
import de.stkl.gbgvertretungsplan.values.Sync;

/**
 * Created by Steffen Klee on 27.11.13.
 */
public class MainActivity extends ActionBarActivity
        implements NavigationDrawerFragment.NavigationDrawerCallbacks, MainFragment.ComInterface {

    private static final String LOG_TAG = "MainActivity";

    private static final String STATE_PROGRESSBAR_VISIBLE = "state_progressbar_visible";

    private Fragment curFragment = null;
    public android.accounts.Account mDefaultAccount = null;
    private boolean mAccCreationCalled = false;

    private boolean mProgressBarVisible = false;

    private Sync.SYNC_STATUS syncState = Sync.SYNC_STATUS.NONE;

    /**
     * Fragment managing the behaviors, interactions and presentation of the navigation drawer.
     */
    private NavigationDrawerFragment mNavigationDrawerFragment;

    /**
     * Used to store the last screen title. For use in {@link #restoreActionBar()}.
     */
    private CharSequence mTitle;

    private final BroadcastReceiver syncFinishedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            boolean finished = false, error = false;
            switch((Sync.SYNC_STATUS)intent.getSerializableExtra("status")) {
                case START:
                    break;
                case ERROR:
                    finished = true;
                    error = true;
                    break;
                case OK:
                    finished = true;
                    break;
            }
            syncState = (Sync.SYNC_STATUS)intent.getSerializableExtra("status");
            final boolean finalFinished = finished;
            final boolean finalError = error;
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    updateSyncStatus(finalFinished, finalError);
                }
            });
        }
    };

    public void onSectionAttached(int i) {

    }

    private class CreateAccountDialog extends DialogFragment {

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
            builder.setMessage(getString(R.string.acccreation_notice, getString(R.string.app_name)))
                    .setPositiveButton(getString(R.string.acccreation_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(Settings.ACTION_ADD_ACCOUNT);
                            intent.putExtra(Settings.EXTRA_AUTHORITIES, new String[] { getString(Account.CONTENT_AUTHORITY) });
                            startActivityForResult(intent, 123);
                            dismiss();
                        }
                    });
            return builder.create();
        }

        @Override
        public void onCancel(DialogInterface dialog) {
            super.onCancel(dialog);
            getActivity().finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "onCreate");
        super.onCreate(savedInstanceState);
        supportRequestWindowFeature(Window.FEATURE_PROGRESS);
        supportRequestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        //supportRequestWindowFeature(Window.FEATURE_ACTION_BAR_OVERLAY);

        setContentView(R.layout.activity_main);

        mNavigationDrawerFragment = (NavigationDrawerFragment)
                getSupportFragmentManager().findFragmentById(R.id.navigation_drawer);

        // Set up the drawer.
        mNavigationDrawerFragment.setUp(
                R.id.navigation_drawer,
                (DrawerLayout) findViewById(R.id.drawer_layout));

        // restore instance state
        if (savedInstanceState != null)
            setProgressBarState(savedInstanceState.getBoolean(STATE_PROGRESSBAR_VISIBLE));

        // tos
        final Context _this = this;
        final Eula eula = new Eula(this, new Eula.CallbackInterface() {
            @Override
            public void onAccept() {
                // get default account
                mDefaultAccount = Authenticator.getDefaultAccount(_this);
                if (mDefaultAccount == null) {    // if no accounts exists, present the account creation activity to the user
                    CreateAccountDialog dialog = new CreateAccountDialog();
                    dialog.show(getSupportFragmentManager(), "CreateAccountDialog");
                }
            }

            @Override
            public void onDecline() {
            }
        });
        if (eula.mustShow())
            eula.show();
        else {
            // get default account
            mDefaultAccount = Authenticator.getDefaultAccount(this);
            if (mDefaultAccount == null) {    // if no accounts exists, present the account creation activity to the user
                CreateAccountDialog dialog = new CreateAccountDialog();
                dialog.show(getSupportFragmentManager(), "CreateAccountDialog");
            }
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(syncFinishedReceiver);
    }

    @Override
    protected void onResume() {
        super.onResume();
        registerReceiver(syncFinishedReceiver, new IntentFilter(Sync.ACTION_SYNC_FINISHED));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //super.onActivityResult(requestCode, resultCode, data);
        // TODO request code AND resultCode check (not working directly because it is a grand child..)
        if (/* requestCode == 123 && resultCode == RESULT_OK*/ Authenticator.getDefaultAccount(this) != null){   // if account authenticator activity has created a account
            if (curFragment instanceof MainFragment)
                ((MainFragment)curFragment).actionRefresh();

                //((MainFragment)curFragment).updateTable(null, -1, getSupportActionBar().getSelectedNavigationIndex(), true);
        }
        else /*if (requestCode == 123)*/
            finish();
    }

    public void updateSyncStatus(boolean done, boolean error) {
        if (curFragment instanceof MainFragment)
            setProgressBarState(!done);
        if (done && mNavigationDrawerFragment != null)
            mNavigationDrawerFragment.createNavigationList();
        // TODO race condition between navigationList and updateTable
        if (done && curFragment instanceof MainFragment) {
            if (error)
                Toast.makeText(this, getString(R.string.status_error), Toast.LENGTH_SHORT).show();
            else {
                Toast.makeText(this, getString(R.string.status_updated), Toast.LENGTH_SHORT).show();
                ((MainFragment)curFragment).updateTable(null, -1, getSupportActionBar().getSelectedNavigationIndex(), true);
            }
        }
    }

    public void setProgressBarState(boolean visible) {
        mProgressBarVisible = visible;
        //setSupportProgressBarIndeterminate(false);
        setSupportProgressBarIndeterminateVisibility(mProgressBarVisible);
        //setSupportProgressBarVisibility(mProgressBarVisible);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(STATE_PROGRESSBAR_VISIBLE, mProgressBarVisible);
    }

    @Override
    public void onNavigationDrawerItemSelected(int position, Object selItem) {
        // update the main_activity content by replacing fragments
        FragmentManager fragmentManager = getSupportFragmentManager();
        Fragment frag = null;
        if (BuildConfig.DEBUG)
            Log.i(LOG_TAG, "Selected item: "+String.valueOf(position));
        FragmentTransaction ft = fragmentManager.beginTransaction();
        if (curFragment != null)
            ft.detach(curFragment);

        int CId = mNavigationDrawerFragment != null ? mNavigationDrawerFragment.getItemCId(position) : -1;
        // sub items
        // unique id, that matches an item
        if (CId >= 0)
            selItem = mNavigationDrawerFragment.getItem(mNavigationDrawerFragment.getSectionForPosition(position));

        // group headers
        if (selItem.equals(getString(R.string.title_sec_main))) {
            mTitle = getString(R.string.title_sec_main);
            frag = fragmentManager.findFragmentByTag("main");
            if (frag == null) {
                frag = MainFragment.newInstance(0, mNavigationDrawerFragment, CId);
                ft.add(R.id.container, frag, "main");
            } else {
                ((MainFragment)frag).setCId(CId);
                ft.attach(frag);
            }
        } else if (selItem.equals(getString(R.string.title_sec_options))) {
            mTitle = getString(R.string.title_sec_options);
            frag = fragmentManager.findFragmentByTag("settings");
            if (frag == null) {
                frag = PlaceholderFragment.newInstance(1, CId);
                ft.add(R.id.container, frag, "settings");
            } else {
                ft.attach(frag);
            }
        } else if (selItem.equals(getString(R.string.title_sec_about))) {
            mTitle = getString(R.string.title_sec_about);
            frag = fragmentManager.findFragmentByTag("about");
            if (frag == null) {
                frag = AboutFragment.newInstance(2, mNavigationDrawerFragment, CId);
                ft.add(R.id.container, frag, "about");
            } else {
                ft.attach(frag);
            }
        }

        ft.commit();
        curFragment = frag;

        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "onNavigationDrawerItemSelected: "+mTitle);

        /*switch (position) {
            case 0:
                mTitle = getString(R.string.title_sec_main);
                break;
            case 1:
                mTitle = getString(R.string.title_sec_options);
                break;
            case 2:
                mTitle = getString(R.string.title_sec_about);
                break;
        }*/

    }

    @Override
    public List<String> getStoredClasses() throws IOException, ClassNotFoundException, FileNotFoundException {
        if (syncState != Sync.SYNC_STATUS.START) {
            List<String> classNames = Storage.loadFromDisk2(this, 0).getClassNames();
            classNames.addAll(Storage.loadFromDisk2(this, 1).getClassNames());
            Collections.sort(classNames);
            return classNames;
        } else
            return null;
    }

    public void restoreActionBar() {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "restoreActionBar: "+mTitle);
        ActionBar actionBar = getSupportActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setTitle(mTitle);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "onCreateOptionsMenu");
        if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            getMenuInflater().inflate(R.menu.main_activity, menu);
            restoreActionBar();
            return true;
        }
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        return super.onOptionsItemSelected(item);
    }

    public NavigationDrawerFragment getNavigationDrawerFragment() {
        return mNavigationDrawerFragment;
    }

    @Override
    public void setTitle(String title) {
        if (title == mTitle)
            return;
        mTitle = title;
        if (mNavigationDrawerFragment != null && !mNavigationDrawerFragment.isDrawerOpen()) {
            ActionBar actionBar = getSupportActionBar();
            actionBar.setTitle(mTitle);
        }
    }

    @Override
    public Sync.SYNC_STATUS getStatus() {
        return syncState;
    }
}
