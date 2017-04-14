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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.uber.jaeger.metrics.Metrics;
import com.uber.jaeger.metrics.NullStatsReporter;
import com.uber.jaeger.metrics.StatsFactoryImpl;
import com.uber.jaeger.reporters.RemoteReporter;
import com.uber.jaeger.reporters.Reporter;
import com.uber.jaeger.samplers.ProbabilisticSampler;
import com.uber.jaeger.samplers.Sampler;
import com.uber.jaeger.senders.Sender;
import com.uber.jaeger.senders.UDPSender;
import io.opentracing.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.hawkular.apm.qe.model.QESpan;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * @author Kevin Earls
 */
@Slf4j
public class JaegerQEBase {
    public static Integer JAEGER_FLUSH_INTERVAL = 100;
    public static Integer JAEGER_MAX_PACKET_SIZE = 0;
    public static Integer JAEGER_MAX_QUEUE_SIZE = 50;
    public static Double JAEGER_SAMPLING_RATE = 1.0;
    public static Integer JAEGER_PORT = 0;
    public static String JAEGER_URL = System.getenv().getOrDefault("JAEGER_URL", "localhost");
    public static String SERVICE_NAME = System.getenv()
            .getOrDefault("SERVICE_NAME", "qe-automation");

    public ObjectMapper jsonObjectMapper = new ObjectMapper();

    /**
     *
     * @return
     */
    public Tracer getTracer() {
        return getJaegerTracer(JAEGER_URL);
    }


    /**
     *
     * @param url
     * @return
     */
    public Tracer getJaegerTracer(String url) {
        _logger.info("creating tracer with url [{}]", url);
        Sender sender = new UDPSender(url, JAEGER_PORT, JAEGER_MAX_PACKET_SIZE);
        Metrics metrics = new Metrics(new StatsFactoryImpl(new NullStatsReporter()));
        Reporter reporter = new RemoteReporter(sender, JAEGER_FLUSH_INTERVAL, JAEGER_MAX_QUEUE_SIZE, metrics );
        Sampler sampler = new ProbabilisticSampler(JAEGER_SAMPLING_RATE);

        Tracer tracer = new com.uber.jaeger.Tracer.Builder(SERVICE_NAME, reporter, sampler)
                .build();

        return tracer;
    }

    /**
     * Make sure spans are flushed before trying to retrieve them
     */
    public void waitForFlush() {
        try {
            Thread.sleep(JAEGER_FLUSH_INTERVAL);   // TODO is this adequate?
        } catch (InterruptedException e) {
        }
    }


    /**
     * Get all traces from the server
     */
    public List<JsonNode> getTraces() throws Exception {
        return getTraces("");
    }

    /**
     * GET all traces for a service: http://localhost:3001/api/traces?service=something
     * GET a Trace by id: http://localhost:3001/api/traces/23652df68bd54e15
     * GET services http://localhost:3001/api/services
     *
     * GET after a specific time: http://localhost:3001/api/traces?service=something&start=1492098196598
     * Doesn't seem to work.   See query_parent.go
     *
     *
     *
     * @throws Exception
     */
    public List<JsonNode> getTraces(String parameters) throws Exception {
        Client client = ClientBuilder.newClient();
        String targetUrl = "http://localhost:3001/api/traces?service=" + SERVICE_NAME;
        if (parameters != null && !parameters.trim().isEmpty()) {
            targetUrl = targetUrl + "&" + parameters;     // TODO pass parameters as Map?
        }
        _logger.info("using targetURL [{}]", targetUrl);

        WebTarget target = client.target(targetUrl);

        Invocation.Builder builder = target.request();
        builder.accept(MediaType.APPLICATION_XML);
        String result = builder.get(String.class);   // TODO can I get as JsonNode?

        _logger.info("GOT {}", result);

        JsonNode jsonPayload = jsonObjectMapper.readTree(result);
        JsonNode data = jsonPayload.get("data");
        Iterator<JsonNode> traceIterator = data.iterator();

        List<JsonNode> traces = new ArrayList<>();
        while (traceIterator.hasNext()) {
            traces.add(traceIterator.next());
        }

        return traces;
    }


    public List<JsonNode> getSpansFromTrace(JsonNode trace) {
        List<JsonNode> spans = new ArrayList<>();
        Iterator<JsonNode> spanIterator = trace.get("spans").iterator();
        while (spanIterator.hasNext()) {
            // TODO here is where we could convert them
            spans.add(spanIterator.next());
        }
        return spans;
    }

    /* Span contains at least:  (Not including logs)
    {
  "traceID" : "63b5b56ef175dc0",
  "spanID" : "63b5b56ef175dc0",
  "operationName" : "writeASingleSpanTest-1",
  "startTime" : 1492159466057000,
  "duration" : 2258,
  "tags" : [ {
    "key" : "component",
    "type" : "string",
    "value" : "qe-automation"
  }, {
    "key" : "sampler.type",
    "type" : "string",
    "value" : "probabilistic"
  }, {
    "key" : "sampler.param",
    "type" : "string",
    "value" : "1.0"
  }, {
    "key" : "simple",
    "type" : "string",
    "value" : "true"
  }, {
    "key" : "errZeroParentID",
    "type" : "string",
    "value" : "0"
  } ],
  "processID" : "p1"
}

     */

    public QESpan createSpanFromJsonNode(JsonNode jsonSpan) {
        Map<String, Object> tags = new HashMap<>();

        JsonNode jsonTags = jsonSpan.get("tags");
        Iterator<JsonNode> jsonTagsIterator = jsonTags.iterator();
        while (jsonTagsIterator.hasNext()) {
            JsonNode jsonTag = jsonTagsIterator.next();
            //_logger.info("Creating tag with key {} value {} ", jsonTag.get("key").textValue(), jsonTag.get("value").textValue());
            tags.put(jsonTag.get("key").textValue(), jsonTag.get("value").textValue());
        }

        Long start = jsonSpan.get("startTime").asLong();
        System.out.println(">>>>>> JSON Start Time " + start);
        Long duration = jsonSpan.get("duration").asLong();
        Long end = start + duration;
        String operation = jsonSpan.get("operationName").textValue();
        String id = jsonSpan.get("spanID").textValue();

        // TODO how to get Parent; redefine spanObj?

        // 1 tags. 2 Long start.  3. Long end.  4. Long duration 5. operation string
        // 6. id String.  7 QESpan parent.  8:     SpanObj -- the span itself.
        QESpan qeSpan = new QESpan(tags, start, end, duration, operation, id, null, null);
        return qeSpan;
    }


    /**
     * Return a formatted JSON String
     * @param json
     * @return
     * @throws JsonProcessingException
     */
    public String prettyPrintJson(JsonNode json) throws JsonProcessingException {
        return jsonObjectMapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(json);
    }
}
