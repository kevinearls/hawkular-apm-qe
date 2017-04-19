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
package org.hawkular.apm.qe.model;

import java.util.Map;

import org.hawkular.apm.qe.tracer.ITracer;

import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.samplers.Sampler;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UDPSender;

import io.opentracing.Tracer;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by kearls on 19/04/2017.
 */
@Slf4j
public class JaegerTracer implements ITracer {
    private static Map<String, String> evs = System.getenv();
    private static Integer JAEGER_FLUSH_INTERVAL = new Integer(evs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
    private static Integer JAEGER_MAX_PACKET_SIZE = new Integer(evs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));
    private static Integer JAEGER_MAX_QUEUE_SIZE = new Integer(evs.getOrDefault("JAEGER_MAX_QUEUE_SIZE", "50"));
    private static Double JAEGER_SAMPLING_RATE = new Double(evs.getOrDefault("JAEGER_SAMPLING_RATE", "1.0"));
    private static Integer JAEGER_UDP_PORT = new Integer(evs.getOrDefault("JAEGER_UDP_PORT", "5775"));
    private static String JAEGER_SERVER_HOST = evs.getOrDefault("JAEGER_SERVER_HOST", "localhost");
    private static String SERVICE_NAME = evs.getOrDefault("SERVICE_NAME", "qe-automation");

    @Override
    public Tracer getTracer() {
        _logger.info("creating tracer with host [{}] and port {}", JAEGER_SERVER_HOST, JAEGER_UDP_PORT);
        Sender sender = new UDPSender(JAEGER_SERVER_HOST, JAEGER_UDP_PORT, JAEGER_MAX_PACKET_SIZE);
        Metrics metrics = new Metrics(new StatsFactoryImpl(new NullStatsReporter()));
        Reporter reporter = new RemoteReporter(sender, JAEGER_FLUSH_INTERVAL, JAEGER_MAX_QUEUE_SIZE, metrics );
        Sampler sampler = new ProbabilisticSampler(JAEGER_SAMPLING_RATE);

        Tracer tracer = new com.uber.jaeger.Tracer.Builder(SERVICE_NAME, reporter, sampler)
                .build();

        return tracer;
    }
}
