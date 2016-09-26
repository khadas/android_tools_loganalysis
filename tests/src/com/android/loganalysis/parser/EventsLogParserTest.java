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

import com.android.loganalysis.item.LatencyItem;
import com.android.loganalysis.item.TransitionDelayItem;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

/**
 * Unit tests for {@link EventsLogParser}.
 */
public class EventsLogParserTest extends TestCase {

    private File mTempFile = null;

    /**
     * Test for empty events logs passed to the transition delay parser
     */
    public void testEmptyEventsLog() throws IOException {
        List<String> lines = Arrays.asList("");
        List<TransitionDelayItem> transitionItems = (new EventsLogParser()).
                parseTransitionDelayInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Transition Delay items list should be empty", 0,transitionItems.size());
    }

    /**
     * Test for no transition delay info in the events log
     */
    public void testNoTransitionDelayInfo() throws IOException {
        List<String> lines = Arrays
                .asList(
                        "08-25 12:56:15.850  1152  8968 I am_focused_stack: [0,0,1,appDied setFocusedActivity]",
                        "08-25 12:56:15.850  1152  8968 I wm_task_moved: [6,1,1]",
                        "08-25 12:56:15.852  1152  8968 I am_focused_activity: [0,com.google.android.apps.nexuslauncher/.NexusLauncherActivity,appDied]",
                        "08-25 12:56:15.852  1152  8968 I wm_task_removed: [27,removeTask]",
                        "08-25 12:56:15.852  1152  8968 I wm_stack_removed: 1");
        List<TransitionDelayItem> transitionItems = (new EventsLogParser()).
                parseTransitionDelayInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Transition Delay items list should be empty", 0,
                transitionItems.size());
    }

    /**
     * Test for Cold launch transition delay info
     */
    public void testColdLaunchTransitionDelay() throws IOException {
        List<String> lines = Arrays
                .asList("08-25 13:01:19.412  1152  9031 I am_restart_activity: [0,85290699,38,com.google.android.gm/.ConversationListActivityGmail]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [321,85]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [320,1]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [319,85]");
        List<TransitionDelayItem> transitionItems = (new EventsLogParser()).
                parseTransitionDelayInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Transition Delay items list should have one item", 1,
                transitionItems.size());
        assertEquals("Component name not parsed correctly",
                "com.google.android.gm/.ConversationListActivityGmail",
                transitionItems.get(0).getComponentName());
        assertEquals("Cold launch info is not set correctly", 85,
                transitionItems.get(0).getStartingWindowDelay());
        assertEquals("Hot launch info is set which is not expected", -1,
                transitionItems.get(0).getTransitionDelay());
    }

    /**
     * Test for Hot launch transition delay
     */
    public void testHotLaunchTransitionDelay() throws IOException {
        List<String> lines = Arrays
                .asList("08-25 13:02:04.740  1152  2715 I am_resume_activity: [0,85290699,38,com.google.android.gm/.ConversationListActivityGmail]",
                        "08-25 13:02:04.754  1152  1180 I sysui_count: [window_time_0,23]",
                        "08-25 13:02:04.755  1152  1226 I sysui_action: [320,0]",
                        "08-25 13:02:04.755  1152  1226 I sysui_action: [319,37]");
        List<TransitionDelayItem> transitionItems = (new EventsLogParser()).
                parseTransitionDelayInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Transition Delay items list shopuld have one item", 1,
                transitionItems.size());
        assertEquals("Component name not parsed correctly",
                "com.google.android.gm/.ConversationListActivityGmail",
                transitionItems.get(0).getComponentName());
        assertEquals("Cold launch info is set which is not expected", -1,
                transitionItems.get(0).getStartingWindowDelay());
        assertEquals("Hot launch info is not set correctly", 37,
                transitionItems.get(0).getTransitionDelay());
    }

    /**
     * Test for same app transition delay items order after parsing from the events log
     */
    public void testTransitionDelayOrder() throws IOException {
        List<String> lines = Arrays
                .asList("08-25 13:01:19.412  1152  9031 I am_restart_activity: [0,85290699,38,com.google.android.gm/.ConversationListActivityGmail]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [321,85]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [320,1]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [319,85]",
                        "08-25 12:56:15.850  1152  8968 I am_focused_stack: [0,0,1,appDied setFocusedActivity]",
                        "08-25 12:56:15.850  1152  8968 I wm_task_moved: [6,1,1]",
                        "08-25 12:56:15.852  1152  8968 I am_focused_activity: [0,com.google.android.apps.nexuslauncher/.NexusLauncherActivity,appDied]",
                        "08-25 12:56:15.852  1152  8968 I wm_task_removed: [27,removeTask]",
                        "08-25 12:56:15.852  1152  8968 I wm_stack_removed: 1",
                        "08-25 13:02:04.740  1152  2715 I am_resume_activity: [0,85290699,38,com.google.android.gm/.ConversationListActivityGmail]",
                        "08-25 13:02:04.754  1152  1180 I sysui_count: [window_time_0,23]",
                        "08-25 13:02:04.755  1152  1226 I sysui_action: [320,0]",
                        "08-25 13:02:04.755  1152  1226 I sysui_action: [319,37]");
        List<TransitionDelayItem> transitionItems = (new EventsLogParser()).
                parseTransitionDelayInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Transition Delay items list should have two items", 2,
                transitionItems.size());
        assertEquals("Cold launch transition delay is not the first item", 85,
                transitionItems.get(0).getStartingWindowDelay());
        assertEquals("Hot launch transition delay is not the second item", 37,
                transitionItems.get(1).getTransitionDelay());
    }

    /**
     * Test for two different different apps transition delay items
     */
    public void testDifferentAppTransitionDelay() throws IOException {
        List<String> lines = Arrays
                .asList("08-25 13:01:19.412  1152  9031 I am_restart_activity: [0,85290699,38,com.google.android.gm/.ConversationListActivityGmail]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [321,85]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [320,1]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [319,85]",
                        "08-25 12:56:15.850  1152  8968 I am_focused_stack: [0,0,1,appDied setFocusedActivity]",
                        "08-25 12:56:15.850  1152  8968 I wm_task_moved: [6,1,1]",
                        "08-25 12:56:15.852  1152  8968 I am_focused_activity: [0,com.google.android.apps.nexuslauncher/.NexusLauncherActivity,appDied]",
                        "08-25 12:56:15.852  1152  8968 I wm_task_removed: [27,removeTask]",
                        "08-25 12:56:15.852  1152  8968 I wm_stack_removed: 1",
                        "08-25 13:03:35.528  1152  2715 I am_restart_activity: [0,32358360,39,com.google.android.apps.maps/com.google.android.maps.MapsActivity]",
                        "08-25 13:03:35.540  1152  1179 I am_pss  : [7727,10032,com.google.android.apps.nexuslauncher,50991104,45486080,0]",
                        "08-25 13:03:35.566  1152  1179 I am_pss  : [9207,10045,com.google.android.googlequicksearchbox:search,111955968,102227968,0]",
                        "08-25 13:03:35.569  1152  1226 I sysui_action: [321,92]",
                        "08-25 13:03:35.569  1152  1226 I sysui_action: [320,1]",
                        "08-25 13:03:35.569  1152  1226 I sysui_action: [319,92]");
        List<TransitionDelayItem> transitionItems = (new EventsLogParser()).
                parseTransitionDelayInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Transition Delay items list should have two items", 2,
                transitionItems.size());
        assertEquals("Gmail is not the first transition delay item",
                "com.google.android.gm/.ConversationListActivityGmail",
                transitionItems.get(0).getComponentName());
        assertEquals("Maps is not the second transition delay item",
                "com.google.android.apps.maps/com.google.android.maps.MapsActivity",
                transitionItems.get(1).getComponentName());
    }

    /**
     * Test for invalid transition delay items pattern
     */
    public void testInvalidTransitionPattern() throws IOException {
        List<String> lines = Arrays
                .asList("08-25 13:01:19.412  1152  9031 I am_restart_activity: [com.google.android.gm/.ConversationListActivityGmail,0,85290699,38]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [321,85]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [320,1]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [319,85]",
                        "08-25 12:56:15.850  1152  8968 I am_focused_stack: [0,0,1,appDied setFocusedActivity]",
                        "08-25 12:56:15.850  1152  8968 I wm_task_moved: [6,1,1]",
                        "08-25 12:56:15.852  1152  8968 I am_focused_activity: [0,com.google.android.apps.nexuslauncher/.NexusLauncherActivity,appDied]",
                        "08-25 12:56:15.852  1152  8968 I wm_task_removed: [27,removeTask]",
                        "08-25 12:56:15.852  1152  8968 I wm_stack_removed: 1",
                        "08-25 13:02:04.740  1152  2715 I am_res_activity: [0,85290699,38,com.google.android.gm/.ConversationListActivityGmail]",
                        "08-25 13:02:04.754  1152  1180 I sysui_count: [window_time_0,23]",
                        "08-25 13:02:04.755  1152  1226 I sysui_action: [320,0]",
                        "08-25 13:02:04.755  1152  1226 I sysui_action: [319,37]");
        List<TransitionDelayItem> transitionItems = (new EventsLogParser()).
                parseTransitionDelayInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Transition Delay items list should be empty", 0,
                transitionItems.size());
    }

    /**
     * Test for valid latency item
     */
    public void testValidLatencyInfo() throws IOException {
        List<String> lines = Arrays
                .asList("08-25 13:01:19.412  1152  9031 I am_restart_activity: [com.google.android.gm/.ConversationListActivityGmail,0,85290699,38]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [321,85]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [320,1]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [319,85]",
                        "08-25 12:56:15.850  1152  8968 I am_focused_stack: [0,0,1,appDied setFocusedActivity]",
                        "09-19 11:53:16.893  1080  1160 I sysui_latency: [1,50]");
        List<LatencyItem> latencyItems = (new EventsLogParser()).
                parseLatencyInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("One latency item should present in the list", 1, latencyItems.size());
        assertEquals("Action Id is not correct", 1, latencyItems.get(0).getActionId());
        assertEquals("Delay is not correct", 50L, latencyItems.get(0).getDelay());
    }

    /**
     * Test for empty delay info
     */
    public void testInvalidLatencyInfo() throws IOException {
        List<String> lines = Arrays
                .asList("08-25 13:01:19.412  1152  9031 I am_restart_activity: [com.google.android.gm/.ConversationListActivityGmail,0,85290699,38]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [321,85]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [320,1]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [319,85]",
                        "08-25 12:56:15.850  1152  8968 I am_focused_stack: [0,0,1,appDied setFocusedActivity]",
                        "09-19 11:53:16.893  1080  1160 I sysui_latency: [1]");
        List<LatencyItem> latencyItems = (new EventsLogParser()).
                parseLatencyInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Latency items list should be empty", 0, latencyItems.size());
    }

    /**
     * Test for empty latency info
     */
    public void testEmptyLatencyInfo() throws IOException {
        List<String> lines = Arrays
                .asList("08-25 13:01:19.412  1152  9031 I am_restart_activity: [com.google.android.gm/.ConversationListActivityGmail,0,85290699,38]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [321,85]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [320,1]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [319,85]",
                        "08-25 12:56:15.850  1152  8968 I am_focused_stack: [0,0,1,appDied setFocusedActivity]",
                        "09-19 11:53:16.893  1080  1160 I sysui_latency: []");
        List<LatencyItem> latencyItems = (new EventsLogParser()).
                parseLatencyInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Latency items list should be empty", 0, latencyItems.size());
    }


    /**
     * Test for order of the latency items
     */
    public void testLatencyInfoOrder() throws IOException {
        List<String> lines = Arrays
                .asList("09-19 11:53:16.893  1080  1160 I sysui_latency: [1,50]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [321,85]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [320,1]",
                        "08-25 13:01:19.437  1152  1226 I sysui_action: [319,85]",
                        "08-25 12:56:15.850  1152  8968 I am_focused_stack: [0,0,1,appDied setFocusedActivity]",
                        "09-19 11:53:16.893  1080  1160 I sysui_latency: [2,100]");
        List<LatencyItem> latencyItems = (new EventsLogParser()).
                parseLatencyInfo(readInputBuffer(getTempFile(lines)));
        assertEquals("Latency list should have 2 items", 2, latencyItems.size());
        assertEquals("First latency id is not 1", 1, latencyItems.get(0).getActionId());
        assertEquals("Second latency id is not 2", 2, latencyItems.get(1).getActionId());
    }

    /**
     * Write list of strings to file and use it for testing.
     */
    public File getTempFile(List<String> sampleEventsLogs) throws IOException {
        mTempFile = File.createTempFile("events_logcat", ".txt");
        BufferedWriter out = new BufferedWriter(new FileWriter(mTempFile));
        for (String line : sampleEventsLogs) {
            out.write(line);
            out.newLine();
        }
        out.close();
        return mTempFile;
    }

    /**
     * Reader to read the input from the given temp file
     */
    public BufferedReader readInputBuffer(File tempFile) throws IOException {
        return (new BufferedReader(new InputStreamReader(new FileInputStream(tempFile))));
    }

}