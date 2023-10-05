package com.swang.metering;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@SuppressWarnings("DefaultAnnotationParam")
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "_t")
@JsonSubTypes({
        @JsonSubTypes.Type(value = Report.class, name = "report"),
})
public interface JSONSerdeCompatible {

}
