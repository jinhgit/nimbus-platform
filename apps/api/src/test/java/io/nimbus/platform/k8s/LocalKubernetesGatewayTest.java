package io.nimbus.platform.k8s;

import io.nimbus.platform.k8s.client.LocalKubernetesGateway;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LocalKubernetesGatewayTest {

    @Test
    void sanitizeK8sName() {
        assertThat(LocalKubernetesGateway.sanitizeK8sName("payment-api"))
                .isEqualTo("payment-api");
        assertThat(LocalKubernetesGateway.sanitizeK8sName("Payment_API!"))
                .isEqualTo("payment-api");
        assertThat(LocalKubernetesGateway.sanitizeK8sName("a".repeat(100)).length())
                .isLessThanOrEqualTo(63);
    }
}
