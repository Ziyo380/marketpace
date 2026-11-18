
package com.marketplace.config;

import java.time.Clock;
import java.util.Optional;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.auditing.DateTimeProvider;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(dateTimeProviderRef = "auditingDateTimeProvider")
public class JpaConfig {

    @Bean
    @ConditionalOnMissingBean(Clock.class)
    public Clock applicationClock() {
        return Clock.systemUTC();
    }

    @Bean
    public DateTimeProvider auditingDateTimeProvider(Clock applicationClock) {
        return () -> Optional.of(applicationClock.instant());
    }
}
