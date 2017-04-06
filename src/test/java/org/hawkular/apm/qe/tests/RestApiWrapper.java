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
package org.hawkular.apm.qe.tests;

import java.util.ArrayList;
import java.util.List;

import io.opentracing.Span;
import io.opentracing.Tracer;
import org.hawkular.apm.api.model.Property;
import org.hawkular.apm.api.model.events.CompletionTime;
import org.hawkular.apm.api.model.trace.ContainerNode;
import org.hawkular.apm.api.model.trace.Node;
import org.hawkular.apm.api.model.trace.Trace;
import org.hawkular.apm.client.HawkularApmClient;
import org.hawkular.apm.client.model.Criteria;
import org.hawkular.apm.qe.model.QESpan;
import org.hawkular.apm.qe.model.QESpanBuilder;
import org.hawkular.client.core.ClientResponse;
import org.testng.Assert;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
public class RestApiWrapper implements IServer {
    private HawkularApmClient client = null;

    public RestApiWrapper(HawkularApmClient client) {
        this.client = client;
    }

    private Criteria getCriteria(String operation, Long startTime, Long endTime) {
        Criteria criteria = new Criteria();
        criteria.setOperation(operation);
        criteria.setStartTime(startTime == null ? 0L : startTime);
        criteria.setEndTime(endTime == null ? 0L : endTime);
        return criteria;
    }

    private List<QESpan> buildSpan(List<Node> nodes, List<QESpan> spans, QESpan parent) {
        if (nodes != null && !nodes.isEmpty()) {
            for (Node node : nodes) {
                Tracer.SpanBuilder qeSpanBuilder = QESpanBuilder.offlineBuilder(node.getOperation())
                        .withStartTimestamp(node.getTimestamp())
                        .asChildOf(parent);
                QESpan span = ((QESpanBuilder)qeSpanBuilder).build();

                span.finish(node.getTimestamp() + node.getDuration());
                for (Property property : node.getProperties()) {
                    if (!(property.getName().equalsIgnoreCase("service")
                    || property.getName().equalsIgnoreCase("buildStamp"))) {
                        span.setTag(property.getName(), property.getValue());
                    }
                }
                spans.add(span);
                if (nodes.get(0) instanceof ContainerNode) {
                    buildSpan(((ContainerNode) (Object) node).getNodes(), spans, span);
                }
            }
        }
        return spans;
    }

    private void assertResponse(ClientResponse<?> response) {
        Assert.assertTrue(response.isSuccess());
    }

    @Override
    public QESpan getSpan(Criteria criteria) {
        throw new RuntimeException("Not implemented!");
    }

    @Override
    public List<QESpan> listSpan(Criteria criteria) {
        List<QESpan> spans = new ArrayList<QESpan>();
        ClientResponse<List<CompletionTime>> response = client.analytics().listTraceCompletionTime(criteria);
        assertResponse(response);
        if (response.getEntity().isEmpty()) {
            return spans;
        }
        CompletionTime completionTime = response.getEntity().get(0);
        ClientResponse<Trace> traceResponse = client.trace().getTrace(completionTime.getId());
        assertResponse(traceResponse);
        List<Node> nodes = traceResponse.getEntity().getNodes();
        if (!nodes.isEmpty()) {
            buildSpan(nodes, spans, null);
        }
        return spans;
    }

    @Override
    public List<QESpan> listSpan(String operation, Long startTime, Long endTime) {
        return listSpan(getCriteria(operation, startTime, endTime));
    }
}
