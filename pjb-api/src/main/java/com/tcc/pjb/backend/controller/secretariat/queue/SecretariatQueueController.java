package com.tcc.pjb.backend.controller.secretariat.queue;


import java.time.Duration;
import com.tcc.pjb.backend.core.operational.OperationalApiRoutes;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatInboxSummaryDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAgendaSnapshotDto;
import com.tcc.pjb.backend.model.dto.security.OperationalStepUpChallengeResponse;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueAttendanceRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueCompletionEventRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueItemDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueOperationalActionResponse;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueuePanelSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatGovernanceSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatExceptionDeskSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatCoverageSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.governance.SecretariatFormalCatalogSnapshotDto;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueParticipantNotificationRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueProcessReturnRequest;
import com.tcc.pjb.backend.model.dto.secretariat.queue.SecretariatQueueVenueConfirmationRequest;
import com.tcc.pjb.backend.service.secretariat.access.SecretariatInstitutionalVisibilityService;
import com.tcc.pjb.backend.service.secretariat.operational.SecretariatQueueOperationalActionService;
import com.tcc.pjb.backend.service.secretariat.query.queue.SecretariatQueueAgendaFilter;
import com.tcc.pjb.backend.service.secretariat.query.queue.SecretariatQueueQueryService;
import com.tcc.pjb.backend.service.security.operational.OperationalFunctionCredentialService;

@RestController
@RequestMapping(OperationalApiRoutes.SECRETARIAT_BASE)
@PreAuthorize("hasAnyAuthority('ROLE_SERVIDOR_JUDICIARIO','ROLE_ADMINISTRADOR')")
public class SecretariatQueueController {

  private final SecretariatQueueQueryService service;
  private final SecretariatInstitutionalVisibilityService visibilityService;
  private final SecretariatQueueOperationalActionService actionService;
  private final OperationalFunctionCredentialService credentialService;

  public SecretariatQueueController(SecretariatQueueQueryService service,
                                    SecretariatInstitutionalVisibilityService visibilityService,
                                    SecretariatQueueOperationalActionService actionService,
                                    OperationalFunctionCredentialService credentialService) {
    this.service = Objects.requireNonNull(service);
    this.visibilityService = Objects.requireNonNull(visibilityService);
    this.actionService = Objects.requireNonNull(actionService);
    this.credentialService = Objects.requireNonNull(credentialService);
  }

