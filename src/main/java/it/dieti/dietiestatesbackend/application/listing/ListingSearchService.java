package it.dieti.dietiestatesbackend.application.listing;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.InternalServerErrorException;
import it.dieti.dietiestatesbackend.application.feature.FeatureService;
import it.dieti.dietiestatesbackend.application.media.listing.ListingMediaService;
import it.dieti.dietiestatesbackend.domain.feature.Feature;
import it.dieti.dietiestatesbackend.domain.feature.FeatureRepository;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingType;
import it.dieti.dietiestatesbackend.domain.listing.ListingTypeRepository;
import it.dieti.dietiestatesbackend.domain.listing.search.ListingSearchRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatus;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusesEnum;
import it.dieti.dietiestatesbackend.application.media.listing.ListingMediaService.ListingPhotoView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class ListingSearchService {

    private static final Logger log = LoggerFactory.getLogger(ListingSearchService.class);
    private static final int DEFAULT_PAGE = 0;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private static final int MIN_RADIUS_METERS = 50;
    private static final int MAX_RADIUS_METERS = 100_000;

    private final ListingSearchRepository listingSearchRepository;
    private final ListingTypeRepository listingTypeRepository;
    private final ListingStatusRepository listingStatusRepository;
    private final FeatureRepository featureRepository;
    private final FeatureService featureService;
    private final ListingMediaService listingMediaService;

    public ListingSearchService(ListingSearchRepository listingSearchRepository,
                                ListingTypeRepository listingTypeRepository,
                                ListingStatusRepository listingStatusRepository,
                                FeatureRepository featureRepository,
                                FeatureService featureService,
                                ListingMediaService listingMediaService) {
        this.listingSearchRepository = listingSearchRepository;
        this.listingTypeRepository = listingTypeRepository;
        this.listingStatusRepository = listingStatusRepository;
        this.featureRepository = featureRepository;
        this.featureService = featureService;
        this.listingMediaService = listingMediaService;
    }

    public record SearchQuery(
            String type,
            String city,
            Integer minPrice,
            Integer maxPrice,
            Integer rooms,
            String energyClass,
            List<String> features,
            String status,
            Double latitude,
            Double longitude,
            Integer radiusMeters,
            Boolean hasPhotos,
            UUID agencyId,
            UUID ownerAgentId,
            Integer page,
            Integer size,
            String sort,
            boolean enforcePublishedOnly
    ) {}

    public record SearchResult(List<SearchItem> items, int page, int size, long total) {}

    public record SearchItem(
            Listing listing,
            ListingType listingType,
            ListingStatus listingStatus,
            List<ListingPhotoView> photos,
            List<Feature> features
    ) {}

    public SearchResult search(SearchQuery query) {
        Objects.requireNonNull(query, "query is required");

        int page = Optional.ofNullable(query.page()).orElse(DEFAULT_PAGE);
        int size = Optional.ofNullable(query.size()).orElse(DEFAULT_SIZE);
        validatePagination(page, size);
        var sort = resolveSort(query.sort());

        var listingType = resolveListingType(query.type());
        var status = resolveStatus(query.status(), query.enforcePublishedOnly());
        var featureIds = resolveFeatureFilters(query.features());
        var normalizedCity = normalizeUpper(query.city());
        var normalizedEnergyClass = normalizeUpper(query.energyClass());
        validatePriceRange(query.minPrice(), query.maxPrice());
        validateRadiusFilters(query.latitude(), query.longitude(), query.radiusMeters());

        var filters = new ListingSearchRepository.SearchFilters(
                listingType != null ? listingType.id() : null,
                status != null ? status.id() : null,
                normalizedCity,
                query.minPrice(),
                query.maxPrice(),
                query.rooms(),
                normalizedEnergyClass,
                featureIds,
                query.latitude(),
                query.longitude(),
                query.radiusMeters(),
                query.hasPhotos(),
                query.agencyId(),
                query.ownerAgentId(),
                sort.sortColumn(),
                sort.ascending(),
                page,
                size
        );

        var repositoryResult = listingSearchRepository.search(filters);
        var listings = repositoryResult.listings();

        var listingTypesById = loadListingTypes(listings);
        var statusesById = loadListingStatuses(listings);
        var featuresByListingId = loadListingFeatures(listings);
        var photosByListingId = loadListingPhotos(listings);

        var items = listings.stream()
                .map(listing -> new SearchItem(
                        listing,
                        resolveListingTypeFor(listing, listingTypesById),
                        resolveListingStatusFor(listing, statusesById),
                        photosByListingId.getOrDefault(listing.id(), List.of()),
                        featuresByListingId.getOrDefault(listing.id(), List.of())
                ))
                .toList();

        return new SearchResult(items, page, size, repositoryResult.total());
    }

    private void validatePagination(int page, int size) {
        if (page < 0) {
            throw BadRequestException.forField("page", "Il parametro 'page' deve essere maggiore o uguale a 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw BadRequestException.forField("size", "Il parametro 'size' deve essere compreso tra 1 e " + MAX_PAGE_SIZE + ".");
        }
    }

    private void validatePriceRange(Integer minPrice, Integer maxPrice) {
        if (minPrice != null && minPrice < 0) {
            throw BadRequestException.forField("minPrice", "Il parametro 'minPrice' deve essere maggiore o uguale a 0.");
        }
        if (maxPrice != null && maxPrice < 0) {
            throw BadRequestException.forField("maxPrice", "Il parametro 'maxPrice' deve essere maggiore o uguale a 0.");
        }
        if (minPrice != null && maxPrice != null && minPrice > maxPrice) {
            throw BadRequestException.forField("maxPrice", "Il parametro 'maxPrice' deve essere maggiore o uguale a 'minPrice'.");
        }
    }

    private void validateRadiusFilters(Double latitude, Double longitude, Integer radius) {
        if (latitude == null && longitude == null && radius == null) {
            return;
        }
        if (latitude == null || longitude == null || radius == null) {
            throw BadRequestException.forField("within", "Per filtrare per raggio devi fornire lat, lng e radiusMeters.");
        }
        validateCoordinates(latitude, longitude);
        if (radius < MIN_RADIUS_METERS || radius > MAX_RADIUS_METERS) {
            throw BadRequestException.forField("radiusMeters", "Il parametro 'radiusMeters' deve essere compreso tra " + MIN_RADIUS_METERS + " e " + MAX_RADIUS_METERS + ".");
        }
    }

    private SortDescriptor resolveSort(String sortRaw) {
        if (sortRaw == null || sortRaw.isBlank()) {
            return new SortDescriptor("published_at", false);
        }

        var parts = sortRaw.split(",");
        var field = parts[0].trim();
        var direction = parts.length > 1 ? parts[1].trim().toLowerCase(Locale.ROOT) : "asc";

        boolean ascending = switch (direction) {
            case "asc" -> true;
            case "desc" -> false;
            default -> throw BadRequestException.forField("sort", "Direzione ordinamento non valida. Usa 'asc' o 'desc'.");
        };

        return switch (field) {
            case "priceCents" -> new SortDescriptor("price_cents", ascending);
            case "createdAt" -> new SortDescriptor("created_at", ascending);
            case "publishedAt" -> new SortDescriptor("published_at", ascending);
            default -> throw BadRequestException.forField("sort", "Campo di ordinamento non supportato: " + field + ".");
        };
    }

    private ListingType resolveListingType(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            return null;
        }
        var normalized = typeCode.trim().toUpperCase(Locale.ROOT);
        return listingTypeRepository.findByCode(normalized)
                .orElseThrow(() -> BadRequestException.forField("type", "listingType non supportato: " + normalized + "."));
    }

    private ListingStatus resolveStatus(String statusCode, boolean enforcePublishedOnly) {
        if (enforcePublishedOnly) {
            return listingStatusRepository.findByCode(ListingStatusesEnum.PUBLISHED.getDescription())
                    .orElseThrow(() -> new InternalServerErrorException("Stato PUBLISHED non configurato."));
        }
        if (statusCode == null || statusCode.isBlank()) {
            return null;
        }
        var normalized = statusCode.trim().toUpperCase(Locale.ROOT);
        return listingStatusRepository.findByCode(normalized)
                .orElseThrow(() -> BadRequestException.forField("status", "listingStatus non supportato: " + normalized + "."));
    }

    private List<UUID> resolveFeatureFilters(List<String> requestedFeatures) {
        if (requestedFeatures == null || requestedFeatures.isEmpty()) {
            return List.of();
        }
        var normalized = new ArrayList<String>();
        var seen = new HashSet<String>();
        for (String raw : requestedFeatures) {
            if (raw == null || raw.trim().isEmpty()) {
                throw BadRequestException.forField("features", "I codici delle feature non possono essere vuoti.");
            }
            var code = raw.trim().toUpperCase(Locale.ROOT);
            if (seen.add(code)) {
                normalized.add(code);
            }
        }

        List<UUID> featureIds = new ArrayList<>();
        for (String code : normalized) {
            var feature = featureRepository.findByCode(code)
                    .orElseThrow(() -> BadRequestException.forField("features", "Feature sconosciuta: " + code + "."));
            featureIds.add(feature.id());
        }
        return featureIds;
    }

    private Map<UUID, ListingType> loadListingTypes(List<Listing> listings) {
        Map<UUID, ListingType> result = new HashMap<>();
        for (Listing listing : listings) {
            var typeId = listing.listingTypeId();
            if (typeId == null || result.containsKey(typeId)) {
                continue;
            }
            var type = listingTypeRepository.findById(typeId)
                    .orElseThrow(() -> {
                        log.error("Listing {} referenzia listingType {} inesistente", listing.id(), typeId);
                        return new InternalServerErrorException("Errore nella configurazione dei tipi annuncio.");
                    });
            result.put(typeId, type);
        }
        return result;
    }

    private Map<UUID, ListingStatus> loadListingStatuses(List<Listing> listings) {
        Map<UUID, ListingStatus> result = new HashMap<>();
        for (Listing listing : listings) {
            var statusId = listing.statusId();
            if (statusId == null || result.containsKey(statusId)) {
                continue;
            }
            var status = listingStatusRepository.findById(statusId)
                    .orElseThrow(() -> {
                        log.error("Listing {} referenzia listingStatus {} inesistente", listing.id(), statusId);
                        return new InternalServerErrorException("Errore nella configurazione degli stati annuncio.");
                    });
            result.put(statusId, status);
        }
        return result;
    }

    private Map<UUID, List<Feature>> loadListingFeatures(List<Listing> listings) {
        Map<UUID, List<Feature>> result = new HashMap<>();
        for (Listing listing : listings) {
            var listingId = listing.id();
            if (listingId == null) {
                continue;
            }
            var features = featureService.getListingFeatures(listingId);
            result.put(listingId, features);
        }
        return result;
    }

    private Map<UUID, List<ListingPhotoView>> loadListingPhotos(List<Listing> listings) {
        Map<UUID, List<ListingPhotoView>> result = new HashMap<>();
        for (Listing listing : listings) {
            var listingId = listing.id();
            if (listingId == null) {
                continue;
            }
            var photos = listingMediaService.getListingPhotos(listingId);
            result.put(listingId, photos);
        }
        return result;
    }

    private ListingType resolveListingTypeFor(Listing listing, Map<UUID, ListingType> listingTypesById) {
        var typeId = listing.listingTypeId();
        if (typeId == null) {
            log.error("Listing {} privo di listingTypeId", listing.id());
            throw new InternalServerErrorException("Configurazione annuncio non valida.");
        }
        var type = listingTypesById.get(typeId);
        if (type == null) {
            log.error("Listing {} con listingType {} non trovato in cache", listing.id(), typeId);
            throw new InternalServerErrorException("Configurazione annuncio non valida.");
        }
        return type;
    }

    private ListingStatus resolveListingStatusFor(Listing listing, Map<UUID, ListingStatus> statusesById) {
        var statusId = listing.statusId();
        if (statusId == null) {
            log.error("Listing {} privo di statusId", listing.id());
            throw new InternalServerErrorException("Configurazione annuncio non valida.");
        }
        var status = statusesById.get(statusId);
        if (status == null) {
            log.error("Listing {} con listingStatus {} non trovato in cache", listing.id(), statusId);
            throw new InternalServerErrorException("Configurazione annuncio non valida.");
        }
        return status;
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (Double.isNaN(latitude)) {
            throw BadRequestException.forField("lat", "Il parametro 'lat' deve essere un numero valido.");
        }
        if (Double.isNaN(longitude)) {
            throw BadRequestException.forField("lng", "Il parametro 'lng' deve essere un numero valido.");
        }
        if (latitude < -90 || latitude > 90) {
            throw BadRequestException.forField("lat", "Il parametro 'lat' deve essere compreso tra -90 e 90.");
        }
        if (longitude < -180 || longitude > 180) {
            throw BadRequestException.forField("lng", "Il parametro 'lng' deve essere compreso tra -180 e 180.");
        }
    }

    private String normalizeUpper(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toUpperCase(Locale.ROOT);
    }

    private record SortDescriptor(String sortColumn, boolean ascending) {}
}
