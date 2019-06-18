package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface LetterRepository extends JpaRepository<Letter, UUID> {

    List<Letter> findFirst3ByStatus(LetterStatus status);

    List<Letter> findByStatus(LetterStatus status);

    @Query("select l from Letter l where l.status not in ('Posted', 'Aborted')"
        + " and l.createdAt < :createdBefore and l.type <> '"
        + UploadLettersTask.SMOKE_TEST_LETTER_TYPE
        + "' order by l.createdAt asc")
    List<BasicLetterInfo> findStaleLetters(
        @Param("createdBefore") LocalDateTime createdBefore
    );

    Optional<Letter> findByChecksumAndStatusOrderByCreatedAtDesc(String checksum, LetterStatus status);

    Optional<Letter> findById(UUID id);

    Optional<Letter> findByIdAndService(UUID id, String service);

    int countByStatus(LetterStatus status);
}
