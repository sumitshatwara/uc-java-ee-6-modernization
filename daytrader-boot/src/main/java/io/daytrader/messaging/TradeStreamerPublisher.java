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

import io.daytrader.entity.QuoteDataBean;
import io.daytrader.util.Log;
import java.math.BigDecimal;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * No-op counterpart to DTStreamer3MDB. The legacy MDB only collected timing statistics for
 * "updateQuote"/"ping"; Boot TradeService does not publish price changes (publish-quote-price-change=false).
 * The target is an SNS topic fanned out to SQS (docs/TARGET_STATE.md), not wired here.
 */
@Component
@Profile("sqs")
public class TradeStreamerPublisher {
    public void publishQuotePriceChange(QuoteDataBean quote, BigDecimal oldPrice, BigDecimal changeFactor,
                                        double sharesTraded) {
        Log.trace("TradeStreamerPublisher: price-change event not published", quote, oldPrice, changeFactor,
                sharesTraded);
    }
}
