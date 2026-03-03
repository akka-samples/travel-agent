# Tasks: Travel Planner Agent

**Input**: Design documents from `/specs/001-travel-planner-agent/`
**Prerequisites**: plan.md (required), spec.md (required), research.md, data-model.md, contracts/

**Tests**: Test tasks are included as they are integral to the Akka SDK development workflow (constitution Principle III).

**Organization**: Tasks are grouped by user story to enable independent implementation and testing of each story.

## Format: `[ID] [P?] [Story] Description`

- **[P]**: Can run in parallel (different files, no dependencies)
- **[Story]**: Which user story this task belongs to (e.g., US1, US2, US3)
- Include exact file paths in descriptions

## Phase 1: Setup (Shared Infrastructure)

**Purpose**: Project initialization, dependencies, and configuration

- [x] T001 Create Maven project with `akka-javasdk-parent` POM and required dependencies in `pom.xml`
- [x] T002 [P] Configure LLM provider settings (OpenAI GPT-4o-mini) in `src/main/resources/application.conf`
- [x] T003 [P] Create package structure: `com.example.travel.domain`, `com.example.travel.application`, `com.example.travel.api` under `src/main/java/`

---

## Phase 2: Foundational (Blocking Prerequisites)

**Purpose**: Shared domain model records that multiple user stories depend on

**⚠️ CRITICAL**: No user story work can begin until this phase is complete

- [x] T004 [P] Create `TravelPreference` record with `PreferenceType` enum (6 categories: ACCOMMODATION_TYPE, TRANSPORTATION_TYPE, CUISINE, ACTIVITY, CLIMATE, BUDGET_RANGE), value, and priority fields in `src/main/java/com/example/travel/domain/TravelPreference.java`
- [x] T005 [P] Create `TravelPlan` record hierarchy: `TravelPlan` (summary, totalEstimatedCost, days), `DayPlan` (dayNumber, date, accommodation, transportation, activities, meals, dailyEstimatedCost), `Accommodation` (name, description, estimatedCost), `Transportation` (type, description, estimatedCost), `Activity` (name, description, estimatedCost, timeOfDay), `Meal` (type, suggestion, estimatedCost) in `src/main/java/com/example/travel/domain/TravelPlan.java`

**Checkpoint**: Foundation ready - shared domain records available for all user stories

---

## Phase 3: User Story 1 - Create and Manage User Profile (Priority: P1) 🎯 MVP

**Goal**: Users can create profiles, update their info, add travel preferences, and retrieve their complete profile with preferences and trip history.

**Independent Test**: Create a user via REST API, update name/email, add preferences, retrieve profile and verify all data is returned correctly.

### Domain Layer for User Story 1

- [x] T006 [P] [US1] Create `UserProfile` state record with userId, name, email, preferences (List\<TravelPreference\>), pastTripIds (List\<String\>), immutable `with*` update methods, and validation logic in `src/main/java/com/example/travel/domain/UserProfile.java`
- [x] T007 [P] [US1] Create `UserEvent` sealed interface with event records: `UserProfileCreated`, `UserProfileUpdated`, `TravelPreferenceAdded`, `TripCompleted` (each with `@TypeName`) in `src/main/java/com/example/travel/domain/UserEvent.java`

### Application Layer for User Story 1

- [x] T008 [US1] Create `UserProfileEntity` event sourced entity with command handlers: `createUserProfile` (name, email), `updateUserProfile` (name, email), `addTravelPreference` (TravelPreference), `addCompletedTrip` (tripId), `getUserProfile`; event handlers for all UserEvent types in `src/main/java/com/example/travel/application/UserProfileEntity.java`

### Tests for User Story 1

- [x] T009 [US1] Create `UserProfileEntityTest` unit tests using `EventSourcedTestKit`: test create profile, update profile, add preference, add completed trip, get profile, and validation error cases in `src/test/java/com/example/travel/application/UserProfileEntityTest.java`

### API Layer for User Story 1

- [x] T010 [US1] Create `UserProfileEndpoint` HTTP endpoint with `@Acl` annotation: `POST /users/{userId}` (create), `GET /users/{userId}` (get), `PATCH /users/{userId}` (update), `POST /users/{userId}/preferences` (add preference); define request/response records as inner types; use `ComponentClient` with synchronous `.invoke()` in `src/main/java/com/example/travel/api/UserProfileEndpoint.java`

### Integration Tests for User Story 1

- [x] T011 [US1] Create `UserProfileEndpointIntegrationTest` extending `TestKitSupport` using `httpClient`: test create user, get user, update user, add preference via REST in `src/test/java/com/example/travel/api/UserProfileEndpointIntegrationTest.java`

**Checkpoint**: User Story 1 fully functional — users can create profiles, update info, add preferences, and retrieve profiles via REST API

---

