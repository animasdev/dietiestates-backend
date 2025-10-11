package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.Agency;
import it.dieti.dietiestatesbackend.api.model.AgencyCreateRequest;
import it.dieti.dietiestatesbackend.application.agency.AgencyOnboardingService;
import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ConflictException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.UnauthorizedException;
import it.dieti.dietiestatesbackend.application.onboarding.OnboardingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AgenciesApiDelegateImpl implements AgenciesApiDelegate {
    private final AgencyOnboardingService agencyOnboardingService;
    private static final Logger log = LoggerFactory.getLogger(AgenciesApiDelegateImpl.class);

    public AgenciesApiDelegateImpl(AgencyOnboardingService agencyOnboardingService) {
        this.agencyOnboardingService = agencyOnboardingService;
    }

    @Override
    public ResponseEntity<Agency> agenciesPost(AgencyCreateRequest agencyCreateRequest) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            log.warn("Tentativo di completare profilo agenzia senza token");
            throw UnauthorizedException.bearerTokenMissing();
        }

        var userId = UUID.fromString(jwtAuth.getToken().getSubject());
        try {
            var command = new AgencyOnboardingService.CompleteAgencyProfileCommand(
                    agencyCreateRequest.getName(),
                    agencyCreateRequest.getDescription(),
                    agencyCreateRequest.getLogoMediaId()
            );
            var agency = agencyOnboardingService.completeProfile(userId, command);
            return ResponseEntity.status(HttpStatus.CREATED).body(toApi(agency));
        } catch (OnboardingException ex) {
            throw translateOnboardingException(ex, userId);
        }
    }

    private RuntimeException translateOnboardingException(OnboardingException ex, UUID userId) {
        return switch (ex.reason()) {
            case USER_NOT_FOUND -> {
                log.warn("Utente {} non trovato durante onboarding agenzia", userId);
                yield UnauthorizedException.userNotFound();
            }
            case ROLE_NOT_ALLOWED -> {
                log.warn("Utente {} senza ruolo valido per onboarding agenzia", userId);
                yield ForbiddenException.actionRequiresRole("AGENCY");
            }
            case PROFILE_ALREADY_EXISTS -> {
                log.warn("Utente {} ha già un profilo agenzia", userId);
                yield ConflictException.of("Profilo agenzia già completato.");
            }
            case FIRST_ACCESS_ALREADY_COMPLETED -> {
                log.warn("Utente {} ha già completato il firstAccess", userId);
                yield ConflictException.of("Profilo agenzia già confermato.");
            }
            case AGENCY_NOT_FOUND -> {
                log.warn("Agenzia non trovata durante onboarding per utente {}", userId);
                yield BadRequestException.of("Agenzia di riferimento non trovata.");
            }
        };
    }

    private Agency toApi(it.dieti.dietiestatesbackend.domain.agency.Agency agency) {
        Agency body = new Agency();
        body.setId(agency.id());
        body.setName(agency.name());
        body.setDescription(agency.description());
        body.setUserId(agency.userId());
        body.setLogoMediaId(agency.logoMediaId());
        body.setApprovedBy(agency.approvedBy());
        body.setApprovedAt(agency.approvedAt());
        body.setCreatedAt(agency.createdAt());
        body.setUpdatedAt(agency.updatedAt());
        return body;
    }
}