  @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE)
  public ResponseEntity<Page<SecretariatQueueItemDto>> list(
      @RequestParam("inboxKey") String inboxKey,
      @RequestParam(value = "statuses", required = false) String statuses,
      @RequestHeader(name = "If-None-Match", required = false) String ifNoneMatch,
      @RequestParam(value = "page", defaultValue = "0") int page,
      @RequestParam(value = "size", defaultValue = "50") int size
  ) {
    String normalized = visibilityService.requireInboxAccess(inboxKey);
    Pageable pageable = PageRequest.of(Math.max(0, page), clampSize(size));
    List<String> st = parseStatuses(statuses);
    String etag = service.etagForInbox(normalized, st, pageable);
    if (ifNoneMatch != null && ifNoneMatch.equals(etag)) {
      return ResponseEntity.status(304)
          .eTag(etag)
          .cacheControl(CacheControl.maxAge(Duration.ofSeconds(2)).cachePrivate())
          .build();
    }
    return ResponseEntity.ok()
        .eTag(etag)
        .cacheControl(CacheControl.maxAge(Duration.ofSeconds(2)).cachePrivate())
        .body(service.list(normalized, st, pageable));
  }



  @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_SUMMARY)
  public ResponseEntity<SecretariatInboxSummaryDto> summary(
      @RequestParam("inboxKey") String inboxKey,
      @RequestParam(value = "statuses", required = false) String statuses
  ) {
    String normalized = visibilityService.requireInboxAccess(inboxKey);
    List<String> st = parseStatuses(statuses);
    return ResponseEntity.ok(service.summary(normalized, st));
  }

  @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_PANEL)
  public ResponseEntity<SecretariatQueuePanelSnapshotDto> panel(
      @RequestParam("inboxKey") String inboxKey,
      @RequestParam(value = "statuses", required = false) String statuses,
      @RequestParam(value = "limit", defaultValue = "120") int limit
  ) {
    String normalized = visibilityService.requireInboxAccess(inboxKey);
    List<String> st = parseStatuses(statuses);
    return ResponseEntity.ok(service.panel(normalized, st, limit));
  }


  @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_AGENDA)
  public ResponseEntity<SecretariatQueueAgendaSnapshotDto> agenda(
      @RequestParam("inboxKey") String inboxKey,
      @RequestParam(value = "statuses", required = false) String statuses,
      @RequestParam(value = "daysPast", defaultValue = "3") int daysPast,
      @RequestParam(value = "daysFuture", defaultValue = "45") int daysFuture,
      @RequestParam(value = "limit", defaultValue = "180") int limit,
      @RequestParam(value = "tribunal", required = false) String tribunal,
      @RequestParam(value = "foro", required = false) String foro,
      @RequestParam(value = "vara", required = false) String vara,
      @RequestParam(value = "orgao", required = false) String orgao,
      @RequestParam(value = "secretaria", required = false) String secretaria,
      @RequestParam(value = "rito", required = false) String rito,
      @RequestParam(value = "cellCode", required = false) String cellCode,
      @RequestParam(value = "responsavel", required = false) String responsavel,
      @RequestParam(value = "categoria", required = false) String categoria
  ) {
    String normalized = visibilityService.requireInboxAccess(inboxKey);
    List<String> st = parseStatuses(statuses);
    return ResponseEntity.ok(service.agenda(normalized, st, daysPast, daysFuture, limit,
        new SecretariatQueueAgendaFilter(tribunal, foro, vara, orgao, secretaria, rito, cellCode, responsavel, categoria)));
  }


  @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_GOVERNANCE)
  public ResponseEntity<SecretariatGovernanceSnapshotDto> governance(
      @RequestParam("inboxKey") String inboxKey,
      @RequestParam(value = "statuses", required = false) String statuses
  ) {
    String normalized = visibilityService.requireInboxAccess(inboxKey);
    List<String> st = parseStatuses(statuses);
    return ResponseEntity.ok(service.governance(normalized, st));
  }

  @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_EXCEPTIONS)
  public ResponseEntity<SecretariatExceptionDeskSnapshotDto> exceptions(
      @RequestParam("inboxKey") String inboxKey,
      @RequestParam(value = "statuses", required = false) String statuses
  ) {
    String normalized = visibilityService.requireInboxAccess(inboxKey);
    List<String> st = parseStatuses(statuses);
    return ResponseEntity.ok(service.exceptions(normalized, st));
  }

  @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_COVERAGE)
  public ResponseEntity<SecretariatCoverageSnapshotDto> coverage(
      @RequestParam("inboxKey") String inboxKey,
      @RequestParam(value = "statuses", required = false) String statuses
  ) {
    String normalized = visibilityService.requireInboxAccess(inboxKey);
    List<String> st = parseStatuses(statuses);
    return ResponseEntity.ok(service.coverage(normalized, st));
  }

  @GetMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_FORMAL_CATALOG)
  public ResponseEntity<SecretariatFormalCatalogSnapshotDto> formalCatalog(
      @RequestParam("inboxKey") String inboxKey,
      @RequestParam(value = "statuses", required = false) String statuses
  ) {
    String normalized = visibilityService.requireInboxAccess(inboxKey);
    List<String> st = parseStatuses(statuses);
    return ResponseEntity.ok(service.formalCatalog(normalized, st));
  }

  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_VENUE_CONFIRMATION)
  public ResponseEntity<SecretariatQueueOperationalActionResponse> confirmVenue(
      @PathVariable("workItemId") Long workItemId,
      @Valid @RequestBody(required = false) SecretariatQueueVenueConfirmationRequest request,
      @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
      Authentication authentication
  ) {
    credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE, "SECRETARIAT_CONFIRM_VENUE", String.valueOf(workItemId), unlockToken);
    return ResponseEntity.ok(actionService.confirmVenue(workItemId, request, authentication == null ? null : authentication.getName()));
  }

  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_PARTICIPANT_NOTIFICATION_CHALLENGE)
  public ResponseEntity<OperationalStepUpChallengeResponse> issueParticipantNotificationChallenge(
      @PathVariable("workItemId") Long workItemId
  ) {
    return ResponseEntity.ok(actionService.issueParticipantNotificationChallenge(workItemId));
  }

  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_PARTICIPANT_NOTIFICATION)
  public ResponseEntity<SecretariatQueueOperationalActionResponse> confirmParticipantNotification(
      @PathVariable("workItemId") Long workItemId,
      @Valid @RequestBody(required = false) SecretariatQueueParticipantNotificationRequest request,
      @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
      Authentication authentication
  ) {
    credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE, "SECRETARIAT_PARTICIPANT_NOTIFICATION", String.valueOf(workItemId), unlockToken);
    return ResponseEntity.ok(actionService.confirmParticipantNotification(workItemId, request, authentication == null ? null : authentication.getName()));
  }

  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_ATTENDANCE)
  public ResponseEntity<SecretariatQueueOperationalActionResponse> registerAttendance(
      @PathVariable("workItemId") Long workItemId,
      @Valid @RequestBody SecretariatQueueAttendanceRequest request,
      Authentication authentication
  ) {
    return ResponseEntity.ok(actionService.registerAttendance(workItemId, request, authentication == null ? null : authentication.getName()));
  }

  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_COMPLETION_EVENT)
  public ResponseEntity<SecretariatQueueOperationalActionResponse> registerCompletionEvent(
      @PathVariable("workItemId") Long workItemId,
      @Valid @RequestBody SecretariatQueueCompletionEventRequest request,
      @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
      Authentication authentication
  ) {
    credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE, "SECRETARIAT_COMPLETION_EVENT", String.valueOf(workItemId), unlockToken);
    return ResponseEntity.ok(actionService.registerCompletionEvent(workItemId, request, authentication == null ? null : authentication.getName()));
  }

  @PostMapping(OperationalApiRoutes.PATH_SECRETARIAT_QUEUE_PROCESS_RETURN)
  public ResponseEntity<SecretariatQueueOperationalActionResponse> executeProcessReturn(
      @PathVariable("workItemId") Long workItemId,
      @Valid @RequestBody(required = false) SecretariatQueueProcessReturnRequest request,
      @RequestHeader(name = OperationalFunctionCredentialService.HEADER_UNLOCK_TOKEN, required = false) String unlockToken,
      Authentication authentication
  ) {
    credentialService.consumeUnlockTokenForCurrentUser(OperationalFunctionCredentialService.SECRETARIAT_PROCESS_WRITE, "SECRETARIAT_PROCESS_RETURN", String.valueOf(workItemId), unlockToken);
    return ResponseEntity.ok(actionService.executeProcessReturn(workItemId, request, authentication == null ? null : authentication.getName()));
  }

  private static int clampSize(int size) {
    if (size <= 0) return 50;
    if (size > 200) return 200;
    return size;
  }

  private static List<String> parseStatuses(String raw) {
    if (raw == null || raw.isBlank()) {
      return List.of("PENDENTE", "EM_EXECUCAO");
    }
    return Arrays.stream(raw.split(","))
        .map(String::trim)
        .filter(s -> !s.isBlank())
        .map(String::toUpperCase)
        .collect(Collectors.toList());
  }
}
