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

import org.hawkular.apm.qe.JaegerQEBase;
import org.hawkular.apm.qe.model.QESpan;
import org.hawkular.apm.qe.model.QESpanBuilder;
import org.hawkular.client.core.ClientResponse;
import org.testng.Assert;

import com.uber.jaeger.rest.JaegerRestClient;
import com.uber.jaeger.rest.model.Criteria;
import com.uber.jaeger.rest.model.Reference;
import com.uber.jaeger.rest.model.Result;
import com.uber.jaeger.rest.model.Span;
import com.uber.jaeger.rest.model.Tag;
import com.uber.jaeger.rest.model.Trace;

import io.opentracing.Tracer;
import lombok.extern.slf4j.Slf4j;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
@Slf4j
public class JaegerRestApiWrapper implements IServer {
    private JaegerRestClient client = null;

    public JaegerRestApiWrapper(JaegerRestClient client) {
        this.client = client;
    }

    private Criteria getCriteria(String operation, Long startTime, Long endTime) {
        return Criteria.builder()
                .service(JaegerQEBase.getJaegerConf().getServiceName())
                .operation(operation)
                .start(startTime)
                .end(endTime)
                .build();
    }

    private void updateCriteria(Criteria criteria) {
        if (criteria.getService() == null) {
            criteria.setService(JaegerQEBase.getJaegerConf().getServiceName());
        }
        if (criteria.getStart() != null) {
            criteria.setStart(criteria.getStart() * 1000L);
        }
        if (criteria.getEnd() != null) {
            criteria.setEnd(criteria.getEnd() * 1000L);
        }
    }

    private void buildSpan(List<QESpan> qeSpans, List<Span> spans) {
        QESpan parent = null;
        if (spans == null || spans.isEmpty()) {
            return;
        }
        for (Span span : spans) {
            Tracer.SpanBuilder qeSpanBuilder = QESpanBuilder.offlineBuilder(span.getOperationName())
                    .withStartTimestamp(span.getStartTime())
                    .asChildOf(parent);
            QESpan qeSpan = ((QESpanBuilder) qeSpanBuilder).build();
            qeSpan.finish(span.getStartTime() + span.getDuration());
            qeSpan.setSpanId(span.getSpanID());
            for (Tag tag : span.getTags()) {
                if (!tag.getKey().equalsIgnoreCase("service")) {
                    if (tag.getType().equalsIgnoreCase("string")) {
                        qeSpan.setTag(tag.getKey(), (String) tag.getValue());
                    } else if (tag.getType().equalsIgnoreCase("bool")) {
                        qeSpan.setTag(tag.getKey(), (Boolean) tag.getValue());
                    } else if (tag.getType().equalsIgnoreCase("int64")) {
                        qeSpan.setTag(tag.getKey(), (Integer) tag.getValue());
                    } else if (tag.getType().equalsIgnoreCase("float64")) {
                        qeSpan.setTag(tag.getKey(), (Float) tag.getValue());
                    } else if (tag.getType().equalsIgnoreCase("binary")) {
                        qeSpan.setTag(tag.getKey(), (Boolean) tag.getValue());
                    }
                }
            }
            qeSpans.add(qeSpan);
        }
        //Update parent spans
        for (Span span : spans) {
            if (span.getReferences() != null && !span.getReferences().isEmpty()) {
                Reference refrance = span.getReferences().get(0);
                if (refrance.getRefType().equalsIgnoreCase("CHILD_OF")) {
                    QESpan qeSpan = getSpanById(qeSpans, span.getSpanID());
                    qeSpan.setParent(getSpanById(qeSpans, refrance.getSpanID()));
                }
            }
        }
    }

    private QESpan getSpanById(List<QESpan> qeSpans, String spanId) {
        for (QESpan qeSpan : qeSpans) {
            if (qeSpan.getSpanId().equals(spanId)) {
                return qeSpan;
            }
        }
        return null;
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
        updateCriteria(criteria);
        List<QESpan> qeSpans = new ArrayList<QESpan>();
        ClientResponse<Result<Trace>> traceResponse = client.trace().list(
                criteria);
        assertResponse(traceResponse);
        if (traceResponse.getEntity().getData() == null
                || traceResponse.getEntity().getData().isEmpty()) {
            return qeSpans;
        }
        List<Span> jaegerSpans = traceResponse.getEntity().getData().get(0).getSpans();
        _logger.debug("{}, Result: {}", criteria, jaegerSpans);
        if (!jaegerSpans.isEmpty()) {
            buildSpan(qeSpans, jaegerSpans);
        }
        return qeSpans;
    }

    public int traceCount(Criteria criteria) {
        updateCriteria(criteria);
        ClientResponse<Result<Trace>> traceResponse = client.trace().list(criteria);
        assertResponse(traceResponse);
        if (traceResponse.getEntity().getData() == null
                || traceResponse.getEntity().getData().isEmpty()) {
            return 0;
        }
        return traceResponse.getEntity().getData().size();
    }

    @Override
    public List<QESpan> listSpan(String operation, Long startTime, Long endTime) {
        return listSpan(getCriteria(operation, startTime, endTime));
    }
}
