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
import java.util.EnumSet;
import java.util.List;

/**
 * Created by Steffen Klee on 25.04.2014.
 */
public abstract class SubstitutionTable {
    public interface Keys {
    }

    public static int getHeaderSize() {
        return 8;
    }

    public static class GeneralData {
        public String date = "";    // day + date
        public String updateTime = "";  // last time table got updated on server-side
        public int dataType = 0;    // data type (0: student OR 1: teacher)
        public final EnumSet<Flags> flags;
        public final List<DailyInfo> dailyInfos;  // daily information

        public static enum Flags {
            HIDE_CLASSNAME_TEACHER,
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

    public static abstract class Header {
        public static String get(Keys key) {
            throw new IllegalStateException("");
        }
        public static String get(int i) {
            throw new IllegalStateException("");
        }

//        public String get(int i) {
//            return get(Keys.eValues()[i]);
//        }
    }

    public static abstract class Entry {
        protected Table parent = null;

        public int backgroundRID = 0;
        public boolean visible = true;

        public Entry() {
        }

        public Entry(Table parent) {
            this.parent = parent;
        }

        // respects flags
        public abstract String get(int i);
        public abstract String get(Keys key);
    }

    public static abstract class Table {
        public final GeneralData generalData;
        protected final List<Entry> entries;

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
            int c = generalData.dataType == 1 ? 10 : 8;
            if (generalData.flags.contains(GeneralData.Flags.HIDE_CLASSNAME_TEACHER))
                c--;
            if (generalData.flags.contains(GeneralData.Flags.HIDE_SUBSTITUTIONINFO))
                c--;
            if (generalData.flags.contains(GeneralData.Flags.HIDE_TYPE))
                c--;
            return c;
        }

        public abstract String getHeader(int index);

        // does not respect flags
        public abstract List<String> getClassNames();
    }
}
