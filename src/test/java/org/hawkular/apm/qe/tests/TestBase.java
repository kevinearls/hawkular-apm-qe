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

import org.hawkular.apm.qe.JaegerQEBase;
import org.hawkular.apm.qe.model.QETracer;
import org.testng.annotations.BeforeSuite;

import com.uber.jaeger.rest.JaegerRestClient;

import io.opentracing.Tracer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
@Slf4j
public class TestBase extends JaegerQEBase {
    private static Tracer _tracer = null;
    private static QETracer _qeTracer = null;
    private static JaegerRestClient _restClient = null;
    private static IServer _server = null;

    //Returns tracer instance to test classes
    public Tracer tracer() {
        return _tracer;
    }

    //Returns internal QEtracer instance to test classes
    public QETracer qeTracer() {
        return _qeTracer;
    }

    //Returns rest client instance to test classes
    public JaegerRestClient restClient() {
        return _restClient;
    }

    //Returns server instance to test classes
    public IServer server() {
        return _server;
    }

    @BeforeSuite
    public void loadRequiredinstance() throws URISyntaxException {
        _tracer = JaegerQEBase.getInstrumentation(INSTRUMENTATION_TYPE.JAEGER_OPENTRACING).getTracer();
        _qeTracer = new QETracer(tracer());
        _restClient = JaegerQEBase.getRestClient();
        _server = new JaegerRestApiWrapper(restClient());
    }

    public void sleep() {
        //Give some delay to update data on Jaeger server from client
        sleep(getJaegerConf().getAgent().getFlushInterval() * 10);
    }

    public void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException ex) {
            _logger.error("Exception,", ex);
        }
    }

}
