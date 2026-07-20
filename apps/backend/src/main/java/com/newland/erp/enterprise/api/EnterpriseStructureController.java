package com.newland.erp.enterprise.api;

import com.newland.erp.enterprise.application.EnterpriseStructureCommands;
import com.newland.erp.enterprise.application.EnterpriseStructureService;
import com.newland.erp.enterprise.application.RequestMetadata;
import com.newland.erp.enterprise.domain.Address;
import com.newland.erp.enterprise.domain.BranchCode;
import com.newland.erp.enterprise.domain.CompanyCode;
import com.newland.erp.enterprise.domain.CountryCode;
import com.newland.erp.enterprise.domain.CurrencyCode;
import com.newland.erp.enterprise.domain.DisplayName;
import com.newland.erp.enterprise.domain.EnterpriseCode;
import com.newland.erp.enterprise.domain.LegalEntityCode;
import com.newland.erp.enterprise.domain.LocalizedName;
import com.newland.erp.enterprise.domain.LocationCode;
import com.newland.erp.enterprise.domain.TimeZoneId;
import com.newland.erp.enterprise.domain.WarehouseCode;
import com.newland.erp.enterprise.domain.WarehouseType;
import com.newland.erp.enterprise.domain.ZoneCode;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api/v1/enterprise-structure")
public final class EnterpriseStructureController {
    private static final String ACTOR_HEADER = "X-Newland-Actor";
    private static final String CORRELATION_HEADER = "X-Correlation-Id";

    private final EnterpriseStructureService service;

    public EnterpriseStructureController(final EnterpriseStructureService enterpriseStructureService) {
        this.service = enterpriseStructureService;
    }

    @PostMapping("/enterprises")
    @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseStructureDtos.EnterpriseResponse createEnterprise(
            @Valid @RequestBody final EnterpriseStructureDtos.CreateEnterpriseRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.CreateEnterprise command = new EnterpriseStructureCommands.CreateEnterprise(
                new EnterpriseCode(request.code()),
                new DisplayName(request.name()),
                localized(request.localizedName())
        );
        return EnterpriseStructureDtos.EnterpriseResponse.from(service.createEnterprise(command,
                metadata(actor, correlationId)));
    }

    @GetMapping("/enterprises")
    public List<EnterpriseStructureDtos.EnterpriseResponse> listEnterprises() {
        return service.listEnterprises().stream().map(EnterpriseStructureDtos.EnterpriseResponse::from).toList();
    }

    @GetMapping("/enterprises/{enterpriseId}")
    public EnterpriseStructureDtos.EnterpriseResponse getEnterprise(@PathVariable final UUID enterpriseId) {
        return EnterpriseStructureDtos.EnterpriseResponse.from(service.getEnterprise(enterpriseId));
    }

    @PutMapping("/enterprises/{enterpriseId}")
    public EnterpriseStructureDtos.EnterpriseResponse updateEnterprise(
            @PathVariable final UUID enterpriseId,
            @Valid @RequestBody final EnterpriseStructureDtos.UpdateEnterpriseRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.UpdateEnterprise command = new EnterpriseStructureCommands.UpdateEnterprise(
                enterpriseId,
                new DisplayName(request.name()),
                localized(request.localizedName()),
                request.expectedVersion()
        );
        return EnterpriseStructureDtos.EnterpriseResponse.from(service.updateEnterprise(command,
                metadata(actor, correlationId)));
    }

