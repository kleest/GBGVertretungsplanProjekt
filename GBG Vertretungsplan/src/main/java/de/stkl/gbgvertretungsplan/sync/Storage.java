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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import de.stkl.gbgvertretungsplan.R;
import de.stkl.gbgvertretungsplan.content.SubstitutionTable;
import de.stkl.gbgvertretungsplan.content.SubstitutionTableStudent;
import de.stkl.gbgvertretungsplan.content.SubstitutionTableTeacher;
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

        SubstitutionTable.Table table = null;
        int dataType = Integer.parseInt(generalData.get(Sync.GENERAL_DATA_DATATYPE) == null ? "0" : generalData.get(Sync.GENERAL_DATA_DATATYPE));

        // student
        if (dataType == 0)
            table = new SubstitutionTableStudent.Table();
        else if (dataType == 1)
            table = new SubstitutionTableTeacher.Table();
        else
            throw new IOException();

        // parse general data
        table.generalData.date = generalData.get(Sync.GENERAL_DATA_DATE);
        table.generalData.updateTime = generalData.get(Sync.GENERAL_DATA_UPDATETIME);
        table.generalData.dataType = dataType;

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
                if (title != null && description != null) {
                    if (dataType == 0)
                        table.generalData.dailyInfos.add(new SubstitutionTableStudent.GeneralData.DailyInfo(title, description));
                    else if (dataType == 1)
                        table.generalData.dailyInfos.add(new SubstitutionTableTeacher.GeneralData.DailyInfo(title, description));
                }
            }
        }


        if (dataType == 0) {
            for (List<String> row : allRows) {
                SubstitutionTable.Entry newEntry = new SubstitutionTableStudent.Entry(table);

                int i = 0;
                for (String categ : categories) {
                    String item = row.get(i);
                    switch (SubstitutionTableStudent.Keys.lookupKey(categ)) {
                        case CLASSNAME:
                            ((SubstitutionTableStudent.Entry) newEntry).className = item;
                            break;
                        case LESSON:
                            ((SubstitutionTableStudent.Entry) newEntry).lesson = item;
                            break;
                        case NEW_ROOM:
                            ((SubstitutionTableStudent.Entry) newEntry).newRoom = item;
                            break;
                        case NEW_SUBJECT:
                            ((SubstitutionTableStudent.Entry) newEntry).newSubject = item;
                            break;
                        case OLD_ROOM:
                            ((SubstitutionTableStudent.Entry) newEntry).oldRoom = item;
                            break;
                        case OLD_SUBJECT:
                            ((SubstitutionTableStudent.Entry) newEntry).oldSubject = item;
                            break;
                        case TYPE:
                            ((SubstitutionTableStudent.Entry) newEntry).type = item;
                            break;
                        case SUBSTITUTIONINFO:
                            ((SubstitutionTableStudent.Entry) newEntry).substitutionInfo = item.replaceAll("\u00a0", "");
                            break;
                        default:
                            break;
                    }
                    i++;
                }
                table.addEntry(newEntry);
            }
        } else if (dataType == 1) {
            for (List<String> row : allRows) {
                SubstitutionTable.Entry newEntry = new SubstitutionTableTeacher.Entry(table);

                int i = 0;
                for (String categ : categories) {
                    String item = row.get(i);
                    switch (SubstitutionTableTeacher.Keys.lookupKey(categ)) {
                        case TEACHER:
                            ((SubstitutionTableTeacher.Entry) newEntry).teacher = item;
                            break;
                        case TYPE:
                            ((SubstitutionTableTeacher.Entry) newEntry).type = item;
                            break;
                        case LESSON:
                            ((SubstitutionTableTeacher.Entry) newEntry).lesson = item;
                            break;
                        case OLD_TEACHER:
                            ((SubstitutionTableTeacher.Entry) newEntry).oldTeacher = item;
                            break;
                        case CLASSNAME:
                            ((SubstitutionTableTeacher.Entry) newEntry).className = item;
                            break;
                        case OLD_ROOM:
                            ((SubstitutionTableTeacher.Entry) newEntry).oldRoom = item;
                            break;
                        case NEW_SUBJECT:
                            ((SubstitutionTableTeacher.Entry) newEntry).newSubject = item;
                            break;
                        case NEW_ROOM:
                            ((SubstitutionTableTeacher.Entry) newEntry).newRoom = item;
                            break;
                        case OLD_SUBJECT:
                            ((SubstitutionTableTeacher.Entry) newEntry).oldSubject = item;
                            break;
                        case SUBSTITUTIONINFO:
                            ((SubstitutionTableTeacher.Entry) newEntry).substitutionInfo = item.replaceAll("\u00a0", "");
                            break;
                        default:
                            break;
                    }
                    i++;
                }
                table.addEntry(newEntry);
            }
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
        FILTER_CLASS_TEACHER
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
            case FILTER_CLASS_TEACHER:
                input.generalData.flags.add(SubstitutionTable.GeneralData.Flags.HIDE_CLASSNAME_TEACHER);
                break;
            default:
                break;
        }
        // always remove "substitution type" category because it is indicated by colors
        input.generalData.flags.add(SubstitutionTableStudent.GeneralData.Flags.HIDE_TYPE);

        // translate "substitution type" into a color
        for (SubstitutionTable.Entry entry: input.getEntries()) {
            if (filterType == FilterType.FILTER_CLASS_TEACHER) {
                if (entry instanceof SubstitutionTableStudent.Entry && !((SubstitutionTableStudent.Entry) entry).className.equals(filterString))
                    entry.visible = false;
                else if (entry instanceof SubstitutionTableTeacher.Entry && !((SubstitutionTableTeacher.Entry) entry).teacher.equals(filterString))
                    entry.visible = false;
            }
            int backgroundRID = R.drawable.row_background_a;
            String type = "";

            if (entry instanceof SubstitutionTableStudent.Entry)
                type = ((SubstitutionTableStudent.Entry) entry).type;
            else
                type = ((SubstitutionTableTeacher.Entry) entry).type;

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
