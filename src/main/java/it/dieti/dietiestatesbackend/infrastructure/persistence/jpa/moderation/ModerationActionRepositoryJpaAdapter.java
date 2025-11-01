package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.moderation;

import it.dieti.dietiestatesbackend.domain.moderation.ModerationAction;
import it.dieti.dietiestatesbackend.domain.moderation.ModerationActionRepository;
import it.dieti.dietiestatesbackend.domain.moderation.ModerationActionType;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing.ListingEntity;
import it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.user.UserEntity;
import org.springframework.stereotype.Repository;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public class ModerationActionRepositoryJpaAdapter implements ModerationActionRepository {

    private final ModerationActionJpaRepository jpaRepository;
    private final ModerationActionTypeJpaRepository actionTypeRepository;

    public ModerationActionRepositoryJpaAdapter(ModerationActionJpaRepository jpaRepository,
                                                ModerationActionTypeJpaRepository actionTypeRepository) {
        this.jpaRepository = jpaRepository;
        this.actionTypeRepository = actionTypeRepository;
    }

    @Override
    public ModerationAction save(ModerationAction action) {
        var entity = action.id() != null
                ? jpaRepository.findById(action.id()).orElseThrow(() -> new IllegalArgumentException("Moderation action not found: " + action.id()))
                : new ModerationActionEntity();

        var listingRef = new ListingEntity();
        listingRef.setId(action.listingId());
        entity.setListing(listingRef);

        var userRef = new UserEntity();
        userRef.setId(action.performedByUserId());
        entity.setPerformedBy(userRef);
        entity.setPerformedByRole(action.performedByRole().name());

        var actionType = action.actionType();
        if (actionType == null) {
            throw new IllegalArgumentException("Moderation action type is required");
        }
        var actionTypeEntity = actionTypeRepository.findByCode(actionType.name())
                .orElseThrow(() -> new IllegalStateException("Moderation action type not found: " + actionType));
        entity.setActionType(actionTypeEntity);

        entity.setReason(action.reason());

        if (action.createdAt() != null) {
            entity.setCreatedAt(action.createdAt());
        } else if (entity.getCreatedAt() == null) {
            entity.setCreatedAt(OffsetDateTime.now());
        }

        var saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<ModerationAction> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ModerationAction> findByListingIds(Collection<UUID> listingIds) {
        if (listingIds == null || listingIds.isEmpty()) {
            return List.of();
        }
        return jpaRepository.findAllByListing_IdInOrderByCreatedAtDesc(listingIds).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public List<ModerationAction> findByListingId(UUID listingId) {
        return jpaRepository.findAllByListing_IdOrderByCreatedAtDesc(listingId).stream()
                .map(this::toDomain)
                .toList();
    }

    private ModerationAction toDomain(ModerationActionEntity entity) {
        return new ModerationAction(
                entity.getId(),
                entity.getListing() != null ? entity.getListing().getId() : null,
                entity.getPerformedBy() != null ? entity.getPerformedBy().getId() : null,
                toRole(entity.getPerformedByRole()),
                toActionType(entity.getActionType()),
                entity.getReason(),
                entity.getCreatedAt()
        );
    }

    private RolesEnum toRole(String role) {
        try {
            return RolesEnum.valueOf(role);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Unsupported role stored in moderation action: " + role, ex);
        }
    }

    private ModerationActionType toActionType(ModerationActionTypeEntity actionType) {
        if (actionType == null || !StringUtils.hasText(actionType.getCode())) {
            return null;
        }
        try {
            return ModerationActionType.valueOf(actionType.getCode());
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException("Unsupported moderation action type: " + actionType.getCode(), ex);
        }
    }
}
