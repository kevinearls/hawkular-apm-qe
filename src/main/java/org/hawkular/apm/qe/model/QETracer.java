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

import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.propagation.Format;
import org.jboss.resteasy.spi.NotImplementedYetException;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
public class QETracer  implements Tracer {
    Tracer tracer = null;

    public QETracer(Tracer tracer) {
        this.tracer = tracer;
    }

    public QESpanBuilder buildSpan(String operation) {
        return new QESpanBuilder(this.tracer, operation);
    }

    @Override
    public <C> void inject(SpanContext spanContext, Format<C> format, C c) {
        throw new NotImplementedYetException();
    }

    @Override
    public <C> SpanContext extract(Format<C> format, C c) {
        throw new NotImplementedYetException();
    }
}