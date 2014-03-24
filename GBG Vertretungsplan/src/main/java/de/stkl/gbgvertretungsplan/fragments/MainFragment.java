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
import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBar.OnNavigationListener;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.webkit.WebView;
import android.widget.ArrayAdapter;
import android.widget.RelativeLayout;
import android.widget.SpinnerAdapter;
import android.widget.TextView;

import com.inqbarna.tablefixheaders.TableFixHeaders;
import com.inqbarna.tablefixheaders.adapters.BaseTableAdapter;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stkl.gbgvertretungsplan.BuildConfig;
import de.stkl.gbgvertretungsplan.R;
import de.stkl.gbgvertretungsplan.Util;
import de.stkl.gbgvertretungsplan.activities.MainActivity;
import de.stkl.gbgvertretungsplan.adapters.NavigationListAdapter;
import de.stkl.gbgvertretungsplan.authenticator.Authenticator;
import de.stkl.gbgvertretungsplan.content.SubstitutionTable;
import de.stkl.gbgvertretungsplan.errorreporting.ErrorReporter;
import de.stkl.gbgvertretungsplan.sync.Storage;
import de.stkl.gbgvertretungsplan.values.Account;
import de.stkl.gbgvertretungsplan.values.Sync;

/**
 * Created by Steffen Klee on 22.01.14.
 */
/**
 * Fragment containing the main_activity view ("Vertretungsplan")
 */
public class MainFragment extends PlaceholderFragment {
    private WebView webView = null;
    private Boolean loaded = false;

    private Bundle savedState = null;
    private boolean saveIndicatorObj = false;
    private boolean createStateInDestroyView;
    private static final String SAVED_BUNDLE_TAG = "saved_bundle";
    private static final String SAVED_INDICATOR = "saved_indicator";

    private static final String LOG_TAG = "MainFragment";

    private SpinnerAdapter mSpinnerAdapter = null;
    private ArrayList<String> mSpinnerList;
    private int mSpinnerPos = 0;

    private int lastCId = -1;
    private int lastDay = -1;

    ComInterface mCallback;


    public interface ComInterface {
        Sync.SYNC_STATUS getStatus();
        NavigationDrawerFragment getNavigationDrawerFragment();
        void setTitle(String title);
    }

    final OnNavigationListener mOnNavigationListener = new OnNavigationListener() {
        // Get the same strings provided for the drop-down's ArrayAdapter
        //String[] strings = getResources().getStringArray(R.array.action_spinner_mainview);
        @Override
        public boolean onNavigationItemSelected(int position, long itemId) {
            mSpinnerPos = position;
            if (BuildConfig.DEBUG)
                Log.d(LOG_TAG, "onNavigationItemSelected: "+String.valueOf(position));
            // reload the substitution table view
            updateTable(getView(), CId, position);
            return true;
        }
    };

