package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.stereotype.Service;

@Service
public class SasTokenGeneratorService {

    public String generateSasToken(String serviceName) {
        return "dummy";
    }
}
