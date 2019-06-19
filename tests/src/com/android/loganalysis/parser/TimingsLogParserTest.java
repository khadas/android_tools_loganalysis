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

import com.android.loganalysis.item.SystemServicesTimingItem;

import junit.framework.TestCase;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;

/** Unit Test for {@link TimingsLogParser} */
public class TimingsLogParserTest extends TestCase {

    /** Test that system services duration can be parsed as expected */
    public void testParseSystemServicesTimingInfo_system_services_duration() throws IOException {
        String log =
                String.join(
                        "\n",
                        "01-10 01:25:57.675   981   981 D SystemServerTiming: StartWatchdog took to complete: 38ms",
                        "01-10 01:25:57.675   981   981 I SystemServer: Reading configuration...",
                        "01-10 01:25:57.675   981   981 I SystemServer: ReadingSystemConfig",
                        "01-10 01:25:57.676   981   981 D SystemServerTiming: ReadingSystemConfig took to complete: 0.53ms",
                        "01-10 01:25:57.676   981   981 D SystemServerTiming: ReadingSystemConfig took to complete: 0.53ms", // Parser should skip duplicated log line
                        "01-10 01:25:57.677   465   465 I snet_event_log: [121035042,-1,]",
                        "01-10 01:25:57.678   900   900 I FakeComponent: FakeSubcomponent wrong format took to complete: 10ms",
                        "01-10 01:25:57.678   900   900 I FakeComponent: FakeSubcomponent took to complete: 20s",
                        "01-10 01:25:57.680   981   981 D SystemServerTiming: StartInstaller took to complete: 5ms wrong format",
                        "01-10 01:25:57.682   981   981 D SystemServerTiming: DeviceIdentifiersPolicyService took to complete: 2ms",
                        "01-10 01:25:57.682   981   981 D SystemServerTiming: DeviceIdentifiersPolicyService took to complete: 2ms",
                        "06-06 19:23:54.410  1295  1295 D OtherService  : StartTestStack took to complete: 7ms",
                        "06-06 19:23:55.410   129   129 D FakeService  : Validtook to complete: 8ms",
                        "06-06 19:23:56.410   981   981 D SystemServerTiming: StartWatchdog took to complete: 38ms"); //Parser should parse the same metric at a different time

        List<SystemServicesTimingItem> items =
                new TimingsLogParser().parseSystemServicesTimingItems(createBufferedReader(log));
        assertNotNull(items);
        assertEquals(6, items.size());
        assertEquals("SystemServerTiming", items.get(0).getComponent());
        assertEquals(38.0, items.get(0).getDuration());
        assertNull(items.get(0).getStartTime());
        assertEquals("ReadingSystemConfig", items.get(1).getSubcomponent());
        assertEquals(0.53, items.get(1).getDuration());
        assertNull(items.get(1).getStartTime());
        assertEquals("DeviceIdentifiersPolicyService", items.get(2).getSubcomponent());
        assertEquals("OtherService", items.get(3).getComponent());
        assertEquals("StartTestStack", items.get(3).getSubcomponent());
        assertEquals(7.0, items.get(3).getDuration());
        assertEquals("FakeService", items.get(4).getComponent());
        assertEquals("Valid", items.get(4).getSubcomponent());
        assertEquals(8.0, items.get(4).getDuration());
        assertNull(items.get(4).getStartTime());
        assertEquals("SystemServerTiming", items.get(5).getComponent());
        assertEquals("StartWatchdog", items.get(5).getSubcomponent());
        assertEquals(38.0, items.get(5).getDuration());
    }

    /** Test that system services start time can be parsed as expected */
    public void testParse_system_services_start_time() throws IOException {
        String log =
                String.join(
                        "\n",
                        "01-10 01:24:45.536  1079  1079 D BootAnimation: BootAnimationStartTiming start time: 8611ms",
                        "01-10 01:24:45.537  1079  1079 D BootAnimation: BootAnimationPreloadTiming start time: 8611ms",
                        "01-10 01:24:45.556   874  1021 I ServiceManager: Waiting for service 'package_native' on '/dev/binder'...",
                        "01-10 01:24:45.561   466   466 I snet_event_log: [121035042,-1,]",
                        "01-10 01:24:45.583  1080  1080 I SystemServer: InitBeforeStartServices start time: 2345ms wrong format",
                        "01-10 01:25:24.095  1014  1111 D BootAnimation: BootAnimationShownTiming start time: 9191s",
                        "06-06 19:23:49.299   603   603 E qdmetadata: Unknown paramType 2",
                        "06-06 19:23:49.299   603   603 I FakeComponent : wrong subcomponent start time: 234ms",
                        "06-06 19:23:49.299   603   603 D FakeComponent: Subcomponent start time 234ms",
                        "06-06 19:23:49.299  1079  1079 D BootAnimation: BootAnimationStopTiming start time: 24839ms",
                        "06-06 19:23:59.299   179   179 D FakeService  : Validstart time: 34839ms");

        List<SystemServicesTimingItem> items =
                new TimingsLogParser().parseSystemServicesTimingItems(createBufferedReader(log));
        assertNotNull(items);
        assertEquals(4, items.size());
        assertEquals("BootAnimation", items.get(0).getComponent());
        assertEquals("BootAnimationStartTiming", items.get(0).getSubcomponent());
        assertEquals(8611.0, items.get(0).getStartTime());
        assertNull(items.get(0).getDuration());
        assertEquals("BootAnimationPreloadTiming", items.get(1).getSubcomponent());
        assertEquals("BootAnimation", items.get(2).getComponent());
        assertEquals("BootAnimationStopTiming", items.get(2).getSubcomponent());
        assertEquals(24839.0, items.get(2).getStartTime());
        assertNull(items.get(2).getDuration());
        assertEquals("FakeService", items.get(3).getComponent());
        assertEquals("Valid", items.get(3).getSubcomponent());
        assertEquals(34839.0, items.get(3).getStartTime());
        assertNull(items.get(3).getDuration());
    }

    private BufferedReader createBufferedReader(String input) {
        InputStream inputStream = new ByteArrayInputStream(input.getBytes());
        InputStreamReader reader = new InputStreamReader(inputStream);
        return new BufferedReader(reader);
    }
}
