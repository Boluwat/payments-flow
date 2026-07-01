package com.structure.payment.payment.model;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

import java.time.Instant;

@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY)
public class SagaStep {
    private String  name;
    private String  status;
    private Object  data;    // holdId, settlementId, error message, etc.


//    @JsonSerialize(using = InstantSerializer.class)
//    @JsonDeserialize(using = InstantDeserializer.class)
    private Instant at;

}
