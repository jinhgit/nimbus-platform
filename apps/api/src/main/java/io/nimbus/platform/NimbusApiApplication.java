package io.nimbus.platform;

import io.nimbus.platform.auth.security.GithubProperties;
import io.nimbus.platform.auth.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, GithubProperties.class})
public class NimbusApiApplication {

    public static void main(String[] args) {
        SpringApplication.run(NimbusApiApplication.class, args);
    }
}
