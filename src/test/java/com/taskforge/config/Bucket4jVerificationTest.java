package com.taskforge.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

@SpringBootTest
@TestPropertySource(properties = {
        "bucket4j.enabled=true",
        "bucket4j.cache-to-use=redis-jedis",
        "spring.data.redis.client-type=jedis"
})
class Bucket4jVerificationTest {

    @Test
    void contextLoads() {
        // verified that context loads with bucket4j enabled and redis-jedis configured
    }
}
