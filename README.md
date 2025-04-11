# Travel Planner Agent

The Travel Planner Agent is an AI-powered application built on the Akka SDK that helps users create personalized travel itineraries. The service leverages event sourcing and large language models (LLMs) to generate detailed travel plans based on user preferences and trip parameters.

To understand the Akka concepts that are the basis for this example, see [Development Process](https://doc.akka.io/concepts/development-process.html) in the documentation.

This project contains a skeleton to create an agentic AI service. To understand more about these components, see [Developing services](https://doc.akka.io/java/index.html). Other examples can be found [here](https://doc.akka.io/java/samples.html).

# Run locally

Provide your OpenAI API key in environment variable `OPENAI_API_KEY`.

To start your service locally, run:

```shell
mvn compile exec:java
```

## User Profile API

The Travel Planner service provides a REST API for managing user profiles. Below are examples of how to interact with the API using curl.

Create a new user profile with name and email:

```shell
curl -i localhost:9000/users/user-123 \
  --header "Content-Type: application/json" \
  -XPOST \
  --data '{"name":"John Traveler","email":"john@example.com"}'
```

Retrieve a user profile by ID:

```shell
curl -i localhost:9000/users/user-123
````

Update a user's name and email:

```shell
curl -i localhost:9000/users/user-123 \
  --header "Content-Type: application/json" \
  -XPATCH \
  --data '{"name":"John Updated","email":"john.updated@example.com"}'
```

Add a travel preference to a user's profile:

```shell
curl -i localhost:9000/users/user-123/preferences \
  --header "Content-Type: application/json" \
  -XPOST \
  --data '{
    "type": "ACCOMMODATION_TYPE",
    "value": "hotel",
    "priority": 5
  }'
```

Available preference types:

* ACCOMMODATION_TYPE (e.g., "hotel", "hostel", "apartment")
* TRANSPORTATION_TYPE (e.g., "flight", "train", "car")
* CUISINE (e.g., "italian", "vegetarian")
* ACTIVITY (e.g., "hiking", "museums", "beaches")
* CLIMATE (e.g., "warm", "cold", "moderate")
* BUDGET_RANGE (e.g., "budget", "mid-range", "luxury")

## Travel Planner API

Create a travel plan using the workflow, which will generate the plan, store it, and update the user's profile.
The response includes the trip ID.

```shell
curl -i localhost:9000/travel-planner/create \
  --header "Content-Type: application/json" \
  -XPOST \
  --data '{
    "userId": "user-123",
    "destination": "Paris, France",
    "startDate": "2025-06-15",
    "endDate": "2025-06-22",
    "budget": 2000.00
  }'
```

Get a trip by ID:

```shell
curl -i localhost:9000/travel-planner/trip/<tripId>
```

### Run tests

Run unit tests with:

```shell
mvn test
```

Integration tests require that you have defined the OpenAI API key in the environment variable `OPENAI_API_KEY`. Then run integration tests with:

```shell
mvn verify
```

## Deployment

Build container image:

```shell
mvn clean install -DskipTests
```

Install the `akka` CLI as documented in [Install Akka CLI](https://doc.akka.io/reference/cli/index.html).

Deploy the service using the image tag from above `mvn install`:

```shell
akka service deploy travel-agent travel-agent:tag-name --push
```

Refer to [Deploy and manage services](https://doc.akka.io/operations/services/deploy-service.html)
for more information.

## Architecture Overview

### System Components

The system consists of the following key components:

1. **Domain Models**: Core business entities like `UserProfile`, `TravelPreference`, and `TravelPlan`
2. **Entities**: Event-sourced entities that manage state changes (`UserProfileEntity`, `TripEntity`)
3. **Workflow**: Orchestrates the travel planning process (`TravelPlannerWorkflow`)
4. **Agent**: Interfaces with LLMs to generate travel plans (`TravelPlannerAgent`)
5. **HTTP Endpoints**: REST APIs for client interaction (`UserProfileEndpoint`, `TravelPlannerEndpoint`)

### Component Interactions

#### Travel Plan Creation Flow

```mermaid
sequenceDiagram
    participant Client
    participant TravelAPI as TravelPlannerEndpoint
    participant Workflow as TravelPlannerWorkflow
    participant Agent as TravelPlannerAgent
    participant UserEntity as UserProfileEntity
    participant TripEntity
    participant LLM as OpenAI GPT-4o-mini

    Client->>TravelAPI: POST /travel-planner/create
    TravelAPI->>Workflow: createTravelPlan(command)
    
    Workflow->>Agent: generateTravelPlan(userId, destination, dates, budget)
    Agent->>UserEntity: getUserProfile()
    UserEntity-->>Agent: UserProfile (with preferences)
    
    Agent->>LLM: createTravelPlanJson(tripDetails)
    LLM-->>Agent: JSON response
    Agent-->>Workflow: TravelPlan
    
    Workflow->>TripEntity: createTrip(tripId, plan, etc.)
    TripEntity-->>Workflow: Done
    
    Workflow->>UserEntity: addCompletedTrip(tripId)
    UserEntity-->>Workflow: Done
    
    Workflow-->>TravelAPI: CreateTravelPlanResponse
    TravelAPI-->>Client: {tripId: "..."}
