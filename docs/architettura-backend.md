# Architettura Backend (DietiEstates25)

Obiettivo: backend monolite modulare con confini chiari tra sottosistemi e persistenza affidabile.

## Moduli applicativi
- Auth & Users: autenticazione (JWT proprietari), federazione OAuth2/OIDC (Google, in seguito), profili utente/ruoli.
- Listings: gestione annunci (censimento, foto, versioning, soft delete, moderazione e ricerca).
- (TODO) Notifications: invio email asincrono (coda outbox + worker), audit delle notifiche.

## Persistenza e migrazioni
- DB: PostgreSQL 16 + PostGIS (estensione abilitata via Flyway).
- Migrazioni: Flyway (`db/migration`), schema versionato e ripetibile.
-  (TODO) Media: Supabase Storage (locale, solo per sviluppo) per file/immagini; URL memorizzati nel DB.

## Integrazioni e librerie
- Spring Security + OAuth2 Client (Google) per federazione login; emissione di token applicativi prevista lato backend.
- Springdoc OpenAPI UI per documentare le API (`/v3/api-docs`, `/swagger-ui.html`).
- Actuator per health/info e diagnostica runtime.
- OpenAPI Generator (Maven) per generazione automatica delle API: vedi `apis.md`.

## Stile architetturale
- Esagonale (Ports & Adapters) / Clean Architecture.
- Confini chiari:
  - Adapter “driving” (ingress): Web REST (OpenAPI generator, `*ApiDelegateImpl`).
  - Application: orchestrano validazioni, transazioni, policy.
  - Domain: modello e interfacce senza dipendenze da framework.
  - Adapter “driven” (egress): persistenza JPA, storage esterni, API di terze parti.

## Pattern Repository (persistenza)
- Il dominio espone un'interfaccia `Repository` che descrive le operazioni necessarie.
- Un adapter di persistenza implementa l'interfaccia usando Spring Data JPA; mappa tra `Entity` (JPA) e `Domain` (puro).
- Benefici: isolamento dal DB, testabilità, sostituibilità degli adapter.

## Esempio concreto: GET /features
Implementazione completa basata su Hexagonal + Repository pattern.

- Domain (puro, senza annotazioni):
  - Modello: `src/main/java/it/dieti/dietiestatesbackend/domain/feature/Feature.java`
  - Interfaccia: `src/main/java/it/dieti/dietiestatesbackend/domain/feature/FeatureRepository.java`
- Application (casi d’uso):
  - Servizio: `src/main/java/it/dieti/dietiestatesbackend/application/feature/FeatureService.java`
- Persistence adapter (JPA):
  - Entity JPA: `src/main/java/it/dieti/dietiestatesbackend/infrastructure/persistence/jpa/feature/FeatureEntity.java`
  - Spring Data repo: `src/main/java/it/dieti/dietiestatesbackend/infrastructure/persistence/jpa/feature/FeatureJpaRepository.java`
  - Adapter porta→JPA: `src/main/java/it/dieti/dietiestatesbackend/infrastructure/persistence/jpa/feature/FeatureRepositoryJpaAdapter.java`
- Web adapter (OpenAPI):
  - Delegate: `src/main/java/it/dieti/dietiestatesbackend/api/FeaturesApiDelegateImpl.java`

## Guida step-by-step (teoria → esempio)
1) Definisci il modello e l'interfaccia di dominio;
   - Esempio: vedi `Feature` e `FeatureRepository`:
     - `domain/feature/Feature.java`
     - `domain/feature/FeatureRepository.java`

2) Crea il servizio applicativo che conterrà la logica di business;
   - Esempio: vedi `FeatureService`:
     - `application/feature/FeatureService.java`

3) Modella l’entity JPA e la repository Spring Data;
   - l’Entity riflette la tabella; la repo Spring Data fornisce CRUD e query; non trapela al dominio.
   - Esempio: vedi `FeatureEntity` e `FeatureJpaRepository`:
     - `infrastructure/persistence/jpa/feature/FeatureEntity.java`
     - `infrastructure/persistence/jpa/feature/FeatureJpaRepository.java`

4) Implementa l’adapter della porta
   - classe `@Repository` che implementa l'interfaccia di dominio e mappa Entity <---> Domain.
   - Esempio: vedi `FeatureRepositoryJpaAdapter` (metodo `toDomain`):
     - `infrastructure/persistence/jpa/feature/FeatureRepositoryJpaAdapter.java`

5) Integra nel Web adapter (OpenAPI)
   - il delegate traduce DTO <---> dominio e chiama il service;
   - Esempio: vedi `FeaturesApiDelegateImpl` (stream -> `.map(Feature::code)`):
     - `api/FeaturesApiDelegateImpl.java`

Note progettuali
- Il dominio resta privo di annotazioni; la mappatura avviene negli adapter.
- Le transazioni sono dichiarate nel service; per query usare `@Transactional(readOnly = true)`.
- Ordinamento e paging lato persistence adapter (es. `Sort.by("code").ascending()`).
- Lo schema DB è gestito da Flyway (`db/migration`); la tabella `features` è creata/seedata in `V2__lookups.sql`.

## Bootstrap: SUPERADMIN di default
- All'avvio un `ApplicationRunner` verifica se esiste l'utente SUPERADMIN; se mancante, lo crea con una password casuale e la registra nei log una sola volta.
- Servizio: `UserService.createDefaultSuperAdminIfMissing()` restituisce la password in chiaro generata oppure `null` se esiste già.
- Credenziali di default:
  - email: `superadmin@dietiestates.local`
  - displayName: `Super Admin`
- Attenzione: la password è loggata solo alla prima creazione; cambiarla al primo accesso.

## Autenticazione & JWT
- Flusso di login
  - Endpoint: `POST /auth/login` (permitAll) riceve `{ email, password }` e risponde con `{ access_token }`.
  - Il servizio verifica l'hash (bcrypt per default) e rilascia un JWT valido 1 ora.
- Dato ausiliario in risposta
  - Il login restituisce anche `first_access: true|false` per consentire al FE di forzare il cambio password al primo accesso senza chiamate aggiuntive.
- Token
  - Algoritmo: HS256 tramite Spring Security (Nimbus), chiave in `app.jwt.secret`.
  - Claims: minimi — `sub` (userId), `iat`, `exp`, `iss`. Nessun dato personale nel token.
  - Validazione: Resource Server (`spring-boot-starter-oauth2-resource-server`) controlla i Bearer sui percorsi protetti.
- Configurazione sicurezza
  - Pubblici: `/auth/login`, `/auth/refresh` (futuro), `/v3/api-docs/**`, `/swagger-ui/**`, `/actuator/health`, `/actuator/info`.
  - Protetti: tutti gli altri endpoint con `Authorization: Bearer <JWT>`.
- Uso lato FE
  - Dopo il login, salvare `access_token` e chiamare le API protette con Bearer.
  - Se servono i dati profilo, usare un endpoint `/me` subito dopo (meglio che inserire PII nel token).
