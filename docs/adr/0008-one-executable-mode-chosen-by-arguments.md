# ADR 0008: One executable; the mode is chosen by its arguments

Status: Accepted · Date: 2026-10-10

## Context
The assignment needs a GUI, a command-line batch mode (`<serviceName> <input.csv> <output.csv>`) and a REST API.

## Options considered
1. Three applications or Maven modules — clean separation, three things to build and run in the demo
2. One Spring Boot jar: no positional arguments → web server (GUI + REST); two file arguments → batch run, then exit

## Decision
Option 2. `main()` starts without a web server when file arguments are present; `BatchCommandLineRunner` runs
the job and exits with 0 (completed), 1 (failed) or 2 (bad arguments). GUI and CLI share `JobRunService`.

## Consequences
+ One build, one artifact, one set of reference data and rules
+ CLI does not occupy port 8080 or wait for Tomcat
− Web-only beans are still created in CLI mode (cheap); if the CLI grew, split it into its own module
