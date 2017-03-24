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

import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.client.core.ClientResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.opentracing.Span;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
public class MyTestNG extends TestBase {

    @Test
    public void test() throws InterruptedException {
        long end = System.currentTimeMillis() * 1000L;
        long start = end - 1000L * 1000L;
        Span parentSpan = tracer().buildSpan("simpleTest")
                .withTag("start", start)
                .withTag("end", end)
                .withTag("type", "m-start-end")
                .withStartTimestamp(start)
                .start();
        Span childSpan = tracer().buildSpan("simpleTest-child")
                .withTag("node", "child")
                .asChildOf(parentSpan)
                .start();
        childSpan.finish();
        parentSpan.finish(end);
        /*
        ClientResponse<Trace> respose = restClient().trace().getTrace("hello");
        Assert.assertEquals(start, respose.getEntity().getTimestamp());
        Assert.assertEquals(end - start, respose.getEntity().calculateDuration());
        */
    }
}
