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

package de.stkl.gbgvertretungsplan.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import de.stkl.gbgvertretungsplan.R;

/**
 * Created by Steffen Klee on 22.01.14.
 */
public class NavigationListAdapter<V extends Map<?, ? extends List<?>>> extends BaseAdapter {

    public static class SubItem {
        public final String text;
        public final int cid;
        public SubItem(String text, int cid) {
            this.text = text;
            this.cid = cid;
//            this.number = number;
        }
    }

    public static final int VIEW_TYPE_HEADER = 0;
    public static final int VIEW_TYPE_LISTITEM = 1;

    protected V data;
    protected int[] sectionsStart;
    protected Object[] sections;
    protected int count;

    public NavigationListAdapter(V data) {
        this.data = data;
        onSetData();
    }

    @Override
    public int getCount() {
        return count;
    }

    @Override
    public Object getItem(int position) {
        int sectionIndex = getSectionForPosition(position);
        int innerIndex = position - sectionsStart[sectionIndex];
        if (innerIndex == 0) { //head
            return sections[sectionIndex];
        }
        else { //values
            return data.get(sections[sectionIndex]).get(innerIndex - 1);
        }
    }

    public Object getItemByCId(int CId) {
        if (data == null)
            return null;
        for (List<?> l: data.values()) {
            if (l == null)
                continue;
            for (Object i: l) {
                if (i == null)
                    continue;
                if (i instanceof SubItem && ((SubItem)i).cid == CId)
                    return i;
            }
        }
        return null;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public int getItemCId(int position) {
        if (getItem(position) instanceof SubItem)
            return ((SubItem)getItem(position)).cid;
        else
            return -1;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getItemViewType(int position) {
        return Arrays.binarySearch(sectionsStart, position) < 0 ? VIEW_TYPE_LISTITEM : VIEW_TYPE_HEADER;
    }

    public int getPositionForSection(int section) {
        return sectionsStart[section];
    }

    public int getSectionForPosition(int position) {
        int section = Arrays.binarySearch(sectionsStart, position);
        return section < 0 ? -section - 2 : section;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if(getItemViewType(position) == VIEW_TYPE_HEADER) {
            return getHeaderView(position, convertView, parent);
        }
        else {
            return getListItemView(position, convertView, parent);
        }
    }

    @Override
    public void notifyDataSetInvalidated() {
        data = null;
        onSetData();
        super.notifyDataSetInvalidated();
    }

    @Override
    public void notifyDataSetChanged() {
        onSetData();
        super.notifyDataSetChanged();
    }

    protected void onSetData() {
        if (data == null) {
            sectionsStart = null;
            sections = null;
            count = 0;
        }
        else {
            sectionsStart = new int[data.size()];
            sections = data.keySet().toArray(new Object[data.size()]);
            count = 0;
            int i = 0;
            for (List<?> v : data.values()) {
                sectionsStart[i] = count;
                i++;
                count += 1 + (v == null ? 0 : v.size());
            }
        }
    }

    protected View getHeaderView(int position, View convertView,
                                 ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TextView v = null;
        if (convertView == null)
            v = (TextView)inflater.inflate(R.layout.navigation_item, parent, false);
        else
            v = (TextView) convertView;
        /*TextView v = convertView == null ?
                new TextView(parent.getContext()) : (TextView) convertView;*/
        v.setText((String) getItem(position));
        return v;
    }

    protected View getListItemView(int position, View convertView,
                                   ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        TextView v = null;
        if (convertView == null)
            v = (TextView)inflater.inflate(R.layout.navigation_item_indented, parent, false);
        else
            v = (TextView) convertView;
        SubItem item = (SubItem) getItem(position);
        v.setText(item.text);
        return v;
    }
}
