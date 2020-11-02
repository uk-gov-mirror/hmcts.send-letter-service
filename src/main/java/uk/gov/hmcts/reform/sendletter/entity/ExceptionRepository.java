package uk.gov.hmcts.reform.sendletter.entity;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ExceptionRepository extends JpaRepository<ExceptionLetter, UUID> {
}
