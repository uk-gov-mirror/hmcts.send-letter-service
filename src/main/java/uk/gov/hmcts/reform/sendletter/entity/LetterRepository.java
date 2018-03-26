package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import javax.persistence.LockModeType;

public interface LetterRepository extends JpaRepository<Letter, UUID> {
    // This lockmode locks the returned rows
    // for both reading and writing.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Stream<Letter> findByState(LetterState state);


    Optional<Letter> findOptionalById(UUID id);

    Optional<Letter> findOptionalByIdAndService(UUID id, String service);
}
