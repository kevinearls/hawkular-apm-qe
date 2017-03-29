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
import lombok.Builder;
import lombok.Data;
import lombok.ToString;

/**
 * @author Jeeva Kandasamy (jkandasa)
 */
@Data
@Builder
@ToString
public class QESpan {
    private Map<String, Object> tags = new HashMap<String, Object>();
    private Long start;
    private Long end;
    private Long duration;
    private String operation;
    private String id;

    public void setTag(String name, String value) {
        this.tags.put(name, value);
    }

    public void setTag(String name, Boolean value) {
        this.tags.put(name, value);
    }

    public void setTag(String name, Number value) {
        this.tags.put(name, value);
    }

    public void setObjectTag(String name, Object value) {
        this.tags.put(name, value);
    }

    public void updateTags(Span span) {
        for (String name : tags.keySet()) {
            if (tags.get(name) instanceof String) {
                span.setTag(name, (String) tags.get(name));
            } else if (tags.get(name) instanceof Number) {
                span.setTag(name, (Number) tags.get(name));
            } else if (tags.get(name) instanceof Boolean) {
                span.setTag(name, (Boolean) tags.get(name));
            } else {
                //Not supported
            }

        }
    }

    public Long getEnd() {
        if (end == null && duration != null) {
            return start + duration;
        }
        return end;
    }

    public Long getDuration() {
        if (start != null && end != null) {
            return end - start;
        }
        return duration;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !QESpan.class.isAssignableFrom(obj.getClass())) {
            return false;
        }
        final QESpan other = (QESpan) obj;
        if (!getOperation().equals(other.getOperation())) {
            return false;
        }
        if (getStart().compareTo(other.getStart()) != 0) {
            return false;
        }
        if (getDuration().compareTo(other.getDuration()) != 0) {
            return false;
        }
        if (!getTags().keySet().equals(other.getTags().keySet())) {
            return false;
        }
        for (String name : getTags().keySet()) {
            if (getTags().get(name) instanceof Number) {
                if (!getTags().get(name).toString().equals(other.getTags().get(name).toString())) {
                    return false;
                }
            } else if (tags.get(name) instanceof Boolean) {
                if (getTags().get(name) != other.getTags().get(name)) {
                    return false;
                }
            } else {
                if (!getTags().get(name).equals(other.getTags().get(name))) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + operation.hashCode();
        return result;
    }

    public static class QESpanBuilder {
        private Map<String, Object> tags = new HashMap<String, Object>();

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
}
