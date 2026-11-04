package io.daytrader.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.awspring.cloud.sqs.operations.SqsTemplate;
import io.awspring.cloud.sqs.listener.SqsMessageListenerContainer;
import io.daytrader.service.OrderQueue;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.core.env.Environment;

@SpringBootTest
class DefaultProfileHasNoSqsTest {
    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private Environment environment;

    @Test
    void defaultProfileDisablesSqsAndCreatesNoMessagingBeans() {
        assertFalse(Arrays.asList(environment.getActiveProfiles()).contains("sqs"));
        assertFalse(environment.getProperty("spring.cloud.aws.sqs.enabled", Boolean.class, true));
        assertTrue(applicationContext.getBeansOfType(SqsTemplate.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(OrderQueue.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(TradeBrokerListener.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(SqsAsyncClient.class).isEmpty());
        assertTrue(applicationContext.getBeansOfType(SqsMessageListenerContainer.class).isEmpty());
    }
}
