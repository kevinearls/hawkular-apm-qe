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
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.awaitility.Awaitility.await;
import static org.testng.Assert.*;


/**
 * Created by Kevin Earls on 14/04/2017.
 *
 */
@Slf4j
public class FirstJaegerTest extends JaegerQEBase {
    Tracer tracer;
    AtomicInteger operationId = new AtomicInteger(0);
    long startTime;


    @BeforeMethod
    public void beforeMethod() {
        startTime = Instant.now().toEpochMilli();
        operationId.incrementAndGet();
    }

    @BeforeTest
    public void setup() {
        tracer = getTracer();
    }

    /**
     * A simple test that just creates one span, and verifies that it was created correctly.
     *
     * @throws Exception
     */
    @Test
    public void writeASingleSpanTest() throws Exception {
        String operationName = "writeASingleSpanTest" + operationId.getAndIncrement();
        Span span = tracer.buildSpan(operationName)
                .withTag("simple", true)
                .start();
        span.finish();

        waitForTracesSinceStart(startTime, 1);

        List<JsonNode> traces = restClient.getTracesSinceTestStart(startTime);
        assertEquals(traces.size(), 1, "Expected 1 trace");

        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals(spans.size(), 1, "Expected 1 span");
        QESpan qeSpan = spans.get(0);
        _logger.debug(restClient.prettyPrintJson(qeSpan.getJson()));

        assertEquals(qeSpan.getOperation(), operationName);

        Map<String, Object> tags = qeSpan.getTags();
        Object simpleTag = tags.get("simple");
        assertNotNull(simpleTag);
        assertEquals("true", simpleTag.toString());
    }


    /**
     * Simple test of creating a span with children
     *
     * @throws Exception
     */
    @Test
    public void spanWithChildrenTest() throws Exception {
        String operationName = "spanWithChildrenTest" + operationId.getAndIncrement();
        Span parentSpan = tracer.buildSpan(operationName)
                .withTag("simple", true)
                .start();

        Span childSpan1 = tracer.buildSpan(operationName + "-child1")
                .asChildOf(parentSpan)
                .withTag("child", 1)
                .start();
        Thread.sleep(100);

        Span childSpan2 = tracer.buildSpan(operationName + "-child2")
                .asChildOf(parentSpan)
                .withTag("child", 2)
                .start();
        Thread.sleep(50);

        childSpan1.finish();
        childSpan2.finish();

        parentSpan.finish();

        List<JsonNode> traces = restClient.getTracesSinceTestStart(startTime);
        assertEquals(traces.size(), 1, "Expected 1 trace");

        List<QESpan> spans = getSpansFromTrace(traces.get(0));
        assertEquals(spans.size(), 3, "Expected 1 spans");
        // TODO validate parent child structure, operationNames, etc.
    }


    /**
     * A simgple test of the start and end options when fetching traces.
     *
     * @throws Exception
     */
    @Test
    public void testStartEndTest() throws Exception {
        String operationName = "startEndTest" + operationId.getAndIncrement();
        long end = 0;
        for (int i=0; i < 5; i++) {
            if (i == 3) {
                end = System.currentTimeMillis();
                Thread.sleep(50);
            }
            Span testSpan = tracer.buildSpan(operationName)
                    .withTag("startEndTestSpan", i)
                    .start();
            testSpan.finish();
        }

        waitForTracesBetween(startTime, end, 3);

        List<JsonNode> traces = restClient.getTracesBetween(startTime, end);
        assertEquals(traces.size(), 3, "Expected 3 traces");

        // TODO more assertions here ?
    }




    /**
     * This should create 2 traces as Jaeger closes a trace when finish() is called
     * on the top-level span
     *
     * @throws Exception
     */
    @Test
    public void successiveSpansTest() throws Exception {
        String operationName = "successiveSpansTest" + operationId.getAndIncrement();
        Span firstSpan = tracer.buildSpan(operationName)
                .withTag("firstSpan", true)
                .start();
        Thread.sleep(50);
        firstSpan.finish();

        operationName = "successiveSpansTest" + operationId.getAndIncrement();
        Span secondSpan = tracer.buildSpan(operationName)
                .withTag("secondSpan", true)
                .start();
        Thread.sleep(75);
        secondSpan.finish();

        waitForTracesSinceStart(startTime, 2);
        List<JsonNode> traces = restClient.getTracesSinceTestStart(startTime);
        assertEquals(traces.size(), 2, "Expected 2 traces");

        // TODO more assertions here....
        //dumpAllTraces(traces);

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
        String operationName = "tagsShouldBeTypedTest";
        Span span = tracer.buildSpan(operationName)
                .withTag("booleanTag", true)
                .withTag("numberTag", 42)
                .withTag("stringTag", "I am a tag")
                .start();
        span.finish();

        List<JsonNode> traces = restClient.getTracesSinceTestStart(startTime);
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
