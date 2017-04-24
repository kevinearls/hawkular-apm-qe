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
import java.util.Map;

import org.apache.commons.lang3.RandomUtils;
import org.hawkular.apm.qe.model.conf.JaegerAgentConf;
import org.hawkular.apm.qe.model.conf.JaegerConf;
import org.hawkular.apm.qe.model.conf.JaegerServerConf;
import org.hawkular.apm.qe.tracer.ITracer;
import org.hawkular.apm.qe.tracer.JaegerOpenTracing;

import com.uber.jaeger.rest.JaegerRestClient;

/**
 * @author Jeeva Kandasamy (jkandasa)
 * @author kearls
 */
public class JaegerQEBase {
    private static Map<String, String> evs = System.getenv();
    private static Integer JAEGER_AGENT_FLUSH_INTERVAL = new Integer(evs.getOrDefault("JAEGER_AGENT_FLUSH_INTERVAL",
            "100"));
    private static Integer JAEGER_AGENT_PACKET_SIZE = new Integer(evs.getOrDefault("JAEGER_AGENT_PACKET_SIZE", "0"));
    private static Integer JAEGER_AGENT_QUEUE_SIZE = new Integer(evs.getOrDefault("JAEGER_AGENT_QUEUE_SIZE", "50"));
    private static Double JAEGER_AGENT_SAMPLING_RATE = new Double(
            evs.getOrDefault("JAEGER_AGENT_SAMPLING_RATE", "1.0"));
    private static Integer JAEGER_AGENT_PORT = new Integer(evs.getOrDefault("JAEGER_AGENT_PORT", "5775"));
    private static String JAEGER_SERVER_HOST = evs.getOrDefault("JAEGER_SERVER_HOST", "localhost");
    private static Integer JAEGER_SERVER_REST_PORT = new Integer(evs.getOrDefault("JAEGER_SERVER_REST_PORT", "16686"));
    private static String SERVICE_NAME = evs.getOrDefault("SERVICE_NAME", "qe-automation");

    public enum INSTRUMENTATION_TYPE {
        JAEGER_OPENTRACING,
        ZIPKIN_OPENTRACING
    }

    private static JaegerAgentConf jaegerAgentConf = JaegerAgentConf.builder()
            .host(JAEGER_SERVER_HOST)
            .port(JAEGER_AGENT_PORT)
            .packetSize(JAEGER_AGENT_PACKET_SIZE)
            .queueSize(JAEGER_AGENT_QUEUE_SIZE)
            .samplingRate(JAEGER_AGENT_SAMPLING_RATE)
            .flushInterval(JAEGER_AGENT_FLUSH_INTERVAL)
            .build();

    private static JaegerServerConf jaegerServerConf = JaegerServerConf.builder()
            .host(JAEGER_SERVER_HOST)
            .restPort(JAEGER_SERVER_REST_PORT)
            .protocol("http")
            .build();

    private static JaegerConf jaegerConf = JaegerConf.builder()
            .server(jaegerServerConf)
            .agent(jaegerAgentConf)
            .serviceName(SERVICE_NAME)
            .build();
    private static JaegerRestClient jaegerRestClient = null;

    public static JaegerConf getJaegerConf() {
        return jaegerConf;
    }

    public static ITracer getInstrumentation(INSTRUMENTATION_TYPE type) {
        switch (type) {
            case JAEGER_OPENTRACING:
                return JaegerOpenTracing.getInstance();
            default:
                throw new RuntimeException("Not implemented yet");
        }
    }

    public static JaegerRestClient getRestClient() throws URISyntaxException {
        if (jaegerRestClient == null) {
            jaegerRestClient = JaegerRestClient.builder()
                    .uri(getJaegerConf().getServer().getUrl())
                    .build();
        }
        return jaegerRestClient;
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
