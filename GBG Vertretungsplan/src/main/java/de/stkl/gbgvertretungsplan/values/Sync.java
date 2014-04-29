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

package de.stkl.gbgvertretungsplan.values;

/**
 * Created by Steffen Klee on 15.02.14.
 */
public final class Sync {
    public static enum SYNC_STATUS {
        NONE,
        START,
        OK,
        ERROR
    }
    public static final String GENERAL_DATA_UPDATETIME = "updateTime";
    public static final String GENERAL_DATA_DATE = "date";
    public static final int GENERAL_DATA_DAILYINFO_MAX = 3;
    public static final String GENERAL_DATA_DAILYINFO_1_TITLE = "dailyInfo1Title";
    public static final String GENERAL_DATA_DAILYINFO_1_DESCRIPTION = "dailyInfo1Description";
    public static final String GENERAL_DATA_DAILYINFO_2_TITLE = "dailyInfo2Title";
    public static final String GENERAL_DATA_DAILYINFO_2_DESCRIPTION = "dailyInfo2Description";
    public static final String GENERAL_DATA_DAILYINFO_3_TITLE = "dailyInfo3Title";
    public static final String GENERAL_DATA_DAILYINFO_3_DESCRIPTION = "dailyInfo3Description";
    public static final String GENERAL_DATA_DATATYPE = "dataType";  // defines the data type (student: 0 OR teacher: 1)

    public static final String ACTION_SYNC_FINISHED = "gbgvertretungsplan.intent.action.SYNC_FINISHED";
}
