package it.dieti.dietiestatesbackend.application.feature;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.domain.feature.Feature;
import it.dieti.dietiestatesbackend.domain.feature.FeatureRepository;
import it.dieti.dietiestatesbackend.domain.feature.listing.ListingFeature;
import it.dieti.dietiestatesbackend.domain.feature.listing.ListingFeatureRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FeatureServiceTest {

    @Mock
    private FeatureRepository featureRepository;
    @Mock
    private ListingFeatureRepository listingFeatureRepository;

    @InjectMocks
    private FeatureService featureService;

    @Test
    void syncListingFeatures_returnsWhenCodesNull() {
        var listingId = UUID.randomUUID();

        featureService.syncListingFeatures(listingId, null);

        verify(listingFeatureRepository, never()).save(any(ListingFeature.class));
    }

    @Test
    void syncListingFeatures_throwsBadRequestForDuplicates() {
        var listingId = UUID.randomUUID();
        var codes = List.of("wifi", "wifi");

        assertThrows(BadRequestException.class, () -> featureService.syncListingFeatures(listingId, codes));
        verify(listingFeatureRepository, never()).save(any(ListingFeature.class));
    }

    @Test
    void syncListingFeatures_persistsUniqueCodes() {
        var listingId = UUID.randomUUID();
        var wifiId = UUID.randomUUID();
        var poolId = UUID.randomUUID();
        when(featureRepository.findByCode("wifi")).thenReturn(Optional.of(new Feature(wifiId, "wifi", "Wi-Fi")));
        when(featureRepository.findByCode("pool")).thenReturn(Optional.of(new Feature(poolId, "pool", "Pool")));
        when(listingFeatureRepository.findByListingId(listingId)).thenReturn(List.of());
        when(listingFeatureRepository.save(any(ListingFeature.class))).thenAnswer(invocation -> invocation.getArgument(0));

        featureService.syncListingFeatures(listingId, List.of("wifi", "pool"));

        verify(listingFeatureRepository).save(new ListingFeature(null, listingId, wifiId, 0, null, null));
        verify(listingFeatureRepository).save(new ListingFeature(null, listingId, poolId, 0, null, null));
    }
}
