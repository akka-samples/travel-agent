# Research: Travel Planner Agent

**Date**: 2026-03-03 | **Feature**: 001-travel-planner-agent

## R1: Akka Agent Component for LLM Integration

**Decision**: Use the Akka SDK Agent component to interface with OpenAI GPT-4o-mini.

**Rationale**: The Agent component provides built-in LLM integration with session memory, structured response parsing (`responseConformsTo`), function tools (`@FunctionTool`), and error handling (`.onFailure()`). It integrates natively with Akka's `ComponentClient` for cross-component calls. No external HTTP client or OpenAI SDK needed.

**Alternatives considered**:
- Direct OpenAI SDK integration: Rejected because it bypasses Akka's session memory, retry semantics, and component lifecycle management.
- External microservice for LLM calls: Rejected as unnecessary complexity for a single-service application.

## R2: Structured LLM Response Parsing

**Decision**: Use `responseConformsTo(TravelPlan.class)` to instruct the LLM to return structured JSON matching the `TravelPlan` record hierarchy.

**Rationale**: The Akka Agent supports structured responses that automatically instruct the model to conform to a Java record schema and parse the JSON response. This is more reliable than manual JSON parsing and prompt-only constraints.

**Alternatives considered**:
- `responseAs()` with manual JSON instructions in the prompt: Works but less reliable; `responseConformsTo` is the recommended approach per Akka SDK guidelines.
- Free-text response with post-processing: Rejected as fragile and hard to validate.

## R3: Workflow Timeout and Recovery Strategy

**Decision**: Use 120-second default step timeout for LLM calls with `maxRetries(2)` and failover to an error step.

**Rationale**: LLM response times are unpredictable (typically 5-30 seconds but can spike). A 120-second timeout provides adequate headroom. Limiting retries to 2 prevents excessive LLM costs while allowing recovery from transient failures.

**Alternatives considered**:
- Shorter timeout (30s): Risk of premature timeouts on complex plans.
- Unlimited retries: Risk of runaway LLM costs and cascading failures.
- No recovery (fail immediately): Too brittle for production use.

## R4: Event Sourcing for Both Entities

**Decision**: Use Event Sourced Entities for both `UserProfileEntity` and `TripEntity`.

**Rationale**: Event sourcing provides a complete audit trail of all profile changes and trip lifecycle events. This enables future features like trip history analysis, preference evolution tracking, and undo/replay capabilities. Both entities have clear event streams (profile changes, trip creation).

**Alternatives considered**:
- Key Value Entity for UserProfile: Simpler but loses the audit trail of preference additions and profile updates.
- Key Value Entity for Trip: Would work since trips are mostly write-once, but event sourcing keeps the door open for BOOKED/COMPLETED status transitions.

## R5: Session ID Strategy for Agent

**Decision**: Use the workflow ID as the agent session ID when called from the workflow. Use a random UUID for direct agent calls.

**Rationale**: Using the workflow ID ensures that all agent interactions within a single plan generation share the same session context. This enables the agent to maintain conversational context if the workflow involves multiple agent calls in the future.

**Alternatives considered**:
- Always use random UUID: Loses session continuity across workflow steps.
- Use userId: Would mix sessions across different plan requests from the same user.

## R6: LLM Configuration

**Decision**: Configure the default LLM model in `application.conf` with the OpenAI API key provided via environment variable `OPENAI_API_KEY`.

**Rationale**: Externalizing configuration allows deployment-time model selection without code changes. The Akka SDK supports model provider configuration in `application.conf`, which is the recommended approach.

**Alternatives considered**:
- Hardcoded model in agent class: Inflexible; requires code changes to switch models.
- Multiple model provider support: Over-engineering for current scope.
