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

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.TextView;

import java.util.HashMap;
import java.util.Map;

import de.stkl.gbgvertretungsplan.R;

/**
 * Created by Steffen Klee on 18.02.14.
 */
public class SubstitutionDetailDialog extends DialogFragment {
    private Map<String, String> detailInfo;
    private int activityWidth;
    private int dataType;

    public SubstitutionDetailDialog() {

    }

    public SubstitutionDetailDialog(Map<String, String> detailInfo, int dataType) { // dataType: 0: student; 1: teacher
        this.detailInfo = detailInfo;
        this.dataType = dataType;
    }

    public void setDetailInfo(Map<String, String> detailInfo) {
        this.detailInfo = detailInfo;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Map<String, String> detailInfo = null;
        int dataType = -1;
        if (savedInstanceState != null) {
            detailInfo = (HashMap<String, String>) savedInstanceState.getSerializable("detailInfo");
            dataType = savedInstanceState.getInt("dataType", -1);
        }
        if (detailInfo != null)
            this.detailInfo = detailInfo;
        if (dataType != -1)
            this.dataType = dataType;

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());

        LayoutInflater inflater = getActivity().getLayoutInflater();

        View view = inflater.inflate(R.layout.dialog_detail, null);
        if (this.dataType == 0) {
            ((TextView) view.findViewById(R.id.className)).setText(this.detailInfo.get("className"));
            ((TextView) view.findViewById(R.id.type)).setText(this.detailInfo.get("type"));
            ((TextView) view.findViewById(R.id.lesson)).setText(this.detailInfo.get("lesson"));
            ((TextView) view.findViewById(R.id.oldRoomSubject)).setText(this.detailInfo.get("oldRoomSubject"));
            ((TextView) view.findViewById(R.id.newRoomSubject)).setText(this.detailInfo.get("newRoomSubject"));
        } else if (this.dataType == 1) {
            ((TextView) view.findViewById(R.id.className)).setText(this.detailInfo.get("teacher"));
            ((TextView) view.findViewById(R.id.type)).setText(this.detailInfo.get("type"));
            ((TextView) view.findViewById(R.id.lesson)).setText(this.detailInfo.get("lesson"));
            ((TextView) view.findViewById(R.id.oldRoomSubject)).setText(this.detailInfo.get("oldTeacherSubjectRoom"));
            ((TextView) view.findViewById(R.id.newRoomSubject)).setText(this.detailInfo.get("newTeacherSubjectRoom"));

        }
        ((TextView) view.findViewById(R.id.date)).setText(this.detailInfo.get("date"));
        if (this.detailInfo.get("substitutionInfo").trim().equals(""))
            view.findViewById(R.id.substitutionInfo).setVisibility(View.GONE);
        else {
            view.findViewById(R.id.substitutionInfo).setVisibility(View.VISIBLE);
            ((TextView)view.findViewById(R.id.substitutionInfo)).setText(this.detailInfo.get("substitutionInfo"));
        }
        builder.setView(view);

        return builder.create();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable("detailInfo", (HashMap<String, String>)detailInfo);
        outState.putInt("lastActivityWidth", activityWidth);
        outState.putInt("dataType", dataType);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = super.onCreateView(inflater, container, savedInstanceState);

        if (savedInstanceState != null)
            activityWidth = savedInstanceState.getInt("lastActivityWidth", activityWidth);

        return v;
    }

    @Override
    public void onStart() {
        super.onStart();

        // safety check
        if (getActivity() == null || getDialog() == null)
            return;

        int width = getActivity().getWindow().findViewById(Window.ID_ANDROID_CONTENT).getRight();
        if (width == 0)
            return;
        int newWidth = getResources().getDimensionPixelSize(R.dimen.dialog_detail_width);

        // set dialog to width specified in dimen resource
        getDialog().getWindow().setLayout(newWidth >= width ? width : newWidth, getDialog().getWindow().getAttributes().height);

        activityWidth = width;
    }
}
