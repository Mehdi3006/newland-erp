package com.newland.erp.enterprise.api;

import com.newland.erp.enterprise.application.AuditPort;
import com.newland.erp.enterprise.application.AuthorizationPort;
import com.newland.erp.enterprise.application.EnterpriseStructureRepository;
import com.newland.erp.enterprise.application.EnterpriseStructureService;
import com.newland.erp.enterprise.domain.AuditMetadata;
import com.newland.erp.enterprise.domain.DisplayName;
import com.newland.erp.enterprise.domain.Enterprise;
import com.newland.erp.enterprise.domain.EnterpriseCode;
import com.newland.erp.enterprise.domain.LifecycleStatus;
import com.newland.erp.enterprise.domain.LocalizedName;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

final class EnterpriseStructureControllerTest {
    private EnterpriseStructureRepository repository;
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        repository = mock(EnterpriseStructureRepository.class);
        final AuthorizationPort authorization = permission -> {
        };
        final AuditPort audit = event -> {
        };
        final ApplicationEventPublisher events = event -> {
        };
        final EnterpriseStructureService service = new EnterpriseStructureService(repository, authorization, audit,
                events, Clock.fixed(Instant.parse("2026-07-20T00:00:00Z"), ZoneOffset.UTC));
        mvc = MockMvcBuilders.standaloneSetup(new EnterpriseStructureController(service))
                .setControllerAdvice(new EnterpriseStructureProblemHandler())
                .build();
    }

    @Test
    void createsEnterpriseThroughVersionedApi() throws Exception {
        when(repository.enterpriseCodeExists(new EnterpriseCode("NL"))).thenReturn(false);
        when(repository.insertEnterprise(any(Enterprise.class))).thenAnswer(invocation -> invocation.getArgument(0));

        mvc.perform(post("/api/v1/enterprise-structure/enterprises")
                        .header("X-Newland-Actor", "tester")
                        .header("X-Correlation-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "code": "nl",
                                  "name": "Newland",
                                  "localizedName": { "en": "Newland" }
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("NL"))
                .andExpect(jsonPath("$.status").value("DRAFT"));

        verify(repository).insertEnterprise(any(Enterprise.class));
    }

    @Test
    void returnsProblemDetailsForMissingEnterprise() throws Exception {
        final UUID missingId = UUID.randomUUID();
        when(repository.findEnterprise(missingId)).thenReturn(Optional.empty());

        mvc.perform(get("/api/v1/enterprise-structure/enterprises/{enterpriseId}", missingId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("Enterprise Structure resource not found"));
    }

    @Test
    void returnsConflictForInvalidStateTransitions() throws Exception {
        final UUID enterpriseId = UUID.randomUUID();
        when(repository.findEnterprise(enterpriseId)).thenReturn(Optional.of(new Enterprise(enterpriseId,
                new EnterpriseCode("NL"), new DisplayName("Newland"), new LocalizedName(Map.of()),
                LifecycleStatus.ACTIVE, AuditMetadata.created(Instant.parse("2026-07-20T00:00:00Z"), "tester"))));

        mvc.perform(post("/api/v1/enterprise-structure/enterprises/{enterpriseId}/activate", enterpriseId)
                        .header("X-Newland-Actor", "tester"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.title").value("Enterprise Structure conflict"));
    }

    @Test
    void returnsBadRequestForMalformedWarehouseType() throws Exception {
        final UUID warehouseId = UUID.randomUUID();

        mvc.perform(put("/api/v1/enterprise-structure/warehouses/{warehouseId}", warehouseId)
                        .header("X-Newland-Actor", "tester")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Main Warehouse",
                                  "localizedName": {},
                                  "type": "INVALID",
                                  "address": null,
                                  "expectedVersion": 0
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Invalid Enterprise Structure request"));
    }
}
