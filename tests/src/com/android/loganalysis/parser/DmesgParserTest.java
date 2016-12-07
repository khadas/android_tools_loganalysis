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

import com.android.loganalysis.item.ServiceInfoItem;

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
 * Unit tests for {@link DmesgParser}.
 */
public class DmesgParserTest extends TestCase {

    private static final String BOOT_ANIMATION = "bootanim";
    private static final String NETD = "netd";
    private static final String DMESG_LOG = "dmesg_logs";

    private File mTempFile = null;

    /**
     * Test for empty dmesg logs passed to the DmesgParser
     */
    public void testEmptyDmesgLog() throws IOException {
        List<String> lines = Arrays.asList("");
        File testFile = getTempFile(lines);
        BufferedReader bufferedReader = new BufferedReader(readInputBuffer(testFile));
        List<ServiceInfoItem> serviceItems = (new DmesgParser()).
                parseServiceInfo(bufferedReader);
        assertEquals("Service info items list should be empty", 0, serviceItems.size());
        bufferedReader.close();
        testFile.delete();
    }

    /**
     * Test service which logs both the start and end time
     */
    public void testCompleteServiceInfo() throws IOException {
        List<String> lines = Arrays.asList(
                "[   22.962730] init: starting service 'bootanim'...",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   39.855818] init: Service 'bootanim' (pid 588) exited with status 0");
        File testFile = getTempFile(lines);
        BufferedReader bufferedReader = new BufferedReader(readInputBuffer(testFile));
        List<ServiceInfoItem> serviceInfoItems = (new DmesgParser()).
                parseServiceInfo(bufferedReader);
        assertEquals("There should be atleast one service info", 1,
                serviceInfoItems.size());
        assertEquals("Service name is not boot anim", BOOT_ANIMATION,
                serviceInfoItems.get(0).getServiceName());
        assertEquals("Service start time is not correct", new Long(22962), serviceInfoItems.get(0)
                .getStartTime());
        assertEquals("Service end time is not correct", new Long(39855), serviceInfoItems.get(0)
                .getEndTime());
        assertEquals("Service duration is nott correct", new Long(16893), serviceInfoItems.get(0)
                .getServiceDuration());
        bufferedReader.close();
        testFile.delete();
    }

    /**
     * Test service which logs only the start time
     */
    public void testStartServiceInfo() throws IOException {
        List<String> lines = Arrays.asList(
                "[   23.252321] init: starting service 'netd'...",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts");
        File testFile = getTempFile(lines);
        BufferedReader bufferedReader = new BufferedReader(readInputBuffer(testFile));
        List<ServiceInfoItem> serviceInfoItems = (new DmesgParser()).
                parseServiceInfo(bufferedReader);
        assertEquals("There should be exactly one service info", 1,
                serviceInfoItems.size());
        assertEquals("Service name is not netd", NETD,
                serviceInfoItems.get(0).getServiceName());
        bufferedReader.close();
        testFile.delete();
    }

    /**
     * Test multiple service info parsed correctly and stored in the same order logged in
     * the file.
     */
    public void testMultipleServiceInfo() throws IOException {
        List<String> lines = Arrays.asList(
                "[   22.962730] init: starting service 'bootanim'...",
                "[   23.252321] init: starting service 'netd'...",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/inputpoll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   39.855818] init: Service 'bootanim' (pid 588) exited with status 0");
        File testFile = getTempFile(lines);
        BufferedReader bufferedReader = new BufferedReader(readInputBuffer(testFile));
        List<ServiceInfoItem> serviceInfoItems = (new DmesgParser()).
                parseServiceInfo(bufferedReader);
        assertEquals("There should be exactly two service info", 2,
                serviceInfoItems.size());
        assertEquals("First service name is not boot anim", BOOT_ANIMATION,
                serviceInfoItems.get(0).getServiceName());
        assertEquals("Second service name is not netd", NETD,
                serviceInfoItems.get(1).getServiceName());
        bufferedReader.close();
        testFile.delete();
    }

    /**
     * Test invalid patterns on the start and exit service logs
     */
    public void testInvalidServiceLogs() throws IOException {
        // Added space at the end of the start and exit of service logs to make it invalid
        List<String> lines = Arrays.asList(
                "[   22.962730] init: starting service 'bootanim'...  ",
                "[   23.252321] init: starting service 'netd'...  ",
                "[   29.331069] ipa-wan ipa_wwan_ioctl:1428 dev(rmnet_data0) register to IPA",
                "[   32.182592] ueventd: fixup /sys/devices/virtual/input/poll_delay 0 1004 660",
                "[   35.642666] SELinux: initialized (dev fuse, type fuse), uses genfs_contexts",
                "[   39.855818] init: Service 'bootanim' (pid 588) exited with status 0  ");
        File testFile = getTempFile(lines);
        BufferedReader bufferedReader = new BufferedReader(readInputBuffer(testFile));
        List<ServiceInfoItem> serviceInfoItems = (new DmesgParser()).
                parseServiceInfo(bufferedReader);
        assertEquals("No service info should be available", 0,
                serviceInfoItems.size());
        bufferedReader.close();
        testFile.delete();
    }

    /**
     * Write list of strings to file and use it for testing.
     */
    public File getTempFile(List<String> sampleEventsLogs) throws IOException {
        mTempFile = File.createTempFile(DMESG_LOG, ".txt");
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
