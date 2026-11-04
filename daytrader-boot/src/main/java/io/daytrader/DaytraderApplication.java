package io.daytrader;

import io.daytrader.config.TradeConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(TradeConfig.class)
public class DaytraderApplication {
    public static void main(String[] args) {
        SpringApplication.run(DaytraderApplication.class, args);
    }
}
