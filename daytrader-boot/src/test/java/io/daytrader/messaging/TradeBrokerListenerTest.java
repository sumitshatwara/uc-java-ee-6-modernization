package io.daytrader.messaging;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.daytrader.entity.OrderDataBean;
import io.daytrader.repository.OrderRepository;
import io.daytrader.service.TradeService;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TradeBrokerListenerTest {
    private final TradeService tradeService = mock(TradeService.class);
    private final OrderRepository orders = mock(OrderRepository.class);
    private final TradeBrokerListener listener = new TradeBrokerListener(tradeService, orders);

    private OrderDataBean openOrder;

    @BeforeEach
    void createOpenOrder() {
        openOrder = new OrderDataBean();
        openOrder.setOrderID(7);
        openOrder.setOrderStatus("open");
        when(orders.findById(7)).thenReturn(Optional.of(openOrder));
    }

    @Test
    void completesAnOpenOrderUsingItsTwoPhaseFlag() throws Exception {
        listener.onMessage(new OrderMessage(7, true), "1");

        verify(tradeService).completeOrder(7, true);
    }

    @Test
    void ignoresClosedCompletedAndCancelledOrders() throws Exception {
        for (String status : new String[] {"closed", "completed", "cancelled"}) {
            OrderDataBean order = new OrderDataBean();
            order.setOrderID(7);
            order.setOrderStatus(status);
            when(orders.findById(7)).thenReturn(Optional.of(order));
            listener.onMessage(new OrderMessage(7, false), "1");
        }
        verify(tradeService, never()).completeOrder(7, false);
        verify(tradeService, never()).completeOrder(7, true);
    }

    @Test
    void ignoresRedeliveredMessagesBeforeQueryingOrder() throws Exception {
        listener.onMessage(new OrderMessage(7, true), "2");

        verifyNoInteractions(orders, tradeService);
    }

    @Test
    void ignoresMissingOrdersAndNullMessages() throws Exception {
        when(orders.findById(8)).thenReturn(Optional.empty());

        listener.onMessage(new OrderMessage(8, true), "1");
        listener.onMessage(null, "1");
        listener.onMessage(new OrderMessage(null, true), "1");

        verify(tradeService, never()).completeOrder(8, true);
    }

    @Test
    void rethrowsOrderCompletionFailures() {
        when(tradeService.completeOrder(7, true)).thenThrow(new IllegalStateException("failed"));

        assertThrows(IllegalStateException.class, () -> listener.onMessage(new OrderMessage(7, true), "1"));
    }
}
