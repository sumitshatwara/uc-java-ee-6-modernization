package io.daytrader.messaging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SqsOrderQueueTest {
    private final SqsTemplate sqsTemplate = mock(SqsTemplate.class);
    private final SqsOrderQueue queue = new SqsOrderQueue(sqsTemplate, "daytrader-broker");

    @AfterEach
    void clearTransactionSynchronization() {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void sendsImmediatelyOutsideTransaction() {
        queue.submit(7, true);

        verify(sqsTemplate).send("daytrader-broker", new OrderMessage(7, true));
    }

    @Test
    void sendsOnlyAfterActiveTransactionCommits() {
        TransactionSynchronizationManager.initSynchronization();
        queue.submit(7, true);
        verifyNoInteractions(sqsTemplate);

        List<TransactionSynchronization> synchronizations =
                TransactionSynchronizationManager.getSynchronizations();
        synchronizations.forEach(TransactionSynchronization::afterCommit);

        verify(sqsTemplate).send("daytrader-broker", new OrderMessage(7, true));
    }
}
