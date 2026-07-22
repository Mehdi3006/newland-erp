package com.newland.erp.finance.posting.api;

import com.newland.erp.finance.posting.api.PostingRuleApiDtos.CreateRuleRequest;
import com.newland.erp.finance.posting.api.PostingRuleApiDtos.CreateSuccessorRequest;
import com.newland.erp.finance.posting.api.PostingRuleApiDtos.PostingRuleResponse;
import com.newland.erp.finance.posting.application.PostingRuleManagementPort;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/finance/posting/rules")
public final class PostingRuleController {
  private final PostingRuleManagementPort rules;

  public PostingRuleController(final PostingRuleManagementPort postingRules) {
    rules = postingRules;
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  public PostingRuleResponse create(@Valid @RequestBody final CreateRuleRequest request) {
    return PostingRuleResponse.from(rules.create(request.toCommand()));
  }

  @PostMapping("/{id}/versions")
  @ResponseStatus(HttpStatus.CREATED)
  public PostingRuleResponse createSuccessor(
      @PathVariable final UUID id,
      @Valid @RequestBody final CreateSuccessorRequest request) {
    return PostingRuleResponse.from(rules.createSuccessor(request.toCommand(id)));
  }

  @PostMapping("/{id}/activate")
  public PostingRuleResponse activate(@PathVariable final UUID id) {
    return PostingRuleResponse.from(rules.activate(id));
  }

  @PostMapping("/{id}/retire")
  public PostingRuleResponse retire(@PathVariable final UUID id) {
    return PostingRuleResponse.from(rules.retire(id));
  }

  @GetMapping("/{id}")
  public PostingRuleResponse get(@PathVariable final UUID id) {
    return PostingRuleResponse.from(rules.get(id));
  }

  @GetMapping
  public List<PostingRuleResponse> list(
      @RequestParam(required = false) final UUID companyId) {
    return rules.list(companyId).stream().map(PostingRuleResponse::from).toList();
  }
}