    /**
     * Returns a new instance of this fragment for the given section
     * number.
     */
    public static MainFragment newInstance(int sectionNumber, NavigationDrawerFragment drawerFragment, int CId) {
        MainFragment fragment = new MainFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_SECTION_NUMBER, sectionNumber);
        args.putInt(ARG_CID, CId);
        fragment.setArguments(args);
        return fragment;
    }

    public MainFragment() {
    }

    /*public MainFragment(NavigationDrawerFragment drawerFragment) {
        mDrawerFragment = drawerFragment;
    }*/

    private Bundle saveState() {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "saveState");
        Bundle state = new Bundle();
        state.putBoolean(SAVED_INDICATOR, saveIndicatorObj);
        // ***** //
        // save objects
        state.putInt("cid", CId);
        state.putBoolean("loaded", loaded);
        state.putInt("spinner_pos", mSpinnerPos);
        // ***** //
        return state;
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (!saveIndicatorObj)
            outState.putBundle(SAVED_BUNDLE_TAG, savedState);
        else
            outState.putBundle(SAVED_BUNDLE_TAG, createStateInDestroyView ? savedState : saveState());
        createStateInDestroyView = false;

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallback = (ComInterface) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()+ " must implement ComInterface");
        }
    }

    private ActionBar getActionBar() {
        return ((ActionBarActivity)getActivity()).getSupportActionBar();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CId = getArguments().getInt(ARG_CID, -1);
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "onCreate");
        // restore instance state
        if (savedInstanceState != null) {
            savedState = savedInstanceState.getBundle(SAVED_BUNDLE_TAG);
            //loaded = savedState.getBoolean("loaded", false);
        }

        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        saveIndicatorObj = true;
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "onCreateView");
        //mSpinnerAdapter = ArrayAdapter.createFromResource(getActivity(), R.array.action_spinner_mainview, android.R.layout.simple_spinner_dropdown_item);

        mSpinnerList = new ArrayList<String>();
        mSpinnerList.add(getString(R.string.action_spinner_mainview_today));
        mSpinnerList.add(getString(R.string.action_spinner_mainview_tomorrow));
        mSpinnerAdapter = new ArrayAdapter<String>(getActivity(), android.R.layout.simple_spinner_dropdown_item, mSpinnerList);

        // restore instance state
        if (savedState != null) {
            loaded = savedState.getBoolean("loaded", false);
            if (CId == -1)
                CId = savedState.getInt("cid", CId);
            if (mSpinnerPos == 0)
                mSpinnerPos = savedState.getInt("spinner_pos", 0);
        }

        View rootView = inflater.inflate(R.layout.fragment_main, container, false);

        getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        getActionBar().setListNavigationCallbacks(mSpinnerAdapter, mOnNavigationListener);
        getActionBar().setSelectedNavigationItem(mSpinnerPos);  // does not trigger callback!
        updateTable(rootView, CId, mSpinnerPos, true);

        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "onCreateOptionsMenu");
        if (((MainActivity)getActivity()).getNavigationDrawerFragment() != null &&
                !((MainActivity)getActivity()).getNavigationDrawerFragment().isDrawerOpen()) {
            // Only show items in the action bar relevant to this screen
            // if the drawer is not showing. Otherwise, let the drawer
            // decide what to show in the action bar.
            inflater.inflate(R.menu.fragment_main, menu);
            getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
        }
        super.onCreateOptionsMenu(menu, inflater);


        //inflater.inflate(R.menu.fragment_main, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        getActivity();
        switch (id) {
            // request a sync
            case R.id.action_refresh:
                actionRefresh();
                break;
            // show help dialogue for colours
            case R.id.action_legend:
                DialogFragment newFragment = new LegendDialogFragment();
                newFragment.show(getActivity().getSupportFragmentManager(), "legend");
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        savedState = saveState();
        createStateInDestroyView = true;
        saveIndicatorObj = false;
    }

    public void actionRefresh() {
        // only if neither pending nor active
        if (!ContentResolver.isSyncActive(((MainActivity) getActivity()).mDefaultAccount, getString(Account.CONTENT_AUTHORITY)) &&
                !ContentResolver.isSyncPending(((MainActivity)getActivity()).mDefaultAccount, getString(Account.CONTENT_AUTHORITY)) ) {
            Bundle settingsBundle = new Bundle();
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);  // this is a manual sync
            settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED, true);   // start immediately
            ContentResolver.requestSync(Authenticator.getDefaultAccount(getActivity()), getString(Account.CONTENT_AUTHORITY), settingsBundle);
        }
    }

    public void updateTable(View rootView, int CId, int day) {
        updateTable(rootView, CId, day, false);
    }

    public void updateTable(View rootView, int CId, final int day, boolean remoteUpdate) {
        if (BuildConfig.DEBUG)
            Log.d(LOG_TAG, "updateTable(..)");
        if (mCallback.getStatus() == Sync.SYNC_STATUS.START)
            return;
        if (CId == -1 && this.CId != -1)
            CId = this.CId;
        setCId(CId);
        if (rootView == null)
            rootView = getView();

        if (day == lastDay && CId == lastCId && !remoteUpdate)
            return;

        lastDay = day;
        lastCId = CId;

        boolean error = false;
        SubstitutionTable.Table today = null, tomorrow = null;
        try {
            today = Storage.loadFromDisk2(getActivity(), 0); // today
            tomorrow = Storage.loadFromDisk2(getActivity(), 1); // tomorrow
        } catch (IOException e) {
            if (!(e instanceof FileNotFoundException))
                ErrorReporter.reportError(e, getActivity());
            error = true;
        } catch (ClassNotFoundException e) {
            ErrorReporter.reportError(e, getActivity());
            error = true;
        } finally {
            if (!error) {
                // if new data from server has arrived, update spinner items for today AND tomorrow; it is independent from currently selected day!
                if (remoteUpdate) {
                    SubstitutionTable.GeneralData generalData = null;
                    for (int i=0; i<2; i++) {
                        if (i == 0)
                            generalData = today.generalData;
                        else if (i == 1)
                            generalData = tomorrow.generalData;

                        if (!generalData.date.equals("")) {
                            // set spinner item (if generalData['date']) if present; index is the current day, so this is "just" a text change
                            mSpinnerList.set(i, generalData.date);
                        }
                    }
                    if (mSpinnerAdapter instanceof ArrayAdapter)
                        ((ArrayAdapter)mSpinnerAdapter).notifyDataSetChanged();
                }
                // append info to view (only selected day!)
                final View finRootView = rootView;
                final SubstitutionTable.Table finToday = today, finTomorrow = tomorrow;
                new Handler().postDelayed(new Runnable() {  // FIXME baaaaad hack (has to wait until activity has been rendered)
                    @Override
                    public void run() {
                        getActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (day == 0)
                                    fillTable(finRootView, finToday);
                                else if (day == 1)
                                    fillTable(finRootView, finTomorrow);
                            }
                        });
                    }
                }, 500);
            }
        }
    }

    private class SubstitutionPlanAdapter extends BaseTableAdapter implements TextCalculation {
        private final float density;
        private final Context context;
        private final LayoutInflater inflater;
        private String headers[];
        private List<Pair<List<String>, Integer>> rows;
        private Storage.FilterType filterType;
        private String filterString;
        private final int[] columnWidths;
        private Map<String, String> generalData;

        private SubstitutionTable.Table table;

        public SubstitutionPlanAdapter(Context context, String headers[], List<Pair<List<String>, Integer>> rows, int[] columnWidths, Map<String, String> generalData) {
            this.context = context;
            this.headers = headers;
            this.rows = rows;
            this.density = context.getResources().getDisplayMetrics().density;
            this.inflater = getActivity().getLayoutInflater();
            this.columnWidths = columnWidths;
            this.generalData = generalData;
        }

        public SubstitutionPlanAdapter(Context context, SubstitutionTable.Table table, int[] columnWidths) {
            this.context = context;
            this.density = context.getResources().getDisplayMetrics().density;
            this.inflater = getActivity().getLayoutInflater();
            this.columnWidths = columnWidths;
            this.table = table;
        }

        @Override
        public int getRowCount() {
//            return rows.size();
            return table.getEntryCount();
        }

        @Override
        public int getColumnCount() {
//            return headers.length;
            return table.getHeaderCount();
        }

        @Override
        public View getView(int row, int col, View convertView, ViewGroup parent) {
            final View view;
            switch (getItemViewType(row, col)) {
                // top-left
                case 0:
                    view = getFirstHeader(col, convertView, parent);
                    break;
                // header top
                case 1:
                    view = getHeader(col, convertView, parent);
                    break;
                // first col: dont need a first column as header
                case -1:
                    view = new View(context);
                    break;
                // the rest
                case 3:
                    view = getItem(row, col, convertView, parent);
                    break;
                default:
                    view = null;
                    break;
            }
            return view;
        }

        private View getItem(int row, int col, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.table_item, parent, false);
                if (convertView == null)
                    return null;
            }

            // assign onClick handler
            final int finalRow = row;
            convertView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Map<String, String> data = new HashMap<String, String>();
                    String date = table.generalData.date;
                    String[] dateArr = date.split(",");
                    if (dateArr.length > 0)
                        date = dateArr[0];
                    data.put("date", date);
                    data.put("className", table.getEntry(finalRow).className);
                    data.put("lesson", "Stunde(n): "+table.getEntry(finalRow).lesson);
                    data.put("oldRoomSubject", table.getEntry(finalRow).oldSubject + " in " + table.getEntry(finalRow).oldRoom);
                    data.put("newRoomSubject", table.getEntry(finalRow).newSubject + " in " + table.getEntry(finalRow).newRoom);
                    data.put("type", table.getEntry(finalRow).type);
                    data.put("substitutionInfo", table.getEntry(finalRow).substitutionInfo);

                    DialogFragment newFragment = new SubstitutionDetailDialog(data);
                    newFragment.show(getActivity().getSupportFragmentManager(), "subst_detail");
                }
            });

            int padLeft = convertView.getPaddingLeft(), padTop = convertView.getPaddingTop(), padRight = convertView.getPaddingRight(), padBot = convertView.getPaddingBottom();

            convertView.setBackgroundResource(table.getEntry(row).backgroundRID);
            convertView.setPadding(padLeft, padTop, padRight, padBot);

            ((TextView)convertView).setText(table.getEntry(row).get(col)/*+"("+String.valueOf(row)+"-"+String.valueOf(col)+")"*/);

            return convertView;
        }

        private View getFirstHeader(int col, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = new TextView(context);
            }
            ((TextView)convertView).setText(headers[col+1]/*+"("+String.valueOf(col)+")"*/);
            return convertView;
        }

        private View getHeader(int col, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.table_header, parent, false);
            }
            //((TextView)convertView).setText(headers[col]/*+"("+String.valueOf(col)+")"*/);
            ((TextView)convertView).setText(table.getHeader(col));
            return convertView;
        }

        @Override
        public int getWidth(int column) {
            if (column == -1)
                return 0;
            // calculate a columns' width based on its content
            //return calculateTableColWidth(headers, rows, this, column) + Math.round(density*5);
            return columnWidths[column] + Math.round(density*5);
        }

        @Override
        public int getHeight(int row) {
            return Math.round(30*density);
        }

        @Override
        public int getItemViewType(int row, int col) {
            // top-left
            if (row == -1 && col == -1)
                return -1;
            // first row: header
            else if (row == -1)
                return 1;
            // first column: don't need as header
            else if (col == -1)
                return -1;
            // rest
            else
                return 3;
        }

        @Override
        public int getViewTypeCount() {
            return 4;
        }
    }

    private int calculateTableCellWidth(String content, View textContainer) {
        if (!(textContainer instanceof TextView))
            return 0;
        Rect bounds = new Rect();
        Paint textPaint = ((TextView)textContainer).getPaint();
        textPaint.getTextBounds(content, 0, content.length(), bounds);
        //Log.d("calculateTableCellWidth", content+": "+bounds.width());
        //RelativeLayout.MarginLayoutParams params = ((RelativeLayout.MarginLayoutParams)((TextView) textContainer).getLayoutParams());
        int width = /*params.leftMargin + params.rightMargin + */((TextView)textContainer).getTotalPaddingLeft()+((TextView)textContainer).getTotalPaddingRight()+bounds.width();
        return width;
    }

    public interface TextCalculation {
        public View getView(int row, int col, View convertView, ViewGroup parent);
    }
    private int calculateTableColWidth(SubstitutionTable.Table day, TextCalculation tc, int col, ViewGroup parent) {
        if (parent == null)
            parent = (ViewGroup)getView().findViewById(R.id.table2);
        String curContent;
        int width = 0;
        for (int iRow = 0; iRow<day.getEntryCount()+1; iRow++) {
            if (iRow > 0)
                curContent = day.getEntry(iRow-1).get(col);
            else
                curContent = SubstitutionTable.Header.get(col);
            int cWidth;
            if (iRow == 0)
                cWidth = calculateTableCellWidth(curContent, tc.getView(-1, col, null, parent));
            else
                cWidth = calculateTableCellWidth(curContent, tc.getView(iRow-1, col, null, parent));
            if (cWidth > width)
                width = cWidth;
        }
        return width;
    }

    private int calculateTableWidth(SubstitutionTable.Table day, TextCalculation tc) {
        int width = 0;

        for (int iCol=0; iCol<day.getHeaderCount(); iCol++)
            width += calculateTableColWidth(day, tc, iCol, null);

        return width;
    }

    private void fillTable(View view, SubstitutionTable.Table day) {
        NavigationDrawerFragment ndf = mCallback.getNavigationDrawerFragment();
        NavigationListAdapter.SubItem o = (NavigationListAdapter.SubItem)ndf.getItemByCId(CId);

        TextView textPlaceholder = (TextView)view.findViewById(R.id.placeholder);

        TableFixHeaders tableFixHeaders = (TableFixHeaders) view.findViewById(R.id.table2);
        TextView updateTime = (TextView) view.findViewById(R.id.updateTime);

        // init values
        SubstitutionTable.Table newDay;

        // filter, if a particular class is selected
        if (o != null)
            newDay = Storage.filter(day, Storage.FilterType.FILTER_CLASS, o.text);
        else
            newDay = Storage.filter(day, Storage.FilterType.FILTER_NONE, null);

        // display substitution info only if display width is greater than treshold
        int width = getActivity().getWindow().findViewById(Window.ID_ANDROID_CONTENT).getRight();
        int threshold = getResources().getDimensionPixelSize(R.dimen.table_substitutioninfo_threshold);
        if (width < threshold)
            newDay.generalData.flags.add(SubstitutionTable.GeneralData.Flags.HIDE_SUBSTITUTIONINFO);

        int columnWidth[] = new int[day.getHeaderCount()];

        if (newDay.getEntryCount() > 0) {
            SubstitutionPlanAdapter adapter = new SubstitutionPlanAdapter(getActivity(), newDay, columnWidth);

            // initial calculation of column width
            for (int i=0; i<day.getHeaderCount(); i++) {
                columnWidth[i] = calculateTableColWidth(newDay, adapter, i, tableFixHeaders);
            }

            // assign adapter
            tableFixHeaders.setAdapter(adapter);
            tableFixHeaders.setVisibility(View.VISIBLE);
            textPlaceholder.setVisibility(View.GONE);
        } else {
            tableFixHeaders.setVisibility(View.GONE);
            textPlaceholder.setVisibility(View.VISIBLE);
        }

        // set update time
        updateTime.setText(newDay.generalData.updateTime);

        ViewGroup container = (ViewGroup)view.findViewById(R.id.container);
        container.removeView(container.findViewById(R.id.dailyInfo));

        // set daily infos
        int i=0;
        for (SubstitutionTable.GeneralData.DailyInfo info : newDay.generalData.dailyInfos) {
            View v = getActivity().getLayoutInflater().inflate(R.layout.table_dailyinfo, container, false);
            TextView tvTitle = (TextView)v.findViewById(R.id.title);
            TextView tvDescription = (TextView)v.findViewById(R.id.description);
            tvTitle.setText(info.title);
            tvDescription.setText(info.description);

            if (i != 0) {
                RelativeLayout.LayoutParams p2 = (RelativeLayout.LayoutParams) v.getLayoutParams();
                RelativeLayout.LayoutParams p = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT);
                p2.addRule(RelativeLayout.BELOW, R.id.dailyInfo2);
                v.setLayoutParams(p2);
            } else
                v.setId(R.id.dailyInfo2);

            container.addView(v, 0);
            i++;
        }
    }

    @Override
    public void setCId(int CId) {
        super.setCId(CId);

        // set action bar title to the subItem's name
        if (CId != -1 && mCallback != null)
            mCallback.setTitle(((NavigationListAdapter.SubItem)mCallback.getNavigationDrawerFragment().getItemByCId(CId)).text);
    }
}
