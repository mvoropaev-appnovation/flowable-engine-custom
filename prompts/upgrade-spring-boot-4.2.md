# Prompt: Upgrade the custom Flowable 6.8.0 fork to Spring Boot 4.2

Use this prompt to repeat (or continue) the migration of this fork from
Spring Boot 2.7 / Spring Security 5.8 to **Spring Boot 4.2 / Spring Framework 7.1 / Spring Security 7.2**.

## Goal

Get the standalone **`flowable-ui` WAR** (`modules/flowable-ui/flowable-ui-app`) running on the newest Spring
with the least effort. The driver is a CVE in Spring Security HTTP headers (CVE-2026-22732), not new features.

Target versions (all verified as latest on Maven Central):
Boot 4.2.0, Framework 7.1.0, Security 7.2.0, Spring AMQP/Kafka/LDAP/Data JPA 4.2.0, Spring Integration 7.2.0,
Tomcat 11.0.29, Java 21 (Amazon Corretto, see `.sdkmanrc`).

## Scope

In scope: Flowable UI (modeler, admin, IDM, task apps), the Spring Boot starters/autoconfigure, event-registry
(Kafka/Rabbit/JMS channels), HTTP client, Keycloak/OIDC login, Postgres.

Out of scope (not used in our setting, do not spend time on them):
Flowable 5 compatibility, Mule/Camel/CXF/CDI/OSGi modules, Flowable 7 UI, scripting changes, custom form changes.
**Tests are deferred**: build with `-Dmaven.test.skip=true`; test migration (JUnit 4 -> 5, Mockito 5, Boot test packages) is a follow-up.

## Working rules

- Work on a branch (`wip/spring-boot-4.2`). Verify by building and booting the WAR, then log in and call endpoints.
- Keep **Jackson 2**. Flowable's public APIs expose Jackson 2 types everywhere; moving to Jackson 3 would touch hundreds of files.
- Prefer reading published `-sources.jar` artifacts over decompiling bytecode when investigating library behaviour.
- Commit messages start with `EAUT-2070: ` and end with the trailer
  `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`. Commits must be signed (`git commit -S`).
  Commit only the files requested, one logical commit at a time.

## Build and run

```bash
export JAVA_HOME=<corretto 21 home>
mvn clean install -T1 -Pui,war -pl :flowable-ui-app -am -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
java -cp 'lib/*:flowable-ui.war' org.springframework.boot.loader.launch.WarLauncher
```

Notes: put `clean` before `install` and use `-T1` together with `clean` (mixing with `-T4` breaks the reactor).
The WAR is `flowable-ui.6.8.0-tomcat-11.0.29.war`. The launcher class moved in Boot 3.2+. Remove old
`httpclient5`/`httpcore5` jars from `lib/` if the run command uses `lib/*`.

## Steps

### 1. Dependencies (poms)
- Bump Boot, Framework, Security, Kafka, AMQP, LDAP, Data JPA, Integration; set `tomcat.version` to 11.0.29.
- `httpclient5` 5.6.5 (Spring 7.1 needs `HttpClient.executeOpen`; 5.1.3 fails with `NoSuchMethodError`).
- `postgresql` 42.7.15 and add it to `flowable-ui-app` with `runtime` scope.
- Add `spring-boot-jackson2` to `flowable-ui-app` so Boot still provides the Jackson 2 `ObjectMapper`.
- Keep the compiler `-parameters` flag. Add `.sdkmanrc` with `java=21.0.11-amzn`.

### 2. `javax.*` -> `jakarta.*`
Rename servlet, persistence, validation, annotation, mail, etc. (including string references such as
`@ConditionalOnBean(type = "jakarta.persistence.EntityManagerFactory")` and `persistence.xml` property names).
`javax.sql`, `javax.xml`, `javax.naming` stay as they are.

### 3. Boot package/class renames
- `spring.factories` auto-configuration entries move to
  `META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports` (6 modules).
