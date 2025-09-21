package it.dieti.dietiestatesbackend.domain.user.role;

import java.util.UUID;

public record Role(UUID id, String code, String name, String description) {
}
