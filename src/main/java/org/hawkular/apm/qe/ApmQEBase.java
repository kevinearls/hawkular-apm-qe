/*
 * Copyright 2015-2017 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.hawkular.apm.qe;

import java.net.URISyntaxException;

import org.apache.commons.lang3.RandomUtils;
import org.hawkular.apm.client.HawkularApmClient;
import org.hawkular.apm.qe.model.ApmServerConf;
import org.hawkular.apm.qe.tracer.ITracer;
import org.hawkular.apm.qe.tracer.OpenTracing;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
public class ApmQEBase {
    public enum INSTRUMENTATION_TYPE {
        OPEN_TRACING,
        REST_API
    }

    private static ApmServerConf apmServerConf = ApmServerConf.builder()
            .tenant("hawkular")
            .url("http://jee-hawkular.bc.jonqe.lab.eng.bos.redhat.com:8080")
            .username("admin")
            .password("password")
            .serviceName("qe-automation")
            .buildStamp("1")
            .build();

    private static HawkularApmClient apmClient = null;

    public static ApmServerConf getApmServerConf() {
        return apmServerConf;
    }

    public static ITracer getInstrumentation(INSTRUMENTATION_TYPE type) {
        switch (type) {
            case OPEN_TRACING:
                return OpenTracing.getInstance();
            default:
                throw new RuntimeException("Not implemented yet");
        }
    }

    public static HawkularApmClient getRestClient() throws URISyntaxException {
        if (apmClient == null) {
            apmClient = HawkularApmClient.builder(getApmServerConf().getTenant())
                    .basicAuthentication(getApmServerConf().getUsername(), getApmServerConf().getPassword())
                    .uri(getApmServerConf().getUrl())
                    .build();
        }
        return apmClient;
    }

    public long randomLong() {
        return randomLong(0L, 100000L);
    }

    public long randomLong(long endExclusive) {
        return randomLong(0L, endExclusive);
    }

    public long randomLong(long startInclusive, long endExclusive) {
        return RandomUtils.nextLong(startInclusive, endExclusive);
    }

    public int randomInt() {
        return randomInt(0, 1000);
    }

    public int randomInt(int endExclusive) {
        return randomInt(0, endExclusive);
    }

    public int randomInt(int startInclusive, int endExclusive) {
        return RandomUtils.nextInt(startInclusive, endExclusive);
    }

}
