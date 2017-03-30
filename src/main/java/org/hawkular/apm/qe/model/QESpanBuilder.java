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
package org.hawkular.apm.qe.model;

import java.util.HashMap;
import java.util.Map;

import io.opentracing.Span;
import io.opentracing.Tracer;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
public class QESpanBuilder {
    private Map<String, Object> tags = new HashMap<String, Object>();
    private Long start;
    private Long end;
    private Long duration;
    private String operation;
    private String id;
    private QESpan parent;
    private Span spanObj;
    private Tracer tracer;

    public static QESpanBuilder offlineBuilder(String operation) {
        return new QESpanBuilder(null, operation);
    }

    public QESpanBuilder(Tracer tracer, String operation) {
        this.tracer = tracer;
        this.operation = operation;
    }

    public QESpanBuilder withStartTimestamp(Long start) {
        this.start = start;
        return this;
    }

    public QESpanBuilder asChildOf(QESpan parent) {
        this.parent = parent;
        return this;
    }

    public QESpan start() {
        if (tracer == null) {
            throw new RuntimeException("Tracer has been not set. Call 'build()' method to create offline instance");
        }
        if (start == null) {
            start = System.currentTimeMillis() * 1000L;
        }
        if (parent == null) {
            spanObj = tracer.buildSpan(operation)
                    .withStartTimestamp(start)
                    .start();
        } else {
            spanObj = tracer.buildSpan(operation)
                    .withStartTimestamp(start)
                    .asChildOf(parent.getSpanObj())
                    .start();
        }
        //Update Tags
        for (String name : tags.keySet()) {
            if (tags.get(name) instanceof String) {
                spanObj.setTag(name, (String) tags.get(name));
            } else if (tags.get(name) instanceof Number) {
                spanObj.setTag(name, (Number) tags.get(name));
            } else if (tags.get(name) instanceof Boolean) {
                spanObj.setTag(name, (Boolean) tags.get(name));
            } else {
                //Not supported
            }
        }

        return new QESpan(tags, start, end, duration, operation, id, parent, spanObj);
    }

    public QESpan build() {
        if (tracer != null) {
            throw new RuntimeException("Tracer has been set. Call 'start()' method to create online instance");
        }
        return new QESpan(tags, start, end, duration, operation, id, parent, null);
    }

    public QESpanBuilder withTag(String name, Number value) {
        this.tags.put(name, value);
        return this;
    }

    public QESpanBuilder withTag(String name, Boolean value) {
        this.tags.put(name, value);
        return this;
    }

    public QESpanBuilder withTag(String name, String value) {
        this.tags.put(name, value);
        return this;
    }
}
