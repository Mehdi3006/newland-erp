package com.newland.erp.finance.posting.infrastructure;

import org.jooq.DSLContext;
import org.springframework.stereotype.Component;

/**
 * jOOQ adapter boundary for the posting schema; persistence is replaced by the configured
 * repository adapter.
 */
@Component
public final class JooqPostingRepository {
  private final DSLContext dsl;

  public JooqPostingRepository(final DSLContext dslContext) {
    dsl = dslContext;
  }

  public DSLContext context() {
    return dsl;
  }
}
