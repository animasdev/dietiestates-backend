package it.dieti.dietiestatesbackend.api;

import it.dieti.dietiestatesbackend.application.feature.FeatureService;
import it.dieti.dietiestatesbackend.domain.feature.Feature;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FeaturesApiDelegateImpl  implements FeaturesApiDelegate {

    private final FeatureService featureService;

    public FeaturesApiDelegateImpl(FeatureService featureService) {
        this.featureService = featureService;
    }

    @Override
    public ResponseEntity<List<String>> featuresGet(){
        var codes = featureService.findAll().stream().map(Feature::code).toList();
        return ResponseEntity.ok(codes);
    }
}
