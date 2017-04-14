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

package org.hawkular.apm.qe.tests.simple;

import com.fasterxml.jackson.databind.JsonNode;
import io.opentracing.Span;
import io.opentracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.hawkular.apm.qe.JaegerQEBase;
import org.hawkular.apm.qe.model.QESpan;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by Kevin Earls on 14/04/2017.
 */
@Slf4j
public class FirstJaegerTest extends JaegerQEBase {
    Tracer tracer;
    AtomicInteger operationId = new AtomicInteger(0);

    @BeforeTest
    public void setup() {
        tracer = getTracer();
        operationId.incrementAndGet();
    }

    @Test
    public void writeASingleSpanTest() throws Exception {
        long startTime = System.currentTimeMillis();
        _logger.info("START " + (startTime));
        String operationName = "writeASingleSpanTest" + operationId.getAndIncrement();
        Span span = tracer.buildSpan(operationName)
                .withTag("simple", true)
                .start();
        //span.log("event " + operationId);        FIXME investigate
        span.finish();

        waitForFlush();

        List<JsonNode> traces = getTraces("start=" + (startTime * 1000));   // Gets all traces since test start
        _logger.info("Got {} traces", traces.size());

        assertEquals(1, traces.size(), "Expected 1 trace");

        List<JsonNode> jsonSpans = getSpansFromTrace(traces.get(0));       // TODO change to return QESpans?
        assertEquals(1, jsonSpans.size(), "Expected 1 span");
        QESpan qeSpan = createSpanFromJsonNode(jsonSpans.get(0));
        assertEquals(qeSpan.getOperation(), operationName);

        Map<String, Object> tags = qeSpan.getTags();
        // TODO how to check for size?
        Object simpleTag = tags.get("simple");  // TODO extract value
        assertNotNull(simpleTag);
        System.out.println("SimpleTag is a " + simpleTag.getClass().getCanonicalName());
        assertEquals("true", simpleTag.toString());

        /*
        int i=1;
        for (JsonNode trace : traces) {
            List<JsonNode> spans = getSpansFromTrace(trace);
            _logger.info("For trace {} got {} spans", i, spans.size());
            for (JsonNode s : spans) {
                //System.out.println(prettyPrintJson(s));
                QESpan qeSpan = createSpanFromJsonNode(s);
                System.out.println("Got QESpan " + qeSpan.getId() + " with operationName " + qeSpan.getOperation() + " start " + qeSpan.getStart());;
            }
            i++;
        }
        */
    }
}
