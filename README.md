# DietiEstates Backend

Backend Spring Boot per l’applicazione DietiEstates (progetto d’esame di Ingegneria del Software). Espone API per autenticazione, gestione utenti, annunci, moderazione e media.

## Requisiti
- Docker e Docker Compose
- (Opzionale per esecuzione senza Docker) JDK 21 e Maven 3.9+

## Avvio con Docker Compose
1) Preparazione `.env`
- Copia `.env.example` in `.env` e, se necessario, personalizza le variabili. Il sistema ha default sensati, quindi puoi anche avviare senza modifiche.

Variabili principali (.env):
- `APP_PORT` (default `8080`): porta HTTP dell’app esposta sull’host
- `DB_NAME` (default `dietiestates25`), `DB_USER` (default `app`), `DB_PASSWORD` (default `app`)
- `DB_PORT_ON_HOST` (default `54322`): porta Postgres esposta sull’host
- Opzionali (override): `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`, `SPRING_FLYWAY_ENABLED`, `SPRING_PROFILES_ACTIVE`, `JAVA_OPTS`
- Mail: `MAIL_ENABLED`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_FROM`, `MAIL_FROM_NAME`
- Bootstrap superadmin: `APP_BOOTSTRAP_SUPERADMIN_ENABLED`, `APP_BOOTSTRAP_SUPERADMIN_EMAIL`, `APP_BOOTSTRAP_SUPERADMIN_NAME`

2) Avvio
- `docker compose up -d --build`

3) Verifica
- App: `http://localhost:${APP_PORT:-8080}/actuator/health`
- Swagger UI: `http://localhost:${APP_PORT:-8080}/swagger-ui.html`
- Log applicazione: `docker compose logs -f app`
- Stato servizi: `docker compose ps`

### Persistenza dati e reset
- I dati del database sono salvati nel volume Docker `db_data`. I file media sono nel volume `media_data`.
- Per un database pulito:
  - Cambia `DB_NAME` in `.env` (nuovo DB) oppure
  - Esegui reset distruttivo dei volumi: `docker compose down -v && docker compose up -d --build`

### Bootstrap Superadmin
- Se `APP_BOOTSTRAP_SUPERADMIN_ENABLED=true`, all’avvio viene creato un account SUPERADMIN se assente, usando l’email `APP_BOOTSTRAP_SUPERADMIN_EMAIL` e un nome visualizzato `APP_BOOTSTRAP_SUPERADMIN_NAME`.
- Una password sicura temporanea viene generata e loggata una sola volta. Controlla: `docker compose logs -f app` (cerca "Created default SUPERADMIN account").
- Dopo la creazione puoi impostare `APP_BOOTSTRAP_SUPERADMIN_ENABLED=false` se vuoi evitare il controllo ad ogni avvio.

## Esecuzione locale (senza Docker)
- Per default `src/main/resources/application.yml` punta a `jdbc:postgresql://127.0.0.1:54322/dietiestates25` (allineato a Docker Compose).
- Avvia un Postgres (ad es. il servizio `db` di compose) e poi:
  - `mvn spring-boot:run`
- Per collegarti a un DB differente, esporta:
  - `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`

## Strumenti
- Git report: esegui `python3 git-report.py` dalla root del repo per stampare statistiche rapide (commits per mese, giorno della settimana, file toccati per commit).
- SonarCloud: analisi di qualità del codice su
  - https://sonarcloud.io/project/overview?id=animasdev_dietiestates-backend

## Stack (riepilogo)
- Java 21, Spring Boot 3.x
- Spring Web, Security, Data JPA, Validation
- PostgreSQL 16 + PostGIS, Flyway
- Actuator, Springdoc OpenAPI UI
