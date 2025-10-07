package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.api.model.Agency;
import it.dieti.dietiestatesbackend.api.model.AgencyCreateRequest;
import it.dieti.dietiestatesbackend.application.agency.AgencyOnboardingService;
import it.dieti.dietiestatesbackend.application.onboarding.OnboardingException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AgenciesApiDelegateImpl implements AgenciesApiDelegate {
    private final AgencyOnboardingService agencyOnboardingService;

    public AgenciesApiDelegateImpl(AgencyOnboardingService agencyOnboardingService) {
        this.agencyOnboardingService = agencyOnboardingService;
    }

    @Override
    public ResponseEntity<Agency> agenciesPost(AgencyCreateRequest agencyCreateRequest) {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (!(auth instanceof JwtAuthenticationToken jwtAuth)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
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
            return ResponseEntity.status(mapStatus(ex.reason())).build();
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }

    private HttpStatus mapStatus(OnboardingException.Reason reason) {
        return switch (reason) {
            case USER_NOT_FOUND -> HttpStatus.UNAUTHORIZED;
            case ROLE_NOT_ALLOWED -> HttpStatus.FORBIDDEN;
            case PROFILE_ALREADY_EXISTS, FIRST_ACCESS_ALREADY_COMPLETED -> HttpStatus.CONFLICT;
            case AGENCY_NOT_FOUND -> HttpStatus.BAD_REQUEST;
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
