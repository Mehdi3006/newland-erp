package com.newland.erp.finance.posting.infrastructure;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.newland.erp.finance.application.FinanceRepository;
import com.newland.erp.finance.domain.Account;
import com.newland.erp.finance.domain.CostCenter;
import com.newland.erp.finance.domain.FinanceException;
import com.newland.erp.finance.domain.ProfitCenter;
import com.newland.erp.finance.posting.domain.PostingException;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class FinanceReferenceAdapterTest {
  @Test
  void resolvesAnAttributeAccountOnlyInsideThePostingCompany() {
    final FinanceRepository repository = mock(FinanceRepository.class);
    final UUID companyId = UUID.randomUUID();
    final UUID accountId = UUID.randomUUID();
    when(repository.findAccount(companyId, accountId))
        .thenReturn(Optional.of(account(companyId, accountId, true, true)));
    final var adapter =
        new PostingInfrastructureAdapters.FinanceReferenceAdapter(repository);

    final UUID resolved =
        adapter.resolveAttribute(companyId, "account", Map.of("account", accountId.toString()));

    assertEquals(accountId, resolved);
    verify(repository).findAccount(companyId, accountId);
  }

  @Test
  void rejectsMissingCrossCompanyInactiveAndNonPostableAccounts() {
    final FinanceRepository repository = mock(FinanceRepository.class);
    final UUID companyId = UUID.randomUUID();
    final UUID missingId = UUID.randomUUID();
    final UUID inactiveId = UUID.randomUUID();
    final UUID parentId = UUID.randomUUID();
    when(repository.findAccount(companyId, missingId)).thenReturn(Optional.empty());
    when(repository.findAccount(companyId, inactiveId))
        .thenReturn(Optional.of(account(companyId, inactiveId, true, false)));
    when(repository.findAccount(companyId, parentId))
        .thenReturn(Optional.of(account(companyId, parentId, false, true)));
    final var adapter =
        new PostingInfrastructureAdapters.FinanceReferenceAdapter(repository);

    assertThrows(PostingException.class, () -> adapter.requireAccount(companyId, missingId));
    assertThrows(FinanceException.class, () -> adapter.requireAccount(companyId, inactiveId));
    assertThrows(FinanceException.class, () -> adapter.requireAccount(companyId, parentId));
  }

  @Test
  void validatesActiveCostAndProfitCentersInsideThePostingCompany() {
    final FinanceRepository repository = mock(FinanceRepository.class);
    final UUID companyId = UUID.randomUUID();
    final UUID costCenterId = UUID.randomUUID();
    final UUID profitCenterId = UUID.randomUUID();
    when(repository.findCostCenter(companyId, costCenterId))
        .thenReturn(Optional.of(new CostCenter(costCenterId, companyId, "COST", true)));
    when(repository.findProfitCenter(companyId, profitCenterId))
        .thenReturn(Optional.of(new ProfitCenter(profitCenterId, companyId, "PROFIT", true)));
    final var adapter =
        new PostingInfrastructureAdapters.FinanceReferenceAdapter(repository);

    adapter.requireCostCenter(companyId, costCenterId);
    adapter.requireProfitCenter(companyId, profitCenterId);

    verify(repository).findCostCenter(companyId, costCenterId);
    verify(repository).findProfitCenter(companyId, profitCenterId);
  }

  @Test
  void rejectsInactiveCostAndProfitCenters() {
    final FinanceRepository repository = mock(FinanceRepository.class);
    final UUID companyId = UUID.randomUUID();
    final UUID costCenterId = UUID.randomUUID();
    final UUID profitCenterId = UUID.randomUUID();
    when(repository.findCostCenter(companyId, costCenterId))
        .thenReturn(Optional.of(new CostCenter(costCenterId, companyId, "COST", false)));
    when(repository.findProfitCenter(companyId, profitCenterId))
        .thenReturn(Optional.of(new ProfitCenter(profitCenterId, companyId, "PROFIT", false)));
    final var adapter =
        new PostingInfrastructureAdapters.FinanceReferenceAdapter(repository);

    assertThrows(
        PostingException.class, () -> adapter.requireCostCenter(companyId, costCenterId));
    assertThrows(
        PostingException.class, () -> adapter.requireProfitCenter(companyId, profitCenterId));
  }

  @Test
  void validatesFinancialDimensionsAgainstActiveCompanyScopedDefinitions() {
    final FinanceRepository repository = mock(FinanceRepository.class);
    final UUID companyId = UUID.randomUUID();
    when(repository.financialDimensionIsActive(companyId, "PROJECT-A")).thenReturn(true);
    final var adapter = new PostingInfrastructureAdapters.DimensionsAdapter(repository);

    adapter.requireDimension(companyId, "PROJECT-A");

    assertThrows(
        PostingException.class, () -> adapter.requireDimension(companyId, "INACTIVE-PROJECT"));
    verify(repository).financialDimensionIsActive(companyId, "PROJECT-A");
    verify(repository).financialDimensionIsActive(companyId, "INACTIVE-PROJECT");
  }

  private static Account account(
      final UUID companyId,
      final UUID accountId,
      final boolean postable,
      final boolean active) {
    return new Account(
        accountId,
        companyId,
        "1000",
        "Posting account",
        Account.AccountType.ASSET,
        null,
        postable,
        active);
  }
}
