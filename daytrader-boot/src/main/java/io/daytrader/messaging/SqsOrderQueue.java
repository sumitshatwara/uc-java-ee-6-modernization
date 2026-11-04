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

import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.daytrader.service.OrderQueue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Component
@Profile("sqs")
public class SqsOrderQueue implements OrderQueue {
    private final SqsTemplate sqsTemplate;
    private final String queue;

    public SqsOrderQueue(SqsTemplate sqsTemplate,
                         @Value("${daytrader.messaging.broker-queue:daytrader-broker}") String queue) {
        this.sqsTemplate = sqsTemplate;
        this.queue = queue;
    }

    @Override
    public void submit(Integer orderID, boolean twoPhase) {
        OrderMessage message = new OrderMessage(orderID, twoPhase);
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    send(message);
                }
            });
        } else {
            send(message);
        }
    }

    private void send(OrderMessage message) {
        sqsTemplate.send(queue, message);
    }
}
