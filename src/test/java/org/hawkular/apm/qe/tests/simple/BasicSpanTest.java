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

import java.util.ArrayList;
import java.util.List;

import org.hawkular.apm.qe.model.QESpan;
import org.hawkular.apm.qe.tests.TestBase;
import org.testng.Assert;
import org.testng.annotations.Test;

import io.opentracing.Span;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
public class BasicSpanTest extends TestBase {
    @Test(priority = 0)
    public void rootSpantest() throws InterruptedException {
        /*Description: Create root/single span send it to server via tracer
         * validate it from server.
         * Steps:
         * 1. Create root span
         * 2. send it to server by calling finish() method of span
         * 3. validate from server
         */
        long end = System.currentTimeMillis() * 1000L; // in microseconds
        long start = end - randomLong(100L * 1000L);
        String operation = "rootSpanTest_" + randomInt(0, 100);
        List<QESpan> spansExpected = new ArrayList<QESpan>();
        Span parentSpan = qeTracer().buildSpan(operation)
                .withStartTimestamp(start)
                .withTag("start", start)
                .withTag("end", end)
                .withTag("type", "m-start-end")
                .start();
        spansExpected.add((QESpan) parentSpan);
        parentSpan.finish(end);

        //Give some delay to settle down spans to server
        sleep();

        //Validate span on server
        List<QESpan> spansActual = server().listSpan(operation, start / 1000, end / 1000);
        Assert.assertEquals(spansActual.size(), spansExpected.size());
        //Validate span on server
        for (QESpan spanExpected : spansExpected) {
            boolean found = false;
            for (int index = 0; index < spansActual.size(); index++) {
                if (spanExpected.equals(spansActual.get(index))) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found, "Not found: " + spanExpected.toString());
        }

    }

    @Test(priority = 1)
    public void childSpantest() throws InterruptedException {
        /*Description: Create parent span and child span send it to server via tracer
         * validate it from server.
         * Steps:
         * 1. Create parent span and child span
         * 2. send it to server by calling finish() method of span
         * 3. validate from server
         */
        long end = System.currentTimeMillis() * 1000L; // in microseconds
        long start = end - randomLong(100L * 1000L);
        String operation = "basicSpanTestWithSingleChild_" + randomInt(0, 100);
        List<QESpan> spansExpected = new ArrayList<QESpan>();
        Span parentSpan = qeTracer().buildSpan(operation)
                .withStartTimestamp(start)
                .withTag("start", start)
                .withTag("end", end)
                .withTag("type", "m-start-end")
                .start();
        spansExpected.add((QESpan) parentSpan);
        //Child span
        Span childSpan = qeTracer().buildSpan(operation + "_child1")
                .withStartTimestamp(start)
                .withTag("node", "child1")
                .asChildOf(parentSpan)
                .start();
        spansExpected.add((QESpan) childSpan);
        //Send span to server
        childSpan.finish(randomLong(start, end));
        parentSpan.finish(end);

        //Give some delay to settle down spans to server
        sleep();

        //Validate span on server
        List<QESpan> spansActual = server().listSpan(operation, start / 1000, end / 1000);
        Assert.assertEquals(spansActual.size(), spansExpected.size());
        //Validate span on server
        for (QESpan spanExpected : spansExpected) {
            boolean found = false;
            for (int index = 0; index < spansActual.size(); index++) {
                if (spanExpected.equals(spansActual.get(index))) {
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found, "Not found: " + spanExpected.toString());
        }
    }
}
