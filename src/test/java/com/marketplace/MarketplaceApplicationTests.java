
package com.marketplace;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
@ActiveProfiles("test")
class MarketplaceApplicationTests {

    @Test
    void contextLoads() {
    }
}
