package it.dieti.dietiestatesbackend.domain.listing.status;

import lombok.Getter;

@Getter
public enum ListingStatusesEnum {
    DRAFT("DRAFT"),
    PUBLISHED("PUBLISHED"),
    PENDING_DELETE("PENDING_DELETE"),
    DELETED("DELETED");

    private final String description;

    ListingStatusesEnum(String description) {
        this.description = description;
    }
}
