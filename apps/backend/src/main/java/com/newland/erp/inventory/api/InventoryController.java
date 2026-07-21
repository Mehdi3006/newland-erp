package com.newland.erp.inventory.api;

import com.newland.erp.inventory.application.InventoryCommands;
import com.newland.erp.inventory.application.InventoryService;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/inventory")
public final class InventoryController {
    private static final String ACTOR_HEADER = "X-Newland-Actor";
    private final InventoryService service;

    public InventoryController(final InventoryService inventoryService) {
        this.service = inventoryService;
    }

    @PostMapping("/transactions")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryDtos.TransactionResponse post(@Valid @RequestBody
                                                  final InventoryDtos.PostTransactionRequest request,
                                                  @RequestHeader(name = ACTOR_HEADER,
                                                          defaultValue = "system") final String actor) {
        return InventoryDtos.TransactionResponse.from(service.post(new InventoryCommands.PostTransaction(
                request.movementType(), request.idempotencyKey(), request.businessDate(),
                request.lines().stream().map(line -> new InventoryCommands.PostLine(line.item().toDomain(),
                        line.fromLocation() == null ? null : line.fromLocation().toDomain(),
                        line.toLocation() == null ? null : line.toLocation().toDomain(), line.quantity().toDomain(),
                        line.inventoryStatus(), line.lotCode(), line.serialCode(), line.expiryDate())).toList(),
                request.attachmentIds(), actor)));
    }

    @PostMapping("/reservations")
    @ResponseStatus(HttpStatus.CREATED)
    public void reserve(@Valid @RequestBody final InventoryDtos.ReserveRequest request,
                        @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        service.reserve(new InventoryCommands.Reserve(request.item().toDomain(), request.location().toDomain(),
                request.quantity().toDomain(), request.idempotencyKey(), actor));
    }

    @PostMapping("/reservations/{reservationId}/release")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable final UUID reservationId,
                        @Valid @RequestBody final InventoryDtos.ReleaseRequest request,
                        @RequestHeader(name = ACTOR_HEADER, defaultValue = "system") final String actor) {
        service.release(new InventoryCommands.Release(reservationId, request.idempotencyKey(), actor));
    }

    @PostMapping("/transactions/{transactionId}/reverse")
    public InventoryDtos.TransactionResponse reverse(@PathVariable final UUID transactionId,
                                                     @Valid @RequestBody
                                                     final InventoryDtos.ReverseRequest request,
                                                     @RequestHeader(name = ACTOR_HEADER,
                                                             defaultValue = "system") final String actor) {
        return InventoryDtos.TransactionResponse.from(service.reverse(new InventoryCommands.Reverse(transactionId,
                request.idempotencyKey(), actor)));
    }

    @GetMapping("/transactions")
    public List<InventoryDtos.TransactionResponse> transactions() {
        return service.transactions().stream().map(InventoryDtos.TransactionResponse::from).toList();
    }

    @GetMapping("/balances/{skuId}")
    public List<InventoryDtos.BalanceResponse> balances(@PathVariable final UUID skuId) {
        return service.balances(skuId).stream().map(InventoryDtos.BalanceResponse::from).toList();
    }
}
