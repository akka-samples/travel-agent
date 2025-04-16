# CLAUDE.md - Akka Java SDK Project Guidelines

## Build Commands
```
mvn compile                   # Compile the project
mvn test                      # Run all unit tests
mvn test -Dtest=MyTest        # Run a specific test class
mvn test -Dtest=MyTest#test   # Run a specific test method
mvn verify                    # Run all tests, including integration tests
mvn compile exec:java         # Run the service locally
mvn clean install -DskipTests # Build container image
```

## Code Style Guidelines
- **Imports**: Organize imports with standard Java packages first, then third-party packages, then project-specific packages
- **Package Structure**: Follow `com.[domain-module].[api|application|domain]` organization
- **Naming**: Use camelCase for methods/variables, PascalCase for classes
- **Comments**: Use JavaDoc for public classes and methods
- **Access Control**: Be mindful of Akka @Acl annotations for endpoints

## Domain Model
- Use Java records for the domain model, events, commands, requests and responses, because those are more concise and are serializable by default.
- Prefer Java Optional for nullable values.

## Entities
- Commands can be defined as records inside the entity and they don't have to have a common interface.
- Command handlers should be implemented in the entity and not in the domain object. The domain object should have methods for business logic validations but it should not handle commands directly or be concerned with entity effects.
- Command handlers that make updates without returning any information should return akka.Done for successful responses and effects().error() for valiation errors.
- Events should be defined in the domain package and the events for an entity should have a common sealed interface, and define `@TypeName` for serialization.
- `applyEvent` method should never return null, return the current state or throw an exception.
- Key Value Entities, Event Sourced Entities, Workflows can accept only single method parameter, wrap multiple parameters in a record class.

## Workflows
- State of a workflow can be defined as a record inside the workflow.

## Endpoints
- Implement HTTP endpoints with `@HttpEndpoint` and path annotations.
- Request and response endpoints should be defined as records inside the endpoint. Domain objects should not be exposed to the outside by the endpoint but instead be converted to the request and response objects of the endpoint. Include a fromDomain conversion method in the response record if there are many fields or nested records that needs to be converted from domain to endpoint records.
- Endpoints can return the response directly, without CompletionStage, since we prefer to use non-async code. For endpoint methods that create or update something it can return HttpResponse and return for example `HttpResponses.created()` or `HttpResponses.ok()`.
- Use ComponentClient for inter-component communication. When using the componentClient you should use the `method` followed by `invoke`. Don't use `invokeAsync` since we prefer to use non-async code, without composition of CompletionStages.

## Testing
- Extend `TestKitSupport` for integration tests.
- Use `EventSourcedTestKit` for unit tests of Event Sourced Entity. Create a new instance of the testkit for each test method.
- Use JUnit 5 annotations (@Test, etc.)
- Use `componentClient` for testing components.
