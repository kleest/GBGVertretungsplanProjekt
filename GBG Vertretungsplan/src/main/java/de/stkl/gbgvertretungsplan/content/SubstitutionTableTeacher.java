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
import java.util.List;

/**
 * Created by Steffen Klee on 26.04.14.
 */
public class SubstitutionTableTeacher extends SubstitutionTable {

    public static enum Keys implements SubstitutionTable.Keys {
        TEACHER,
        TYPE,
        LESSON,
        OLD_TEACHER,
        CLASSNAME,
        OLD_ROOM,
        NEW_SUBJECT,
        NEW_ROOM,
        OLD_SUBJECT,
        SUBSTITUTIONINFO,
        INVALID;

        public static Keys lookupKey(String value) {
            for (Keys keyVal: Keys.values()) {
                if (value.equals(Header.get(keyVal)))
                    return keyVal;
            }
            return Keys.INVALID;
        }
    }

    public static class Header extends SubstitutionTable.Header {
        public static final String TEACHER = "Lehrer";
        public static final String TYPE = "Art";
        public static final String LESSON = "Stunde";
        public static final String OLD_TEACHER = "abwes. Lehrkraft";
        public static final String CLASSNAME = "Klasse";
        public static final String OLD_ROOM = "(Raum)";
        public static final String NEW_SUBJECT = "Fach";
        public static final String NEW_ROOM = "Raum";
        public static final String OLD_SUBJECT = "(Fach)";
        public static final String SUBSTITUTIONINFO = "Vertretungs-Text";

        private static final int size = 8;

        public static String get(SubstitutionTable.Keys key) {
            switch ((Keys)key) {
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
                case TEACHER:
                    return TEACHER;
                case TYPE:
                    return TYPE;
                case SUBSTITUTIONINFO:
                    return SUBSTITUTIONINFO;
                case OLD_TEACHER:
                    return OLD_TEACHER;
                default:
                    return null;
            }
        }

        public static String get(int i) {
            return get(Keys.values()[i]);
        }
    }

    public static class Entry extends SubstitutionTable.Entry {
        public String teacher;
        public String type;
        public String lesson;
        public String oldTeacher;
        public String className;
        public String oldSubject;
        public String oldRoom;
        public String newSubject;
        public String newRoom;
        public String substitutionInfo;

        public Entry(SubstitutionTable.Table table) {
            super(table);
        }

        // respects flags
        @Override
        public String get(int i) {
            if (parent != null) {
                Keys keys[] = Keys.values();
                List<Keys> keysList = new ArrayList<Keys>();
                Collections.addAll(keysList, keys);

                for (GeneralData.Flags f : this.parent.generalData.flags) {
                    if (f.equals(GeneralData.Flags.HIDE_CLASSNAME_TEACHER))
                        keysList.remove(Keys.TEACHER);
                    else if (f.equals(GeneralData.Flags.HIDE_SUBSTITUTIONINFO))
                        keysList.remove(Keys.SUBSTITUTIONINFO);
                    else if (f.equals(GeneralData.Flags.HIDE_TYPE))
                        keysList.remove(Keys.TYPE);
                }
                return get(keysList.get(i));

            }

            switch (i) {
                case 0: return teacher;
                case 1: return type;
                case 2: return lesson;
                case 3: return oldTeacher;
                case 4: return className;
                case 5: return oldRoom;
                case 7: return newSubject;
                case 8: return newRoom;
                case 9: return oldSubject;
                case 10: return substitutionInfo;
                default: return null;
            }
        }

        @Override
        public String get(SubstitutionTable.Keys key) {
            if (key instanceof Keys) {
                switch ((Keys)key) {
                    case CLASSNAME:
                        return className;
                    case LESSON:
                        return lesson;
                    case OLD_SUBJECT:
                        return oldSubject;
                    case OLD_ROOM:
                        return oldRoom;
                    case NEW_SUBJECT:
                        return newSubject;
                    case NEW_ROOM:
                        return newRoom;
                    case TEACHER:
                        return teacher;
                    case TYPE:
                        return type;
                    case SUBSTITUTIONINFO:
                        return substitutionInfo;
                    case OLD_TEACHER:
                        return oldTeacher;
                    default:
                        return null;
                }
            } else
                return null;
        }
    }

    public static class Table extends SubstitutionTable.Table {

        @Override
        public String getHeader(int index) {
            Keys keys[] = Keys.values();
            List<Keys> keysList = new ArrayList<Keys>();
            Collections.addAll(keysList, keys);

            for (GeneralData.Flags f : generalData.flags) {
                if (f.equals(GeneralData.Flags.HIDE_CLASSNAME_TEACHER))
                    keysList.remove(Keys.TEACHER);
                else if (f.equals(GeneralData.Flags.HIDE_SUBSTITUTIONINFO))
                    keysList.remove(Keys.SUBSTITUTIONINFO);
                else if (f.equals(GeneralData.Flags.HIDE_TYPE))
                    keysList.remove(Keys.TYPE);
            }
            return Header.get(keysList.get(index));
        }

        // does not respect flags
        @Override
        public List<String> getClassNames() {
            List<String> classNames = new ArrayList<String>();
            for (SubstitutionTable.Entry e : entries) {
                if (e instanceof Entry)
                    if (!classNames.contains(((Entry)e).teacher))
                        classNames.add(((Entry)e).teacher);
            }
            return classNames;
        }
    }
}
