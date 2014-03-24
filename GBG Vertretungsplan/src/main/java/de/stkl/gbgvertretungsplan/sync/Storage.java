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

package de.stkl.gbgvertretungsplan.sync;

import android.content.Context;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.stkl.gbgvertretungsplan.R;
import de.stkl.gbgvertretungsplan.Util;
import de.stkl.gbgvertretungsplan.content.SubstitutionTable;
import de.stkl.gbgvertretungsplan.errorreporting.ErrorReporter;
import de.stkl.gbgvertretungsplan.values.Sync;

/**
 * Created by Steffen Klee on 17.12.13.
 */
public class Storage {
    // TODO error handling: display error
    public static SubstitutionTable.Table loadFromDisk2(Context context, int day) throws IOException, ClassNotFoundException {
        // read saved database info
        FileInputStream is = null;
        ObjectInputStream ois = null;

        Map<String, String> generalData = null;
        List<String> categories = null;
        List<List<String>> allRows = null;

        // 1. general data
        is = context.openFileInput(String.format("generalData_%d.dat", day));
        ois = new ObjectInputStream(is);
        generalData = (Map<String, String>)ois.readObject();
        ois.close();
        is.close();

        // 2. categories
        is = context.openFileInput(String.format("categories_%d.dat", day));
        ois = new ObjectInputStream(is);
        categories = (ArrayList<String>)ois.readObject();
        ois.close();
        is.close();

        // 3. all the data
        is = context.openFileInput(String.format("rowdata_%d.dat", day));
        ois = new ObjectInputStream(is);
        allRows = (ArrayList<List<String>>)ois.readObject();
        ois.close();
        is.close();

        SubstitutionTable.Table table = new SubstitutionTable.Table();

        // parse general data
        table.generalData.date = generalData.get(Sync.GENERAL_DATA_DATE);
        table.generalData.updateTime = generalData.get(Sync.GENERAL_DATA_UPDATETIME);

        // general data: daily info
        for (int i=0; i<Sync.GENERAL_DATA_DAILYINFO_MAX; i++) {
            String keyTitle = "", keyDescription = "";
            switch(i) {
                case 0: keyTitle = Sync.GENERAL_DATA_DAILYINFO_1_TITLE; keyDescription = Sync.GENERAL_DATA_DAILYINFO_1_DESCRIPTION;
                    break;
                case 1: keyTitle = Sync.GENERAL_DATA_DAILYINFO_2_TITLE; keyDescription = Sync.GENERAL_DATA_DAILYINFO_2_DESCRIPTION;
                    break;
                case 2: keyTitle = Sync.GENERAL_DATA_DAILYINFO_3_TITLE; keyDescription = Sync.GENERAL_DATA_DAILYINFO_3_DESCRIPTION;
                    break;
                default:
                    break;
            }
            if (!keyTitle.equals("")) {
                String title = generalData.get(keyTitle);
                String description = generalData.get(keyDescription);
                if (title != null && description != null)
                    table.generalData.dailyInfos.add(new SubstitutionTable.GeneralData.DailyInfo(title, description));
            }
        }


        for (List<String> row: allRows) {
            SubstitutionTable.Entry newEntry = new SubstitutionTable.Entry(table);

            int i=0;
            for (String categ: categories) {
                String item = row.get(i);
                switch (SubstitutionTable.lookupKey(categ)) {
                    case CLASSNAME:
                        newEntry.className = item;
                        break;
                    case LESSON:
                        newEntry.lesson = item;
                        break;
                    case NEW_ROOM:
                        newEntry.newRoom = item;
                        break;
                    case NEW_SUBJECT:
                        newEntry.newSubject = item;
                        break;
                    case OLD_ROOM:
                        newEntry.oldRoom = item;
                        break;
                    case OLD_SUBJECT:
                        newEntry.oldSubject = item;
                        break;
                    case TYPE:
                        newEntry.type = item;
                        break;
                    case SUBSTITUTIONINFO:
                        newEntry.substitutionInfo = item.replaceAll("\u00a0","");
                        break;
                    default:
                        break;
                }
                i++;
            }
            table.addEntry(newEntry);
        }

        return table;
        //return new Pair<Map<String,String>, List<SubstitutionTable.Entry>>(generalData, entries);
    }

    // TODO error handling: display errors in activity so user is notified
    public static void saveToDisk(Context context, Map<String, String> generalData, List<String> categories, List<List<String>> rows, int day) {
        try {
            FileOutputStream os = null;
            ObjectOutputStream oos = null;

            // 1. general data
            os = context.openFileOutput(String.format("generalData_%d.dat", day), Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(os);
            oos.writeObject(generalData);
            oos.close();
            os.close();

            // 2. categories
            os = context.openFileOutput(String.format("categories_%d.dat", day), Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(os);
            oos.writeObject(categories);
            oos.close();
            os.close();

            // 3. all the substitution data
            os = context.openFileOutput(String.format("rowdata_%d.dat", day), Context.MODE_PRIVATE);
            oos = new ObjectOutputStream(os);
            oos.writeObject(rows);
            oos.close();
            os.close();
        } catch (IOException e) {
            ErrorReporter.reportError(e, context);
        }
    }

    public static enum FilterType {
        FILTER_NONE,
        FILTER_CLASS
    }

    public static List<SubstitutionTable.Entry> cloneList(List<SubstitutionTable.Entry> list) {
        List<SubstitutionTable.Entry> clone = new ArrayList<SubstitutionTable.Entry>(list.size());
        for (SubstitutionTable.Entry item: list)
            clone.add(item);
        return clone;
    }

    public static SubstitutionTable.Table filter(SubstitutionTable.Table input, FilterType filterType, String filterString) {

        switch (filterType) {
            //  if class filter is applied, first category (class information) is removed
            case FILTER_CLASS:
                input.generalData.flags.add(SubstitutionTable.GeneralData.Flags.HIDE_CLASSNAME);
                break;
            default:
                break;
        }
        // always remove "substitution type" category because it is indicated by colors
        input.generalData.flags.add(SubstitutionTable.GeneralData.Flags.HIDE_TYPE);

        // translate "substitution type" into a color
        for (SubstitutionTable.Entry entry: input.getEntries()) {
            if (filterType == FilterType.FILTER_CLASS && !entry.className.equals(filterString))
                entry.visible = false;
            int backgroundRID = R.drawable.row_background_a;
            String type = entry.type;
            if (type.equals("Entfall"))
                backgroundRID = R.drawable.row_background_elimination;
            else if (type.equals("Unterricht geändert") || type.equals("Verlegung") || type.equals("Tausch"))
                backgroundRID = R.drawable.row_background_class_change;
            else if (type.equals("Vertretung") || type.equals("Statt-Vertretung") || type.equals("Betreuung") || type.equals("Sondereins.") || type.equals("Sondereinsatz"))
                backgroundRID = R.drawable.row_background_substitution;
            else if (type.equals("Raum-Vtr."))
                backgroundRID = R.drawable.row_background_room_substitution;

            entry.backgroundRID = backgroundRID;
        }

        return input;
    }

}
