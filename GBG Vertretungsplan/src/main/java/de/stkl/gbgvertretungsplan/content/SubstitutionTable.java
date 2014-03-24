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

package de.stkl.gbgvertretungsplan.content;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

/**
 * Created by Steffen Klee on 18.02.14.
 */
public class SubstitutionTable {
    public static enum Keys {
        CLASSNAME,
        LESSON,
        OLD_SUBJECT,
        OLD_ROOM,
        NEW_SUBJECT,
        NEW_ROOM,
        TYPE,
        SUBSTITUTIONINFO,
        INVALID
    }

    public static Keys lookupKey(String value) {
        for (Keys keyVal: Keys.values()) {
            if (value.equals(Header.get(keyVal)))
                return keyVal;
        }
        return Keys.INVALID;
    }

    public static class GeneralData {
        public String date = "";    // day + date
        public String updateTime = "";  // last time table got updated on server-side
        public final EnumSet<Flags> flags;
        public final List<DailyInfo> dailyInfos;  // daily information

        public static enum Flags {
            HIDE_CLASSNAME,
            HIDE_SUBSTITUTIONINFO,
            HIDE_TYPE
        }

        public GeneralData() {
            flags = EnumSet.noneOf(Flags.class);
            dailyInfos = new ArrayList<DailyInfo>();
        }

        public static class DailyInfo {
            public String title;
            public String description;

            public DailyInfo() {
            }

            public DailyInfo(String title, String description) {
                this.title = title;
                this.description = description;
            }
        }
    }

    public static class Header {
        public static final String CLASSNAME = "Klasse(n)";
        public static final String LESSON = "Stunde";
        public static final String OLD_SUBJECT = "(Fach)";
        public static final String OLD_ROOM = "(Raum)";
        public static final String NEW_SUBJECT = "Fach";
        public static final String NEW_ROOM = "Raum";
        public static final String TYPE = "Art";
        public static final String SUBSTITUTIONINFO = "Vertretungs-Text";

        private static final int size = 8;

        public static String get(Keys key) {
            switch (key) {
               case CLASSNAME:
                   return CLASSNAME;
               case LESSON:
                   return LESSON;
               case OLD_SUBJECT:
                   return OLD_SUBJECT;
               case OLD_ROOM:
                   return OLD_ROOM;
               case NEW_SUBJECT:
                   return NEW_SUBJECT;
               case NEW_ROOM:
                   return NEW_ROOM;
               case TYPE:
                   return TYPE;
               case SUBSTITUTIONINFO:
                   return SUBSTITUTIONINFO;
               default:
                   return null;
            }
        }

        public static String get(int i) {
            return get(Keys.values()[i]);
        }
    }

    public static class Entry {
        private Table parent = null;

        public String className;
        public String lesson;
        public String oldSubject;
        public String oldRoom;
        public String newSubject;
        public String newRoom;
        public String type;
        public String substitutionInfo;
        public int backgroundRID = 0;
        public boolean visible = true;

        public Entry() {

        }

        public Entry(Table parent) {
            this.parent = parent;
        }

        // respects flags
        public String get(int i) {
            if (parent != null) {
                Keys keys[] = Keys.values();
                List<Keys> keysList = new ArrayList<Keys>();
                Collections.addAll(keysList, keys);

                for (GeneralData.Flags f : this.parent.generalData.flags) {
                    if (f.equals(GeneralData.Flags.HIDE_CLASSNAME))
                        keysList.remove(Keys.CLASSNAME);
                    else if (f.equals(GeneralData.Flags.HIDE_SUBSTITUTIONINFO))
                        keysList.remove(Keys.SUBSTITUTIONINFO);
                    else if (f.equals(GeneralData.Flags.HIDE_TYPE))
                        keysList.remove(Keys.TYPE);
                }
                return get(keysList.get(i));

            }

            switch (i) {
                case 0: return className;
                case 1: return lesson;
                case 2: return oldSubject;
                case 3: return oldRoom;
                case 4: return newSubject;
                case 5: return newRoom;
                case 7: return type;
                case 6: return substitutionInfo;
                default: return null;
            }
        }

        public String get(Keys key) {
            switch (key) {
                case CLASSNAME: return className;
                case LESSON: return lesson;
                case OLD_SUBJECT: return oldSubject;
                case OLD_ROOM: return oldRoom;
                case NEW_SUBJECT: return newSubject;
                case NEW_ROOM: return newRoom;
                case TYPE: return type;
                case SUBSTITUTIONINFO: return substitutionInfo;
                default: return null;
            }
        }
    }
    public static class Table {
        public final GeneralData generalData;
        private final List<Entry> entries;

        public Table() {
            generalData = new GeneralData();
            entries = new ArrayList<Entry>();
        }

        public void addEntry(Entry newEntry) {
            entries.add(newEntry);
        }

        public List<Entry> getEntries() {
            return entries;
        }

        // respects flags
        public Entry getEntry(int index) {
            int i = 0;
            for (Entry entry: entries) {
                if (entry.visible)
                    if (i == index)
                        return entry;
                    else
                        i++;
            }
            return null;
        }

        // respects flags
        public int getEntryCount() {
            int c = 0;
            for (Entry entry: entries)
                if (entry.visible)
                    c++;
            return c;
        }

        // respects flags
        public int getHeaderCount() {
            int c = Header.size;
            if (generalData.flags.contains(GeneralData.Flags.HIDE_CLASSNAME))
                c--;
            if (generalData.flags.contains(GeneralData.Flags.HIDE_SUBSTITUTIONINFO))
                c--;
            if (generalData.flags.contains(GeneralData.Flags.HIDE_TYPE))
                c--;
            return c;
        }

        public String getHeader(int index) {
            Keys keys[] = Keys.values();
            List<Keys> keysList = new ArrayList<Keys>();
            Collections.addAll(keysList, keys);

            for (GeneralData.Flags f : generalData.flags) {
                if (f.equals(GeneralData.Flags.HIDE_CLASSNAME))
                    keysList.remove(Keys.CLASSNAME);
                else if (f.equals(GeneralData.Flags.HIDE_SUBSTITUTIONINFO))
                    keysList.remove(Keys.SUBSTITUTIONINFO);
                else if (f.equals(GeneralData.Flags.HIDE_TYPE))
                    keysList.remove(Keys.TYPE);
            }
            return Header.get(keysList.get(index));
        }

        // does not respect flags
        public List<String> getClassNames() {
            List<String> classNames = new ArrayList<String>();
            for (Entry e: entries) {
                if (!classNames.contains(e.className))
                    classNames.add(e.className);
            }
            return classNames;
        }
    }
}
