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

import static org.testng.Assert.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.apm.qe.JaegerQEBase;
import org.hawkular.apm.qe.model.QESpan;

import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.databind.JsonNode;

import io.opentracing.Span;
import io.opentracing.Tracer;

import lombok.extern.slf4j.Slf4j;


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

    /**
     * A simple test that just creates one span, and verifies that it was created correctly.
     *
     * @throws Exception
     */
    @Test
    public void writeASingleSpanTest() throws Exception {
        long startTime = Instant.now().toEpochMilli();
        String operationName = "writeASingleSpanTest" + operationId.getAndIncrement();
        Span span = tracer.buildSpan(operationName)
                .withTag("simple", true)
                .start();
//        span.log("event " + operationId);
        span.finish();

        List<JsonNode> traces = getTracesSinceTestStart(startTime);
        assertEquals(1, traces.size(), "Expected 1 trace");

        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals(1, spans.size(), "Expected 1 span");
        QESpan qeSpan = spans.get(0);
        _logger.info(prettyPrintJson(qeSpan.getJson()));

        assertEquals(qeSpan.getOperation(), operationName);

        Map<String, Object> tags = qeSpan.getTags();        // TODO how to check tags for size?
        Object simpleTag = tags.get("simple");
        assertNotNull(simpleTag);
        assertEquals("true", simpleTag.toString());
    }

    /**
     * TODO Open a bug on this.
     *
     * @throws Exception
     */
    @Test(expectedExceptions = AbstractMethodError.class)
    public void spanDotLogIsBrokenTest() throws Exception {
        String operationName = "spanDotLogIsBrokenTest";
        Span span = tracer.buildSpan(operationName)
                .start();
        span.log("event");
        Assert.fail("Jaeger must have fixed this, update the tests.");
    }


    /**
     * TODO Open a bug on this.  According to the OpenTracing spec tags can be
     * String, Number, or Boolean, but as far as I can see so far the Jaeger
     * Rest API only returns strings.
     *
     * @throws Exception
     */
    @Test(enabled = false)
    public void tagsShouldBeTypedTest() throws Exception {
        long startTime = Instant.now().toEpochMilli();
        String operationName = "tagsShouldBeTypedTest";
        Span span = tracer.buildSpan(operationName)
                .withTag("booleanTag", true)
                .withTag("numberTag", 42)
                .withTag("stringTag", "I am a tag")
                .start();
        span.finish();

        List<JsonNode> traces = getTracesSinceTestStart(startTime);
        assertEquals(1, traces.size(), "Expected 1 trace");
        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals(1, spans.size(), "Expected only 1 span");
        QESpan qeSpan = spans.get(0);

        Map<String, Object> tags = qeSpan.getTags();
        Object booleanTag = tags.get("booleanTag");
        Object numberTag = tags.get("numberTag");
        Object stringTag = tags.get("stringTag");

        assertNotNull(booleanTag);
        assertNotNull(numberTag);
        assertNotNull(stringTag);

        assertTrue(booleanTag instanceof Boolean, "booleanTag should be a boolean");
        assertTrue(numberTag instanceof Number, "numberTag should be a boolean");
        assertTrue(stringTag instanceof String, "stringTag should be a boolean");

        assertEquals(true, booleanTag);
        assertEquals(42, numberTag);
        assertEquals("I am a tag", stringTag);
    }
}
