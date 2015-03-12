/*
 * Copyright (C) 2015 The Android Open Source Project
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
import com.android.loganalysis.item.DumpsysProcStatsItem;
import com.android.loganalysis.item.ProcessUsageItem;
import com.android.loganalysis.item.ProcessUsageItem.ProcessUsageInfoItem;

import org.json.JSONException;
import org.json.JSONObject;


/**
 * Rules definition for Process usage
 */
public class ProcessUsageRule extends AbstractPowerRule {

    private static final String PROCESS_USAGE_ANALYSIS = "PROCESS_USAGE_ANALYSIS";
    private static final long ALARM_THRESHOLD = 60000;

    private StringBuffer mAnalysisBuffer;

    public ProcessUsageRule (BugreportItem bugreportItem) {
        super(bugreportItem);
    }


    @Override
    public void applyRule() {
        mAnalysisBuffer = new StringBuffer();
        ProcessUsageItem processUsageItem = getDetailedAnalysisItem().getProcessUsageItem();
        DumpsysProcStatsItem procStatsItem = getProcStatsItem();
        if (processUsageItem != null && procStatsItem!= null) {
            for (ProcessUsageInfoItem usage : processUsageItem.getProcessUsage()) {
                if (usage.getAlarmWakeups() > 0) {
                    final long alarmsPerMs = getTimeOnBattery()/usage.getAlarmWakeups();
                    if (alarmsPerMs < ALARM_THRESHOLD) {
                        final String processName = procStatsItem.get(usage.getProcessUID());
                        if (processName != null) {
                            mAnalysisBuffer.append(processName);
                        } else {
                            mAnalysisBuffer.append(usage.getProcessUID());
                        }
                        mAnalysisBuffer.append(" has requested frequent repeating alarms");
                    }
                }
            }
        }
    }

    @Override
    public JSONObject getAnalysis() {
        JSONObject usageAnalysis = new JSONObject();
        try {
            usageAnalysis.put(PROCESS_USAGE_ANALYSIS, mAnalysisBuffer.toString());
        } catch (JSONException e) {
          // do nothing
        }
        return usageAnalysis;
    }
}
