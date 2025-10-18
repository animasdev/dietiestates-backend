package it.dieti.dietiestatesbackend.application.feature;

import it.dieti.dietiestatesbackend.application.exception.NotFoundException;
import it.dieti.dietiestatesbackend.domain.feature.Feature;
import it.dieti.dietiestatesbackend.domain.feature.FeatureRepository;
import it.dieti.dietiestatesbackend.domain.feature.listing.ListingFeature;
import it.dieti.dietiestatesbackend.domain.feature.listing.ListingFeatureRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class FeatureService {
    private FeatureRepository featureRepository;
    private ListingFeatureRepository listingFeatureRepository;

    public FeatureService(FeatureRepository featureRepository, ListingFeatureRepository listingFeatureRepository) {
        this.featureRepository = featureRepository;
        this.listingFeatureRepository = listingFeatureRepository;
    }

    public List<Feature> findAll() {
        return featureRepository.findAll();
    }

    @Transactional
    public void saveListingFeatues(UUID listingId, List<String> codes) {
        codes.forEach(code -> {
            var featureId = findByCode(code).id();
            ListingFeature entity =  new ListingFeature(
                    null,
                    listingId,
                    featureId,
                    0,
                    null,
                    null
            );
            listingFeatureRepository.save(entity);
        });
    }

    public Feature findByCode(String code) {
        return featureRepository.findByCode(code).orElseThrow(
                () -> NotFoundException.resourceNotFound("feature",code)
        );
    }

    public List<Feature> getListingFeatures(UUID listingId) {
        return listingFeatureRepository.findByListingId(listingId).stream().map(f-> featureRepository.findById(f.featureId()).orElseThrow(
                ()-> NotFoundException.resourceNotFound("feature",f.featureId())
        )).toList();
    }

}
