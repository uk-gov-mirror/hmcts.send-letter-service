package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class ExecusionServiceTest {
    private ExecusionService service = new ExecusionService();
    private AtomicInteger secondCounter = new AtomicInteger(0);

    @Test
    void testSuccess() {
        AtomicInteger counter = new AtomicInteger(0);
        service.run(() -> counter.set(10), () -> secondCounter.set(20),
            () -> {
                throw new RuntimeException("This should not be invoked"); },
            data -> Collections.emptyMap());

        assertThat(counter.get()).isEqualTo(10);
        assertThat(secondCounter.get()).isEqualTo(20);
    }

    @Test
    void testException() {
        StringBuilder consumer = new StringBuilder("This is consumer ");
        assertDoesNotThrow(() -> service.run(() -> {
            throw new RuntimeException("Error"); },
            () -> secondCounter.set(20),
            () -> {
                throw new RuntimeException("This should not be invoked"); },
            data -> consumer.append(data)));
        String result = consumer.toString();
        assertThat(result).isEqualTo("This is consumer Error");
    }

    @Test
    void shouldExecutedData() {
        AtomicInteger counter = new AtomicInteger(0);
        service.run(() -> {
            throw new DataIntegrityViolationException("Error"); },
            () -> secondCounter.set(20),
            counter::incrementAndGet,
            data -> Collections.emptyMap());

        assertThat(counter.get()).isEqualTo(1);
        assertThat(secondCounter.get()).isEqualTo(20);
    }
}