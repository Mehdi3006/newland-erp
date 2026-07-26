package com.newland.erp.servicewarranty.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.newland.erp.servicewarranty.application.ServiceWarrantySecurityPort;
import com.newland.erp.servicewarranty.application.ServiceWarrantyService;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.security.access.AccessDeniedException;

final class ServiceWarrantyControllerTest {
  @Test
  void authorizesCompanyScopeBeforeTicketRetrieval() {
    final ServiceWarrantyService service = mock(ServiceWarrantyService.class);
    final ServiceWarrantySecurityPort security = mock(ServiceWarrantySecurityPort.class);
    final UUID ticketId = UUID.randomUUID();
    final UUID companyId = UUID.randomUUID();
    when(security.currentActor()).thenReturn("actor");
    final ServiceWarrantyController controller = new ServiceWarrantyController(service, security);

    assertThatThrownBy(
            () -> {
              org.mockito.Mockito.doThrow(new AccessDeniedException("denied"))
                  .when(security)
                  .require("actor", "service.ticket.manage", companyId);
              controller.ticket(ticketId, companyId);
            })
        .isInstanceOf(AccessDeniedException.class);

    final InOrder order = inOrder(security, service);
    order.verify(security).currentActor();
    order.verify(security).require("actor", "service.ticket.manage", companyId);
    order.verifyNoMoreInteractions();
  }
}
