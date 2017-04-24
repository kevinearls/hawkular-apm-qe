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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.hawkular.apm.qe.model.QESpan;
import org.hawkular.apm.qe.tests.TestBase;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.uber.jaeger.rest.model.Criteria;

import io.opentracing.Span;

/**
 * Created by Kevin Earls on 14/04/2017.
 *
 */
public class FirstJaegerTest extends TestBase {
    AtomicInteger operationId = new AtomicInteger(0);
    List<QESpan> spansExpected = new ArrayList<QESpan>();

    long startTime;

    @BeforeMethod
    public void beforeMethod() {
        startTime = Instant.now().toEpochMilli();
        operationId.incrementAndGet();
        spansExpected.clear();
    }

    /**
     * A simple test that just creates one span, and verifies that it was created correctly.
     *
     * @throws Exception
     */
    @Test
    public void writeASingleSpanTest() throws Exception {
        String operationName = "writeASingleSpanTest" + operationId.getAndIncrement();
        Span span = qeTracer().buildSpan(operationName)
                .withTag("simple", true)
                .start();
        spansExpected.add((QESpan) span);
        span.finish();
        sleep();
        Criteria criteria = Criteria.builder().operation(operationName).start(startTime).build();
        assertEquals(server().traceCount(criteria), 1, "Expected 1 trace");

        List<QESpan> spans = server().listSpan(criteria);
        assertEquals(spans.size(), 1, "Expected 1 span");
        QESpan qeSpan = spans.get(0);
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
        Span parentSpan = qeTracer().buildSpan(operationName)
                .withTag("simple", true)
                .start();
        spansExpected.add((QESpan) parentSpan);

        Span childSpan1 = qeTracer().buildSpan(operationName + "-child1")
                .asChildOf(parentSpan)
                .withTag("child", 1)
                .start();
        spansExpected.add((QESpan) childSpan1);
        sleep(100);

        Span childSpan2 = qeTracer().buildSpan(operationName + "-child2")
                .asChildOf(parentSpan)
                .withTag("child", 2)
                .start();
        spansExpected.add((QESpan) childSpan2);
        sleep(50);

        childSpan1.finish();
        childSpan2.finish();

        parentSpan.finish();

        sleep();

        Criteria criteria = Criteria.builder().operation(operationName).start(startTime).build();
        assertEquals(server().traceCount(criteria), 1, "Expected 1 trace");

        List<QESpan> spans = server().listSpan(criteria);
        assertEquals(spans.size(), spansExpected.size());
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
        for (int i = 0; i < 5; i++) {
            if (i == 3) {
                end = System.currentTimeMillis();
                Thread.sleep(50);
            }
            Span testSpan = qeTracer().buildSpan(operationName)
                    .withTag("startEndTestSpan", i)
                    .start();
            if (end == 0) {
                spansExpected.add((QESpan) testSpan);
            }
            testSpan.finish();
        }

        sleep();
        Criteria criteria = Criteria.builder().start(startTime).end(end).build();
        assertEquals(server().traceCount(criteria), spansExpected.size(), "Expected 3 traces");
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
        Span firstSpan = qeTracer().buildSpan(operationName)
                .withTag("firstSpan", true)
                .start();
        spansExpected.add((QESpan) firstSpan);
        Thread.sleep(50);
        firstSpan.finish();

        operationName = "successiveSpansTest" + operationId.getAndIncrement();
        Span secondSpan = qeTracer().buildSpan(operationName)
                .withTag("secondSpan", true)
                .start();
        spansExpected.add((QESpan) secondSpan);
        Thread.sleep(75);
        secondSpan.finish();

        sleep();

        Criteria criteria = Criteria.builder().start(startTime).build();
        assertEquals(server().traceCount(criteria), spansExpected.size(), "Expected 2 traces");

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
        Span span = qeTracer().buildSpan(operationName).start();
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
        Span span = qeTracer().buildSpan(operationName)
                .withTag("booleanTag", true)
                .withTag("numberTag", 42)
                .withTag("stringTag", "I am a tag")
                .start();
        spansExpected.add((QESpan) span);
        span.finish();
        sleep();

        Criteria criteria = Criteria.builder().start(startTime).build();

        assertEquals(server().traceCount(criteria), 1, "Expected 1 trace");
        List<QESpan> spans = server().listSpan(criteria);
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
