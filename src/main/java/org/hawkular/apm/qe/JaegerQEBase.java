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

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import org.hawkular.apm.qe.model.QESpan;

import org.jboss.resteasy.spi.NotImplementedYetException;

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

/**
 * @author Kevin Earls 14 April 2017
 */
@Slf4j
public class JaegerQEBase {
    private static Map<String, String> evs = System.getenv();
    private static Integer JAEGER_FLUSH_INTERVAL = new Integer(evs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
    private static Integer JAEGER_MAX_PACKET_SIZE = new Integer(evs.getOrDefault("JAEGER_MAX_PACKET_SIZE", "0"));
    private static Integer JAEGER_MAX_QUEUE_SIZE = new Integer(evs.getOrDefault("JAEGER_MAX_QUEUE_SIZE", "50"));
    private static Double JAEGER_SAMPLING_RATE = new Double(evs.getOrDefault("JAEGER_SAMPLING_RATE", "1.0"));
    private static Integer JAEGER_UDP_PORT = new Integer(evs.getOrDefault("JAEGER_UDP_PORT", "5775"));
    private static Integer JAEGER_API_PORT = new Integer(evs.getOrDefault("JAEGER_API_PORT", "16686"));
    private static String JAEGER_SERVER_HOST = evs.getOrDefault("JAEGER_SERVER_HOST", "localhost");
    private static String SERVICE_NAME = evs.getOrDefault("SERVICE_NAME", "qe-automation");

    private ObjectMapper jsonObjectMapper = new ObjectMapper();

    /**
     *
     * @return A tracer
     */
    public Tracer getTracer() {
        return getJaegerTracer(JAEGER_SERVER_HOST);
    }


    /**
     *
     * @param url for the Jaeger server
     * @return A Tracer
     */
    public Tracer getJaegerTracer(String url) {
        _logger.info("creating tracer with url [{}]", url);
        Sender sender = new UDPSender(url, JAEGER_UDP_PORT, JAEGER_MAX_PACKET_SIZE);
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
     * Return all of the traces created since the start time given.  NOTE: The Jaeger Rest API
     * requires a time in microseconds.  For convenience this method accepts milliseconds and converts.
     *
     * @param testStartTime in milliseconds
     * @return A List of Traces created after the time specified.
     * @throws Exception
     */
    public List<JsonNode> getTracesSinceTestStart(long testStartTime) throws Exception {
        List<JsonNode> traces = getTraces("start=" + (testStartTime * 1000));
        return traces;
    }


    /**
     * Return all of the traces created between the start and end times given.  NOTE: Times should be in
     * milliseconds.  The Jaeger Rest API requires times in microseconds, but for convenience this method
     * will accept milliseconds and do the conversion
     *
     * @param start start time in milliseconds
     * @param end end time in milliseconds
     * @return A List of traces created between the times specified.
     * @throws Exception
     */
    public List<JsonNode> getTracesBetween(long start, long end) throws Exception {
        String parameters = "start=" + (start * 1000) + "&end=" + (end * 1000);
        List<JsonNode> traces = getTraces(parameters);
        return traces;

    }


    /**
     * TODO: Figure out the Jaeger REST Api.  Key code can be found
     *
     *  https://github.com/uber/jaeger/blob/master/cmd/query/app/handler.go#L120-L130 with parameter info
     *  https://github.com/uber/jaeger/blob/master/cmd/query/app/query_parser.go#L68-L81
     *
     * GET all traces for a service: http://localhost:3001/api/traces?service=something
     * GET a Trace by id: http://localhost:3001/api/traces/23652df68bd54e15
     * GET services http://localhost:3001/api/services
     *
     * GET after a specific time: http://localhost:3001/api/traces?service=something&start=1492098196598
     * NOTE: time is in MICROseconds.
     *
     *
     *
     * @throws Exception
     */
    public List<JsonNode> getTraces(String parameters) throws Exception {
        waitForFlush(); // TODO make sure this is necessary
        Client client = ClientBuilder.newClient();
        String targetUrl = "http://" + JAEGER_SERVER_HOST + ":" + JAEGER_API_PORT + "/api/traces?service=" + SERVICE_NAME;
        if (parameters != null && !parameters.trim().isEmpty()) {
            targetUrl = targetUrl + "&" + parameters;     // TODO pass parameters as Map?
        }
        _logger.debug("using targetURL [{}]", targetUrl);

        WebTarget target = client.target(targetUrl);

        Invocation.Builder builder = target.request();
        builder.accept(MediaType.APPLICATION_XML);
        String result = builder.get(String.class);

        JsonNode jsonPayload = jsonObjectMapper.readTree(result);
        JsonNode data = jsonPayload.get("data");
        Iterator<JsonNode> traceIterator = data.iterator();

        List<JsonNode> traces = new ArrayList<>();
        while (traceIterator.hasNext()) {
            traces.add(traceIterator.next());
        }

        return traces;
    }


    /**
     * Convert all JSON Spans in the Trace to QESpans
     *
     * TODO: Figure out how to deal with parents
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
     * Convert a Span in JSON returned from the Rest API to a QESpan
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

    /**
     * Debugging method
     *
     * @param traces A list of traces to print
     * @throws Exception
     */
    protected void dumpAllTraces(List<JsonNode> traces) throws Exception {
        _logger.info("Got " + traces.size() + " traces");

        for (JsonNode trace : traces) {
            _logger.info("------------------ Trace {} ------------------", trace.get("traceId"));
            List<QESpan> qeSpans = getSpansFromTrace(trace);
            for (QESpan qeSpan : qeSpans) {
                _logger.info(prettyPrintJson(qeSpan.getJson()));
            }
        }
    }
}
