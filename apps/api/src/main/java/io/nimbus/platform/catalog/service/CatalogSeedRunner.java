package io.nimbus.platform.catalog.service;

import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.catalog.domain.ServiceTemplate;
import io.nimbus.platform.catalog.domain.TemplateType;
import io.nimbus.platform.catalog.repository.ServiceTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class CatalogSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(CatalogSeedRunner.class);

    private final ServiceTemplateRepository templateRepository;

    public CatalogSeedRunner(ServiceTemplateRepository templateRepository) {
        this.templateRepository = templateRepository;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (templateRepository.count() > 0) {
            return;
        }
        log.info("Seeding official service catalog templates...");

        templateRepository.save(ServiceTemplate.create(
                "Spring Boot REST API",
                "Golden Path: Spring Boot 3 / Java 21 REST API with Helm + GitHub Actions",
                TemplateType.BACKEND,
                RuntimeType.SPRING_BOOT,
                "Java",
                true,
                """
                        runtime: spring-boot
                        language: java
                        type: rest-api
                        database: postgres
                        cache: redis
                        monitoring: prometheus
                        """,
                """
                        replicaCount: 2
                        image:
                          repository: payment-api
                          tag: "1.0.0"
                        resources:
                          requests:
                            cpu: 250m
                            memory: 512Mi
                          limits:
                            cpu: 500m
                            memory: 1Gi
                        autoscaling:
                          enabled: true
                          minReplicas: 2
                          maxReplicas: 10
                        ingress:
                          enabled: true
                        """,
                """
                        project     = "payment-api"
                        environment = "production"
                        region      = "local"
                        node_count  = 2
                        """,
                """
                        name: ci-cd
                        on:
                          push:
                            branches: [ main ]
                        jobs:
                          build:
                            runs-on: ubuntu-latest
                            steps:
                              - uses: actions/checkout@v4
                              - name: Build
                                run: ./gradlew build
                              - name: Docker
                                run: docker build -t app .
                        """,
                "spring,backend,rest,java,golden-path"
        ));

        templateRepository.save(ServiceTemplate.create(
                "Next.js Frontend",
                "Next.js 15 App Router frontend with static deploy path",
                TemplateType.FRONTEND,
                RuntimeType.NEXTJS,
                "TypeScript",
                true,
                """
                        runtime: nextjs
                        language: typescript
                        type: frontend
                        """,
                """
                        replicaCount: 2
                        image:
                          repository: web
                          tag: "1.0.0"
                        ingress:
                          enabled: true
                        """,
                """
                        project     = "web"
                        environment = "production"
                        """,
                """
                        name: ci
                        on: [push]
                        jobs:
                          build:
                            runs-on: ubuntu-latest
                            steps:
                              - uses: actions/checkout@v4
                              - run: npm ci && npm run build
                        """,
                "nextjs,frontend,typescript"
        ));

        templateRepository.save(ServiceTemplate.create(
                "FastAPI Service",
                "Python FastAPI microservice template",
                TemplateType.BACKEND,
                RuntimeType.FASTAPI,
                "Python",
                true,
                """
                        runtime: fastapi
                        language: python
                        type: rest-api
                        database: postgres
                        """,
                """
                        replicaCount: 2
                        resources:
                          requests:
                            cpu: 100m
                            memory: 256Mi
                        """,
                """
                        project = "fastapi-service"
                        """,
                """
                        name: ci
                        on: [push]
                        jobs:
                          test:
                            runs-on: ubuntu-latest
                            steps:
                              - uses: actions/checkout@v4
                              - run: pip install -r requirements.txt && pytest
                        """,
                "python,fastapi,backend"
        ));

        templateRepository.save(ServiceTemplate.create(
                "NestJS API",
                "Node.js NestJS REST API golden path",
                TemplateType.BACKEND,
                RuntimeType.NESTJS,
                "TypeScript",
                true,
                """
                        runtime: nestjs
                        language: typescript
                        type: rest-api
                        database: postgres
                        cache: redis
                        """,
                """
                        replicaCount: 2
                        """,
                """
                        project = "nestjs-api"
                        """,
                """
                        name: ci
                        on: [push]
                        jobs:
                          build:
                            runs-on: ubuntu-latest
                            steps:
                              - uses: actions/checkout@v4
                              - run: npm ci && npm test && npm run build
                        """,
                "nestjs,node,backend"
        ));

        log.info("Catalog seed complete: {} templates", templateRepository.count());
    }
}
