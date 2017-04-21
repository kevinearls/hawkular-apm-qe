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
package org.hawkular.apm.qe.tracer;

import org.hawkular.apm.qe.JaegerQEBase;
import org.hawkular.apm.qe.model.conf.JaegerAgentConf;

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
 * @author Jeeva Kandasamy (jkandasa)
 * @author kearls
 */
@Slf4j
public class JaegerOpenTracing implements ITracer {

    private static JaegerOpenTracing _INSTANCE = new JaegerOpenTracing();
    private Tracer tracer = null;

    private JaegerOpenTracing() {

    }

    public static JaegerOpenTracing getInstance() {
        return _INSTANCE;
    }

    @Override
    public Tracer getTracer() {
        if (tracer == null) {
            JaegerAgentConf agent = JaegerQEBase.getJaegerConf().getAgent();
            _logger.info("creating tracer with {}", agent);
            Sender sender = new UDPSender(agent.getHost(), agent.getPort(), agent.getPacketSize());
            Metrics metrics = new Metrics(new StatsFactoryImpl(new NullStatsReporter()));
            Reporter reporter = new RemoteReporter(sender, agent.getFlushInterval(), agent.getQueueSize(), metrics);
            Sampler sampler = new ProbabilisticSampler(agent.getSamplingRate());
            tracer = new com.uber.jaeger.Tracer.Builder(JaegerQEBase.getJaegerConf().getServiceName(), reporter,
                    sampler)
                    .build();

        }
        return tracer;
    }
}
