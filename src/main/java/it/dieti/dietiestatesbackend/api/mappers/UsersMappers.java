package it.dieti.dietiestatesbackend.api.mappers;

import it.dieti.dietiestatesbackend.api.model.UserInfoAgencyProfile;
import it.dieti.dietiestatesbackend.api.model.UserInfoAgentProfile;
import it.dieti.dietiestatesbackend.application.user.UserProfileService;

import java.net.URI;

public class UsersMappers {
    private UsersMappers(){}
    public static UserInfoAgentProfile toApi(UserProfileService.AgentProfile profile) {
        if (profile == null )return null;
        UserInfoAgentProfile api = new UserInfoAgentProfile();
        api.setAgencyId(profile.agencyId());
        api.setReaNumber(profile.reaNumber());
        api.agentId(profile.agentId());
        if (profile.profilePhotoUrl() != null && !profile.profilePhotoUrl().isBlank()) {
            api.setProfilePhotoUrl(URI.create(profile.profilePhotoUrl()));
        }
        return api;
    }
    public static UserInfoAgencyProfile toApi(UserProfileService.AgencyProfile profile) {
        if (profile == null )return null;
        UserInfoAgencyProfile api = new UserInfoAgencyProfile();
        api.setName(profile.name());
        api.setDescription(profile.description());
        api.agencyId(profile.agencyId());
        if (profile.logoUrl() != null && !profile.logoUrl().isBlank()) {
            api.setLogoUrl(URI.create(profile.logoUrl()));
        }
        return api;
    }
}
