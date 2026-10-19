# Cash Card

A learning project: a small Spring Boot REST API for managing cash cards, with persistence, pagination, and per-owner security.

- **Stack:** Spring Boot 4.1.1, Java 17, Maven (wrapper included), Spring Web MVC, Spring Data JDBC, H2 (in-memory), Spring Security
- **Package:** `com.example.cashcard`

## Quick start

```bash
./mvnw spring-boot:run      # start the app on http://localhost:8081
./mvnw test                 # run all tests
```

The app runs on port **8081** (`server.port` in `application.properties`) because 8080 was already used by a Docker container. If you ever see `Port 8080 was already in use`, find the culprit with:

```bash
lsof -nP -iTCP:8080 -sTCP:LISTEN
docker ps
```

The database is in memory, so data is wiped each time the app stops. `data.sql` lives in `src/test/resources`, so seed data only loads during tests. When running the app, create cards yourself first.

## API

All endpoints are under `/cashcards` and need HTTP Basic auth plus the `CARD-OWNER` role. Cards are scoped to their owner, so a user only ever sees and changes their own.

| Method | Path | Success | Failure |
|---|---|---|---|
| `GET` | `/cashcards/{id}` | `200` + card | `404` if missing or not yours |
| `GET` | `/cashcards` | `200` + list | |
| `POST` | `/cashcards` | `201` + `Location` header + card | |
| `PUT` | `/cashcards/{id}` | `204` | `404` if missing or not yours |
| `DELETE` | `/cashcards/{id}` | `204` | `404` if missing or not yours |

Any request without valid credentials gets `401`. A logged-in user without the `CARD-OWNER` role gets `403`.

Listing supports pagination and sorting, defaulting to page 0, size 20, sorted by `amount` ascending:

```
GET /cashcards?page=0&size=1&sort=amount,desc
```

### Test users (in memory, `SecurityConfig`)

| User | Password | Role |
|---|---|---|
| `sarah1` | `abc123` | `CARD-OWNER` |
| `hank-owns-no-cards` | `qrs456` | `NON-OWNER` |

### Curl examples

```bash
# create
curl -i -u sarah1:abc123 -X POST http://localhost:8081/cashcards \
  -H "Content-Type: application/json" -d '{"amount": 250.0}'

# read one / list all
curl -i -u sarah1:abc123 http://localhost:8081/cashcards/1
curl -s -u sarah1:abc123 http://localhost:8081/cashcards | jq

# update / delete
curl -i -u sarah1:abc123 -X PUT http://localhost:8081/cashcards/1 \
  -H "Content-Type: application/json" -d '{"amount": 500.0}'
curl -i -u sarah1:abc123 -X DELETE http://localhost:8081/cashcards/1

# security checks
curl -i http://localhost:8081/cashcards                                   # 401
curl -i -u sarah1:wrong http://localhost:8081/cashcards                   # 401
curl -i -u hank-owns-no-cards:qrs456 http://localhost:8081/cashcards      # 403
```

## Project layout

```
src/main/java/com/example/cashcard/
  CashcardApplication.java   entry point
  CashCard.java              record (id, amount, owner)
  CashCardRepository.java    Spring Data JDBC repository
  CashCardController.java    REST endpoints
  SecurityConfig.java        auth rules and users
src/main/resources/
  application.properties     port, logging
  schema.sql                 creates the cash_card table (runs automatically)
src/test/java/...            CashcardApplicationTests, CashCardJsonTest
src/test/resources/
  data.sql                   test seed data
  com/example/cashcard/expected.json
```

## Running tests

```bash
./mvnw test                                                        # everything
./mvnw test -Dtest=CashCardJsonTest                                # one class
./mvnw test -Dtest=CashCardApplicationTests#shouldCreateANewCashCard   # one method
./mvnw test -Dtest="*JsonTest"                                     # pattern
./mvnw -Dstyle.color=always test                                   # keep log colors when piping
```

Use the **class name**, which is case-sensitive, not the file name. Here the class `CashCardApplicationTests` lives in the file `CashcardApplicationTests.java`.

There's no watch mode in Maven. To re-run tests on file changes:

```bash
brew install fswatch
fswatch -o src | xargs -n1 -I{} ./mvnw -q test
```

## What I learned

### Maven and dependencies

