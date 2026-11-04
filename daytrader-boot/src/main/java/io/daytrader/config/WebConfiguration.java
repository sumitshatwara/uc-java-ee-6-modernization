package io.daytrader.config;

import org.apache.catalina.valves.rewrite.RewriteValve;
import org.apache.catalina.LifecycleException;
import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import io.daytrader.web.OrdersAlertInterceptor;

@Configuration
public class WebConfiguration implements WebMvcConfigurer {
    private final OrdersAlertInterceptor ordersAlertInterceptor;

    public WebConfiguration(OrdersAlertInterceptor ordersAlertInterceptor) {
        this.ordersAlertInterceptor = ordersAlertInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ordersAlertInterceptor).addPathPatterns("/app");
    }

    @Bean
    WebServerFactoryCustomizer<TomcatServletWebServerFactory> restRewriteValve() {
        return factory -> {
            RewriteValve valve = new RewriteValve() {
                @Override
                protected void startInternal() throws LifecycleException {
                    try {
                        setConfiguration("RewriteRule ^/rest/(.*)$ /daytrader/rest/$1 [L]\n"
                                + "RewriteRule ^/rest$ /daytrader/rest [L]");
                    } catch (Exception exception) {
                        throw new LifecycleException("Unable to configure REST URL rewriting", exception);
                    }
                    super.startInternal();
                }
            };
            factory.addEngineValves(valve);
        };
    }
}
