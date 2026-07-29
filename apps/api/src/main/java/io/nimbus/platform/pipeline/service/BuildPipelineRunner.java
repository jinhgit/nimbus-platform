package io.nimbus.platform.pipeline.service;

import io.nimbus.platform.pipeline.domain.BuildPipeline;
import io.nimbus.platform.pipeline.domain.PipelineStatus;
import io.nimbus.platform.pipeline.repository.BuildPipelineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * free-only 이미지 빌드 파이프라인 시뮬레이션.
 * 실제 Docker daemon 이 있으면 향후 docker build 단계로 교체 가능.
 */
@Component
public class BuildPipelineRunner {

    private static final Logger log = LoggerFactory.getLogger(BuildPipelineRunner.class);

    private final BuildPipelineRepository pipelineRepository;

    public BuildPipelineRunner(BuildPipelineRepository pipelineRepository) {
        this.pipelineRepository = pipelineRepository;
    }

    @Async
    public void runAsync(UUID pipelineId) {
        try {
            List<Step> steps = List.of(
                    new Step("Checkout source", 10),
                    new Step("Resolve Dockerfile", 20),
                    new Step("docker build (layer cache)", 45),
                    new Step("Tag image", 65),
                    new Step("Push registry (local/sim)", 85),
                    new Step("Publish artifact metadata", 100)
            );
            start(pipelineId);
            for (Step step : steps) {
                Thread.sleep(600);
                update(pipelineId, step.name(), step.progress());
            }
            succeed(pipelineId);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            fail(pipelineId, "Interrupted");
        } catch (Exception e) {
            log.error("Pipeline {} failed", pipelineId, e);
            fail(pipelineId, e.getMessage());
        }
    }

    @Transactional
    protected void start(UUID id) {
        BuildPipeline p = pipelineRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        if (p == null) return;
        p.start();
        pipelineRepository.save(p);
    }

    @Transactional
    protected void update(UUID id, String step, int progress) {
        BuildPipeline p = pipelineRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        if (p == null || p.getStatus() == PipelineStatus.CANCELLED) return;
        p.updateStep(step, progress);
        pipelineRepository.save(p);
    }

    @Transactional
    protected void succeed(UUID id) {
        BuildPipeline p = pipelineRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        if (p == null) return;
        String tag = "nimbus/" + p.getServiceName() + ":1.0.0-" + id.toString().substring(0, 8);
        p.succeed(tag);
        pipelineRepository.save(p);
        log.info("Pipeline {} SUCCESS {}", id, tag);
    }

    @Transactional
    protected void fail(UUID id, String reason) {
        BuildPipeline p = pipelineRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
        if (p == null) return;
        p.fail(reason != null ? reason : "unknown");
        pipelineRepository.save(p);
    }

    private record Step(String name, int progress) {
    }
}