- Boot 4 modularised packages, for example:
  `...autoconfigure.jdbc` -> `org.springframework.boot.jdbc.autoconfigure`,
  `...autoconfigure.orm.jpa.HibernateJpaAutoConfiguration` -> `org.springframework.boot.hibernate.autoconfigure`,
  `...autoconfigure.ldap` -> `org.springframework.boot.ldap.autoconfigure`,
  `...autoconfigure.kafka` -> `org.springframework.boot.kafka.autoconfigure`,
  `...autoconfigure.amqp` -> `org.springframework.boot.rabbitmq.autoconfigure`,
  `...actuate.health.HealthEndpoint` -> `org.springframework.boot.health.actuate.endpoint`,
  `...env.EnvironmentPostProcessor` -> `org.springframework.boot.EnvironmentPostProcessor`.
- `ConfigFileApplicationListener.DEFAULT_ORDER` -> `ConfigDataEnvironmentPostProcessor.ORDER`
  (both `BackwardsCompatiblePropertiesLoader` classes and `FlowableDefaultPropertiesEnvironmentPostProcessor`).
- `TaskExecutorCustomizer` -> `ThreadPoolTaskExecutorCustomizer`; `PropertyMapper.alwaysApplyingWhenNonNull()` removed.

### 4. Spring Security 7
Rewrite `FlowableUiSecurityAutoConfiguration` and the idm/modeler/task security configurations:
- `WebSecurityConfigurerAdapter` -> `SecurityFilterChain` `@Bean`s with `@Order`.
- `antMatchers` -> `requestMatchers`; `authorizeRequests` -> `authorizeHttpRequests`; lambda DSL everywhere;
  `http.apply` -> `http.with`; `requestMatcher` -> `securityMatcher`.
- Delete the unused `CustomAntPathMatcher` and `CustomFormLoginConfig`.
- Behaviour changes to compensate for:
  - Unmatched requests and FORWARD dispatches (`/` -> `/index.html`) are denied: end the main chains with
    `.anyRequest().permitAll()`.
  - With several authentication mechanisms (basic + bearer) anonymous requests get 403: set an explicit
    `BasicAuthenticationEntryPoint` (realm "Flowable") on the actuator chain.
- `/actuator/health` is public; `info` needs authentication; all other endpoints need the ADMIN authority.
  Run actuator on a separate port with `MANAGEMENT_SERVER_PORT=8089` (path has no `/flowable-ui` prefix).

### 5. Spring Framework 7 API changes
- `AsyncListenableTaskExecutor` -> `AsyncTaskExecutor`, `submitListenable(..).completable()` -> `submitCompletable(..)`.
- `ResponseEntity.getStatusCodeValue()` -> `getStatusCode().value()`; `HttpStatus` -> `HttpStatusCode`;
  reason phrase via `HttpStatus.resolve(..)`.
- `HttpHeaders` is no longer a `Map`: `containsKey` -> `containsHeader`, `putAll` -> `forEach(headers::put)`.
- `UriComponentsBuilder.fromHttpUrl` -> `fromUriString`.
- Spring Kafka 4 (`KafkaChannelDefinitionProcessor`, `SimpleKafkaListenerEndpoint`): Spring Retry back-off policies
  -> `org.springframework.util.backoff.*`, `FixedDelayStrategy` -> `SameIntervalTopicReuseStrategy`,
  `PartitionPausingBackOffManagerFactory` -> `ContainerPartitionPausingBackOffManagerFactory`, new
  `DestinationTopicProcessor.Context` and `DefaultDestinationTopicResolver` constructors, `getAckMode()`.

### 6. Jackson 2 message converters
Spring 7's `WebMvcConfigurationSupport` registers the Jackson 3 converter first, which fails on Flowable's Jackson 2
request bodies (`InvalidDefinitionException: Conflicting setter definitions for property 'all'`).
- In each servlet context (idm api/app, modeler api/app, admin, spring-boot-autoconfigure
  `DispatcherServletConfiguration`) override `configureMessageConverters(HttpMessageConverters.ServerBuilder)` and call
  `withJsonConverter(new MappingJackson2HttpMessageConverter(objectMapper))`.
