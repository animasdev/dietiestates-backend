package it.dieti.dietiestatesbackend.domain.user.role;

import lombok.Getter;

@Getter
public enum RolesEnum {

    SUPERADMIN("SUPERADMIN"),
    ADMIN("ADMIN"),
    AGENCY("AGENCY"),
    AGENT("AGENT"),
    USER("USER");


    private final String description;

    RolesEnum(String description) {
        this.description = description;
    }
}
