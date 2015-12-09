/*
 * Copyright (C) 2016 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.loganalysis.rule;

import com.android.loganalysis.item.BugreportItem;
import com.android.loganalysis.item.LocationDumpsItem;
import com.android.loganalysis.item.LocationDumpsItem.LocationInfoItem;

import java.util.concurrent.TimeUnit;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Rules definition for Process usage
 */
public class LocationUsageRule extends AbstractPowerRule {

    private static final String LOCATION_USAGE_ANALYSIS = "LOCATION_USAGE_ANALYSIS";
    private static final float LOCATION_REQUEST_DURATION_THRESHOLD = 0.1f; // 10%
    // GSA requests for location every 285 seconds, anything more frequent is an issue
    private static final int LOCATION_INTERVAL_THRESHOLD = 285;

    private StringBuffer mAnalysisBuffer;

    private BugreportItem mBugreportItem = null;

    public LocationUsageRule (BugreportItem bugreportItem) {
        super(bugreportItem);
        mBugreportItem = bugreportItem;
    }


    @Override
    public void applyRule() {
        mAnalysisBuffer = new StringBuffer();
        LocationDumpsItem locationDumpsItem = mBugreportItem.getActivityService()
                .getLocationDumps();
        final long locationRequestThresholdMs = (long) (getTimeOnBattery() *
                LOCATION_REQUEST_DURATION_THRESHOLD);
        if (locationDumpsItem != null) {
            for (LocationInfoItem locationClient : locationDumpsItem.getLocationClients()) {
                final String packageName = locationClient.getPackage();
                final String priority = locationClient.getPriority();
                final int effectiveIntervalSec = locationClient.getEffectiveInterval();
                if (effectiveIntervalSec < LOCATION_INTERVAL_THRESHOLD &&
                        !priority.equals("PRIORITY_NO_POWER") &&
                        (TimeUnit.MINUTES.toMillis(locationClient.getDuration()) >
                        locationRequestThresholdMs)) {
                    mAnalysisBuffer.append(String.format("Package %s is requesting for location "
                            + "updates every %d secs with priority %s", packageName,
                            effectiveIntervalSec, priority));
                }
            }
        }
    }

    @Override
    public JSONObject getAnalysis() {
        JSONObject usageAnalysis = new JSONObject();
        try {
            usageAnalysis.put(LOCATION_USAGE_ANALYSIS, mAnalysisBuffer.toString());
        } catch (JSONException e) {
          // do nothing
        }
        return usageAnalysis;
    }
}
