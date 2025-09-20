# API planning
Di seguito è indicata la pianificazione degli endpoint restful esposti dall'applicazione:
- `/users` CRUD per utenti
- `/agents` CRUD per agenti
- `/agencies` CRUD per agenzie
- `/auth` per gestire l'autenticazione ( emissione e refresh dei token) e gli identity providers (ad esempio Google)
- `/listings` per gestire gli annunci: CRUD; gestione degli stati; moderazione da parte degli admin; foto collegate.
- `/notifications` per gestire le notifiche email verso gli utenti
Inoltre i seguenti endpoint gestiscono le entità di dominio:
- `/features` per gestire i servizi aggiuntivi


## API‑First e Codegen (OpenAPI)
Per ridurre lo scaffolding adottiamo un approccio API‑First con generazione del codice a partire dalla specifica OpenAPI.
- Specifica: `docs/openapi.yaml`.
- Generazione: plugin Maven `openapi-generator-maven-plugin` (v7.x) con generator "spring" e `delegatePattern=true`.
- Output generato: `target/generated-sources/openapi` (non viene committato).
- Pacchetti:
    - API: `it.dieti.dietiestatesbackend.api`
    - Modelli: `it.dieti.dietiestatesbackend.api.model` (suffisso `Dto` per evitare collisioni con tipi di dominio o Spring Data `Page`).

### Come funziona (Delegate Pattern)
- Il generatore crea i controller REST e le interfacce `*ApiDelegate` per ciascun tag dell’OpenAPI (Auth, Listings, Listing Photos, Features).
- Noi implementiamo solo piccole classi Spring (`@Service`) che implementano le interfacce `*ApiDelegate` (es. `ListingsApiDelegateImpl`).
- Rigenerare la specifica è sicuro: i nostri `*DelegateImpl` non vengono sovrascritti.

### Comandi
- Generazione codice: `mvn -DskipTests generate-sources`
- Avvio applicazione: `mvn spring-boot:run` → Swagger UI su `http://localhost:8080/swagger-ui.html` (protetta dev/dev in dev).
- Dopo modifiche alla specifica: rieseguire `generate-sources` e ricompilare.
