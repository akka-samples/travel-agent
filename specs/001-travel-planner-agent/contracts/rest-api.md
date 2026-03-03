# REST API Contract: Travel Planner Agent

**Date**: 2026-03-03 | **Feature**: 001-travel-planner-agent

## User Profile Endpoint

Base path: `/users`

### POST /users/{userId}

Create a new user profile.

**Request body**:
```json
{
  "name": "John Traveler",
  "email": "john@example.com"
}
```

**Response**: `201 Created`

---

### GET /users/{userId}

Retrieve a user profile.

**Response**: `200 OK`
```json
{
  "userId": "user-123",
  "name": "John Traveler",
  "email": "john@example.com",
  "preferences": [
    {
      "type": "ACCOMMODATION_TYPE",
      "value": "hotel",
      "priority": 5
    }
  ],
  "pastTripIds": ["trip-abc-123"]
}
```

---

### PATCH /users/{userId}

Update a user's name and/or email.

**Request body**:
```json
{
  "name": "John Updated",
  "email": "john.updated@example.com"
}
```

**Response**: `200 OK`

---

### POST /users/{userId}/preferences

Add a travel preference.

**Request body**:
```json
{
  "type": "CUISINE",
  "value": "italian",
  "priority": 3
}
```

**Response**: `200 OK`

**Valid preference types**: ACCOMMODATION_TYPE, TRANSPORTATION_TYPE, CUISINE, ACTIVITY, CLIMATE, BUDGET_RANGE

---

## Travel Planner Endpoint

Base path: `/travel-planner`

### POST /travel-planner/create

Create a travel plan. Initiates the workflow and returns a trip ID immediately.

**Request body**:
```json
{
  "userId": "user-123",
  "destination": "Paris, France",
  "startDate": "2025-06-15",
  "endDate": "2025-06-22",
  "budget": 2000.00
}
```

**Response**: `201 Created`
```json
{
  "tripId": "550e8400-e29b-41d4-a716-446655440000"
}
```

**Validation errors**:
- `400 Bad Request` if end date is before start date
- `400 Bad Request` if budget is zero or negative

---

### GET /travel-planner/trips/{tripId}

Retrieve a trip with its full structured travel plan.

**Response**: `200 OK`
```json
{
  "tripId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "user-123",
  "destination": "Paris, France",
  "startDate": "2025-06-15",
  "endDate": "2025-06-22",
  "budget": 2000.00,
  "status": "PLANNED",
  "plan": {
    "summary": "A week-long exploration of Paris...",
    "totalEstimatedCost": 1850.00,
    "days": [
      {
        "dayNumber": 1,
        "date": "2025-06-15",
        "accommodation": {
          "name": "Hotel Le Marais",
          "description": "Boutique hotel in the heart of Paris",
          "estimatedCost": 150.00
        },
        "transportation": [
          {
            "type": "flight",
            "description": "Arrival at Charles de Gaulle Airport",
            "estimatedCost": 0.00
          }
        ],
        "activities": [
          {
            "name": "Eiffel Tower Visit",
            "description": "Evening visit with city views",
            "estimatedCost": 25.00,
            "timeOfDay": "evening"
          }
        ],
        "meals": [
          {
            "type": "dinner",
            "suggestion": "Le Petit Cler - French bistro",
            "estimatedCost": 35.00
          }
        ],
        "dailyEstimatedCost": 210.00
      }
    ]
  }
}
```

---

### GET /travel-planner/trips/{tripId}/as-text

Retrieve a trip as human-readable formatted text.

**Response**: `200 OK` (Content-Type: text/plain)
```text
Travel Plan: Paris, France
June 15 - June 22, 2025
Budget: $2,000.00

Summary: A week-long exploration of Paris...

Day 1 - June 15, 2025
  Accommodation: Hotel Le Marais - Boutique hotel in the heart of Paris ($150.00)
  Transportation: flight - Arrival at Charles de Gaulle Airport
  Activities:
    - Evening: Eiffel Tower Visit - Evening visit with city views ($25.00)
  Meals:
    - Dinner: Le Petit Cler - French bistro ($35.00)
  Daily Cost: $210.00

...

Total Estimated Cost: $1,850.00
```
