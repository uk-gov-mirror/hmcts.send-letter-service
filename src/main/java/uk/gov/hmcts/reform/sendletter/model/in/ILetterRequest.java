package uk.gov.hmcts.reform.sendletter.model.in;

import java.util.Map;

public interface ILetterRequest {
    String getType();
    Map<String, Object> getAdditionalData();
}
