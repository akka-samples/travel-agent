# Data Model: Travel Planner Agent

**Date**: 2026-03-03 | **Feature**: 001-travel-planner-agent

## Entities

### UserProfile (Event Sourced Entity)

**Purpose**: Stores a user's identity, accumulated travel preferences, and trip history.

| Field | Type | Description |
|-------|------|-------------|
| userId | String | Unique identifier (client-provided) |
| name | String | User's display name |
| email | String | User's email address |
| preferences | List&lt;TravelPreference&gt; | Accumulated travel preferences |
| pastTripIds | List&lt;String&gt; | IDs of completed trips |

**State transitions**: Created → Updated (name/email changes, preference additions, trip completions)

**Events**:
- `UserProfileCreated(userId, name, email)` — Initial profile creation
- `UserProfileUpdated(name, email)` — Name or email change
- `TravelPreferenceAdded(TravelPreference)` — New preference added
- `TripCompleted(tripId)` — Trip added to history

**Validation rules**:
- Name and email must be non-empty on creation
- Preference type must be one of the six defined categories
- Preference priority must be a positive integer

---

### TravelPreference (Value Object)

**Purpose**: Represents a single travel preference with priority weighting.

| Field | Type | Description |
|-------|------|-------------|
| type | PreferenceType (enum) | Category of preference |
| value | String | Descriptive value (e.g., "hotel", "italian") |
| priority | int | Priority weighting (higher = more important) |

**PreferenceType enum values**: ACCOMMODATION_TYPE, TRANSPORTATION_TYPE, CUISINE, ACTIVITY, CLIMATE, BUDGET_RANGE

---

### Trip (Event Sourced Entity)

**Purpose**: Stores a travel plan request and its AI-generated itinerary.

| Field | Type | Description |
|-------|------|-------------|
| tripId | String | Unique identifier (server-generated UUID) |
| userId | String | ID of the requesting user |
| destination | String | Travel destination |
| startDate | LocalDate | Trip start date |
| endDate | LocalDate | Trip end date |
| budget | double | Budget amount |
| plan | TravelPlan | AI-generated travel plan |
| status | TripStatus (enum) | Current trip status |

**TripStatus enum values**: PLANNED, BOOKED, COMPLETED (only PLANNED in scope)

**Events**:
- `TripCreated(tripId, userId, destination, startDate, endDate, budget, plan)` — Trip created with generated plan

**Validation rules**:
- Start date must be before end date
- Budget must be positive
- Destination must be non-empty

---

### TravelPlan (Value Object — AI-generated)

**Purpose**: Structured output from the AI agent, containing a complete day-by-day itinerary.

| Field | Type | Description |
|-------|------|-------------|
| summary | String | Brief overview of the entire trip |
| totalEstimatedCost | double | Aggregated cost across all days |
| days | List&lt;DayPlan&gt; | Day-by-day itinerary |

### DayPlan (Nested Value Object)

| Field | Type | Description |
|-------|------|-------------|
| dayNumber | int | Day sequence number |
| date | String | Date for this day |
| accommodation | Accommodation | Where to stay |
| transportation | List&lt;Transportation&gt; | How to get around |
| activities | List&lt;Activity&gt; | What to do |
| meals | List&lt;Meal&gt; | Where/what to eat |
| dailyEstimatedCost | double | Total cost for this day |

### Accommodation (Nested Value Object)

| Field | Type | Description |
|-------|------|-------------|
| name | String | Accommodation name |
| description | String | Brief description |
| estimatedCost | double | Cost per night |

### Transportation (Nested Value Object)

| Field | Type | Description |
|-------|------|-------------|
| type | String | Mode of transport (flight, train, etc.) |
| description | String | Route/details |
| estimatedCost | double | Transport cost |

### Activity (Nested Value Object)

| Field | Type | Description |
|-------|------|-------------|
| name | String | Activity name |
| description | String | Brief description |
| estimatedCost | double | Activity cost |
| timeOfDay | String | When during the day (morning, afternoon, evening) |

### Meal (Nested Value Object)

| Field | Type | Description |
|-------|------|-------------|
| type | String | Meal type (breakfast, lunch, dinner) |
| suggestion | String | Restaurant or food recommendation |
| estimatedCost | double | Meal cost |

## Entity Relationships

```text
UserProfile 1──* TravelPreference  (embedded list)
UserProfile 1──* Trip              (via pastTripIds reference)
Trip        1──1 TravelPlan        (embedded)
TravelPlan  1──* DayPlan           (embedded list)
DayPlan     1──1 Accommodation     (embedded)
DayPlan     1──* Transportation    (embedded list)
DayPlan     1──* Activity          (embedded list)
DayPlan     1──* Meal              (embedded list)
```

## Workflow State

### TravelPlannerWorkflow State

| Field | Type | Description |
|-------|------|-------------|
| tripId | String | Generated trip ID |
| userId | String | Requesting user |
| destination | String | Travel destination |
| startDate | String | Trip start date |
| endDate | String | Trip end date |
| budget | double | Budget amount |
| status | WorkflowStatus | Current workflow step |

**WorkflowStatus values**: STARTED, PLAN_GENERATED, TRIP_STORED, COMPLETED, ERROR

**Steps**:
1. `generatePlan` — Call TravelPlannerAgent to create itinerary
2. `storeTrip` — Persist trip via TripEntity
3. `updateUserProfile` — Add trip to user's history via UserProfileEntity
4. `errorStep` — Handle unrecoverable failures