- **Dependencies are never auto-added**, in Maven or in Gradle. If a class is missing at runtime, add the dependency yourself. Spring Boot 4 split its modules, so older Boot 3 tutorials can leave out a dependency you now need. `TestRestTemplate` needed `spring-boot-starter-restclient` (test scope).
- **After adding a dependency:** reload the IDE's Maven project, run `./mvnw clean compile`, then `./mvnw clean test`, then start the app and read the startup log. Pick the narrowest scope (`runtime`, `test`).
- **A new dependency can change behavior with no code change.** Adding `spring-boot-starter-data-jdbc` + H2 made Spring create a database at startup. Adding `spring-boot-starter-security` locked every endpoint and broke the tests until I added `SecurityConfig`.
- Leave `<version>` out for Spring Boot artifacts so the parent manages it.
- `redirectTestOutputToFile=false` on the Surefire plugin keeps test output on the console. `showStandardStreams` is a Gradle setting that doesn't exist in Maven.

### Logging

- Spring Boot's default level is **INFO**, so `log.debug(...)` is hidden until you opt in with `logging.level.<logger>=DEBUG`. Each class is its own logger: turning DEBUG on for the controller does **not** turn it on for the test class.
- Enabling DEBUG for a whole package is simple but noisy.
- A log line only prints if the code runs. A crashed context or an unreachable branch means no output.
- Use placeholders, `log.debug("found: {}", card)`, not string concatenation.
- Custom colors: `logging.pattern.console` with `%clr(...)`, plus `spring.output.ansi.enabled=ALWAYS`. Maven strips ANSI codes when output is piped, so use `-Dstyle.color=always`.

### Tests

- A compile error in the test sources fails the whole run before any test starts.
- Surefire matches class names case-sensitively. A "No tests matching pattern" error just means the name is wrong.
- `@SpringBootTest(webEnvironment = RANDOM_PORT)` starts the real app on a random port, so the test port never clashes with 8081.
- **`@DirtiesContext`** discards the cached application context (and its in-memory database) after a test. Spring shares one context across tests, so a test that changes data, like create, update, or delete, would otherwise leak into others. It costs a restart each time, so use it only on tests that modify data.
- Test files for a class go in the same package under `src/test/resources`, such as `expected.json`. But **`schema.sql` and `data.sql` must be at the classpath root**, or Spring won't run them.
- An empty or comment-only `schema.sql` fails with `'script' must not be null or empty`.
- Because tests share a database, don't assert on the exact whole table unless you control the data.
- With security on, send credentials in tests: `restTemplate.withBasicAuth("sarah1", "abc123")`.

### Java and Spring gotchas

- **Records don't have `getX()` methods.** Accessors are `amount()`, `id()`, `owner()`. `getAmount()` is a compile error.
- **Don't use `Optional` as a record component or field.** A `null` id means "new, not saved yet" to Spring Data, which then does an `INSERT` instead of an `UPDATE`.
- `id.equals(99)` compares a `Long` to an `Integer` and is always false. Use `99L`.
- **Import the right `Pageable`:** `org.springframework.data.domain.Pageable`, not `...autoconfigure.web.DataWebProperties.Pageable`. Check the package when an IDE offers several imports.
- A `@Bean` method without `@Bean` compiles but is silently ignored. The `SecurityFilterChain` without `@Bean` meant Spring Boot's defaults applied, with CSRF still on. The symptom was a `401` on `POST` only.
- In the security DSL, `.requestMatchers(...)` takes **one** rule: `.authenticated()` or `.hasRole(...)`. You can't chain them. `hasRole("X")` checks for the authority `ROLE_X`, and the prefix is added for you.
- An unauthenticated request with no `Accept` header can get Spring's HTML login page as a `200`. That caused confusing "found a String, not JSON" errors.

### REST design

- `POST` returns **`201 Created` + a `Location` header** pointing at the new resource, built with `UriComponentsBuilder`.
- `save()` returns a new object with the generated id, because records are immutable. Use the returned value.
- `save()` with a null id inserts, and with a non-null id it updates. Look the row up first, or a `PUT` to a missing id could create a row.
- **Ownership:** take the owner from `Principal`, never from the request body, and query with `findByIdAndOwner`. A user can't create or edit cards as someone else.
- **`401` vs `403` vs `404`:** `401` means "who are you?", `403` means "I know you, but no", and `404` is returned for both "missing" and "not yours" so the API doesn't reveal that another user's card exists.
- `PUT` and `DELETE` return `204 No Content` on success here. `200` with a body is also valid, so pick one and keep tests consistent with the code.
- Pagination: a `Pageable` method parameter is filled from `?page=&size=&sort=`. `getSortOr(...)` supplies a default sort.

### Tooling

- `-u user:pass` in curl sends basic auth. `-i` shows status and headers.
- Only `-q` hides Maven's own output. Piping through `grep` can hide log lines.
- Note the file `CashcardApplicationTests.java` (lowercase `c`) contains the class `CashCardApplicationTests`. A non-public class can differ from its file name, but it's confusing, so keep them matching.
