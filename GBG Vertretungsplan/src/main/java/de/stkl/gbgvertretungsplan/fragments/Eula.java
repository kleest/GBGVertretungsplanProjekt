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

package de.stkl.gbgvertretungsplan.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.text.Html;
import android.view.KeyEvent;

import de.stkl.gbgvertretungsplan.R;
import de.stkl.gbgvertretungsplan.Util;

/**
 * Created by Steffen Klee on 26.02.14.
 */
public class Eula {

    private static final String EULA_PREFIX = "eula_";
    private final String eulaKey;
    private final Activity mActivity;
    private CallbackInterface callbackInterface;

    public Eula(Activity context) {
        mActivity = context;
        eulaKey = EULA_PREFIX;// + versionInfo.versionCode;
    }
    public Eula(Activity context, CallbackInterface callbackInterface) {
        this(context);
        this.callbackInterface = callbackInterface;
    }

    private PackageInfo getPackageInfo() {
        PackageInfo pi = null;
        try {
            pi = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), PackageManager.GET_ACTIVITIES);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return pi;
    }

    public boolean mustShow() {
        // the eulaKey changes every time you increment the version number in the AndroidManifest.xml
        final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        boolean hasBeenShown = prefs.getBoolean(eulaKey, false);
        return !hasBeenShown;
    }

    public void show() {
        if (mustShow()){
            PackageInfo versionInfo = getPackageInfo();
            final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(mActivity);

            // Show the Eula
            String title = mActivity.getString(R.string.app_name) + " v" + versionInfo.versionName;

            //Includes the updates as well so users know what changed.
            String message = Util.convertStreamToString(mActivity.getResources().openRawResource(R.raw.tos));// + "\n\n" + mActivity.getString(R.string.eula);

            AlertDialog.Builder builder = new AlertDialog.Builder(mActivity)
                    .setTitle(title)
                    .setMessage(Html.fromHtml(message))
                    .setPositiveButton(android.R.string.ok, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            // Mark this version as read.
                            SharedPreferences.Editor editor = prefs.edit();
                            editor.putBoolean(eulaKey, true);
                            editor.commit();
                            dialogInterface.dismiss();
                            if (callbackInterface != null)
                                callbackInterface.onAccept();
                        }
                    })
                    .setNegativeButton(android.R.string.cancel, new Dialog.OnClickListener() {

                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Close the activity as they have declined the EULA
                            mActivity.finish();
                            if (callbackInterface != null)
                                callbackInterface.onDecline();
                        }

                    })
                    .setOnCancelListener(new DialogInterface.OnCancelListener() {
                        public void onCancel(DialogInterface dialog) {
                            // Close the activity as they have declined the EULA
                            mActivity.finish();
                            if (callbackInterface != null)
                                callbackInterface.onDecline();
                        }
                    })
                    //prevent back and search key
                    .setOnKeyListener(new DialogInterface.OnKeyListener() {
                        @Override
                        public boolean onKey(DialogInterface dialoginterface,
                                             int keyCode, KeyEvent event) {
                            return !((keyCode == KeyEvent.KEYCODE_HOME) || (keyCode == KeyEvent.KEYCODE_SEARCH));
                        }
                    });
            builder.create().show();
        }
    }

    public static interface CallbackInterface {
        void onAccept();
        void onDecline();
    }
}
