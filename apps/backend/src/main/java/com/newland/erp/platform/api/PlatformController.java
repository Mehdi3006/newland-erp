package com.newland.erp.platform.api;

import com.newland.erp.platform.application.PlatformCommands;
import com.newland.erp.platform.application.PlatformService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/platform")
public final class PlatformController {
    private final PlatformService service;

    public PlatformController(final PlatformService platformService) {
        this.service = platformService;
    }

    @PostMapping("/events")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public PlatformDtos.OutboxResponse publishEvent(@Valid @RequestBody
                                                    final PlatformDtos.PublishEventRequest request) {
        return PlatformDtos.OutboxResponse.from(service.publishEvent(new PlatformCommands.PublishEvent(
                request.sourceContext(), request.eventType(), request.aggregateId(), request.payload())));
    }

    @GetMapping("/outbox")
    public List<PlatformDtos.OutboxResponse> pendingOutbox() {
        return service.pendingOutbox(100).stream().map(PlatformDtos.OutboxResponse::from).toList();
    }

    @PostMapping("/audit-records")
    @ResponseStatus(HttpStatus.CREATED)
    public void recordAudit(@Valid @RequestBody final PlatformDtos.RecordAuditRequest request) {
        service.recordAudit(new PlatformCommands.RecordAudit(request.actor(), request.action(), request.targetType(),
                request.targetId(), request.attributes()));
    }

    @PostMapping("/jobs")
    @ResponseStatus(HttpStatus.CREATED)
    public PlatformDtos.JobResponse scheduleJob(@Valid @RequestBody final PlatformDtos.ScheduleJobRequest request) {
        return PlatformDtos.JobResponse.from(service.scheduleJob(new PlatformCommands.ScheduleJob(request.jobType(),
                request.scheduledAt(), request.parameters())));
    }

    @GetMapping("/jobs")
    public List<PlatformDtos.JobResponse> jobs() {
        return service.jobs().stream().map(PlatformDtos.JobResponse::from).toList();
    }

    @PostMapping("/files")
    @ResponseStatus(HttpStatus.CREATED)
    public void registerFile(@Valid @RequestBody final PlatformDtos.RegisterFileRequest request) {
        service.registerFile(new PlatformCommands.RegisterStoredFile(request.fileName(), request.contentType(),
                request.sizeBytes(), request.checksumSha256()));
    }

    @PostMapping("/configuration")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setConfiguration(@Valid @RequestBody final PlatformDtos.SetConfigurationRequest request) {
        service.setConfiguration(new PlatformCommands.SetConfiguration(request.key(), request.value(),
                request.encrypted(), request.actor()));
    }

    @PostMapping("/feature-flags")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setFeatureFlag(@Valid @RequestBody final PlatformDtos.SetFeatureFlagRequest request) {
        service.setFeatureFlag(new PlatformCommands.SetFeatureFlag(request.key(), request.enabled(),
                request.description(), request.actor()));
    }

    @PostMapping("/localization")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void setLocalization(@Valid @RequestBody final PlatformDtos.SetLocalizationRequest request) {
        service.setLocalization(new PlatformCommands.SetLocalization(request.locale(), request.messageKey(),
                request.message()));
    }

    @GetMapping("/error-catalog")
    public List<PlatformDtos.ErrorCatalogResponse> errorCatalog() {
        return service.errorCatalog().stream().map(PlatformDtos.ErrorCatalogResponse::from).toList();
    }

    @GetMapping("/domain-event-catalog")
    public List<PlatformDtos.EventCatalogResponse> domainEventCatalog() {
        return service.domainEventCatalog().stream().map(PlatformDtos.EventCatalogResponse::from).toList();
    }
}
