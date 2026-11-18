
package com.marketplace.storage.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;
import org.springframework.validation.annotation.Validated;

import java.time.Duration;
import java.nio.file.Path;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    @NotBlank
    private String endpoint = "http://localhost:9000";

    @NotBlank
    private String publicEndpoint = "http://localhost:9000";

    @NotBlank
    private String region = "us-east-1";

    private Path localDirectory = Path.of(System.getProperty("user.home"), "MakonMarketplace", "uploads");

    @NotBlank
    private String accessKey = "minioadmin";

    @NotBlank
    private String secretKey = "minioadmin";

    @NotBlank
    private String bucket = "marketplace-products";

    private Duration readUrlExpiration = Duration.ofHours(1);

    private DataSize maxFileSize = DataSize.ofMegabytes(5);

    @Min(1)
    private int maxImagesPerProduct = 10;
}
