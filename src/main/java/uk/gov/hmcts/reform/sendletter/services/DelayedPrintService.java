package uk.gov.hmcts.reform.sendletter.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.sendletter.entity.Letter;
import uk.gov.hmcts.reform.sendletter.entity.LetterRepository;
import uk.gov.hmcts.reform.sendletter.util.CsvWriter;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.stream.Stream;

@Service
public class DelayedPrintService {
    private final LetterRepository letterRepository;

    @Autowired
    public DelayedPrintService(LetterRepository letterRepository) {
        this.letterRepository = letterRepository;
    }

    @Transactional
    public File getDeplayLettersAttachment(LocalDateTime fromCreatedDate,
                                          LocalDateTime toCreatedDate,
                                          int minProcessingHours) throws IOException {
        try (Stream<Letter> deplayedPostedLetter = letterRepository
                .findDeplayedPostedLetter(fromCreatedDate, toCreatedDate, minProcessingHours)) {
            return CsvWriter.writeDelayedPostedLettersToCsv(deplayedPostedLetter);
        }
    }
}
