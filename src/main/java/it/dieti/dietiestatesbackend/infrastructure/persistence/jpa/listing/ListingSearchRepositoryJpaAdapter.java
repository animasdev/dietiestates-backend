package it.dieti.dietiestatesbackend.infrastructure.persistence.jpa.listing;

import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.search.ListingSearchRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ListingSearchRepositoryJpaAdapter implements ListingSearchRepository {

    private final EntityManager entityManager;

    public ListingSearchRepositoryJpaAdapter(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public SearchResult search(SearchFilters filters) {
        var params = new HashMap<String, Object>();
        var whereClauses = new ArrayList<String>();

        whereClauses.add("l.deleted_at IS NULL");

        buildListingTypeCondition(filters, params, whereClauses);
        buildStatusCondition(filters, params, whereClauses);
        buildCityCondition(filters, params, whereClauses);
        buildPriceCondition(filters, params, whereClauses);
        buildRoomsCondition(filters, params, whereClauses);
        buildEnergyClassCondition(filters, params, whereClauses);
        buildRadiusCondition(filters, params, whereClauses);
        String featureCondition = buildFeatureCondition(filters, params);

        var whereClause = whereClauses.isEmpty()
                ? ""
                : " WHERE " + String.join(" AND ", whereClauses);

        String baseSql = " FROM listings l";
        if (!featureCondition.isEmpty()) {
            whereClause = whereClause.isEmpty()
                    ? " WHERE " + featureCondition
                    : whereClause + " AND " + featureCondition;
        }

        String orderClause = buildOrderClause(filters);
        String searchSql = "SELECT l.*" + baseSql + whereClause + orderClause + " LIMIT :limit OFFSET :offset";
        String countSql = "SELECT COUNT(*)" + baseSql + whereClause;

        params.put("limit", filters.size());
        params.put("offset", filters.page() * filters.size());

        Query searchQuery = entityManager.createNativeQuery(searchSql, ListingEntity.class);
        Query countQuery = entityManager.createNativeQuery(countSql);

        params.forEach((key, value) -> {
            searchQuery.setParameter(key, value);
            if (!"limit".equals(key) && !"offset".equals(key)) {
                countQuery.setParameter(key, value);
            }
        });

        @SuppressWarnings("unchecked")
        List<ListingEntity> entities = searchQuery.getResultList();
        List<Listing> listings = entities.stream()
                .map(ListingEntityMapper::toDomain)
                .toList();
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new SearchResult(listings, total);
    }

    private static void buildListingTypeCondition(
            SearchFilters filters,
            Map<String, Object> params,
            List<String> whereClauses
    ) {
        if (filters.listingTypeId() != null) {
            whereClauses.add("l.listing_type_id = :listingTypeId");
            params.put("listingTypeId", filters.listingTypeId());
        }
    }

    private static void buildStatusCondition(
            SearchFilters filters,
            Map<String, Object> params,
            List<String> whereClauses
    ) {
        if (filters.statusId() != null) {
            whereClauses.add("l.status_id = :statusId");
            params.put("statusId", filters.statusId());
        }
    }

    private static void buildCityCondition(
            SearchFilters filters,
            Map<String, Object> params,
            List<String> whereClauses
    ) {
        if (filters.normalizedCity() != null && !filters.normalizedCity().isBlank()) {
            whereClauses.add("UPPER(l.city) = :city");
            params.put("city", filters.normalizedCity());
        }
    }

    private static void buildPriceCondition(
            SearchFilters filters,
            Map<String, Object> params,
            List<String> whereClauses
    ) {
        if (filters.minPriceCents() != null) {
            whereClauses.add("l.price_cents >= :minPrice");
            params.put("minPrice", filters.minPriceCents());
        }
        if (filters.maxPriceCents() != null) {
            whereClauses.add("l.price_cents <= :maxPrice");
            params.put("maxPrice", filters.maxPriceCents());
        }
    }

    private static void buildRoomsCondition(
            SearchFilters filters,
            Map<String, Object> params,
            List<String> whereClauses
    ) {
        if (filters.rooms() != null) {
            whereClauses.add("l.rooms >= :rooms");
            params.put("rooms", filters.rooms());
        }
    }

    private static void buildEnergyClassCondition(
            SearchFilters filters,
            Map<String, Object> params,
            List<String> whereClauses
    ) {
        if (filters.normalizedEnergyClass() != null && !filters.normalizedEnergyClass().isBlank()) {
            whereClauses.add("UPPER(l.energy_class) = :energyClass");
            params.put("energyClass", filters.normalizedEnergyClass());
        }
    }

    private static void buildRadiusCondition(
            SearchFilters filters,
            Map<String, Object> params,
            List<String> whereClauses
    ) {
        if (filters.latitude() != null && filters.longitude() != null && filters.radiusMeters() != null) {
            whereClauses.add("ST_DWithin(l.geo, ST_SetSRID(ST_MakePoint(:longitude, :latitude), 4326)::geography, :radius)");
            params.put("latitude", filters.latitude());
            params.put("longitude", filters.longitude());
            params.put("radius", filters.radiusMeters());
        }
    }

    private static String buildFeatureCondition(
            SearchFilters filters,
            Map<String, Object> params
    ) {
        if (filters.featureIds() == null || filters.featureIds().isEmpty()) {
            return "";
        }

        List<String> placeholders = new ArrayList<>();
        for (int i = 0; i < filters.featureIds().size(); i++) {
            String key = "featureId" + i;
            placeholders.add(":" + key);
            params.put(key, filters.featureIds().get(i));
        }
        params.put("featureCount", filters.featureIds().size());

        return "l.id IN (SELECT lf.listing_id FROM listing_features lf WHERE lf.feature_id IN ("
                + String.join(",", placeholders)
                + ") GROUP BY lf.listing_id HAVING COUNT(DISTINCT lf.feature_id) = :featureCount)";
    }

    private static String buildOrderClause(SearchFilters filters) {
        String column = switch (filters.sortColumn()) {
            case "price_cents" -> "l.price_cents";
            case "created_at" -> "l.created_at";
            case "published_at" -> "l.published_at";
            default -> "l.published_at";
        };
        String direction = filters.sortAscending() ? "ASC" : "DESC";
        String secondary = filters.sortAscending() ? ", l.created_at ASC" : ", l.created_at DESC";
        return " ORDER BY " + column + " " + direction + secondary;
    }
}