- For Boot's own context add `spring.http.converters.preferred-json-mapper=jackson2` in
  `flowable-ui-app/src/main/resources/application.properties`.

### 7. Runtime fixes found while testing
- **`flowable.database-schema` was ignored** by modeler (`ACT_DE_*`) and admin (`ACT_ADM_*`) Liquibase and MyBatis.
  Read `${flowable.database-schema:}` in `ModelerDatabaseConfiguration` and `AdminDatabaseConfiguration`: set the
  Liquibase default and liquibase schema, and use `schema.` as the MyBatis prefix unless `data-source-prefix` is set.
  If tables already exist in `public`, move them to the target schema or leave the property unset.
- **Trailing slashes no longer match** in Spring 6+: `JobService`/`CmmnJobService` called `management/jobs/`;
  strip the trailing slash from the list URLs.

## Verification checklist
1. Clean build succeeds and the WAR boots on Java 21.
2. Login (form and OIDC/Keycloak), `/flowable-ui/` loads, modeler lists models, admin job lists load.
3. `/actuator/health` returns 200 anonymously, `/actuator/info` returns 401.
4. Modeler and admin POST requests deserialize (no Jackson errors).
5. With `flowable.database-schema` set, no `relation "act_de_model" does not exist` errors.

## Known risks / follow-ups
- Untested: Kafka/JMS/Rabbit channels, Hibernate 7/JPA, mybatis 3.5.10 with mybatis-spring 4,
  `commons-email2-jakarta` 2.0.0-M1 (a milestone), PathPattern differences in security matchers.
- `flowable-app-rest` and the sample modules still use the old Security API; the idm/modeler/task API chains may
  also need an explicit 401 entry point.
- `flowable-ldap-configurator` still depends on the discontinued `spring-ldap-core-tiger` (not in the UI build).
- Migrate the tests (JUnit 4 -> 5, Mockito 5, Boot test packages).

## Licensing (Apache License 2.0)

Flowable 6.8 is Apache 2.0 (see `LICENSE`), so a private fork is permitted. This is general information, not legal
advice; have legal confirm if needed.

- Modifying and keeping changes private is allowed; there is no copyleft or source-disclosure obligation.
- Internal deployment (e.g. the DEV WAR) is fine.
- If redistributed: include the license text, keep copyright/patent/attribution notices, and carry over any `NOTICE`.
- Section 4(b): modified files must carry a prominent notice that they were changed (still to do).
- Section 6 grants no trademark rights: don't present the fork as official Flowable or imply endorsement; use the
  `6.8.0-eip.N` version scheme and avoid Flowable logos/branding.
- Section 3 patent grant terminates if you sue over patents in the work.
- Only 6.x (Apache 2.0) code is in scope; Flowable 7 / Work / Engage are commercial and must not be mixed in.
- Third-party dependencies keep their own licenses.

## Publishing the artifacts (GitHub Packages, org `pfizer-eps`)

The company has no Nexus/Artifactory. `dpp-eip-app-workflow` (`pfizer-eps`) consumes this fork from the GitHub Packages
Maven registry. Artifacts are built on a developer machine and pushed from there.
The consumer side is described in `dpp-eip-app-workflow/prompts/upgrade-spring-boot-4.2.md`.

### Requirements
- The fork repo must live in the org: `pfizer-eps/flowable-engine-custom` (transfer or mirror it from
  `mvoropaev-appnovation`; repo name to be confirmed). Packages belong to the owning repo.
- Registry URL: `https://maven.pkg.github.com/pfizer-eps/flowable-engine-custom`.
- Java 21 (Corretto) and Maven 3.9+.
- A **classic** GitHub personal access token with `write:packages` (and `repo` for private repos), exported as
  `GITHUB_TOKEN`. The Maven registry does not accept fine-grained tokens. Never commit it.
