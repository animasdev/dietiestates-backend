package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.ModerationAction;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.moderation.ModerationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.UUID;

@Service
public class ModerationApiDelegateImpl implements ModerationApiDelegate {

    private static final Logger log = LoggerFactory.getLogger(ModerationApiDelegateImpl.class);

    private final ModerationService moderationService;

    public ModerationApiDelegateImpl(ModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @Override
    public ResponseEntity<List<ModerationAction>> moderationListingsGet(UUID listingId)  {
        var authentication = SecurityContextHolder.getContext().getAuthentication();
        if (!(authentication instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Tentativo di accesso non autorizzato a GET /moderation/listings senza JWT");
            throw UnauthorizedException.bearerTokenMissing();
        }

        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        var actions = moderationService.getModerationActions(userId, listingId);
        var response = actions.stream()
                .map(this::toApi)
                .toList();
        return ResponseEntity.ok(response);
    }

    private ModerationAction toApi(ModerationService.ModerationLogEntry  entry) {
        var action = new ModerationAction();
        action.setId(entry.id());
        action.performedByRole(ModerationAction.PerformedByRoleEnum.valueOf(entry.performedByRole().name()));
        action.performedByUserId(entry.performedByUserId());
        action.actionType(ModerationAction.ActionTypeEnum.valueOf(entry.actionType().name()));
        action.listingId(entry.listingId());
        action.reason(entry.reason());
        return action;
    }

}
