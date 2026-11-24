# Context: Flowable 6.8.0 fork -> Spring Boot 4.2 (saved 2026-11-24)

## Repos
- Fork: `/Users/maxim.voropaev/local/appno/EIP/flowable-engine-custom`, branch `wip/spring-boot-4.2` (remote `mvoropaev-appnovation/flowable-engine-custom`; to move under `pfizer-eps`).
- App: `/Users/maxim.voropaev/local/appno/EIP/dpp-eip-app-workflow` (`pfizer-eps/dpp-eip-app-workflow`), on Boot 2.7.18 / Java 17. Branch `feature/eaut-2096-dependency-upgrade`. Not migrated yet (groomed only).
- Prompts (uncommitted): `<fork>/prompts/upgrade-spring-boot-4.2.md`, `<app>/prompts/upgrade-spring-boot-4.2.md`.

## Goal and constraints
- Reason: Spring CVEs (CVE-2026-22732). Least effort to a working `flowable-ui` WAR on Boot 4.2.0 / Framework 7.1.0 / Security 7.2.0 / Tomcat 11.0.29 / Java 21 (Corretto, `.sdkmanrc`).
- Keep Jackson 2. Tests deferred. Out of scope: Flowable 5, Mule/Camel/CXF/CDI/OSGi, Flowable 7 UI.
- Commits: signed (`-S`), prefix `EAUT-2070: `, trailer `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`. Commit only files the user names.
- User prefers reading `-sources.jar` over decompiling.
- Everything should live under the `pfizer-eps` org. Company has no Nexus/Artifactory; use GitHub Packages (Maven), built and pushed from a local machine. Version scheme `6.8.0-eip.N`.
- Flowable is Apache 2.0; a private fork is allowed (keep LICENSE, mark modified files, don't use the Flowable branding).

## Fork commit history (newest first, all signed; hashes change on rebase)
- bae5f6c7a7 Strip trailing slash from admin job list URLs
- 7f8cc7a167 Honour flowable.database-schema in modeler/admin
- f323e73267 Spring Framework 7 API changes (incl. Kafka)
- bb4f73c648 Keep Jackson 2 for REST message converters
- e64f870bc9 Spring Security 7 migration
- 2d8f6164cc Remap Java imports (Boot renames)
- 953e69ddb1 spring.factories -> AutoConfiguration.imports
- 3c460c9e0d javax -> jakarta
- c061ce97f9 Bump httpclient5 / PostgreSQL, PG runtime scope
- 40c272df70 Upgrade poms (+ .sdkmanrc)
Tree was clean (only untracked files) after the last commit; the two prompt files are uncommitted.

## Build and run
`mvn clean install -T1 -Pui,war -pl :flowable-ui-app -am -Dmaven.test.skip=true -Dmaven.javadoc.skip=true` (JAVA_HOME = Corretto 21; clean + -T1).
Run: `java -cp 'lib/*:flowable-ui.war' org.springframework.boot.loader.launch.WarLauncher`. Local test instance used port 18080 (admin/test), `/tmp/run.sh`.

## Open items
- Modeler "comes up empty" on DEV: not reproducible locally (renders fine in headless Chrome). Suspects: flowable.databaseSchema now creates empty ACT_DE_* tables in the target schema (models still in public), missing `access-modeler` privilege under Keycloak, failing REST call, stale jars in `lib/`.
- Untested: Kafka/JMS/Rabbit channels, Hibernate 7, mybatis-spring 4, commons-email2-jakarta 2.0.0-M1 (milestone), PathPattern matchers, flowable-app-rest/sample modules (old Security API), explicit 401 entry point on idm/modeler/task API chains.
- Tests not migrated (JUnit 4 -> 5, Mockito 5, Boot test packages).
- App migration (groomed, not started): pom baseline (Boot 4.2.0, Java 21, Spring Cloud 2026.0.0 [unconfirmed with Boot 4.2], awspring 4.2.2, Security 7.2.0, spring-boot-jackson2); javax->jakarta (16 validation + PostConstruct); WebClientConfig (EpollChannelOption, UnAuthenticatedServerOAuth2AuthorizedClientRepository); thymeleaf spring5->spring6; SpELExpressionEvaluator gone; port SecurityWebFilterChain; tests (@MockBean -> @MockitoBean).
- Publishing: move fork under pfizer-eps, change distributionManagement to GitHub Packages, classic PAT write:packages, deploy starter closure (`-pl :flowable-spring-boot-starter -am`); not yet tried.
