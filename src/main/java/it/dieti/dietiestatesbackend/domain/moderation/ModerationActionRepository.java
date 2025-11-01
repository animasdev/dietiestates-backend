package it.dieti.dietiestatesbackend.domain.moderation;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface ModerationActionRepository {
    ModerationAction save(ModerationAction action);

    List<ModerationAction> findAll();

    List<ModerationAction> findByListingIds(Collection<UUID> listingIds);

    List<ModerationAction> findByListingId(UUID listingId);
}
