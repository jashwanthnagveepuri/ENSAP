# ADR-0011: Spring functional endpoints over `@RestController`

## Status
Accepted (Phase 0)

## Context
Spring WebFlux supports two API styles: annotation-based
(`@RestController`/`@GetMapping`) and functional
(`RouterFunction`/`HandlerFunction`). The spec mandates the functional
style for all primary application APIs (§31, §32, §39.19–22).

## Decision
Every Java service composes its HTTP API via a central
`config/RouterConfig.java` delegating to per-resource `router/`
classes (`RouterFunction<ServerResponse>`), which route to `handler/`
classes (`HandlerFunction`) that translate HTTP↔domain and delegate to
`service/`. No `@RestController`, `@RequestMapping`, `@GetMapping`,
`@PostMapping`, etc. appear anywhere in service code (§39.4).

## Alternatives considered
- **Annotation-based `@RestController`.** The default, more familiar
  Spring style, and objectively faster to scaffold. Rejected — the spec
  is explicit and repeated on this point (§31, §32, §39.19, §39.22)
  specifically because functional routing keeps routing declarative and
  centrally composable/readable in one place (`RouterConfig`) rather
  than scattered across annotations, which is itself a design pattern
  worth learning and demonstrating.
- **A mix (functional for some endpoints, annotated for others).**
  Rejected for consistency (§39.21 — "follow the established
  RouterConfig pattern consistently across services").

## Consequences
- Slightly more boilerplate per endpoint (explicit router wiring instead
  of an annotation) — accepted as the point of the exercise, not a
  drawback to work around.
- Handlers must manually extract path variables/body
  (`request.pathVariable(...)`, `request.bodyToMono(...)`) instead of
  method-parameter binding — consistent across all three services via
  the shared package layout in `06-component-design.md`.
