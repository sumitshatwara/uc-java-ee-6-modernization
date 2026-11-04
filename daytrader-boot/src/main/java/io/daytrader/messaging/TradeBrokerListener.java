/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.daytrader.messaging;

import io.awspring.cloud.sqs.annotation.SqsListener;
import io.awspring.cloud.sqs.listener.SqsHeaders;
import io.daytrader.entity.OrderDataBean;
import io.daytrader.repository.OrderRepository;
import io.daytrader.service.TradeService;
import io.daytrader.util.Log;
import java.util.Optional;
import org.springframework.context.annotation.Profile;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

@Component
@Profile("sqs")
public class TradeBrokerListener {
    private final TradeService tradeService;
    private final OrderRepository orders;

    public TradeBrokerListener(TradeService tradeService, OrderRepository orders) {
        this.tradeService = tradeService;
        this.orders = orders;
    }

    @SqsListener("${daytrader.messaging.broker-queue:daytrader-broker}")
    public void onMessage(OrderMessage message,
                          @Header(name = SqsHeaders.MessageSystemAttributes.SQS_APPROXIMATE_RECEIVE_COUNT,
                                  required = false) String receiveCount) throws Exception {
        if (receiveCount != null && parseReceiveCount(receiveCount) > 1) {
            Log.log("DTBroker3MDB: The following SQS message was redelivered due to a rollback; ignoring: "
                    + message);
            return;
        }
        if (message == null || message.orderID() == null) {
            Log.trace("DTBroker3MDB:onMessage -- received message with null order ID. Message-->" + message);
            return;
        }

        Optional<OrderDataBean> found = orders.findById(message.orderID());
        if (found.isEmpty()) {
            Log.error("DTBroker3MDB:onMessage -- Unable to find Order " + message.orderID());
            return;
        }
        if (!found.get().isOpen()) {
            Log.log("DTBroker3MDB:onMessage -- Order " + message.orderID() + " is already completed; ignoring");
            return;
        }

        try {
            tradeService.completeOrder(message.orderID(), message.twoPhase());
        } catch (Exception exception) {
            Log.error("DTBroker3MDB:onMessage Exception completing order: " + message.orderID() + "\n", exception);
            throw exception;
        }
    }

    private static int parseReceiveCount(String receiveCount) {
        try {
            return Integer.parseInt(receiveCount);
        } catch (NumberFormatException exception) {
            return 1;
        }
    }
}
