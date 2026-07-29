package io.nimbus.platform.catalog.repository;

import io.nimbus.platform.catalog.domain.RuntimeType;
import io.nimbus.platform.catalog.domain.ServiceTemplate;
import io.nimbus.platform.catalog.domain.TemplateStatus;
import io.nimbus.platform.catalog.domain.TemplateType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ServiceTemplateRepository extends JpaRepository<ServiceTemplate, UUID> {
    Optional<ServiceTemplate> findByIdAndDeletedAtIsNull(UUID id);

    List<ServiceTemplate> findByDeletedAtIsNullOrderByOfficialDescNameAsc();

    List<ServiceTemplate> findByStatusAndDeletedAtIsNullOrderByOfficialDescNameAsc(TemplateStatus status);

    boolean existsByNameAndDeletedAtIsNull(String name);

    List<ServiceTemplate> findByRuntimeAndStatusAndDeletedAtIsNull(RuntimeType runtime, TemplateStatus status);

    List<ServiceTemplate> findByTypeAndStatusAndDeletedAtIsNull(TemplateType type, TemplateStatus status);
}
