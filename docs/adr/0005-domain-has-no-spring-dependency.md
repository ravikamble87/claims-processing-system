# ADR 0005: The domain core has no Spring dependency

Status: Accepted · Date: 2026-10-09

## Context
The same adjudication logic is used by batch, REST and GUI, and must be heavily tested.

## Decision
`domain` and `service` packages are plain Java 21. Spring wiring lives in `batch`, `api`, `web`, `cli`, `refdata`.

## Consequences
+ Unit tests run in milliseconds without a Spring context
+ Logic is reusable as a library by other internal systems
− A little extra wiring code (`@Bean` methods) to expose the services
