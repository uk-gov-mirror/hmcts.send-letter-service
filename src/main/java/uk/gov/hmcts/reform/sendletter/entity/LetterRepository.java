package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

public interface LetterRepository extends JpaRepository<Letter, UUID> {

    List<Letter> findFirst10ByStatus(LetterStatus status);

    Stream<Letter> findByStatus(LetterStatus status);

    Stream<Letter> findByStatusAndSentToPrintAtBefore(LetterStatus status, Timestamp before);

    Optional<Letter> findByMessageIdAndStatusOrderByCreatedAtDesc(String messageId, LetterStatus status);

    Optional<Letter> findById(UUID id);

    Optional<Letter> findByIdAndService(UUID id, String service);

    int countByStatus(LetterStatus status);
}
