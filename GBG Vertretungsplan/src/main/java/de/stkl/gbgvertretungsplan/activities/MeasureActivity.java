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

package de.stkl.gbgvertretungsplan.activities;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;

import de.stkl.gbgvertretungsplan.R;
import de.stkl.gbgvertretungsplan.views.LayoutMeasureView;

public class MeasureActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_measure);

        // screenW is ALWAYS the screen width in portrait mode!
        if (getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            LayoutMeasureView.screenW = getWindowManager().getDefaultDisplay().getWidth();
            LayoutMeasureView.screenH = getWindowManager().getDefaultDisplay().getHeight();
        } else {
            LayoutMeasureView.screenW = getWindowManager().getDefaultDisplay().getHeight();
            LayoutMeasureView.screenH = getWindowManager().getDefaultDisplay().getWidth();
        }



        (new Handler()).post(new Runnable() {
            @Override
            public void run() {
                Intent myIntent = new Intent(MeasureActivity.this, MainActivity.class);
                startActivity(myIntent);
                finish();
            }
        });
    }
}
