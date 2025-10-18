package it.dieti.dietiestatesbackend.application.feature;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.NotFoundException;
import it.dieti.dietiestatesbackend.domain.feature.Feature;
import it.dieti.dietiestatesbackend.domain.feature.FeatureRepository;
import it.dieti.dietiestatesbackend.domain.feature.listing.ListingFeature;
import it.dieti.dietiestatesbackend.domain.feature.listing.ListingFeatureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class FeatureService {
    private final FeatureRepository featureRepository;
    private final ListingFeatureRepository listingFeatureRepository;

    public FeatureService(FeatureRepository featureRepository, ListingFeatureRepository listingFeatureRepository) {
        this.featureRepository = featureRepository;
        this.listingFeatureRepository = listingFeatureRepository;
    }

    public List<Feature> findAll() {
        return featureRepository.findAll();
    }

    @Transactional
    public void syncListingFeatures(UUID listingId, List<String> codes) {
        Objects.requireNonNull(listingId, "listingId is required");
        if (codes == null) {
            return;
        }

        var normalized = normalizeAndValidateCodes(codes);
        var desiredFeatures = new LinkedHashMap<UUID, Feature>();
        for (String code : normalized.keySet()) {
            var feature = findByCode(code);
            desiredFeatures.put(feature.id(), feature);
        }

        var existingAssociations = listingFeatureRepository.findByListingId(listingId);
        var existingByFeatureId = existingAssociations.stream()
                .collect(Collectors.toMap(ListingFeature::featureId, listingFeature -> listingFeature));

        for (var entry : desiredFeatures.entrySet()) {
            if (!existingByFeatureId.containsKey(entry.getKey())) {
                var entity = new ListingFeature(
                        null,
                        listingId,
                        entry.getKey(),
                        0,
                        null,
                        null
                );
                listingFeatureRepository.save(entity);
            }
        }

        for (var existing : existingAssociations) {
            if (!desiredFeatures.containsKey(existing.featureId())) {
                listingFeatureRepository.deleteById(existing.id());
            }
        }
    }

    public Feature findByCode(String code) {
        return featureRepository.findByCode(code).orElseThrow(
                () -> NotFoundException.resourceNotFound("feature", code)
        );
    }

    public List<Feature> getListingFeatures(UUID listingId) {
        return listingFeatureRepository.findByListingId(listingId).stream().map(f -> featureRepository.findById(f.featureId()).orElseThrow(
                () -> NotFoundException.resourceNotFound("feature", f.featureId())
        )).toList();
    }

    private Map<String, Integer> normalizeAndValidateCodes(List<String> codes) {
        var occurrences = new LinkedHashMap<String, Integer>();
        for (String rawCode : codes) {
            if (rawCode == null || rawCode.trim().isEmpty()) {
                throw BadRequestException.forField("features", "I codici delle feature non possono essere vuoti.");
            }
            var normalized = rawCode.trim();
            occurrences.merge(normalized, 1, Integer::sum);
        }

        var duplicates = occurrences.entrySet().stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .toList();
        if (!duplicates.isEmpty()) {
            var detail = "I codici delle feature devono essere univoci. Duplicati: " + String.join(", ", duplicates);
            throw BadRequestException.forField("features", detail);
        }
        return occurrences;
    }
}
