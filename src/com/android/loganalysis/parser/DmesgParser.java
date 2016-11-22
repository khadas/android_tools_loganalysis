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
import com.android.loganalysis.item.ServiceInfoItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Parse the dmesg logs. </p>
 */
public class DmesgParser implements IParser {

    private static final String SERVICENAME = "SERVICENAME";
    private static final String TIMESTAMP = "TIMESTAMP";
    // Matches: [ 14.822691] init:
    private static final String SERVICE_PREFIX = String.format("^\\[\\s+(?<%s>.*)\\] init:\\s+",
            TIMESTAMP);
    // Matches: starting service 'ueventd'...
    private static final String START_SERVICE_SUFFIX = String.format("starting service "
            + "\\'(?<%s>.*)\\'...", SERVICENAME);
    // Matches: Service 'ueventd' (pid 439) exited with status 0
    private static final String EXIT_SERVICE_SUFFIX = String.format("Service \\'(?<%s>.*)\\'\\s+"
            + "\\((?<PID>.*)\\) exited with status 0", SERVICENAME);

    private static final Pattern START_SERVICE = Pattern.compile(
            String.format("%s%s", SERVICE_PREFIX, START_SERVICE_SUFFIX));
    private static final Pattern EXIT_SERVICE = Pattern.compile(
            String.format("%s%s", SERVICE_PREFIX, EXIT_SERVICE_SUFFIX));

    private Map<String, ServiceInfoItem> mServiceInfoItems = new HashMap<String, ServiceInfoItem>();

    @Override
    public IItem parse(List<String> lines) {
        throw new UnsupportedOperationException("Method has not been implemented in lieu"
                + " of others");
    }

    /**
     * Parse init services start time and end time from dmesg logs and store the duration it took to
     * complete the service if the end time stamp is available.
     *
     * @param input dmesg logs
     * @return list of ServiceInfoItems which contains service start and end time
     * @throws IOException
     */
    public List<ServiceInfoItem> parseServiceInfo(BufferedReader input)
            throws IOException {
        String line;
        while ((line = input.readLine()) != null) {
            Matcher match = null;
            if ((match = matches(START_SERVICE, line)) != null) {
                ServiceInfoItem serviceItem = new ServiceInfoItem();
                serviceItem.setServiceName(match.group(SERVICENAME));
                serviceItem.setStartTime((long) (Double.parseDouble(
                        match.group(TIMESTAMP)) * 1000));
                getServiceInfoItems().put(match.group(SERVICENAME), serviceItem);
            } else if ((match = matches(EXIT_SERVICE, line)) != null) {
                if (getServiceInfoItems().containsKey(match.group(SERVICENAME))) {
                    ServiceInfoItem serviceItem = getServiceInfoItems().get(
                            match.group(SERVICENAME));
                    serviceItem.setEndTime((long) (Double.parseDouble(
                            match.group(TIMESTAMP)) * 1000));
                }
            }
        }
        return new ArrayList<ServiceInfoItem>(getServiceInfoItems().values());
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

    public Map<String, ServiceInfoItem> getServiceInfoItems() {
        return mServiceInfoItems;
    }

    public void setServiceInfoItems(Map<String, ServiceInfoItem> serviceInfoItems) {
        this.mServiceInfoItems = serviceInfoItems;
    }

}
