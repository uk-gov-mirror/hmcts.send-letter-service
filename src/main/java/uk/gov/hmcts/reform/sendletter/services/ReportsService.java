package uk.gov.hmcts.reform.sendletter.services;

import org.apache.commons.lang.NotImplementedException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.sendletter.model.out.LettersCountSummary;

import java.time.LocalDate;
import java.util.List;

@Service
public class ReportsService {

    public List<LettersCountSummary> getCountFor(LocalDate date) {
        throw new NotImplementedException("Not yet implemented");
    }
}
