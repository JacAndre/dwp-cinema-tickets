# DWP Cinema Tickets - Ticket Service Implementation

This project is a solution to the **DWP Cinema Tickets coding exercise**, which required implementing `TicketServiceImpl` to enforce business rules, calculate payments, reserve seats, and reject invalid requests.

## Objective

Demonstrate clean, reusable and well-tested code to fulfil the following criteria:
- Enforce business rules for ticketing
- Calculate correct payment and seat reservations
- Integrate with external services
- Reject invalid purchase requests

## Business Rules

| Ticket Type | Price | Seat Required | Adult Required |
|-------------|-------|---------------|----------------|
| ADULT       | 25    | ✅             | ❌              |
| CHILD       | 15    | ✅             | ✅              |
| INFANT      | 0     | ❌             | ✅              |

Additional Rules:
- A maximum of 25 tickets can be purchased in 1 transaction
- Infants must be accompanied by an Adult
- Child or Infant tickets cannot be purchased without at least 1 Adult ticket
- Only Adult and Child tickets count towards seat reservations

## Testing Strategy

### Unit Tests `TicketServiceImplTest`

- Covers all invalid purchase request scenarios (nulls, limits and missing adults)
- Verifies correct calls to `TicketPaymentService` and `SeatReservationService`
- Scalable coverage implemented by parameterised tests
- Utilises fixtures (e.g. `adultTicket(...)`) for improved readability

### Integration Test `TicketServiceIntegrationTest`

- Uses stubbed services with SLF4J logging through Lombok
- Validates end-to-end flow without mocks
- Tagged with `@Tag("Integration")` to allow for selective execution


## Running Tests

To run all tests

```shell
mvn test
```

To run integration tests

```shell
mvn test -Dgroups=Integration
```

## Tech Stack

- Java 21
- JUnit 5
- Mockito
- Lombok
- SLF4J

## Assumptions

- All account IDs > 0 are valid
- External services are defect free
- No modification allowed to `TicketService` or anything in the `thirdparty` package

## Repository Structure

Modified or added files
```text
cinema-tickets-java/
├── src/
│   └── main/java/uk/gov/dwp/uc/pairtest/TicketServiceImpl.java
├── test/java/uk/gov/dwp/uc/pairtest/
│   ├── TicketServiceImplTest.java
│   └── TicketServiceIntegrationTest.java
├── pom.xml
└── README.md
```