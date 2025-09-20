# DietiEstates25 — Backend

Scopo: fornire le API dell’applicazione DietiEstates25 (progetto d’esame di Ingegneria del Software) per la gestione di annunci immobiliari: gestione utenti e autenticazione, censimento/modifica annunci, ricerca, moderazione e notifiche.

## Stack
- Java 21, Spring Boot 3.5.5
- Spring Web, Security (+ OAuth2 Client per Google), Data JPA, Validation
- PostgreSQL 16 + PostGIS, Flyway
- Actuator, Springdoc OpenAPI UI
- (Dev) Supabase Storage in locale per gestione file/immagini

## Avvio rapido
Prerequisiti: JDK 21, Maven 3.9+, Docker.

1. Database (Docker):
   - `docker compose up -d` (mappa host: `55432 -> 5432`)
2. Config app (di default):
   - `src/main/resources/application.yml` → datasource `jdbc:postgresql://127.0.0.1:55432/dietiestates25`, user/pass `app/app`
3. Avvio applicazione:
   - Da IDE (classe main) oppure `mvn spring-boot:run`
4. Verifiche:
   - Health: `http://localhost:8080/actuator/health`
   - Swagger UI: `http://localhost:8080/swagger-ui.html`
   - Credenziali dev: `dev / dev`

## Documentazione
- Vedi cartella `docs/`
