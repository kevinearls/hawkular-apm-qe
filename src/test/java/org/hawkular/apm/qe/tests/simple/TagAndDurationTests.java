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
import static org.testng.Assert.assertTrue;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.hawkular.apm.qe.model.QESpan;
import org.hawkular.apm.qe.tests.TestBase;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.uber.jaeger.rest.model.Criteria;

import io.opentracing.Span;

/**
 * Created by Kevin Earls on 04 April 2017.
 */
public class TagAndDurationTests extends TestBase {
    AtomicLong operationId = new AtomicLong(Instant.now().getEpochSecond());
    long startTime;
    List<QESpan> spansExpected = new ArrayList<QESpan>();

    @BeforeMethod
    public void beforeMethod() {
        startTime = Instant.now().toEpochMilli();
        operationId.incrementAndGet();
        spansExpected.clear();
    }

    /**
     * Write a single span with one tag, and verify that the correct tag is returned
     */
    @Test
    public void simpleTagTest() throws Exception {
        String operationName = "simpleTagTest-" + operationId.getAndIncrement();
        Span span = qeTracer().buildSpan(operationName)
                .withTag("simple", true)
                .start();
        spansExpected.add((QESpan) span);
        span.finish();
        sleep();

        Criteria criteria = Criteria.builder().operation(operationName).start(startTime).build();
        assertEquals(server().traceCount(criteria), 1, "Expected 1 trace");

        List<QESpan> spans = server().listSpan(criteria);
        assertEquals(spans.size(), spansExpected.size(), "Recieved spans: " + spans);
        QESpan receivedSpan = spans.get(0);

        Map<String, Object> tags = receivedSpan.getTags();
        //assertEquals(1, tags.size(), "Expected 1 tag");    // TODO
        String simpleTag = (String) tags.get("simple");
        assertEquals(simpleTag, "true", receivedSpan.toString());
    }

    /**
     * Write a single span with a sleep before the finish, and make sure the
     * duration is correct.
     *
     * @throws InterruptedException
     */
    @Test
    public void simpleDurationTest() throws Exception {
        String operationName = "simpleDurationTest-" + operationId.getAndIncrement();
        Span span = qeTracer().buildSpan(operationName)
                .withTag("simple", true)
                .start();
        spansExpected.add((QESpan) span);
        long expectedMinimumDuration = 100;
        sleep(expectedMinimumDuration);
        span.finish();
        sleep();

        Criteria criteria = Criteria.builder().operation(operationName).start(startTime).build();
        assertEquals(server().traceCount(criteria), 1, "Expected 1 trace");

        List<QESpan> spans = server().listSpan(criteria);
        assertEquals(spans.size(), spansExpected.size(), "Expected 1 span");
        QESpan receivedSpan = spans.get(0);

        long expectedDuration = expectedMinimumDuration * 1000L;

        assertTrue(receivedSpan.getDuration() >= expectedDuration, "Expected duration: " + expectedDuration + ", "
                + receivedSpan);
    }
}
