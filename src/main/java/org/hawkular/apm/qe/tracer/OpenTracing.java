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

import org.hawkular.apm.client.api.recorder.BatchTraceRecorder;
import org.hawkular.apm.client.api.sampler.Sampler;
import org.hawkular.apm.client.opentracing.APMTracer;
import org.hawkular.apm.client.opentracing.DeploymentMetaData;
import org.hawkular.apm.qe.ApmQEBase;
import org.hawkular.apm.trace.publisher.rest.client.TracePublisherRESTClient;

import io.opentracing.Tracer;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
public class OpenTracing implements ITracer {

    private static OpenTracing _INSTANCE = new OpenTracing();
    private Tracer tracer = null;

    private OpenTracing() {

    }

    public static OpenTracing getInstance() {
        return _INSTANCE;
    }

    @Override
    public Tracer getTracer() {
        if (tracer == null) {

            BatchTraceRecorder traceRecorder = new BatchTraceRecorder.BatchTraceRecorderBuilder()
                    .withTracePublisher(new TracePublisherRESTClient(
                            ApmQEBase.getApmServerConf().getUsername(),
                            ApmQEBase.getApmServerConf().getPassword(),
                            ApmQEBase.getApmServerConf().getUrl()))
                    .withTenantId(ApmQEBase.getApmServerConf().getTenant())
                    .build();

            tracer = new APMTracer(traceRecorder, Sampler.ALWAYS_SAMPLE,
                    new DeploymentMetaData(
                            ApmQEBase.getApmServerConf().getServiceName(),
                            ApmQEBase.getApmServerConf().getBuildStamp())
                    );
        }
        return tracer;
    }
}
