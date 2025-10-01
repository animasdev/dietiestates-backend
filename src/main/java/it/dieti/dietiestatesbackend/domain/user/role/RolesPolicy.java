package it.dieti.dietiestatesbackend.domain.user.role;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class RolesPolicy {
    private static final Map<RolesEnum, Set<RolesEnum>> ALLOWED_CREATES =
            new EnumMap<>(Map.of(
                    RolesEnum.SUPERADMIN, EnumSet.of(RolesEnum.ADMIN, RolesEnum.AGENCY),
                    RolesEnum.ADMIN, EnumSet.of(RolesEnum.AGENCY),
                    RolesEnum.AGENCY, EnumSet.of(RolesEnum.AGENT)
            ));
    private RolesPolicy() {}

    public static boolean isAllowedCreate(RolesEnum caller, RolesEnum requestedRole) {
        return ALLOWED_CREATES.getOrDefault(caller, Set.of()).contains(requestedRole);
    }
}
