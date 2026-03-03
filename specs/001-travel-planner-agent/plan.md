# Implementation Plan: Travel Planner Agent

**Branch**: `001-travel-planner-agent` | **Date**: 2026-03-03 | **Spec**: [spec.md](spec.md)
**Input**: Feature specification from `/specs/001-travel-planner-agent/spec.md`

## Summary

Build an AI-powered travel itinerary service using the Akka SDK. The system manages user profiles with accumulated travel preferences, generates personalized day-by-day travel plans via an LLM agent (OpenAI GPT-4o-mini), and orchestrates the multi-step generation process through a durable workflow with retry/recovery. All features are exposed through REST endpoints.

## Technical Context

**Language/Version**: Java 21 (records, sealed interfaces)
**Primary Dependencies**: Akka SDK 3.5+ (parent POM `akka-javasdk-parent`), OpenAI GPT-4o-mini via Akka Agent component
**Storage**: Event sourcing via Akka Event Sourced Entities (no external database)
**Testing**: JUnit 5, AssertJ, Awaitility, Akka TestKit, TestModelProvider (for agent mocking)
**Target Platform**: JVM server, deployable via Akka Console / `akka` CLI
**Project Type**: Web service (REST API)
**Performance Goals**: Trip ID returned within 1 second; full plan available within 2 minutes
**Constraints**: LLM calls require long timeouts (60s+); workflow step recovery with limited retries (2)
**Scale/Scope**: Single service, 2 event-sourced entities, 1 workflow, 1 agent, 2 HTTP endpoints

## Constitution Check

*GATE: Must pass before Phase 0 research. Re-check after Phase 1 design.*

| Principle | Status | Notes |
|-----------|--------|-------|
| I. Domain Design | PASS | Domain records in `domain` package, immutable Java records, no Akka dependencies in domain, sealed event interfaces with `@TypeName` |
| II. Incremental Generation Workflow | PASS | Feature follows step-by-step: Domain → Entity → Tests → Endpoint → Integration Tests |
| III. Test Coverage | PASS | Entity unit tests with `EventSourcedTestKit`, agent tests with `TestModelProvider`, endpoint integration tests with `httpClient` |
| IV. Akka SDK Conventions | PASS | `akka.*` imports, `@Component(id=...)`, `@Acl` on endpoints, synchronous `.invoke()`, method references for workflow steps |
| V. Simplicity & Minimalism | PASS | No speculative features; BOOKED/COMPLETED deferred; domain logic in records, effects in entities |

No violations. All gates pass.

## Project Structure

### Documentation (this feature)

```text
specs/001-travel-planner-agent/
├── plan.md              # This file
├── research.md          # Phase 0 output
├── data-model.md        # Phase 1 output
├── quickstart.md        # Phase 1 output
├── contracts/           # Phase 1 output (REST API contracts)
└── tasks.md             # Phase 2 output (/speckit.tasks command)
```

### Source Code (repository root)

```text
src/main/java/com/example/travel/
├── domain/
│   ├── UserProfile.java           # User state record
│   ├── UserEvent.java             # Sealed event interface for user changes
│   ├── TravelPreference.java      # Preference value object
│   ├── Trip.java                  # Trip state record
│   ├── TripEvent.java             # Sealed event interface for trip changes
│   └── TravelPlan.java            # AI-generated plan structure (with nested DayPlan, etc.)
├── application/
│   ├── UserProfileEntity.java     # Event sourced entity for user profiles
│   ├── TripEntity.java            # Event sourced entity for trips
│   ├── TravelPlannerWorkflow.java # Workflow orchestrating plan generation
│   └── TravelPlannerAgent.java    # AI agent for LLM interaction
└── api/
    ├── UserProfileEndpoint.java   # REST API for user management
    └── TravelPlannerEndpoint.java # REST API for travel planning

src/test/java/com/example/travel/
├── application/
│   ├── UserProfileEntityTest.java    # Entity unit tests (EventSourcedTestKit)
│   ├── TripEntityTest.java           # Entity unit tests (EventSourcedTestKit)
│   └── TravelPlannerAgentIntegrationTest.java  # Agent tests (TestModelProvider)
└── api/
    ├── UserProfileEndpointIntegrationTest.java     # HTTP integration tests
    └── TravelPlannerEndpointIntegrationTest.java   # HTTP integration tests

src/main/resources/
└── application.conf    # LLM provider configuration
```

**Structure Decision**: Standard Akka SDK Maven project with `domain/application/api` package convention per constitution. Single module, no multi-project setup needed.

## Complexity Tracking

No violations to justify. All design choices align with constitution principles.
