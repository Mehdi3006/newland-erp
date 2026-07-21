package com.newland.erp.masterdata.api;

import com.newland.erp.masterdata.application.MasterDataCommands;
import com.newland.erp.masterdata.application.MasterDataService;
import com.newland.erp.masterdata.domain.MasterDataType;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/master-data")
public final class MasterDataController {
    private final MasterDataService service;

    public MasterDataController(final MasterDataService masterDataService) {
        this.service = masterDataService;
    }

    @GetMapping("/types")
    public List<MasterDataDtos.TypeResponse> types() {
        return Arrays.stream(MasterDataType.values()).map(MasterDataDtos.TypeResponse::from).toList();
    }

    @PostMapping("/{type}")
    @ResponseStatus(HttpStatus.CREATED)
    public MasterDataDtos.MasterDataResponse create(@PathVariable final String type,
                                                    @Valid @RequestBody
                                                    final MasterDataDtos.CreateRequest request) {
        return MasterDataDtos.MasterDataResponse.from(service.create(new MasterDataCommands.Create(
                MasterDataType.fromSlug(type), request.code(), request.name(), request.parentId(),
                request.attributes())));
    }

    @GetMapping("/{type}")
    public List<MasterDataDtos.MasterDataResponse> list(@PathVariable final String type) {
        return service.list(MasterDataType.fromSlug(type)).stream().map(MasterDataDtos.MasterDataResponse::from)
                .toList();
    }

    @GetMapping("/records/{id}")
    public MasterDataDtos.MasterDataResponse get(@PathVariable final UUID id) {
        return MasterDataDtos.MasterDataResponse.from(service.get(id));
    }

    @PutMapping("/records/{id}")
    public MasterDataDtos.MasterDataResponse update(@PathVariable final UUID id,
                                                    @Valid @RequestBody
                                                    final MasterDataDtos.UpdateRequest request) {
        return MasterDataDtos.MasterDataResponse.from(service.update(new MasterDataCommands.Update(id,
                request.name(), request.attributes(), request.expectedVersion())));
    }

    @PostMapping("/records/{id}/activate")
    public MasterDataDtos.MasterDataResponse activate(@PathVariable final UUID id,
                                                      @Valid @RequestBody
                                                      final MasterDataDtos.LifecycleRequest request) {
        return MasterDataDtos.MasterDataResponse.from(service.activate(id, request.expectedVersion()));
    }

    @PostMapping("/records/{id}/deactivate")
    public MasterDataDtos.MasterDataResponse deactivate(@PathVariable final UUID id,
                                                        @Valid @RequestBody
                                                        final MasterDataDtos.LifecycleRequest request) {
        return MasterDataDtos.MasterDataResponse.from(service.deactivate(id, request.expectedVersion()));
    }
}
