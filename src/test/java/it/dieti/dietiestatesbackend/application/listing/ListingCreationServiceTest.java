package it.dieti.dietiestatesbackend.application.listing;

import it.dieti.dietiestatesbackend.application.exception.BadRequestException;
import it.dieti.dietiestatesbackend.application.exception.ForbiddenException;
import it.dieti.dietiestatesbackend.application.exception.listing.AgentProfileRequiredException;
import it.dieti.dietiestatesbackend.application.exception.listing.CoordinatesValidationException;
import it.dieti.dietiestatesbackend.application.exception.listing.PriceValidationException;
import it.dieti.dietiestatesbackend.domain.agent.Agent;
import it.dieti.dietiestatesbackend.domain.agent.AgentRepository;
import it.dieti.dietiestatesbackend.domain.listing.Listing;
import it.dieti.dietiestatesbackend.domain.listing.ListingRepository;
import it.dieti.dietiestatesbackend.domain.listing.ListingType;
import it.dieti.dietiestatesbackend.domain.listing.ListingTypeRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatus;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusRepository;
import it.dieti.dietiestatesbackend.domain.listing.status.ListingStatusesEnum;
import it.dieti.dietiestatesbackend.application.feature.FeatureService;
import it.dieti.dietiestatesbackend.application.notification.NotificationService;
import it.dieti.dietiestatesbackend.domain.user.User;
import it.dieti.dietiestatesbackend.domain.user.UserRepository;
import it.dieti.dietiestatesbackend.domain.user.role.Role;
import it.dieti.dietiestatesbackend.domain.user.role.RoleRepository;
import it.dieti.dietiestatesbackend.domain.user.role.RolesEnum;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.locationtech.jts.geom.Point;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ListingCreationServiceTest {

    @Mock
    private ListingRepository listingRepository;
    @Mock
    private ListingTypeRepository listingTypeRepository;
    @Mock
    private ListingStatusRepository listingStatusRepository;
    @Mock
    private AgentRepository agentRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private FeatureService featureService;
    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private ListingCreationService listingCreationService;

    private UUID userId;
    private UUID agentId;
    private UUID agencyId;
    private UUID typeId;
    private UUID statusId;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        agentId = UUID.randomUUID();
        agencyId = UUID.randomUUID();
        typeId = UUID.randomUUID();
        statusId = UUID.randomUUID();
    }

    @Test
    void createListingForUser_persistsListingWithDerivedAgencyAndGeo() {
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(listingTypeRepository.findByCode("SALE")).thenReturn(Optional.of(new ListingType(typeId, "SALE", "Vendita")));
        when(listingStatusRepository.findByCode("DRAFT"))
                .thenReturn(Optional.of(new ListingStatus(statusId, "DRAFT", "Bozza", 10, OffsetDateTime.now())));

        ArgumentCaptor<Listing> toSave = ArgumentCaptor.forClass(Listing.class);
        var savedId = UUID.randomUUID();
        when(listingRepository.save(toSave.capture())).thenAnswer(invocation -> {
            Listing listing = invocation.getArgument(0);
            return new Listing(
                    savedId,
                    listing.agencyId(),
                    listing.ownerAgentId(),
                    listing.listingTypeId(),
                    listing.statusId(),
                    listing.title(),
                    listing.description(),
                    listing.priceCents(),
                    listing.currency(),
                    listing.sizeSqm(),
                    listing.rooms(),
                    listing.floor(),
                    listing.energyClass(),
                    listing.contractDescription(),
                    listing.securityDepositCents(),
                    listing.furnished(),
                    listing.condoFeeCents(),
                    listing.petsAllowed(),
                    listing.addressLine(),
                    listing.city(),
                    listing.postalCode(),
                    listing.geo(),
                    listing.pendingDeleteUntil(),
                    listing.deletedAt(),
                    listing.publishedAt(),
                    OffsetDateTime.now(),
                    OffsetDateTime.now()
            );
        });

        var command = new ListingCreationService.CreateListingCommand(
                "  Appartamento Centro  ",
                "Descrizione",
                "sale",
                180000L,
                BigDecimal.valueOf(95.5),
                4,
                2,
                "A2",
                "Contratto 4+4",
                360000L,
                true,
                12000L,
                true,
                "Via Roma 10",
                "Napoli",
                "80100",
                40.8518,
                14.2681,
                false,
                List.of()
        );

        var result = listingCreationService.createListingForUser(userId, command);

        assertThat(result.id()).isEqualTo(savedId);
        assertThat(toSave.getValue().agencyId()).isEqualTo(agencyId);
        assertThat(toSave.getValue().ownerAgentId()).isEqualTo(agentId);
        assertThat(toSave.getValue().listingTypeId()).isEqualTo(typeId);
        assertThat(toSave.getValue().statusId()).isEqualTo(statusId);
        assertThat(toSave.getValue().priceCents()).isEqualTo(180000L);
        assertThat(toSave.getValue().currency()).isEqualTo("EUR");
        assertThat(toSave.getValue().sizeSqm()).isEqualTo(BigDecimal.valueOf(95.5));
        assertThat(toSave.getValue().title()).isEqualTo("Appartamento Centro");
        assertThat(toSave.getValue().contractDescription()).isEqualTo("Contratto 4+4");
        assertThat(toSave.getValue().securityDepositCents()).isEqualTo(360000L);
        assertThat(toSave.getValue().furnished()).isTrue();
        assertThat(toSave.getValue().condoFeeCents()).isEqualTo(12000L);
        assertThat(toSave.getValue().petsAllowed()).isTrue();
        assertThat(toSave.getValue().geo()).isInstanceOf(Point.class);
        assertThat(toSave.getValue().geo().getY()).isCloseTo(40.8518, within(1e-6));
        assertThat(toSave.getValue().geo().getX()).isCloseTo(14.2681, within(1e-6));
    }

    @Test
    void createListingForUser_requiresAgentProfile() {
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.empty());

        var command = new ListingCreationService.CreateListingCommand(
                "Monolocale",
                "Descrizione",
                "SALE",
                50000L,
                null,
                null,
                null,
                "A2",
                null,
                null,
                null,
                null,
                null,
                "Via",
                "City",
                null,
                41.0,
                12.5,
                false,
                List.of()
        );

        assertThrows(AgentProfileRequiredException.class, () -> listingCreationService.createListingForUser(userId, command));
    }

    @Test
    void createListingForUser_whenUserIsAdmin_throwsForbidden() {
        var adminRoleId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId,
                "Admin",
                "admin@example.com",
                false,
                adminRoleId,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        )));
        when(roleRepository.findById(adminRoleId)).thenReturn(Optional.of(new Role(adminRoleId, RolesEnum.ADMIN.name(), "Admin", "Amministratore")));

        var command = new ListingCreationService.CreateListingCommand(
                "Monolocale",
                "Descrizione",
                "SALE",
                100_000L,
                null,
                null,
                null,
                "A2",
                null,
                null,
                null,
                null,
                null,
                "Via",
                "City",
                null,
                41.0,
                12.5,
                false,
                List.of()
        );

        assertThrows(ForbiddenException.class, () -> listingCreationService.createListingForUser(userId, command));
    }

    @Test
    void createListingForUser_requiresPrice() {
        mockAgentWithBasics();

        var command = new ListingCreationService.CreateListingCommand(
                "Monolocale",
                "Descrizione",
                "SALE",
                null,
                null,
                null,
                null,
                "A2",
                null,
                null,
                null,
                null,
                null,
                "Via",
                "City",
                null,
                41.0,
                12.5,
                false,
                List.of()
        );

        assertThrows(PriceValidationException.class, () -> listingCreationService.createListingForUser(userId, command));
    }

    @Test
    void createListingForUser_requiresEnergyClass() {
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        var command = new ListingCreationService.CreateListingCommand(
                "Monolocale",
                "Descrizione",
                "SALE",
                100_000L,
                BigDecimal.valueOf(45),
                2,
                1,
                null,
                null,
                null,
                null,
                null,
                null,
                "Via Roma 10",
                "Napoli",
                "80100",
                41.0,
                12.5,
                false,
                List.of()
        );

        var exception = assertThrows(BadRequestException.class, () -> listingCreationService.createListingForUser(userId, command));
        assertThat(exception.fieldErrors())
                .anySatisfy(error -> assertThat(error.field()).isEqualTo("energyClass"));
    }

    @Test
    void createListingForUser_rejectsUnsupportedEnergyClass() {
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        var command = new ListingCreationService.CreateListingCommand(
                "Monolocale",
                "Descrizione",
                "SALE",
                100_000L,
                BigDecimal.valueOf(45),
                2,
                1,
                "Z",
                null,
                null,
                null,
                null,
                null,
                "Via Roma 10",
                "Napoli",
                "80100",
                41.0,
                12.5,
                false,
                List.of()
        );

        var exception = assertThrows(BadRequestException.class, () -> listingCreationService.createListingForUser(userId, command));
        assertThat(exception.fieldErrors())
                .anySatisfy(error -> {
                    assertThat(error.field()).isEqualTo("energyClass");
                    assertThat(error.message()).contains("Valori ammessi");
                });
    }

    @Test
    void createListingForUser_rejectsNegativeSecurityDeposit() {
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        var command = new ListingCreationService.CreateListingCommand(
                "Bilocale",
                "Descrizione",
                "SALE",
                100_000L,
                null,
                null,
                null,
                "A2",
                null,
                -100L,
                null,
                null,
                null,
                "Via Roma 10",
                "Napoli",
                "80100",
                41.0,
                12.5,
                false,
                List.of()
        );

        var exception = assertThrows(BadRequestException.class, () -> listingCreationService.createListingForUser(userId, command));
        assertThat(exception.fieldErrors())
                .anySatisfy(error -> assertThat(error.field()).isEqualTo("securityDepositCents"));
    }

    @Test
    void createListingForUser_rejectsNegativeCondoFee() {
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        var command = new ListingCreationService.CreateListingCommand(
                "Bilocale",
                "Descrizione",
                "SALE",
                100_000L,
                null,
                null,
                null,
                "A2",
                null,
                null,
                null,
                -500L,
                null,
                "Via Roma 10",
                "Napoli",
                "80100",
                41.0,
                12.5,
                false,
                List.of()
        );

        var exception = assertThrows(BadRequestException.class, () -> listingCreationService.createListingForUser(userId, command));
        assertThat(exception.fieldErrors())
                .anySatisfy(error -> assertThat(error.field()).isEqualTo("condoFeeCents"));
    }

    @Test
    void requestDeletion_whenOwnerAgent_marksListingPendingDelete() {
        var listingId = UUID.randomUUID();
        var agentRoleId = UUID.randomUUID();
        var pendingStatusId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var listing = new Listing(
                listingId,
                agencyId,
                agentId,
                typeId,
                statusId,
                "Titolo",
                "Descrizione",
                120_000L,
                "EUR",
                BigDecimal.valueOf(80),
                3,
                1,
                "B",
                null,
                0L,
                false,
                0L,
                false,
                "Via Roma",
                "Roma",
                "00100",
                null,
                null,
                null,
                now.minusDays(10),
                now.minusDays(20),
                now.minusDays(10)
        );

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId,
                "Agent User",
                "agent@example.com",
                true,
                agentRoleId,
                null,
                null,
                now.minusYears(1),
                now.minusMonths(1)
        )));
        when(roleRepository.findById(agentRoleId)).thenReturn(Optional.of(new Role(agentRoleId, RolesEnum.AGENT.name(), "Agent", "Agente")));
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, now, now);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(listingStatusRepository.findById(statusId))
                .thenReturn(Optional.of(new ListingStatus(statusId, ListingStatusesEnum.PUBLISHED.getDescription(), "Pubblicato", 2, now)));
        when(listingStatusRepository.findByCode(ListingStatusesEnum.PENDING_DELETE.getDescription()))
                .thenReturn(Optional.of(new ListingStatus(pendingStatusId, ListingStatusesEnum.PENDING_DELETE.getDescription(), "Pending delete", 3, now)));
        when(listingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var result = listingCreationService.requestDeletion(userId, listingId, null);

        assertThat(result.statusId()).isEqualTo(pendingStatusId);
        assertThat(result.pendingDeleteUntil()).isNotNull();
        assertThat(result.pendingDeleteUntil()).isCloseTo(now.plusHours(24), within(1, ChronoUnit.MINUTES));
        verify(notificationService).sendDeleteListing("agent@example.com", "Titolo", listingId, null);
    }

    @Test
    void requestDeletion_whenAdmin_succeedsWithoutAgentProfile() {
        var listingId = UUID.randomUUID();
        var adminRoleId = UUID.randomUUID();
        var pendingStatusId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var listing = new Listing(
                listingId,
                agencyId,
                agentId,
                typeId,
                statusId,
                "Titolo",
                "Descrizione",
                120_000L,
                "EUR",
                BigDecimal.valueOf(80),
                3,
                1,
                "B",
                null,
                0L,
                false,
                0L,
                false,
                "Via Roma",
                "Roma",
                "00100",
                null,
                null,
                null,
                now.minusDays(10),
                now.minusDays(20),
                now.minusDays(10)
        );

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId,
                "Admin",
                "admin@example.com",
                true,
                adminRoleId,
                null,
                null,
                now.minusYears(1),
                now.minusMonths(1)
        )));
        when(roleRepository.findById(adminRoleId)).thenReturn(Optional.of(new Role(adminRoleId, RolesEnum.ADMIN.name(), "Admin", "Amministratore")));
        when(listingStatusRepository.findById(statusId))
                .thenReturn(Optional.of(new ListingStatus(statusId, ListingStatusesEnum.PUBLISHED.getDescription(), "Pubblicato", 2, now)));
        when(listingStatusRepository.findByCode(ListingStatusesEnum.PENDING_DELETE.getDescription()))
                .thenReturn(Optional.of(new ListingStatus(pendingStatusId, ListingStatusesEnum.PENDING_DELETE.getDescription(), "Pending delete", 3, now)));
        var ownerAgent = new Agent(agentId, UUID.randomUUID(), agencyId, "REA123", null, now, now);
        when(agentRepository.findById(agentId)).thenReturn(Optional.of(ownerAgent));
        var ownerUserRoleId = UUID.randomUUID();
        when(userRepository.findById(ownerAgent.userId())).thenReturn(Optional.of(new User(
                ownerAgent.userId(),
                "Owner agent",
                "owner@example.com",
                true,
                ownerUserRoleId,
                null,
                null,
                now.minusYears(2),
                now.minusMonths(2)
        )));
        when(listingRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var reason = "Violazione termini";
        var result = listingCreationService.requestDeletion(userId, listingId, reason);

        assertThat(result.statusId()).isEqualTo(pendingStatusId);
        assertThat(result.pendingDeleteUntil()).isNotNull();
        verify(notificationService).sendDeleteListing("owner@example.com", "Titolo", listingId, reason);
    }

    @Test
    void requestDeletion_whenAgentDoesNotOwnListing_throwsForbidden() {
        var listingId = UUID.randomUUID();
        var agentRoleId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var listing = new Listing(
                listingId,
                agencyId,
                UUID.randomUUID(),
                typeId,
                statusId,
                "Titolo",
                "Descrizione",
                120_000L,
                "EUR",
                BigDecimal.valueOf(80),
                3,
                1,
                "B",
                null,
                0L,
                false,
                0L,
                false,
                "Via Roma",
                "Roma",
                "00100",
                null,
                null,
                null,
                now.minusDays(10),
                now.minusDays(20),
                now.minusDays(10)
        );

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId,
                "Agent User",
                "agent@example.com",
                true,
                agentRoleId,
                null,
                null,
                now.minusYears(1),
                now.minusMonths(1)
        )));
        when(roleRepository.findById(agentRoleId)).thenReturn(Optional.of(new Role(agentRoleId, RolesEnum.AGENT.name(), "Agent", "Agente")));
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, now, now);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));

        assertThrows(ForbiddenException.class, () -> listingCreationService.requestDeletion(userId, listingId, null));
    }

    @Test
    void requestDeletion_whenListingNotPublished_throwsBadRequest() {
        var listingId = UUID.randomUUID();
        var agentRoleId = UUID.randomUUID();
        var now = OffsetDateTime.now();

        var listing = new Listing(
                listingId,
                agencyId,
                agentId,
                typeId,
                statusId,
                "Titolo",
                "Descrizione",
                120_000L,
                "EUR",
                BigDecimal.valueOf(80),
                3,
                1,
                "B",
                null,
                0L,
                false,
                0L,
                false,
                "Via Roma",
                "Roma",
                "00100",
                null,
                null,
                null,
                now.minusDays(10),
                now.minusDays(20),
                now.minusDays(10)
        );

        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));
        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId,
                "Agent User",
                "agent@example.com",
                true,
                agentRoleId,
                null,
                null,
                now.minusYears(1),
                now.minusMonths(1)
        )));
        when(roleRepository.findById(agentRoleId)).thenReturn(Optional.of(new Role(agentRoleId, RolesEnum.AGENT.name(), "Agent", "Agente")));
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, now, now);
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(listingStatusRepository.findById(statusId))
                .thenReturn(Optional.of(new ListingStatus(statusId, ListingStatusesEnum.DRAFT.getDescription(), "Bozza", 2, now)));

        assertThrows(BadRequestException.class, () -> listingCreationService.requestDeletion(userId, listingId, null));
    }

    @Test
    void createListingForUser_rejectsNegativePrice() {
        mockAgentWithBasics();

        var command = new ListingCreationService.CreateListingCommand(
                "Monolocale",
                "Descrizione",
                "SALE",
                -1L,
                null,
                null,
                null,
                "A2",
                null,
                null,
                null,
                null,
                null,
                "Via",
                "City",
                null,
                41.0,
                12.5,
                false,
                List.of()

        );

        assertThrows(PriceValidationException.class, () -> listingCreationService.createListingForUser(userId, command));
    }

    @Test
    void createListingForUser_rejectsLatitudeOutOfRange() {
        mockAgentWithBasics();

        var command = new ListingCreationService.CreateListingCommand(
                "Monolocale",
                "Descrizione",
                "SALE",
                1000L,
                null,
                null,
                null,
                "A2",
                null,
                null,
                null,
                null,
                null,
                "Via",
                "City",
                null,
                95.0,
                12.5,
                false,
                List.of()

        );

        assertThrows(CoordinatesValidationException.class, () -> listingCreationService.createListingForUser(userId, command));
    }

    @Test
    void updateListingForUser_allowsAdminToEditListingsRegardlessOfOwnership() {
        var listingId = UUID.randomUUID();
        var adminRoleId = UUID.randomUUID();
        var listingOwnerAgentId = UUID.randomUUID();
        var listing = new Listing(
                listingId,
                UUID.randomUUID(),
                listingOwnerAgentId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                "Vecchio Titolo",
                "Vecchia descrizione",
                100_000L,
                "EUR",
                BigDecimal.valueOf(80),
                3,
                1,
                "B",
                null,
                0L,
                false,
                0L,
                false,
                "Via Vecchia",
                "Pisa",
                "56100",
                null,
                null,
                null,
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now().minusDays(20),
                OffsetDateTime.now().minusDays(10)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId,
                "Admin User",
                "admin@example.com",
                false,
                adminRoleId,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        )));
        when(roleRepository.findById(adminRoleId)).thenReturn(Optional.of(new Role(adminRoleId, RolesEnum.ADMIN.name(), "Admin", "Amministratore")));
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.empty());
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        ArgumentCaptor<Listing> savedListingCaptor = ArgumentCaptor.forClass(Listing.class);
        when(listingRepository.save(savedListingCaptor.capture())).thenAnswer(invocation -> invocation.getArgument(0));

        var command = new ListingCreationService.UpdateListingCommand(
                "Nuovo Titolo",
                "Nuova descrizione",
                120_000L,
                BigDecimal.valueOf(90),
                4,
                2,
                "A2",
                "Nuovo contratto",
                200000L,
                false,
                8000L,
                true,
                "Via Nuova",
                "Firenze",
                "50100",
                43.7696,
                11.2558,
                List.of("ASCENSORE")
        );

        var result = listingCreationService.updateListingForUser(userId, listingId, command);

        assertThat(result.title()).isEqualTo("Nuovo Titolo");
        assertThat(result.ownerAgentId()).isEqualTo(listingOwnerAgentId);

        var persisted = savedListingCaptor.getValue();
        assertThat(persisted.title()).isEqualTo("Nuovo Titolo");
        assertThat(persisted.addressLine()).isEqualTo("Via Nuova");
        assertThat(persisted.ownerAgentId()).isEqualTo(listingOwnerAgentId);
        assertThat(persisted.contractDescription()).isEqualTo("Nuovo contratto");
        assertThat(persisted.securityDepositCents()).isEqualTo(200000L);
        assertThat(persisted.condoFeeCents()).isEqualTo(8000L);
        assertThat(persisted.petsAllowed()).isTrue();
    }

    @Test
    void updateListingForUser_rejectsUnsupportedEnergyClass() {
        var listingId = UUID.randomUUID();
        var agentRoleId = UUID.randomUUID();
        var listing = new Listing(
                listingId,
                agencyId,
                agentId,
                typeId,
                statusId,
                "Titolo",
                "Descrizione",
                100_000L,
                "EUR",
                BigDecimal.valueOf(70),
                3,
                1,
                "B",
                null,
                0L,
                false,
                0L,
                false,
                "Via Roma",
                "Roma",
                "00100",
                null,
                null,
                null,
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now().minusDays(20),
                OffsetDateTime.now().minusDays(10)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId,
                "Agent",
                "agent@example.com",
                true,
                agentRoleId,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        )));
        when(roleRepository.findById(agentRoleId)).thenReturn(Optional.of(new Role(agentRoleId, RolesEnum.AGENT.name(), "Agent", "Agente")));
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        var command = new ListingCreationService.UpdateListingCommand(
                null,
                null,
                null,
                null,
                null,
                null,
                "Z",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(BadRequestException.class, () -> listingCreationService.updateListingForUser(userId, listingId, command));
    }

    @Test
    void updateListingForUser_rejectsBlankEnergyClass() {
        var listingId = UUID.randomUUID();
        var agentRoleId = UUID.randomUUID();
        var listing = new Listing(
                listingId,
                agencyId,
                agentId,
                typeId,
                statusId,
                "Titolo",
                "Descrizione",
                100_000L,
                "EUR",
                BigDecimal.valueOf(70),
                3,
                1,
                "B",
                null,
                0L,
                false,
                0L,
                false,
                "Via Roma",
                "Roma",
                "00100",
                null,
                null,
                null,
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now().minusDays(20),
                OffsetDateTime.now().minusDays(10)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId,
                "Agent",
                "agent@example.com",
                true,
                agentRoleId,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        )));
        when(roleRepository.findById(agentRoleId)).thenReturn(Optional.of(new Role(agentRoleId, RolesEnum.AGENT.name(), "Agent", "Agente")));
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        var command = new ListingCreationService.UpdateListingCommand(
                null,
                null,
                null,
                null,
                null,
                null,
                "   ",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(BadRequestException.class, () -> listingCreationService.updateListingForUser(userId, listingId, command));
    }

    @Test
    void updateListingForUser_rejectsNegativeSecurityDeposit() {
        var listingId = UUID.randomUUID();
        var agentRoleId = UUID.randomUUID();
        var listing = new Listing(
                listingId,
                agencyId,
                agentId,
                typeId,
                statusId,
                "Titolo",
                "Descrizione",
                100_000L,
                "EUR",
                BigDecimal.valueOf(70),
                3,
                1,
                "B",
                null,
                0L,
                false,
                0L,
                false,
                "Via Roma",
                "Roma",
                "00100",
                null,
                null,
                null,
                OffsetDateTime.now().minusDays(10),
                OffsetDateTime.now().minusDays(20),
                OffsetDateTime.now().minusDays(10)
        );

        when(userRepository.findById(userId)).thenReturn(Optional.of(new User(
                userId,
                "Agent",
                "agent@example.com",
                true,
                agentRoleId,
                null,
                null,
                OffsetDateTime.now(),
                OffsetDateTime.now()
        )));
        when(roleRepository.findById(agentRoleId)).thenReturn(Optional.of(new Role(agentRoleId, RolesEnum.AGENT.name(), "Agent", "Agente")));
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(listingRepository.findById(listingId)).thenReturn(Optional.of(listing));

        var command = new ListingCreationService.UpdateListingCommand(
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                -1L,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null
        );

        assertThrows(BadRequestException.class, () -> listingCreationService.updateListingForUser(userId, listingId, command));
    }

    private void mockAgentWithBasics() {
        var agent = new Agent(agentId, userId, agencyId, "REA123", null, OffsetDateTime.now(), OffsetDateTime.now());
        when(agentRepository.findByUserId(userId)).thenReturn(Optional.of(agent));
        when(listingTypeRepository.findByCode("SALE"))
                .thenReturn(Optional.of(new ListingType(typeId, "SALE", "Vendita")));
        when(listingStatusRepository.findByCode("DRAFT"))
                .thenReturn(Optional.of(new ListingStatus(statusId, "DRAFT", "Bozza", 10, OffsetDateTime.now())));
    }
}