```

#### User Profile Management Flow

```mermaid
sequenceDiagram
    participant Client
    participant UserAPI as UserProfileEndpoint
    participant UserEntity as UserProfileEntity
    
    Client->>UserAPI: POST /users/{userId}
    UserAPI->>UserEntity: createUserProfile(userId, name, email)
    UserEntity-->>UserAPI: Done
    UserAPI-->>Client: Success
    
    Client->>UserAPI: POST /users/{userId}/preferences
    UserAPI->>UserEntity: addTravelPreference(preference)
    UserEntity-->>UserAPI: Done
    UserAPI-->>Client: Success
    
    Client->>UserAPI: GET /users/{userId}
    UserAPI->>UserEntity: getUserProfile()
    UserEntity-->>UserAPI: UserProfile
    UserAPI-->>Client: UserProfileResponse
```

### Key Technologies

- **Akka SDK**: Provides the foundation for building event-sourced, stateful services
- **Event Sourcing**: Used to track all state changes in the system
- **Langchain4j**: Java library for interacting with LLMs
- **OpenAI GPT-4o-mini**: The LLM used to generate travel plans

### Data Flow

1. **User Profile Creation**:
    - User data and preferences are stored in the `UserProfileEntity`
    - Each preference change is tracked as an event

2. **Travel Plan Generation**:
    - User requests a travel plan with destination, dates, and budget
    - The workflow coordinates the process
    - The agent retrieves user preferences and generates a prompt for the LLM
    - The LLM returns a structured JSON response
    - The JSON is parsed into a `TravelPlan` domain object
    - The plan is stored in the `TripEntity`
    - The trip ID is added to the user's profile

3. **Trip Retrieval**:
    - Clients can retrieve trip details using the trip ID
    - The response includes the structured travel plan

## Future Enhancements

1. **Feedback Loop**: Incorporate user feedback to improve future recommendations
2. **Real-time Updates**: Integrate with external APIs for real-time pricing, availability, weather information, etc
3. **Multi-modal Responses**: Support for maps, images, and other rich content
4. **Collaborative Planning**: Allow multiple users to collaborate on a trip
5. **Personalization**: Deeper personalization based on past trips and preferences

## IDE AI assistant hints

This sample was developed together with an IDE AI assistant and the following hints can be useful when developing similar agentic services with Akka.

The assistant needs up-to-date information about Akka SDK documentation. One way to provide the documentation is to place a copy of the full copy of the Akka SDK documentation (HTML) in a directory of the project and include it as (indexable) context for the assistant. Some IDE assistants can use documentation from a custom website and then you should point at https://doc.akka.io/java/, and not https://doc.akka.io/ since the latter also includes documentation about the Akka libraries that you don't want to use for this.

The assistant also needs existing sample code that it can use as template and recommended conventions. Similar to the documentation, you can include sample code as (indexable) context for the assistant. For example, this travel planner example is a good template for developing a similar agentic service with Akka. You may even start out from this sample source code and then add your own agent in a separate package.

It's good to develop iteratively, step by step, instead of asking the assistant to generate too much code at the same time. Agree on an initial design first, and then develop one component at a time. In that way the assistant also learns about your preferences along the way.

There are some recommended practises that you may need to teach the assistant. When given feedback it is often good at making corrections and then follow the same pattern later in the session. You can also try to give some instructions up front, such as the following:

```
Some code conventions when using the Akka SDK:

Use Java records for the domain model, events, commands, requests and responses, because those are more concise and are serializable by default.

Commands can be defined as records inside the entity and they don't have to have a common interface.

Events should be defined in the domain package and the events for an entity should have a common sealed interface, and define @TypeName for serialization.

Request and response endpoints should be defined as records inside the endpoint. Domain objects should not be exposed to the outside by the endpoint but instead be converted to the request and response objects of the endpoint. Include a fromDomain conversion method in the response record if there are many fields or nested records that needs to be converted from domain to endpoint records.

Command handlers should be implemented in the entity and not in the domain object. The domain object should have methods for business logic validations but it should not handle commands directly or be concerned with entity effects.

Command handlers that make updates without returning any information should return akka.Done for successful responses and effects().error() for valiation errors.

State of a workflow can be defined as a record inside the workflow.

When using the componentClient you should use the method followed by invoke. Don't use invokeAsync since we prefer to use non-async code, without composition of CompletionStages.

Endpoints can return the response directly, without CompletionStage, since we prefer to use non-async code. For endpoint methods that create or update something it can return HttpResponse and return for example HttpResponses.created() or HttpResponses.ok()

In the agent I would like to use the Assistant api of Langchain4j.
```

The AI assistant is typically very good at generating tests. Ask in the following way:

```
Add a unit test for the TripEntity using the EventSourcedTestKit of the Akka SDK. Note that a new instance of the testkit should be created for each test.

Create an end-to-end integration test using the TravelPlannerEndpoint and UserProfileEndpoint. Use the http client provided by the TestKitSupport to interact with the endpoint. You can use request and response records of the endpoints in the test to create the json requests and parse the json responses.
```

