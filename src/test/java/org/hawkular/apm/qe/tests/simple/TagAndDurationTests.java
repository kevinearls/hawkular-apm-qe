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

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.hawkular.apm.client.model.Criteria;
import org.hawkular.apm.qe.model.QESpan;
import org.hawkular.apm.qe.tests.TestBase;
import org.testng.annotations.Test;

import io.opentracing.Span;

/**
 * Created by Kevin Earls on 04 April 2017.
 */
public class TagAndDurationTests extends TestBase {
    private AtomicLong operationId = new AtomicLong(System.currentTimeMillis());

    /**
     * Write a single span with one tag, and verify that the correct tag is returned
     */
    @Test
    public void simpleTagTest() {
        long startTime = System.currentTimeMillis();
        Span span = qeTracer().buildSpan("simpleTagTest-" + operationId.getAndIncrement())
                .withTag("simple", true)
                .start();
        span.finish();

        sleep();

        Criteria criteria = new Criteria();
        criteria.setStartTime(startTime);
        List<QESpan> spans = server().listSpan(criteria);
        assertEquals(1, spans.size(), "Expected 1 span");
        QESpan receivedSpan = spans.get(0);

        Map<String, Object> tags = receivedSpan.getTags();
        assertEquals(1, tags.size(), "Expected 1 tag");
        String simpleTag = (String) tags.get("simple");
        assertEquals(simpleTag, "true");
    }

    /**
     * Write a single span with a sleep before the finish, and make sure the
     * duration is correct.
     *
     * @throws InterruptedException
     */
    @Test
    public void simpleDurationTest() throws InterruptedException {
        long startTime = System.currentTimeMillis();
        Span span = qeTracer().buildSpan("simpleDurationTest-" + operationId.getAndIncrement())
                .withTag("simple", true)
                .start();
        long expectedMinimumDuration = 100;
        Thread.sleep(expectedMinimumDuration);
        span.finish();

        sleep();

        Criteria criteria = new Criteria();
        criteria.setStartTime(startTime);
        List<QESpan> spans = server().listSpan(criteria);
        assertEquals(1, spans.size(), "Expected 1 span");
        QESpan receivedSpan = spans.get(0);

        long actualDuration = receivedSpan.getDuration() / 1000; // Duration is stored in microseconds

        assertTrue(actualDuration >= expectedMinimumDuration, actualDuration + " should be at least " + expectedMinimumDuration);
    }
}