## Phase 4: User Story 2 - Generate Personalized Travel Plan (Priority: P1)

**Goal**: Users can request a travel plan and the system generates a personalized day-by-day itinerary using an AI agent, orchestrated through a reliable workflow. The generated plan is stored as a trip and added to the user's history.

**Independent Test**: Create a user with preferences, request a travel plan, verify the workflow completes (trip stored, user profile updated with trip ID).

### Domain Layer for User Story 2

- [x] T012 [P] [US2] Create `Trip` state record with tripId, userId, destination, startDate, endDate, budget, plan (TravelPlan), status (TripStatus enum: PLANNED, BOOKED, COMPLETED), and immutable `with*` methods in `src/main/java/com/example/travel/domain/Trip.java`
- [x] T013 [P] [US2] Create `TripEvent` sealed interface with `TripCreated` event record (with `@TypeName`) in `src/main/java/com/example/travel/domain/TripEvent.java`

### Application Layer for User Story 2

- [x] T014 [US2] Create `TripEntity` event sourced entity with command handlers: `createTrip` (tripId, userId, destination, dates, budget, plan), `getTrip`; event handler for TripCreated in `src/main/java/com/example/travel/application/TripEntity.java`
- [x] T015 [US2] Create `TravelPlannerAgent` extending `Agent` with single command handler `generateTravelPlan` that: retrieves user profile via ComponentClient, formats preferences into prompt, sets system message as expert travel planner, uses `responseConformsTo(TravelPlan.class)` for structured response, handles errors with `.onFailure()` in `src/main/java/com/example/travel/application/TravelPlannerAgent.java`
- [x] T016 [US2] Create `TravelPlannerWorkflow` extending `Workflow` with: state record (tripId, userId, destination, dates, budget, status), `createTravelPlan` command handler, three steps (`generatePlan` calling agent, `storeTrip` calling TripEntity, `updateUserProfile` calling UserProfileEntity), `errorStep` for failure handling; configure `WorkflowSettings` with 120s step timeout and `maxRetries(2)` with failover to errorStep; use workflow ID as agent session ID in `src/main/java/com/example/travel/application/TravelPlannerWorkflow.java`

### Tests for User Story 2

- [x] T017 [US2] Create `TripEntityTest` unit tests using `EventSourcedTestKit`: test create trip with plan, get trip, validation in `src/test/java/com/example/travel/application/TripEntityTest.java`
- [x] T018 [US2] Create `TravelPlannerAgentIntegrationTest` extending `TestKitSupport` with `TestModelProvider`: mock LLM response with sample TravelPlan JSON using `fixedResponse(JsonSupport.encodeToString(...))`, test that agent returns structured TravelPlan in `src/test/java/com/example/travel/application/TravelPlannerAgentIntegrationTest.java`

### API Layer for User Story 2

- [x] T019 [US2] Create `TravelPlannerEndpoint` HTTP endpoint with `@Acl` annotation: `POST /travel-planner/create` (generate UUID tripId, start workflow, return tripId with 201 Created), `GET /travel-planner/trips/{tripId}` (get trip as JSON), `GET /travel-planner/trips/{tripId}/as-text` (get trip as formatted text); validate dates and budget; define request/response records as inner types in `src/main/java/com/example/travel/api/TravelPlannerEndpoint.java`

### Integration Tests for User Story 2

- [x] T020 [US2] Create `TravelPlannerEndpointIntegrationTest` extending `TestKitSupport` with `TestModelProvider` for agent mocking using `httpClient`: test full flow (create user, add preferences, create travel plan, poll for completion, retrieve trip as JSON and as text) in `src/test/java/com/example/travel/api/TravelPlannerEndpointIntegrationTest.java`

**Checkpoint**: User Story 2 fully functional — complete travel plan generation pipeline works end-to-end with AI agent, workflow orchestration, and trip storage

---

## Phase 5: User Story 3 - Retrieve Trip Details (Priority: P2)

**Goal**: Users can retrieve previously generated trips in both structured JSON and human-readable text formats.

**Independent Test**: Retrieve a stored trip by ID in both formats; verify JSON contains full plan structure and text is human-readable.

> Note: The GET endpoints for trip retrieval are already created in T019 as part of the TravelPlannerEndpoint. This phase focuses on ensuring the text formatting logic is complete and tested, and handling error cases.

- [x] T021 [US3] Implement `TravelPlan.asText()` or a `TravelPlanFormatter` utility method that converts the structured TravelPlan into a human-readable text format (day-by-day with accommodation, activities, meals, costs) in `src/main/java/com/example/travel/domain/TravelPlan.java`
- [x] T022 [US3] Add error handling for non-existent trip IDs in `TravelPlannerEndpoint` — return appropriate HTTP error response in `src/main/java/com/example/travel/api/TravelPlannerEndpoint.java`

