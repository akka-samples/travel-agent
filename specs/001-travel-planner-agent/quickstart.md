# Quickstart: Travel Planner Agent

**Date**: 2026-03-03 | **Feature**: 001-travel-planner-agent

## Prerequisites

- Java 21+
- Maven 3.9+
- OpenAI API key

## Setup

1. Set the OpenAI API key:
   ```shell
   export OPENAI_API_KEY="your_openai_api_key"
   ```

2. Build and run:
   ```shell
   mvn compile exec:java
   ```

## Quick Test

1. Create a user:
   ```shell
   curl -i localhost:9000/users/user-123 \
     --header "Content-Type: application/json" \
     -XPOST \
     --data '{"name":"John Traveler","email":"john@example.com"}'
   ```

2. Add a preference:
   ```shell
   curl -i localhost:9000/users/user-123/preferences \
     --header "Content-Type: application/json" \
     -XPOST \
     --data '{"type":"ACTIVITY","value":"museums","priority":5}'
   ```

3. Generate a travel plan:
   ```shell
   curl -i localhost:9000/travel-planner/create \
     --header "Content-Type: application/json" \
     -XPOST \
     --data '{
       "userId":"user-123",
       "destination":"Paris, France",
       "startDate":"2025-06-15",
       "endDate":"2025-06-22",
       "budget":2000.00
     }'
   ```

4. Retrieve the trip (use the tripId from step 3):
   ```shell
   curl -i localhost:9000/travel-planner/trips/<tripId>
   ```

## Running Tests

Unit tests:
```shell
mvn test
```

Integration tests (requires `OPENAI_API_KEY`):
```shell
mvn verify
```

## Key Components

| Component | Type | Purpose |
|-----------|------|---------|
| UserProfileEntity | Event Sourced Entity | User profile and preference management |
| TripEntity | Event Sourced Entity | Trip storage |
| TravelPlannerWorkflow | Workflow | Orchestrates plan generation pipeline |
| TravelPlannerAgent | Agent | LLM interaction for plan generation |
| UserProfileEndpoint | HTTP Endpoint | REST API for user management |
| TravelPlannerEndpoint | HTTP Endpoint | REST API for travel planning |
