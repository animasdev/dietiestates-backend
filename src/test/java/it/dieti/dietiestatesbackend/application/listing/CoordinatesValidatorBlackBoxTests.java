package it.dieti.dietiestatesbackend.application.listing;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@org.junit.jupiter.api.DisplayName("Black-box tests: CoordinatesValidator.validate")
class CoordinatesValidatorBlackBoxTests {

    private CoordinatesValidator validator;

    @BeforeEach
    void setUp() {
        validator = new CoordinatesValidator();
    }

    @Test
    @org.junit.jupiter.api.DisplayName("Valido: lat/lng nei range → Successo")
    // Tupla: T1 — Copre classi: CE1.D (valido), CE2.D (valido)
    void validate_valid_success() {
        double lat = 40.0;
        double lng = 14.0;
        assertDoesNotThrow(() -> validator.validate(lat, lng));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("lat = NaN → BadRequest")
    // Tupla: T2 — Copre classi: CE1.A (NaN), CE2.D (valido)
    void validate_latNaN_badRequest() {
        Double lat = Double.NaN;
        double lng = 14.0;
        assertThrows(BadRequestException.class, () -> validator.validate(lat, lng));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("lat < -90 → BadRequest")
    // Tupla: T3 — Copre classi: CE1.B (< -90), CE2.D (valido)
    void validate_latBelowMin_badRequest() {
        double lat = -91.0;
        double lng = 14.0;
        assertThrows(BadRequestException.class, () -> validator.validate(lat, lng));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("lat > 90 → BadRequest")
    // Tupla: T4 — Copre classi: CE1.C (> 90), CE2.D (valido)
    void validate_latAboveMax_badRequest() {
        double lat = 91.0;
        double lng = 14.0;
        assertThrows(BadRequestException.class, () -> validator.validate(lat, lng));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("lng = NaN → BadRequest")
    // Tupla: T5 — Copre classi: CE1.D (valido), CE2.A (NaN)
    void validate_lngNaN_badRequest() {
        double lat = 40.0;
        Double lng = Double.NaN;
        assertThrows(BadRequestException.class, () -> validator.validate(lat, lng));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("lng < -180 → BadRequest")
    // Tupla: T6 — Copre classi: CE1.D (valido), CE2.B (< -180)
    void validate_lngBelowMin_badRequest() {
        double lat = 40.0;
        double lng = -181.0;
        assertThrows(BadRequestException.class, () -> validator.validate(lat, lng));
    }

    @Test
    @org.junit.jupiter.api.DisplayName("lng > 180 → BadRequest")
    // Tupla: T7 — Copre classi: CE1.D (valido), CE2.C (> 180)
    void validate_lngAboveMax_badRequest() {
        double lat = 40.0;
        double lng = 181.0;
        assertThrows(BadRequestException.class, () -> validator.validate(lat, lng));
    }
}

