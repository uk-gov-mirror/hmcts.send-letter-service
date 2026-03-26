package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import uk.gov.hmcts.reform.sendletter.tasks.UploadLettersTask;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

/**
 * Letter repository.
 */
@SuppressWarnings("checkstyle:LineLength")
public interface LetterRepository extends JpaRepository<Letter, UUID> {

    /**
     * Find the first letter that was created before a given date.
     *
     * @param createdBefore the date
     * @return the letter
     */
    @Query(value = "select * from letters l "
        + " where l.status = 'Created' "
        + " and l.created_at <= :createdBefore order by l.created_at asc limit 1", nativeQuery = true)
    Optional<Letter> findFirstLetterCreated(@Param("createdBefore") LocalDateTime createdBefore);

    /**
     * Find letters by status.
     *
     * @param status the status
     * @return the letters
     */
    List<Letter> findByStatus(LetterStatus status);

    /**
     * Find stale letters.
     * @param createdBefore the date
     * @return BasicLetterInfo letters
     */
    @Query("select new uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo(l.id, l.checksum, l.service, l.status, l.type, l.encryptionKeyFingerprint, l.createdAt, l.sentToPrintAt, l.printedAt)"
        + " from Letter l "
        + " where l.status in ('Created', 'Uploaded', 'FailedToUpload')"
        + " and l.createdAt < :createdBefore"
        + " and l.type <> '" + UploadLettersTask.SMOKE_TEST_LETTER_TYPE + "'"
        + " order by l.createdAt asc")
    List<BasicLetterInfo> findStaleLetters(
        @Param("createdBefore") LocalDateTime createdBefore
    );

    /**
     * Find by status in and created before, ordered by created at ascending.
     *
     * @param statuses the letter statuses
     * @param createdAtBefore the created before date
     * @return the {@link List} of found {@link BasicLetterInfo}s
     */
    List<BasicLetterInfo> findByStatusInAndCreatedAtBeforeOrderByCreatedAtAsc(Collection<LetterStatus> statuses, LocalDateTime createdAtBefore);

    /**
     * Find by status in and created before where sentToPrintAt is not null, ordered by created at
     * ascending.
     *
     * @param statuses the letter statuses
     * @param createdAtBefore the created before date
     * @return the {@link List} of found {@link BasicLetterInfo}s
     */
    List<BasicLetterInfo> findByStatusInAndCreatedAtBeforeAndSentToPrintAtNotNullOrderByCreatedAtAsc(
        Collection<LetterStatus> statuses, LocalDateTime createdAtBefore);

    /**
     * Find by status not in and type not and created at between order by created at asc.
     * @param letterStatuses the letter statuses
     * @param type the type
     * @param from the from
     * @param to localDateTime to
     * @return BasicLetterInfo letters
     */
    Stream<BasicLetterInfo> findByStatusNotInAndTypeNotAndCreatedAtBetweenOrderByCreatedAtAsc(Collection<LetterStatus> letterStatuses,
                                                               String type, LocalDateTime from, LocalDateTime to);

    /**
     * Find pending letters.
     * @return BasicLetterInfo letters
     */
    @Query("select new uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo(l.id, l.checksum, l.service, l.status, l.type, l.encryptionKeyFingerprint, l.createdAt, l.sentToPrintAt, l.printedAt)"
        + " from Letter l "
        + " where l.status = 'Created'"
        + " and l.type <> '" + UploadLettersTask.SMOKE_TEST_LETTER_TYPE + "'"
        + " order by l.createdAt asc")
    List<BasicLetterInfo> findPendingLetters();

    /**
     * Find by created at before and status and type not.
     * @param createdBefore the created before date
     * @param status the status
     * @param type the type
     * @return BasicLetterInfo letters
     */
    Stream<BasicLetterInfo> findByCreatedAtBeforeAndStatusAndTypeNot(LocalDateTime createdBefore, LetterStatus status, String type);

