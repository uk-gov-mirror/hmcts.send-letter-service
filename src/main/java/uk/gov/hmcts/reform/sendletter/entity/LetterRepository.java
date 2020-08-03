package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("checkstyle:LineLength")
public interface LetterRepository extends JpaRepository<Letter, UUID> {

    Optional<Letter> findFirstByStatusOrderByCreatedAtAsc(LetterStatus status);

    List<Letter> findByStatus(LetterStatus status);

    @Query("select new uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo(l.id, l.checksum, l.service, l.status, l.type, l.encryptionKeyFingerprint, l.createdAt, l.sentToPrintAt, l.printedAt)"
        + " from Letter l "
        + " where l.status not in ('Posted', 'Aborted')"
        + " and l.createdAt < :createdBefore"
        + " and l.type <> '" + UploadLettersTask.SMOKE_TEST_LETTER_TYPE + "'"
        + " order by l.createdAt asc")
    List<BasicLetterInfo> findStaleLetters(
        @Param("createdBefore") LocalDateTime createdBefore
    );

    @Query("select new uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo(l.id, l.checksum, l.service, l.status, l.type, l.encryptionKeyFingerprint, l.createdAt, l.sentToPrintAt, l.printedAt)"
        + " from Letter l "
        + " where l.status = 'Created'"
        + " and l.type <> '" + UploadLettersTask.SMOKE_TEST_LETTER_TYPE + "'"
        + " order by l.createdAt asc")
    List<BasicLetterInfo> findPendingLetters();

    @Query("select new uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo(l.id, l.checksum, l.service, l.status, l.type, l.encryptionKeyFingerprint, l.createdAt, l.sentToPrintAt, l.printedAt)"
        + " from Letter l "
        + " where date(l.createdAt) = :createdAt "
        + " and l.type <> '" + UploadLettersTask.SMOKE_TEST_LETTER_TYPE + "'"
        + " order by l.createdAt asc")
    List<BasicLetterInfo> findCreatedAt(@Param("createdAt") LocalDate createdAt);

    Optional<Letter> findByChecksumAndStatusOrderByCreatedAtDesc(String checksum, LetterStatus status);

    Optional<Letter> findById(UUID id);

    @Query("SELECT l.status FROM Letter l WHERE l.id = :id")
    Optional<LetterStatus> findLetterStatus(@Param("id") UUID id);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Letter l SET l.status = 'Posted', l.printedAt = :printedAt, l.fileContent = null WHERE l.id = :id")
    int markLetterAsPosted(@Param("id") UUID id, @Param("printedAt") LocalDateTime printedAt);

    @Modifying(clearAutomatically = true)
    @Query("UPDATE Letter l"
        + " SET l.fileContent = null "
        + " WHERE l.createdAt < :createdBefore "
        + " AND l.status = :status"
    )
    int clearFileContent(
        @Param("createdBefore") LocalDateTime createdBefore,
        @Param("status") LetterStatus status
    );

    int countByStatus(LetterStatus status);
}