- `~/.m2/settings.xml` on the publishing machine:
  ```xml
  <servers>
    <server>
      <id>github-flowable</id>
      <username>YOUR_GITHUB_USER</username>
      <password>${env.GITHUB_TOKEN}</password>
    </server>
  </servers>
  ```
- Root `pom.xml`: replace the Sonatype `distributionManagement` (`sonatype-nexus-snapshots` / `sonatype-nexus-staging`) with
  one `<repository>` (and `<snapshotRepository>` if snapshots are ever needed) whose id is `github-flowable` and whose url is
  the registry URL above. Do not run the GPG/Sonatype release profile for this deploy (`-Dgpg.skip=true`).
- Versions are `6.8.0-eip.N` (N = 1, 2, ...). Never `-SNAPSHOT`, never `6.8.4.x` (implies a non-existent upstream release).
  Treat a published version as immutable and bump N for every change.

### What to publish
Only what the app needs: `flowable-spring-boot-starter` and everything it depends on, including the parent and BOM poms.
The UI WAR, samples and test modules are not published.

### Build and push (per version)
```bash
export JAVA_HOME=<corretto 21 home>
export GITHUB_TOKEN=<classic PAT with write:packages>
git switch wip/spring-boot-4.2 && git pull

# 1. set the version in every module
mvn versions:set -DnewVersion=6.8.0-eip.N -DgenerateBackupPoms=false

# 2. (optional) local smoke test, installs to ~/.m2 only
mvn clean install -T1 -pl :flowable-spring-boot-starter -am -Dmaven.test.skip=true -Dmaven.javadoc.skip=true

# 3. build and push to GitHub Packages
mvn clean deploy -T1 -pl :flowable-spring-boot-starter -am \
    -Dmaven.test.skip=true -Dmaven.javadoc.skip=true -Dgpg.skip=true

# 4. record the release
git commit -S -am "EAUT-2070: Release 6.8.0-eip.N"   # message must keep the EAUT-2070: prefix and Copilot trailer
git tag -s flowable-6.8.0-eip.N
git push origin wip/spring-boot-4.2 --tags
```
Notes:
- Put `clean` before `deploy` and use `-T1` with it (mixing with `-T4` breaks the reactor).
- Add `-Pui` only if a module the starter needs lives in that profile. Use `-DskipTests` instead of `-Dmaven.test.skip=true` if a
  module's test-jar is a dependency of another published module.
- Do not start step 3 if `versions:set` left any module on `6.8.0`: check with `grep -r "<version>6.8.0</version>" --include=pom.xml .`.
- Publishing the same version twice fails or is ignored: bump N instead.

### Verify
1. `gh api "/orgs/pfizer-eps/packages?package_type=maven" --jq '.[].name'` lists `org.flowable.flowable-spring-boot-starter` and its dependencies.
2. In a clean checkout of `dpp-eip-app-workflow` with `~/.m2/repository/org/flowable` removed, `./mvnw dependency:resolve`
   downloads `6.8.0-eip.N` from `github-flowable`.
3. The same resolve succeeds on the `eip` self-hosted runner (it needs a `github-flowable` server entry with a token that
   has `read:packages`, or the package's repository access granted to the consuming repo).

### Troubleshooting
- 401/403 on deploy: wrong token type or scope, or the repo is not under `pfizer-eps`.
- 401/403 on resolve: the runner's token lacks `read:packages`, or the package does not grant the consuming repo access.
- Missing module at resolve time: it was not in the `-am` closure; add it to `-pl` and publish a new `N`.

## Suggested commit structure
1. Upgrade poms (and `.sdkmanrc`)
2. Bump httpclient5 / PostgreSQL driver
3. javax -> jakarta renames
4. `spring.factories` -> `AutoConfiguration.imports`
5. Spring Boot package renames
6. Spring Security 7 migration
7. Keep Jackson 2 for REST message converters
8. Spring Framework 7 API changes
9. Honour `flowable.database-schema` in modeler/admin
10. Strip trailing slash from admin job list URLs
