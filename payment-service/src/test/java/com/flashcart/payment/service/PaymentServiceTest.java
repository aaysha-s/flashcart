package com.flashcart.payment.service;

import com.flashcart.payment.domain.Payment;
import com.flashcart.payment.domain.PaymentRepository;
import com.flashcart.payment.domain.PaymentStatus;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private final PaymentRepository repository = mock(PaymentRepository.class);
    private final MockPaymentGateway gateway = mock(MockPaymentGateway.class);
    private final PaymentService service =
            new PaymentService(repository, gateway, new SimpleMeterRegistry());

    @Test
    void duplicateRequestReplaysStoredResultWithoutChargingAgain() {
        Payment existing = Payment.completed(5L, 1000);
        when(repository.findByOrderId(5L)).thenReturn(Optional.of(existing));

        Payment result = service.process(5L, 1000);

        assertThat(result).isSameAs(existing);
        verifyNoInteractions(gateway);
        verify(repository, never()).save(any());
    }

    @Test
    void approvedChargeIsStoredAsCompleted() {
        when(repository.findByOrderId(6L)).thenReturn(Optional.empty());
        when(gateway.charge(6L, 1000)).thenReturn(ChargeResult.approved());
        when(repository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        Payment result = service.process(6L, 1000);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.COMPLETED);
        assertThat(result.getAmountPaise()).isEqualTo(1000);
    }

    @Test
    void declinedChargeIsStoredAsFailedWithReason() {
        when(repository.findByOrderId(7L)).thenReturn(Optional.empty());
        when(gateway.charge(7L, 1000)).thenReturn(ChargeResult.declined("Card declined"));
        when(repository.save(any(Payment.class))).thenAnswer(call -> call.getArgument(0));

        Payment result = service.process(7L, 1000);

        assertThat(result.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(result.getFailureReason()).isEqualTo("Card declined");
    }
}
