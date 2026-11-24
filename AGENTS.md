# AGENTS.md

Private fork of Flowable 6.8.0 being moved to Spring Boot 4.2 / Spring 7.1 / Security 7.2 / Java 21 (CVE-2026-22732). Working branch: `wip/spring-boot-4.2`.

## Read first
- `prompts/contexts/handoff-summary.md` – current state, open items, rules
- `prompts/contexts/context-boot4-upgrade.md` – full context, commit history, build/run
- `prompts/upgrade-spring-boot-4.2.md` – upgrade prompt, GitHub Packages publishing process, MVP backlog

Always save session contexts and handoffs under `prompts/contexts/` (not in the Copilot session folder), and update them when the state changes.

## Rules
- Commit only the files the user names. Commits must be signed (`git commit -S`).
- Commit message starts with `EAUT-2070: ` and ends with the trailer `Co-authored-by: Copilot <223556219+Copilot@users.noreply.github.com>`.
- Everything lives under the GitHub org `pfizer-eps`.
- Keep Jackson 2. Tests are deferred. Out of scope: Flowable 5, Mule/Camel/CXF/CDI/OSGi, Flowable 7 UI.
- Prefer published `-sources.jar` artifacts over decompiling bytecode.
- Apache 2.0: keep LICENSE and headers, mark modifications, no Flowable branding or endorsement.

## Build
Use Corretto 21 (default `java` is 25); do not use `pkill`, kill by PID.

```
export JAVA_HOME=/Users/maxim.voropaev/Library/Java/JavaVirtualMachines/corretto-21.0.5/Contents/Home
mvn clean install -T1 -Pui,war -pl :flowable-ui-app -am -Dmaven.test.skip=true -Dmaven.javadoc.skip=true
```

Run: `java -cp 'lib/*:flowable-ui.war' org.springframework.boot.loader.launch.WarLauncher`
