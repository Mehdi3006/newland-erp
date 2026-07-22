package com.newland.erp.finance.posting.api;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

final class PostingApiBoundaryTest {
  @Test
  void postingEndpointsExposeDedicatedApiResponses() {
    assertThat(returnType("submit", PostingController.EventRequest.class))
        .isEqualTo(PostingApiDtos.PostingResultResponse.class);
    assertThat(returnType("preview", PostingController.EventRequest.class))
        .isEqualTo(PostingApiDtos.PostingResultResponse.class);
    assertThat(returnType("status", UUID.class))
        .isEqualTo(PostingApiDtos.PostingRequestResponse.class);
    assertThat(returnType("retry", UUID.class))
        .isEqualTo(PostingApiDtos.PostingResultResponse.class);
  }

  @Test
  void ruleEndpointsExposeDedicatedApiResponses() {
    assertThat(
            Arrays.stream(PostingRuleController.class.getDeclaredMethods())
                .filter(method -> !method.isSynthetic())
                .map(method -> method.getReturnType().getName()))
        .noneMatch(name -> name.startsWith("com.newland.erp.finance.posting.domain"));
  }

  private Class<?> returnType(final String methodName, final Class<?> parameterType) {
    try {
      return PostingController.class.getMethod(methodName, parameterType).getReturnType();
    } catch (NoSuchMethodException exception) {
      throw new AssertionError(exception);
    }
  }
}
