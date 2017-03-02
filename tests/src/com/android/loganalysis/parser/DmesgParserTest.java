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

import com.android.loganalysis.item.DmesgActionInfoItem;
import com.android.loganalysis.item.DmesgServiceInfoItem;
import com.android.loganalysis.item.DmesgStageInfoItem;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import junit.framework.TestCase;

/**
 * Unit tests for {@link DmesgParser}.
 */
public class DmesgParserTest extends TestCase {

    private static final String BOOT_ANIMATION = "bootanim";
    private static final String NETD = "netd";

    /**
     * Test for empty dmesg logs passed to the DmesgParser
     */
    public void testEmptyDmesgLog() throws IOException {
        String[] lines = new String[] {""};
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(String.join("\n", lines).getBytes())))) {
            DmesgParser dmesgParser = new DmesgParser();
            dmesgParser.parseInfo(bufferedReader);
            assertEquals("Service info items list should be empty", 0,
                    dmesgParser.getServiceInfoItems().size());
        }
    }

    /**
     * Test for complete dmesg logs
     */
    public void testCompleteDmesgLog() throws IOException {
        String[] lines = new String[] {
                "[   22.962730] init: starting service 'bootanim'...",
                "[   23.252321] init: starting service 'netd'...",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   39.855818] init: Service 'bootanim' (pid 588) exited with status 0",
                "[   41.665818] init: init first stage started!",
                "[   42.425056] init: init second stage started!",
                "[   44.942872] init: processing action (early-init)",
                "[   47.233446] init: processing action (set_mmap_rnd_bits)",
                "[   47.240083] init: processing action (set_kptr_restrict)",
                "[   47.245778] init: processing action (keychord_init)",
                "[   52.361049] init: processing action (persist.sys.usb.config=* boot)",
                "[   52.361108] init: processing action (enable_property_trigger)",
                "[   52.361313] init: processing action (security.perf_harden=1)",
                "[   52.361495] init: processing action (ro.debuggable=1)",
                "[   52.962730] init: starting service 'bootanim'...",
                "[   59.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   62.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   65.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   69.855818] init: Service 'bootanim' (pid 588) exited with status 0"};
        try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(String.join("\n", lines).getBytes())))) {
            DmesgParser dmesgParser = new DmesgParser();
            dmesgParser.parseInfo(bufferedReader);
            assertEquals("Service info items list size should be 2", 2,
                    dmesgParser.getServiceInfoItems().size());
            assertEquals("Stage info items list size should be 2", 2,
                    dmesgParser.getStageInfoItems().size());
            assertEquals("Action info items list size should be 5", 5,
                    dmesgParser.getActionInfoItems().size());
        }
    }

    /**
     * Test service which logs both the start and end time
     */
    public void testCompleteServiceInfo() {
        String[] lines = new String[] {
                "[   22.962730] init: starting service 'bootanim'...",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   39.855818] init: Service 'bootanim' (pid 588) exited with status 0"};
        DmesgParser dmesgParser = new DmesgParser();
        for (String line : lines) {
            dmesgParser.parseServiceInfo(line);
        }
        List<DmesgServiceInfoItem> serviceInfoItems = new ArrayList<>(
                dmesgParser.getServiceInfoItems().values());
        assertEquals("There should be atleast one service info", 1, serviceInfoItems.size());
        assertEquals("Service name is not boot anim", BOOT_ANIMATION,
                serviceInfoItems.get(0).getServiceName());
        assertEquals(
                "Service start time is not correct",
                Long.valueOf(22962),
                serviceInfoItems.get(0).getStartTime());
        assertEquals(
                "Service end time is not correct",
                Long.valueOf(39855),
                serviceInfoItems.get(0).getEndTime());
        assertEquals(
                "Service duration is nott correct",
                Long.valueOf(16893),
                serviceInfoItems.get(0).getServiceDuration());
    }

    /**
     * Test service which logs only the start time
     */
    public void testStartServiceInfo() {
        String[] lines = new String[] {
                "[   23.252321] init: starting service 'netd'...",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts"};
        DmesgParser dmesgParser = new DmesgParser();
        for (String line : lines) {
            dmesgParser.parseServiceInfo(line);
        }
        List<DmesgServiceInfoItem> serviceInfoItems = new ArrayList<>(
                dmesgParser.getServiceInfoItems().values());
        assertEquals("There should be exactly one service info", 1, serviceInfoItems.size());
        assertEquals("Service name is not netd", NETD, serviceInfoItems.get(0).getServiceName());
    }

    /**
     * Test multiple service info parsed correctly and stored in the same order logged in
     * the file.
     */
    public void testMultipleServiceInfo() {
        String[] lines = new String[] {
                "[   22.962730] init: starting service 'bootanim'...",
                "[   23.252321] init: starting service 'netd'...",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/inputpoll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   39.855818] init: Service 'bootanim' (pid 588) exited with status 0"};
        DmesgParser dmesgParser = new DmesgParser();
        for (String line : lines) {
            dmesgParser.parseServiceInfo(line);
        }
        List<DmesgServiceInfoItem> serviceInfoItems = new ArrayList<>(
                dmesgParser.getServiceInfoItems().values());
        assertEquals("There should be exactly two service info", 2, serviceInfoItems.size());
        assertEquals("First service name is not boot anim", BOOT_ANIMATION,
                serviceInfoItems.get(0).getServiceName());
        assertEquals("Second service name is not netd", NETD,
                serviceInfoItems.get(1).getServiceName());
    }

    /**
     * Test invalid patterns on the start and exit service logs
     */
    public void testInvalidServiceLogs() {
        // Added space at the end of the start and exit of service logs to make it invalid
        String[] lines = new String[] {
                "[   22.962730] init: starting service 'bootanim'...  ",
                "[   23.252321] init: starting service 'netd'...  ",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   39.855818] init: Service 'bootanim' (pid 588) exited with status 0  "};
        DmesgParser dmesgParser = new DmesgParser();
        for (String line : lines) {
            dmesgParser.parseServiceInfo(line);
        }
        List<DmesgServiceInfoItem> serviceInfoItems = new ArrayList<>(
                dmesgParser.getServiceInfoItems().values());
        assertEquals("No service info should be available", 0, serviceInfoItems.size());
    }

    /**
     * Test init stages' start time logs
     */
    public void testCompleteStageInfo() {
        String[] lines = new String[] {
                "[   22.962730] init: starting service 'bootanim'...",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   39.855818] init: Service 'bootanim' (pid 588) exited with status 0",
                "[   41.665818] init: init first stage started!",
                "[   42.425056] init: init second stage started!"};
        DmesgStageInfoItem firstStageInfoItem = new DmesgStageInfoItem("first",
                (long) (Double.parseDouble("41665.818")));
        DmesgStageInfoItem secondStageInfoItem = new DmesgStageInfoItem("second",
                (long) (Double.parseDouble("42425.056")));
        DmesgParser dmesgParser = new DmesgParser();
        for (String line : lines) {
            dmesgParser.parseStageInfo(line);
        }
        List<DmesgStageInfoItem> stageInfoItems = dmesgParser.getStageInfoItems();
        assertEquals(2, stageInfoItems.size());
        assertEquals(Arrays.asList(firstStageInfoItem, secondStageInfoItem), stageInfoItems);
    }

    /**
     * Test processing action start time logs
     */
    public void testCompleteActionInfo() {
        String[] lines = new String[] {
                "[   14.942872] init: processing action (early-init)",
                "[   17.233446] init: processing action (set_mmap_rnd_bits)",
                "[   17.240083] init: processing action (set_kptr_restrict)",
                "[   17.245778] init: processing action (keychord_init)",
                "[   22.361049] init: processing action (persist.sys.usb.config=* boot)",
                "[   22.361108] init: processing action (enable_property_trigger)",
                "[   22.361313] init: processing action (security.perf_harden=1)",
                "[   22.361495] init: processing action (ro.debuggable=1)",
                "[   22.962730] init: starting service 'bootanim'...",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   39.855818] init: Service 'bootanim' (pid 588) exited with status 0"};
        List<DmesgActionInfoItem> expectedActionInfoItems = Arrays.asList(
                new DmesgActionInfoItem("early-init", (long) (Double.parseDouble("14942.872"))),
                new DmesgActionInfoItem("set_mmap_rnd_bits",
                        (long) (Double.parseDouble("17233.446"))),
                new DmesgActionInfoItem("set_kptr_restrict",
                        (long) (Double.parseDouble("17240.083"))),
                new DmesgActionInfoItem("keychord_init", (long) (Double.parseDouble("17245.778"))),
                new DmesgActionInfoItem("enable_property_trigger",
                        (long) (Double.parseDouble("22361.108"))));
        DmesgParser dmesgParser = new DmesgParser();
        for (String line : lines) {
            dmesgParser.parseActionInfo(line);
        }
        List<DmesgActionInfoItem> actualActionInfoItems = dmesgParser.getActionInfoItems();
        assertEquals(5, actualActionInfoItems.size());
        assertEquals(expectedActionInfoItems, actualActionInfoItems);
    }

}
