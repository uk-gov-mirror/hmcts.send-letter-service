package uk.gov.hmcts.reform.sendletter.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@ExtendWith(MockitoExtension.class)
class AsyncServiceTest {
    private AsyncService service = new AsyncService();
    private AtomicInteger secondCounter = new AtomicInteger(0);

    @Test
    void testSuccess() {
        AtomicInteger counter = new AtomicInteger(0);

        service.run(() -> counter.set(10), () -> secondCounter.set(20));

        assertThat(counter.get()).isEqualTo(10);
        assertThat(secondCounter.get()).isEqualTo(20);
    }

    @Test
    void testException() {
        assertDoesNotThrow(() -> service.run(this::dividByZero, () -> secondCounter.set(30)));
    }

    private void dividByZero() {
        int numerator = 1;
        int denominator = 0;
        int x = numerator / denominator;
    }
}