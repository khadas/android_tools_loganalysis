/*
 * Copyright (C) 2019 The Android Open Source Project
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
 * limitations under the License
 */

package com.android.loganalysis.parser;

import com.android.loganalysis.item.IItem;
import com.android.loganalysis.item.SystemServicesTimingItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An {@link IParser} to parse boot metrics from logcat. It currently assumes "threadtime" format of
 * logcat. It will parse duration metrics for some system services like System Server, Zygote,
 * System UI, e.t.c.
 *
 * <p>TODO(b/133166326): Add support for parsing thread time info from log lines, and also support
 * dynamically adding new log line patterns.
 */
public class TimingsLogParser implements IParser {

    private static final String SYSTEM_SERVICES_TIME_PREFIX =
            "^\\d*-\\d*\\s*\\d*:\\d*:\\d*.\\d*\\s*"
                    + "\\d*\\s*\\d*\\s*D\\s*(?<componentname>.*):\\s*(?<subname>\\S*)\\s*";
    private static final String SYSTEM_SERVICES_TIME_SUFFIX = ":\\s*(?<time>.*)ms\\s*$";

    /**
     * Match the line with system services duration info like:
     *
     * <p>03-10 21:43:40.328 1005 1005 D SystemServerTiming:
     * StartKeyAttestationApplicationIdProviderService took to complete: 3474ms
     */
    private static final Pattern SYSTEM_SERVICES_DURATION =
            Pattern.compile(
                    String.format(
                            "%stook to complete%s",
                            SYSTEM_SERVICES_TIME_PREFIX, SYSTEM_SERVICES_TIME_SUFFIX));
    /**
     * Match the line with system services start time info like:
     *
     * <p>01-10 01:25:57.249 989 989 D BootAnimation: BootAnimationStartTiming start time: 8343ms
     */
    private static final Pattern SYSTEM_SERVICES_START_TIME =
            Pattern.compile(
                    String.format(
                            "%sstart time%s",
                            SYSTEM_SERVICES_TIME_PREFIX, SYSTEM_SERVICES_TIME_SUFFIX));

    @Override
    public IItem parse(List<String> lines) {
        throw new UnsupportedOperationException(
                "Method has not been implemented in lieu of others");
    }

    /**
     * A method that parses the logcat input for system services timing information. It will ignore
     * duplicated log lines and will keep multiple values for the same timing metric generated at
     * different time in the log
     *
     * @param input Logcat input as a {@link BufferedReader}
     * @return a list of {@link SystemServicesTimingItem}
     * @throws IOException
     */
    public List<SystemServicesTimingItem> parseSystemServicesTimingItems(BufferedReader input)
            throws IOException {
        Set<String> matchedLines = new HashSet<>();
        List<SystemServicesTimingItem> items = new ArrayList<>();
        String line;
        while ((line = input.readLine()) != null) {
            if (matchedLines.contains(line)) {
                continue;
            }
            SystemServicesTimingItem item = parseSystemServicesTimingItem(line);
            if (item == null) {
                continue;
            }
            items.add(item);
            matchedLines.add(line);
        }
        return items;
    }

    /**
     * Parse a particular log line to see if it matches the system service timing pattern and return
     * a {@link SystemServicesTimingItem} if matches, otherwise return null.
     *
     * @param line a single log line
     * @return a {@link SystemServicesTimingItem}
     */
    private SystemServicesTimingItem parseSystemServicesTimingItem(String line) {
        Matcher matcher = SYSTEM_SERVICES_DURATION.matcher(line);
        boolean durationMatched = matcher.matches();
        if (!durationMatched) {
            matcher = SYSTEM_SERVICES_START_TIME.matcher(line);
        }
        if (!matcher.matches()) {
            return null;
        }
        SystemServicesTimingItem item = new SystemServicesTimingItem();
        item.setComponent(matcher.group("componentname").trim());
        item.setSubcomponent(matcher.group("subname").trim());
        if (durationMatched) {
            item.setDuration(Double.parseDouble(matcher.group("time")));
        } else {
            item.setStartTime(Double.parseDouble(matcher.group("time")));
        }
        return item;
    }
}