    /**
     * Find by created at.
     * @param createdAt the created at date
     * @return BasicLetterInfo letters
     */
    @Query("select new uk.gov.hmcts.reform.sendletter.entity.BasicLetterInfo(l.id, l.checksum, l.service, l.status, l.type, l.encryptionKeyFingerprint, l.createdAt, l.sentToPrintAt, l.printedAt)"
        + " from Letter l "
        + " where date(l.createdAt) = :createdAt "
        + " and l.type <> '" + UploadLettersTask.SMOKE_TEST_LETTER_TYPE + "'"
        + " order by l.createdAt asc")
    List<BasicLetterInfo> findCreatedAt(@Param("createdAt") LocalDate createdAt);

    /**
     * Find by checksum and status order by created at desc.
     * @param checksum the checksum
     * @param status the status
     * @return Letter
     */
    Optional<Letter> findByChecksumAndStatusOrderByCreatedAtDesc(String checksum, LetterStatus status);

    /**
     * Find by id.
     * @param id the id
     * @return Letter
     */
    Optional<Letter> findById(UUID id);

    /**
     * Find letter status.
     * @param id the id
     * @return LetterStatus optional
     */
    @Query("SELECT l.status FROM Letter l WHERE l.id = :id")
    Optional<LetterStatus> findLetterStatus(@Param("id") UUID id);

    /**
     * Mark letter as printed.
     * @param id the id
     * @param printedAt the printed at
     * @return int number of updated records
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Letter l SET l.status = 'Posted', l.printedAt = :printedAt, l.fileContent = null WHERE l.id = :id")
    int markLetterAsPosted(@Param("id") UUID id, @Param("printedAt") LocalDateTime printedAt);

    /**
     * Clear file content.
     * @param createdBefore the created before
     * @param status the status
     * @return int number of updated records
     */
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

    /**
     * Count by status.
     * @param status the status
     * @return int number of records
     */
    int countByStatus(LetterStatus status);

    /**
     * Find by status and created at between order by created at asc.
     * @param status the status
     * @param from the localDataTime from
     * @param to the localDataTime to
     * @return BasicLetterInfo letters
     */
    Stream<BasicLetterInfo> findByStatusAndCreatedAtBetweenOrderByCreatedAtAsc(LetterStatus status, LocalDateTime from, LocalDateTime to);

    /**
     * Mark stale letter as not sent.
     * @param id the id
     * @return int number of updated records
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Letter l"
            + " SET l.status = 'NotSent'"
            + " WHERE l.id = :id AND l.status = 'Uploaded'"
    )
    int markStaleLetterAsNotSent(@Param("id") UUID id);

    /**
     * Mark letter as created.
     * @param id the id
     * @return int number of updated records
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Letter l"
        + " SET l.status = 'Created'"
        + " WHERE l.id = :id AND l.status in ('Uploaded', 'FailedToUpload')"
    )
    int markLetterAsCreated(@Param("id") UUID id);

    /**
     * Mark letter as aborted.
     * @param id the id
     * @return int number of updated records
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Letter l"
        + " SET l.status = 'Aborted'"
        + " WHERE l.id = :id"
    )
    int markLetterAsAborted(@Param("id") UUID id);

    /**
     * Mark letter as no report aborted.
     * @param id the id
     * @return int number of updated records
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Letter l"
        + " SET l.status = uk.gov.hmcts.reform.sendletter.entity.LetterStatus.NoReportAborted"
        + " WHERE l.id = :id"
    )
    int markLetterAsNoReportAborted(UUID id);

    /**
     * Mark letter as posted locally.
     * @param id the id
     * @return int number of updated records
     */
    @Modifying(clearAutomatically = true)
    @Query("UPDATE Letter l"
        + " SET l.status = 'PostedLocally'"
        + " WHERE l.id = :id AND l.status in ('Uploaded', 'Posted')"
    )
    int markLetterAsPostedLocally(@Param("id") UUID id);

    /**
     * Find the related service for a letter.
     *
     * @param id the letter id
     * @return the service
     */
    @Query("SELECT l.service FROM Letter l WHERE l.id = :id")
    Optional<String> findLetterService(UUID id);
}
