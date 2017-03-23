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
package org.hawkular.apm.qe.tests;

import java.net.URISyntaxException;

import org.hawkular.apm.client.HawkularApmClient;
import org.hawkular.apm.qe.ApmQEBase;
import org.testng.annotations.BeforeSuite;

import io.opentracing.Tracer;

public class TestBase extends ApmQEBase {
    private static Tracer _tracer = null;
    private static HawkularApmClient _restClient = null;

    @BeforeSuite
    public void loadRequiredinstance() throws URISyntaxException {
        loadTracer(INSTRUMENTATION_TYPE.OPEN_TRACING);
        _restClient = ApmQEBase.getRestClient();
    }

    public void loadTracer(INSTRUMENTATION_TYPE type) {
        _tracer = ApmQEBase.getInstrumentation(type).getTracer();
    }

    public Tracer tracer() {
        return _tracer;
    }

    public HawkularApmClient restClient() {
        return _restClient;
    }
}