**Checkpoint**: Trip retrieval works in both JSON and text formats with proper error handling

---

## Phase 6: User Story 4 - Reliable Plan Generation with Recovery (Priority: P2)

**Goal**: The workflow handles AI failures gracefully with retries and error reporting.

**Independent Test**: Verify workflow retry behavior and error state transitions.

> Note: The workflow with recovery settings is already implemented in T016. This phase focuses on verifying and testing the recovery behavior.

- [x] T023 [US4] Verify `TravelPlannerWorkflow` settings include `maxRetries(2)` and failover to `errorStep`, and that `errorStep` properly records ERROR status in workflow state in `src/main/java/com/example/travel/application/TravelPlannerWorkflow.java`
- [x] T024 [US4] Create `TravelPlannerWorkflowIntegrationTest` extending `TestKitSupport` with `TestModelProvider`: test successful workflow completion (all 3 steps), test workflow error handling when agent fails (verify error status) in `src/test/java/com/example/travel/application/TravelPlannerWorkflowIntegrationTest.java`

**Checkpoint**: Workflow recovery verified — retries work and error states are properly recorded

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: Documentation and final validation

- [x] T025 Update `README.md` with project overview, architecture diagram, API examples (curl commands), build/run/test instructions, and deployment guide
- [ ] T026 Run `quickstart.md` validation — manually test all curl commands from quickstart against running service

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: No dependencies — can start immediately
- **Foundational (Phase 2)**: Depends on Setup completion — BLOCKS all user stories
- **User Story 1 (Phase 3)**: Depends on Foundational phase
- **User Story 2 (Phase 4)**: Depends on Foundational phase AND User Story 1 (needs UserProfileEntity)
- **User Story 3 (Phase 5)**: Depends on User Story 2 (needs TravelPlan and TripEntity)
- **User Story 4 (Phase 6)**: Depends on User Story 2 (needs TravelPlannerWorkflow)
- **Polish (Phase 7)**: Depends on all user stories being complete

### User Story Dependencies

- **User Story 1 (P1)**: Can start after Foundational (Phase 2) — No dependencies on other stories
- **User Story 2 (P1)**: Depends on User Story 1 — needs UserProfileEntity for agent to retrieve preferences
- **User Story 3 (P2)**: Depends on User Story 2 — needs TravelPlan domain model and TripEntity
- **User Story 4 (P2)**: Depends on User Story 2 — tests the workflow created in US2

### Within Each User Story

- Domain records before entities
- Entities before agents/workflows
- Agents before workflows (workflow calls agent)
- Application layer before API layer
- Unit tests after each component
- Integration tests after API layer

### Parallel Opportunities

- T002 and T003 can run in parallel (Setup phase)
- T004 and T005 can run in parallel (Foundational phase — different files)
- T006 and T007 can run in parallel (US1 domain — different files)
- T012 and T013 can run in parallel (US2 domain — different files)
- T017 and T018 can run in parallel (US2 tests — different test targets)

---

## Parallel Example: User Story 1

```bash
# Launch domain records in parallel:
Task T006: "Create UserProfile record in domain/UserProfile.java"
Task T007: "Create UserEvent sealed interface in domain/UserEvent.java"

# Then sequentially:
Task T008: "Create UserProfileEntity (depends on T006, T007)"
Task T009: "Create UserProfileEntityTest (depends on T008)"
Task T010: "Create UserProfileEndpoint (depends on T008)"
Task T011: "Create UserProfileEndpointIntegrationTest (depends on T010)"
```

---

## Implementation Strategy

### MVP First (User Story 1 Only)

1. Complete Phase 1: Setup
2. Complete Phase 2: Foundational domain records
3. Complete Phase 3: User Story 1 (user profile management)
4. **STOP and VALIDATE**: Test User Story 1 independently via REST API
5. Deploy/demo if ready — users can manage profiles and preferences

### Incremental Delivery

1. Complete Setup + Foundational → Foundation ready
2. Add User Story 1 → Test independently → Deploy (Profile management MVP)
3. Add User Story 2 → Test independently → Deploy (AI plan generation)
4. Add User Story 3 → Test independently → Deploy (Text format retrieval)
5. Add User Story 4 → Test independently → Deploy (Recovery verification)
6. Polish → Final validation and documentation

---

## Notes

- [P] tasks = different files, no dependencies
- [Story] label maps task to specific user story for traceability
- Each user story should be independently completable and testable
- Commit after each task or logical group
- Stop at any checkpoint to validate story independently
- Follow Akka SDK conventions: `akka.*` imports, `@Component(id=...)`, `@Acl` on endpoints
- Use `EventSourcedTestKit` for entity unit tests, `httpClient` for endpoint integration tests
- Use `TestModelProvider` with `fixedResponse()` for agent tests
