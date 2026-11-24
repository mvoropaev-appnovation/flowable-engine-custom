# Handoff: Flowable fork → Spring Boot 4.2 (CVE-2026-22732)

## Repos
- Fork: `/Users/maxim.voropaev/local/appno/EIP/flowable-engine-custom`, branch `wip/spring-boot-4.2`
- App: `/Users/maxim.voropaev/local/appno/EIP/dpp-eip-app-workflow`, branch `feature/eaut-2096-dependency-upgrade` (clean, no code changes)

## Goal
Least-effort move of Flowable 6.8.0 fork to Boot 4.2.0 / Spring 7.1 / Security 7.2; standalone `flowable-ui` WAR for DEV. Tests deferred. Out of scope: Flowable 5, Mule/Camel/CXF/CDI/OSGi, Flowable 7 UI.

## Rules
- Commit only files the user names; signed (`git commit -S`).
- Message starts `EAUT-2070: `, ends with `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`.
- Everything under GitHub org `pfizer-eps`.

## State
Fork: 10 signed commits (poms+.sdkmanrc → httpclient5/PG → javax→jakarta → AutoConfiguration.imports → Boot renames → Security 7 → Jackson 2 converters → Framework 7 API → DB schema → admin job URL trailing slash). Untracked: `prompts/upgrade-spring-boot-4.2.md`.
App: untracked `prompts/upgrade-spring-boot-4.2.md` (GitHub Packages process + MVP backlog). Migration not started (user said "groom, don't refactor").

## Build/run
`export JAVA_HOME=/Users/maxim.voropaev/Library/Java/JavaVirtualMachines/corretto-21.0.5/Contents/Home; mvn clean install -T1 -Pui,war -pl :flowable-ui-app -am -Dmaven.test.skip=true -Dmaven.javadoc.skip=true`
Run: `java -cp 'lib/*:flowable-ui.war' org.springframework.boot.loader.launch.WarLauncher`. Default java is 25; use Corretto 21. No `pkill`; kill by PID.

## Open items
- DEV modeler empty: unresolved (suspects: flowable.databaseSchema vs `public` ACT_DE_* tables, missing `access-modeler` privilege, failing REST call, stale lib jars). Locally renders fine.
- WAR not rebuilt since job-URL fix.
- Fork not yet under `pfizer-eps`; nothing published. Replace Sonatype `distributionManagement` (root pom ~L2152) with `https://maven.pkg.github.com/pfizer-eps/flowable-engine-custom` (repo name assumed); version `6.8.0-eip.N`; `-Dgpg.skip=true`; classic PAT (write:packages, repo). Try `mvn deploy -pl :flowable-spring-boot-starter -am`.
- App migration (when approved): Boot 4.2/Java 21, jakarta, WebClientConfig (epoll, UnAuthenticatedServerOAuth2AuthorizedClientRepository), ObjectMapperConfiguration customizer package, SpELExpressionEvaluator replacement, thymeleaf spring6, Security 7 reactive DSL, @MockitoBean, CI Java 21 + registry creds, verify Spring Cloud 2026.0.0 vs Boot 4.2.
- Fork extras: Apache 2.0 modification notice, migrate tests, 401 entry point on idm/modeler/task chains, flowable-app-rest/samples, ldap-configurator (spring-ldap-core-tiger), commons-email2-jakarta milestone.

## Licence
Apache 2.0 → private fork OK; keep LICENSE/headers, mark modifications, no Flowable branding/endorsement.

See also `context-boot4-upgrade.md` in this folder.