    @PostMapping("/enterprises/{enterpriseId}/activate")
    public EnterpriseStructureDtos.EnterpriseResponse activateEnterprise(
            @PathVariable final UUID enterpriseId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.EnterpriseResponse.from(service.activateEnterprise(enterpriseId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/enterprises/{enterpriseId}/deactivate")
    public EnterpriseStructureDtos.EnterpriseResponse deactivateEnterprise(
            @PathVariable final UUID enterpriseId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.EnterpriseResponse.from(service.deactivateEnterprise(enterpriseId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/enterprises/{enterpriseId}/legal-entities")
    @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseStructureDtos.LegalEntityResponse createLegalEntity(
            @PathVariable final UUID enterpriseId,
            @Valid @RequestBody final EnterpriseStructureDtos.CreateLegalEntityRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.CreateLegalEntity command =
                new EnterpriseStructureCommands.CreateLegalEntity(enterpriseId, new LegalEntityCode(request.code()),
                        new DisplayName(request.name()), localized(request.localizedName()),
                        new CountryCode(request.countryCode()), new CurrencyCode(request.baseCurrency()));
        return EnterpriseStructureDtos.LegalEntityResponse.from(service.createLegalEntity(command,
                metadata(actor, correlationId)));
    }

    @GetMapping("/enterprises/{enterpriseId}/legal-entities")
    public List<EnterpriseStructureDtos.LegalEntityResponse> listLegalEntities(
            @PathVariable final UUID enterpriseId
    ) {
        return service.listLegalEntitiesByEnterprise(enterpriseId).stream()
                .map(EnterpriseStructureDtos.LegalEntityResponse::from)
                .toList();
    }

    @GetMapping("/legal-entities/{legalEntityId}")
    public EnterpriseStructureDtos.LegalEntityResponse getLegalEntity(
            @PathVariable final UUID legalEntityId
    ) {
        return EnterpriseStructureDtos.LegalEntityResponse.from(service.getLegalEntity(legalEntityId));
    }

    @PutMapping("/legal-entities/{legalEntityId}")
    public EnterpriseStructureDtos.LegalEntityResponse updateLegalEntity(
            @PathVariable final UUID legalEntityId,
            @Valid @RequestBody final EnterpriseStructureDtos.UpdateLegalEntityRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.UpdateLegalEntity command =
                new EnterpriseStructureCommands.UpdateLegalEntity(legalEntityId, new DisplayName(request.name()),
                        localized(request.localizedName()), new CountryCode(request.countryCode()),
                        new CurrencyCode(request.baseCurrency()), request.expectedVersion());
        return EnterpriseStructureDtos.LegalEntityResponse.from(service.updateLegalEntity(command,
                metadata(actor, correlationId)));
    }

    @PostMapping("/legal-entities/{legalEntityId}/activate")
    public EnterpriseStructureDtos.LegalEntityResponse activateLegalEntity(
            @PathVariable final UUID legalEntityId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.LegalEntityResponse.from(service.activateLegalEntity(legalEntityId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/legal-entities/{legalEntityId}/deactivate")
    public EnterpriseStructureDtos.LegalEntityResponse deactivateLegalEntity(
            @PathVariable final UUID legalEntityId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.LegalEntityResponse.from(service.deactivateLegalEntity(legalEntityId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/legal-entities/{legalEntityId}/companies")
    @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseStructureDtos.CompanyResponse createCompany(
            @PathVariable final UUID legalEntityId,
            @Valid @RequestBody final EnterpriseStructureDtos.CreateCompanyRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.CreateCompany command = new EnterpriseStructureCommands.CreateCompany(
                legalEntityId,
                new CompanyCode(request.code()),
                new DisplayName(request.name()),
                localized(request.localizedName()),
                new CountryCode(request.countryCode()),
                new CurrencyCode(request.baseCurrency()),
                new TimeZoneId(request.timeZoneId()),
                address(request.address())
        );
        return EnterpriseStructureDtos.CompanyResponse.from(service.createCompany(command,
                metadata(actor, correlationId)));
    }

    @GetMapping("/legal-entities/{legalEntityId}/companies")
    public List<EnterpriseStructureDtos.CompanyResponse> listCompanies(@PathVariable final UUID legalEntityId) {
        return service.listCompaniesByLegalEntity(legalEntityId).stream()
                .map(EnterpriseStructureDtos.CompanyResponse::from)
                .toList();
    }

    @GetMapping("/companies/{companyId}")
    public EnterpriseStructureDtos.CompanyResponse getCompany(@PathVariable final UUID companyId) {
        return EnterpriseStructureDtos.CompanyResponse.from(service.getCompany(companyId));
    }

    @PutMapping("/companies/{companyId}")
    public EnterpriseStructureDtos.CompanyResponse updateCompany(
            @PathVariable final UUID companyId,
            @Valid @RequestBody final EnterpriseStructureDtos.UpdateCompanyRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.UpdateCompany command = new EnterpriseStructureCommands.UpdateCompany(
                companyId, new DisplayName(request.name()), localized(request.localizedName()),
                new CountryCode(request.countryCode()), new CurrencyCode(request.baseCurrency()),
                new TimeZoneId(request.timeZoneId()), address(request.address()), request.expectedVersion());
        return EnterpriseStructureDtos.CompanyResponse.from(service.updateCompany(command,
                metadata(actor, correlationId)));
    }

    @PostMapping("/companies/{companyId}/activate")
    public EnterpriseStructureDtos.CompanyResponse activateCompany(
            @PathVariable final UUID companyId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.CompanyResponse.from(service.activateCompany(companyId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/companies/{companyId}/deactivate")
    public EnterpriseStructureDtos.CompanyResponse deactivateCompany(
            @PathVariable final UUID companyId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.CompanyResponse.from(service.deactivateCompany(companyId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/companies/{companyId}/branches")
    @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseStructureDtos.BranchResponse createBranch(
            @PathVariable final UUID companyId,
            @Valid @RequestBody final EnterpriseStructureDtos.CreateBranchRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.CreateBranch command = new EnterpriseStructureCommands.CreateBranch(
                companyId, new BranchCode(request.code()), new DisplayName(request.name()),
                localized(request.localizedName()), address(request.address()));
        return EnterpriseStructureDtos.BranchResponse.from(service.createBranch(command,
                metadata(actor, correlationId)));
    }

    @GetMapping("/companies/{companyId}/branches")
    public List<EnterpriseStructureDtos.BranchResponse> listBranches(@PathVariable final UUID companyId) {
        return service.listBranchesByCompany(companyId).stream()
                .map(EnterpriseStructureDtos.BranchResponse::from)
                .toList();
    }

    @GetMapping("/branches/{branchId}")
    public EnterpriseStructureDtos.BranchResponse getBranch(@PathVariable final UUID branchId) {
        return EnterpriseStructureDtos.BranchResponse.from(service.getBranch(branchId));
    }

    @PutMapping("/branches/{branchId}")
    public EnterpriseStructureDtos.BranchResponse updateBranch(
            @PathVariable final UUID branchId,
            @Valid @RequestBody final EnterpriseStructureDtos.UpdateBranchRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.UpdateBranch command = new EnterpriseStructureCommands.UpdateBranch(
                branchId, new DisplayName(request.name()), localized(request.localizedName()),
                address(request.address()), request.expectedVersion());
        return EnterpriseStructureDtos.BranchResponse.from(service.updateBranch(command,
                metadata(actor, correlationId)));
    }

    @PostMapping("/branches/{branchId}/activate")
    public EnterpriseStructureDtos.BranchResponse activateBranch(
            @PathVariable final UUID branchId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.BranchResponse.from(service.activateBranch(branchId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/branches/{branchId}/deactivate")
    public EnterpriseStructureDtos.BranchResponse deactivateBranch(
            @PathVariable final UUID branchId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.BranchResponse.from(service.deactivateBranch(branchId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/companies/{companyId}/warehouses")
    @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseStructureDtos.WarehouseResponse createWarehouse(
            @PathVariable final UUID companyId,
            @Valid @RequestBody final EnterpriseStructureDtos.CreateWarehouseRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.CreateWarehouse command = new EnterpriseStructureCommands.CreateWarehouse(
                companyId, request.branchId(), new WarehouseCode(request.code()), new DisplayName(request.name()),
                localized(request.localizedName()), warehouseType(request.type()), request.projectReference(),
                address(request.address()));
        return EnterpriseStructureDtos.WarehouseResponse.from(service.createWarehouse(command,
                metadata(actor, correlationId)));
    }

    @GetMapping("/companies/{companyId}/warehouses")
    public List<EnterpriseStructureDtos.WarehouseResponse> listCompanyWarehouses(
            @PathVariable final UUID companyId
    ) {
        return service.listWarehousesByCompany(companyId).stream()
                .map(EnterpriseStructureDtos.WarehouseResponse::from)
                .toList();
    }

    @GetMapping("/branches/{branchId}/warehouses")
    public List<EnterpriseStructureDtos.WarehouseResponse> listBranchWarehouses(@PathVariable final UUID branchId) {
        return service.listWarehousesByBranch(branchId).stream()
                .map(EnterpriseStructureDtos.WarehouseResponse::from)
                .toList();
    }

    @GetMapping("/warehouses/{warehouseId}")
    public EnterpriseStructureDtos.WarehouseResponse getWarehouse(@PathVariable final UUID warehouseId) {
        return EnterpriseStructureDtos.WarehouseResponse.from(service.getWarehouse(warehouseId));
    }

    @PutMapping("/warehouses/{warehouseId}")
    public EnterpriseStructureDtos.WarehouseResponse updateWarehouse(
            @PathVariable final UUID warehouseId,
            @Valid @RequestBody final EnterpriseStructureDtos.UpdateWarehouseRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.UpdateWarehouse command = new EnterpriseStructureCommands.UpdateWarehouse(
                warehouseId, new DisplayName(request.name()), localized(request.localizedName()),
                warehouseType(request.type()), request.projectReference(), address(request.address()),
                request.expectedVersion());
        return EnterpriseStructureDtos.WarehouseResponse.from(service.updateWarehouse(command,
                metadata(actor, correlationId)));
    }

    @PostMapping("/warehouses/{warehouseId}/activate")
    public EnterpriseStructureDtos.WarehouseResponse activateWarehouse(
            @PathVariable final UUID warehouseId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.WarehouseResponse.from(service.activateWarehouse(warehouseId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/warehouses/{warehouseId}/deactivate")
    public EnterpriseStructureDtos.WarehouseResponse deactivateWarehouse(
            @PathVariable final UUID warehouseId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.WarehouseResponse.from(service.deactivateWarehouse(warehouseId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/warehouses/{warehouseId}/zones")
    @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseStructureDtos.ZoneResponse createZone(
            @PathVariable final UUID warehouseId,
            @Valid @RequestBody final EnterpriseStructureDtos.CreateZoneRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.CreateZone command = new EnterpriseStructureCommands.CreateZone(
                warehouseId, new ZoneCode(request.code()), new DisplayName(request.name()),
                localized(request.localizedName()));
        return EnterpriseStructureDtos.ZoneResponse.from(service.createZone(command, metadata(actor, correlationId)));
    }

    @GetMapping("/warehouses/{warehouseId}/zones")
    public List<EnterpriseStructureDtos.ZoneResponse> listZones(@PathVariable final UUID warehouseId) {
        return service.listZonesByWarehouse(warehouseId).stream()
                .map(EnterpriseStructureDtos.ZoneResponse::from)
                .toList();
    }

    @GetMapping("/zones/{zoneId}")
    public EnterpriseStructureDtos.ZoneResponse getZone(@PathVariable final UUID zoneId) {
        return EnterpriseStructureDtos.ZoneResponse.from(service.getZone(zoneId));
    }

    @PutMapping("/zones/{zoneId}")
    public EnterpriseStructureDtos.ZoneResponse updateZone(
            @PathVariable final UUID zoneId,
            @Valid @RequestBody final EnterpriseStructureDtos.UpdateZoneRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.UpdateZone command = new EnterpriseStructureCommands.UpdateZone(
                zoneId, new DisplayName(request.name()), localized(request.localizedName()),
                request.expectedVersion());
        return EnterpriseStructureDtos.ZoneResponse.from(service.updateZone(command, metadata(actor, correlationId)));
    }

    @PostMapping("/zones/{zoneId}/activate")
    public EnterpriseStructureDtos.ZoneResponse activateZone(
            @PathVariable final UUID zoneId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.ZoneResponse.from(service.activateZone(zoneId, metadata(actor, correlationId)));
    }

    @PostMapping("/zones/{zoneId}/deactivate")
    public EnterpriseStructureDtos.ZoneResponse deactivateZone(
            @PathVariable final UUID zoneId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.ZoneResponse.from(service.deactivateZone(zoneId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/zones/{zoneId}/locations")
    @ResponseStatus(HttpStatus.CREATED)
    public EnterpriseStructureDtos.LocationResponse createLocation(
            @PathVariable final UUID zoneId,
            @Valid @RequestBody final EnterpriseStructureDtos.CreateLocationRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.CreateLocation command = new EnterpriseStructureCommands.CreateLocation(
                zoneId, new LocationCode(request.code()), new DisplayName(request.name()),
                localized(request.localizedName()));
        return EnterpriseStructureDtos.LocationResponse.from(service.createLocation(command,
                metadata(actor, correlationId)));
    }

    @GetMapping("/zones/{zoneId}/locations")
    public List<EnterpriseStructureDtos.LocationResponse> listLocations(@PathVariable final UUID zoneId) {
        return service.listLocationsByZone(zoneId).stream()
                .map(EnterpriseStructureDtos.LocationResponse::from)
                .toList();
    }

    @GetMapping("/locations/{locationId}")
    public EnterpriseStructureDtos.LocationResponse getLocation(@PathVariable final UUID locationId) {
        return EnterpriseStructureDtos.LocationResponse.from(service.getLocation(locationId));
    }

    @PutMapping("/locations/{locationId}")
    public EnterpriseStructureDtos.LocationResponse updateLocation(
            @PathVariable final UUID locationId,
            @Valid @RequestBody final EnterpriseStructureDtos.UpdateLocationRequest request,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        final EnterpriseStructureCommands.UpdateLocation command = new EnterpriseStructureCommands.UpdateLocation(
                locationId, new DisplayName(request.name()), localized(request.localizedName()),
                request.expectedVersion());
        return EnterpriseStructureDtos.LocationResponse.from(service.updateLocation(command,
                metadata(actor, correlationId)));
    }

    @PostMapping("/locations/{locationId}/activate")
    public EnterpriseStructureDtos.LocationResponse activateLocation(
            @PathVariable final UUID locationId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.LocationResponse.from(service.activateLocation(locationId,
                metadata(actor, correlationId)));
    }

    @PostMapping("/locations/{locationId}/deactivate")
    public EnterpriseStructureDtos.LocationResponse deactivateLocation(
            @PathVariable final UUID locationId,
            @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor,
            @RequestHeader(name = CORRELATION_HEADER, required = false) final String correlationId
    ) {
        return EnterpriseStructureDtos.LocationResponse.from(service.deactivateLocation(locationId,
                metadata(actor, correlationId)));
    }

    private static RequestMetadata metadata(final String actor, final String correlationId) {
        return new RequestMetadata(actor, correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID() : UUID.fromString(correlationId));
    }

    private static LocalizedName localized(final Map<String, String> values) {
        return new LocalizedName(values);
    }

    private static Address address(final EnterpriseStructureDtos.AddressPayload payload) {
        if (payload == null) {
            return null;
        }
        return new Address(payload.line1(), payload.line2(), payload.city(), payload.region(), payload.postalCode());
    }

    private static WarehouseType warehouseType(final String value) {
        try {
            return WarehouseType.valueOf(value.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("warehouse type must be CENTRAL, BRANCH, or PROJECT.", exception);
        }
    }
}
