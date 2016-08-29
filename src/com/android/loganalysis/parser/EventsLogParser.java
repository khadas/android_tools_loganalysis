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

package com.android.loganalysis.parser;

import com.android.loganalysis.item.IItem;
import com.android.loganalysis.item.TransitionDelayItem;

import java.io.IOException;
import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parse the events logs. </p>
 */
public class EventsLogParser implements IParser {

    // 08-21 17:53:53.876 1053 2135 I am_restart_activity:
    // [0,188098346,127,com.google.android.gm/.ConversationListActivityGmail]
    private static final Pattern ACTIVITY_RESTART = Pattern.compile("^\\d{2}-\\d{2} \\d{2}:\\d{2}"
            + ":\\d{2}.\\d{3}\\s+\\d+\\s+\\d+ I am_restart_activity: "
            + "\\[\\d+\\,\\d+\\,\\d+\\,(?<componentname>.*)\\]$");
    // 08-21 17:57:15.363 1053 2095 I am_resume_activity:
    // [0,228277756,132,com.google.android.gm/.ConversationListActivityGmail]
    private static final Pattern ACTIVITY_RESUME = Pattern.compile("^\\d{2}-\\d{2} \\d{2}:\\d{2}"
            + ":\\d{2}.\\d{3}\\s+\\d+\\s+\\d+ I am_resume_activity: "
            + "\\[\\d+\\,\\d+\\,\\d+\\,(?<componentname>.*)\\]$");
    // 08-21 17:53:53.893 1053 1115 I sysui_action: [321,74]
    private static final Pattern STARTING_WINDOW_DELAY = Pattern.compile("^\\d{2}-\\d{2} \\d{2}:"
            + "\\d{2}:\\d{2}.\\d{3}\\s+\\d+\\s+\\d+ I sysui_action: \\[321,(?<startdelay>.*)\\]$");
    // 08-21 17:54:16.672 1053 1115 I sysui_action: [319,99]
    private static final Pattern TRANSITION_DELAY = Pattern
            .compile("^\\d{2}-\\d{2} \\d{2}:"
                    + "\\d{2}:\\d{2}.\\d{3}\\s+\\d+\\s+\\d+ I sysui_action: \\[319,"
                    + "(?<transitdelay>.*)\\]$");

    @Override
    public IItem parse(List<String> lines) {
        throw new UnsupportedOperationException("Method has not been implemented in lieu"
                + " of others");
    }

    /**
     * Method to parse the transition delay information from the events log
     *
     * @param input
     * @return
     * @throws IOException
     */
    public List<TransitionDelayItem> parseTransitionDelayInfo(BufferedReader input)
            throws IOException {
        List<TransitionDelayItem> transitionDelayItems = new ArrayList<TransitionDelayItem>();
        String line;
        List<String> componentNameStack = new ArrayList<String>();
        boolean isRecentStartWindowDelay = false;
        while ((line = input.readLine()) != null) {
            Matcher match = null;
            if (((match = matches(ACTIVITY_RESTART, line))) != null ||
                    ((match = matches(ACTIVITY_RESUME, line)) != null)) {
                componentNameStack.add(match.group("componentname"));
                isRecentStartWindowDelay = false;
            } else if (((match = matches(STARTING_WINDOW_DELAY, line)) != null)
                    && (componentNameStack.size() > 0)) {
                TransitionDelayItem delayItem = new TransitionDelayItem();
                delayItem.setComponentName(
                        componentNameStack.remove(componentNameStack.size() - 1));
                delayItem.setStartingWindowDelay(Long.parseLong(match.group("startdelay")));
                delayItem.setTransitionDelay(-1);
                transitionDelayItems.add(delayItem);
                isRecentStartWindowDelay = true;
            } else if (((match = matches(TRANSITION_DELAY, line)) != null)
                    && (componentNameStack.size() > 0) && !isRecentStartWindowDelay) {
                TransitionDelayItem delayItem = new TransitionDelayItem();
                delayItem.setComponentName(
                        componentNameStack.remove(componentNameStack.size() - 1));
                delayItem.setTransitionDelay(Long.parseLong(match.group("transitdelay")));
                delayItem.setStartingWindowDelay(-1);
                transitionDelayItems.add(delayItem);
            }
        }
        return transitionDelayItems;
    }

    /**
     * Checks whether {@code line} matches the given {@link Pattern}.
     *
     * @return The resulting {@link Matcher} obtained by matching the {@code line} against
     *         {@code pattern}, or null if the {@code line} does not match.
     */
    private static Matcher matches(Pattern pattern, String line) {
        Matcher ret = pattern.matcher(line);
        return ret.matches() ? ret : null;
    }

}
