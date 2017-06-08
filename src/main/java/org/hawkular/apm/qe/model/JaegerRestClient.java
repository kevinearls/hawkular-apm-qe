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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MediaType;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 * Created by kearls on 19/04/2017.
 */
@Slf4j
public class JaegerRestClient {
    private static Map<String, String> evs = System.getenv();
    private static Integer JAEGER_FLUSH_INTERVAL = new Integer(evs.getOrDefault("JAEGER_FLUSH_INTERVAL", "100"));
    private static Integer JAEGER_API_PORT = new Integer(evs.getOrDefault("JAEGER_API_PORT", "16686"));
    private static String JAEGER_AGENT_HOST = evs.getOrDefault("JAEGER_AGENT_HOST", "localhost");
    private static String SERVICE_NAME = evs.getOrDefault("SERVICE_NAME", "qe-automation");
    private ObjectMapper jsonObjectMapper = new ObjectMapper();

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
     * @throws Exception
     */
    public List<JsonNode> getTraces(String parameters) throws Exception {
        waitForFlush(); // TODO make sure this is necessary
        Client client = ClientBuilder.newClient();
        String targetUrl = "http://" + JAEGER_AGENT_HOST + ":" + JAEGER_API_PORT + "/api/traces?service=" + SERVICE_NAME;
        if (parameters != null && !parameters.trim().isEmpty()) {
            targetUrl = targetUrl + "&" + parameters;
        }
        _logger.debug("using targetURL [{}]", targetUrl);

        WebTarget target = client.target(targetUrl);

        Invocation.Builder builder = target.request();
        builder.accept(MediaType.APPLICATION_JSON);
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
     * Make sure spans are flushed before trying to retrieve them
     */
    public void waitForFlush() {
        try {
            Thread.sleep(JAEGER_FLUSH_INTERVAL);   // TODO is this adequate?
        } catch (InterruptedException e) {
        }
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
            Iterator<JsonNode> spanIterator = trace.get("spans").iterator();
            while (spanIterator.hasNext()) {
                JsonNode span = spanIterator.next();
                prettyPrintJson(span);
            }
        }
    }
}
