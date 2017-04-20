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
package org.hawkular.apm.qe;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.hawkular.apm.qe.model.JaegerRestClient;
import org.hawkular.apm.qe.model.JaegerTracer;
import org.hawkular.apm.qe.model.QESpan;

import org.jboss.resteasy.spi.NotImplementedYetException;

import com.fasterxml.jackson.databind.JsonNode;

import io.opentracing.Tracer;

import lombok.extern.slf4j.Slf4j;

/**
 * @author Kevin Earls 14 April 2017
 */
@Slf4j
public class JaegerQEBase {
    /**
     *
     * @return A tracer
     */
    public Tracer getTracer() {
        return new JaegerTracer().getTracer();
    }

    public JaegerRestClient getJaegerRestClient() {
        return new JaegerRestClient();
    }


    /**
     * Convert all JSON Spans in the trace returned by the Jaeeger ReEST API to QESpans
     *
     * TODO: Figure out how to deal with parents
     * TODO: should this be here, or in the client
     *
     * @param trace
     * @return
     */
    public List<QESpan> getSpansFromTrace(JsonNode trace) {
        List<QESpan> spans = new ArrayList<>();
        Iterator<JsonNode> spanIterator = trace.get("spans").iterator();

        while (spanIterator.hasNext()) {
            JsonNode jsonSpan = spanIterator.next();
            QESpan span = createSpanFromJsonNode(jsonSpan);
            spans.add(span);
        }
        return spans;
    }


    /**
     * Convert a Span in JSON returned from the Jaeger REST API to a QESpan
      * @param jsonSpan
     * @return
     */
    public QESpan createSpanFromJsonNode(JsonNode jsonSpan) {
        Map<String, Object> tags = new HashMap<>();

        JsonNode jsonTags = jsonSpan.get("tags");
        Iterator<JsonNode> jsonTagsIterator = jsonTags.iterator();
        while (jsonTagsIterator.hasNext()) {
            JsonNode jsonTag = jsonTagsIterator.next();
            String type = jsonTag.get("type").asText();
            if (type.equals("string")) {
                String key = jsonTag.get("key").asText();
                String value = jsonTag.get("value").asText();
                tags.put(key, value);
            } else {
                // FIXME tags should have type of String, Boolean, or Integer;
                throw new NotImplementedYetException("Update to handle Numbers and Booleans");
            }
            tags.put(jsonTag.get("key").textValue(), jsonTag.get("value").textValue());
        }

        Long start = jsonSpan.get("startTime").asLong();
        Long duration = jsonSpan.get("duration").asLong();
        Long end = start + duration;
        String operation = jsonSpan.get("operationName").textValue();
        String id = jsonSpan.get("spanID").textValue();

        QESpan qeSpan = new QESpan(tags, start, end, duration, operation, id, null, null, jsonSpan);
        return qeSpan;
    }
}
