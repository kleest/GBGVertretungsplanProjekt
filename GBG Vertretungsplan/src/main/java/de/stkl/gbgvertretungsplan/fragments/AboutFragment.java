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

import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import de.stkl.gbgvertretungsplan.R;
import de.stkl.gbgvertretungsplan.Util;

/**
 * Created by Steffen Klee on 15.02.14.
 */
public class AboutFragment extends PlaceholderFragment {

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static AboutFragment newInstance(int sectionNumber, NavigationDrawerFragment drawerFragment, int CId) {
        AboutFragment fragment = new AboutFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putInt(ARG_CID, CId);
        fragment.setArguments(args);
        return fragment;
    }

    public AboutFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CId = getArguments().getInt(ARG_CID, -1);
        setHasOptionsMenu(false);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
/*
        mSpinnerList = new ArrayList<String>();
        mSpinnerList.add(getString(R.string.action_spinner_mainview_today));
        mSpinnerList.add(getString(R.string.action_spinner_mainview_tomorrow));
        mSpinnerAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, mSpinnerList);
*/

        View rootView = inflater.inflate(R.layout.fragment_about, container, false).getRootView();

        final Resources res = getResources();
        String[] titles = res.getStringArray(R.array.about_item_titles);
        String[] summaries = res.getStringArray(R.array.about_item_summaries);

        if (titles.length == summaries.length) {
            for (int i=0; i< titles.length; i++) {
                // format string
                switch (i) {
                    // copyright and app information
                    case 0:
                        try {
                            titles[i] = String.format(titles[i], getString(R.string.app_name), getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0).versionName);
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        break;
                }

                View aboutItem = inflater.inflate(R.layout.item_about, (ViewGroup)rootView, false);
                ((TextView)aboutItem.findViewById(R.id.title)).setText(titles[i]);
                if (!summaries[i].equals(""))
                    ((TextView)aboutItem.findViewById(R.id.summary)).setText(summaries[i]);
                else {
                    View v = aboutItem.findViewById(R.id.summary);
                    ((ViewGroup)v.getParent()).removeView(v);
                }

                // assign onclick handler
                switch (i) {
                    // terms of use
                    case 1:
                        aboutItem.findViewById(R.id.container).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DialogFragment newFragment = null;
                                newFragment = new PopupDialog(Util.convertStreamToString(res.openRawResource(R.raw.tos)));
                                newFragment.show(getActivity().getSupportFragmentManager(), "tos");
                            }
                        });
                        break;
                    // open source licenses
                    case 2:
                        aboutItem.findViewById(R.id.container).setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                DialogFragment newFragment = null;
                                newFragment = new PopupDialog(Util.convertStreamToString(res.openRawResource(R.raw.licenses)));
                                newFragment.show(getActivity().getSupportFragmentManager(), "license");
                            }
                        });
                        break;
                }

                ((ViewGroup) rootView).addView(aboutItem);
            }
        }
        return rootView;
    }
}
