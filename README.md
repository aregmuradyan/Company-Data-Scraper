# UK Company Data Scraper

A Spring Boot service that searches for UK companies by scraping Companies House and returns structured JSON containing company details, officers, and Persons with Significant Control (PSCs).

Search results are stored in a persistent H2 database and cached for 24 hours to avoid repeatedly requesting the same data from Companies House.

## Tech Stack

- Java 21
- Spring Boot
- Spring Web
- Spring Data JPA / Hibernate
- Jsoup
- H2 Database
- Lombok
- JUnit 5
- Maven

## Features

- Search UK companies by query
- Scrape company overview information
- Retrieve company officers
- Retrieve Persons with Significant Control (PSCs)
- Follow Companies House search pagination
- Maximum of 100 fully fetched companies per query
- Persistent H2 storage
- 24-hour search cache
- Optional `forceRefresh` cache bypass
- Graceful handling of scraping failures
- 500 ms delay between Companies House requests
- Meaningful User-Agent on outgoing requests

## API

### Search Companies

```text
GET /api/companies/search
```

### Parameters

| Parameter      | Required | Description                                                 |
| -------------- | -------- | ----------------------------------------------------------- |
| `query`        | Yes      | Company search query                                        |
| `forceRefresh` | No       | Bypasses the cache when set to `true`. Defaults to `false`. |

| Parameter | Required | Description |
|---|---|---|
| `query` | Yes | Company search query |
| `forceRefresh` | No | Bypasses the cache when set to `true`. Defaults to `false`. |

### Example Request

```text
http://localhost:8080/api/companies/search?query=tesco
```

To explicitly request fresh data:

```text
http://localhost:8080/api/companies/search?query=tesco&forceRefresh=true
```

### Example Response

```json
[
  {
    "companyNumber": "12345678",
    "companyName": "EXAMPLE COMPANY LTD",
    "status": "Active",
    "companyType": "Private limited company",
    "creationDate": "10 August 2026",
    "address": "Example Street, London, United Kingdom",
    "officers": [
      {
        "name": "JOHN SMITH",
        "role": "Director",
        "appointedOn": "5 March 2026"
      }
    ],
    "pscs": [
      {
        "name": "JOHN SMITH",
        "natureOfControl": "Ownership of shares – 75% or more"
      }
    ]
  }
]
```

The exact values depend on the information currently available on Companies House.

If no companies match the query, the endpoint returns:

```json
[]
```

## Data Collected

For each company, the service collects:

- Company number
- Company name
- Status
- Company type
- Creation date
- Registered address
- Officers
  - Name
  - Role
  - Appointment date
- Persons with Significant Control
  - Name
  - Nature of control

PSC information is treated as optional. If the PSC page cannot be retrieved, the company can still be returned with an empty PSC list.

## How It Works

For a new search query, the service first searches Companies House and parses the matching companies.

For each result, it follows the company's page to retrieve overview information, then requests the corresponding officers and PSC pages. It continues through the available search pages until there are no more results or 100 companies have been fully collected.

The completed records are stored in H2 and returned to the caller as JSON.

On later requests for the same query, the cache is checked before Companies House is contacted.

## Caching and Freshness

Each search query is stored together with the time it was fetched and the companies returned by that search.

Cached results remain valid for **24 hours**. If the same query is requested during that period, the stored companies are returned directly without making another Companies House request.

After 24 hours, the query is scraped again and the stored data is refreshed.

I chose 24 hours as a simple compromise between keeping company information reasonably current and avoiding unnecessary requests to a free public service.

The optional `forceRefresh` parameter allows the caller to bypass a still-valid cache when fresh data is specifically required:

```text
/api/companies/search?query=tesco&forceRefresh=true
```

## Database

The project uses an embedded, file-backed **H2 database**:

```properties
spring.datasource.url=jdbc:h2:file:./data/companysearch
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update
```

Because the database is file-backed, cached searches remain available after the application is stopped and restarted.

No external database installation is required.

The generated `data/` directory is excluded from Git.

Automated tests use a separate in-memory H2 database so they do not modify the application's persistent data.

## Companies House Politeness

The scraper is intentionally conservative when interacting with Companies House.

Every outgoing request uses a meaningful User-Agent:

```text
UK-Company-Data-Scraper/1.0 | (educational project; contact: aregmuradyan.dev@gmail.com)
```

A **500 ms delay** is inserted between requests, and a single search stops after a maximum of **100 fully fetched companies**.

The delay was particularly important during development because sending requests more aggressively resulted in HTTP 403 responses.

## Failure Handling

Because the application depends on an external website, individual requests can fail.

If an HTTP error occurs while fetching a company's required data, that company is skipped rather than terminating the entire search.

PSC retrieval is handled separately. Since PSC data is optional, a failure on the PSC page does not cause the whole company to be discarded.

Missing HTML fields are checked before their values are accessed, allowing unavailable fields to be represented as `null` rather than causing a parsing failure.

## Optional Extensions Implemented

Two optional extensions were implemented:

**Failure handling** — individual scraping failures are handled so that one problematic record does not unnecessarily terminate the whole search.

**`forceRefresh` flag** — callers can explicitly bypass the 24-hour cache and request fresh Companies House data.

## Running the Project

### Requirements

- Java 21

The repository includes the Maven Wrapper, so installing Maven separately is not required.

### Clone

```bash
git clone <repository-url>
cd Polixis-Task-Company-Data-Search-Service
```

### Windows

```bash
mvnw.cmd spring-boot:run
```

### macOS / Linux

```bash
./mvnw spring-boot:run
```

The application will start on:

```text
http://localhost:8080
```

Example:

```text
http://localhost:8080/api/companies/search?query=tesco
```

The H2 database is created automatically when the application runs for the first time.

## Running Tests

### Windows

```bash
mvnw.cmd test
```

### macOS / Linux

```bash
./mvnw test
```

The current tests verify that the Spring application context starts correctly and that a fresh cached search can be retrieved through the service.

Tests use a temporary in-memory H2 database rather than the application's persistent database.

## Project Structure

```text
src/main/java/com/polixis/companysearch/
├── controller/
│   └── CompanyController
├── model/
│   ├── Company
│   ├── Officer
│   ├── Psc
│   └── SearchCache
├── repository/
│   ├── CompanyRepository
│   └── SearchCacheRepository
├── service/
│   └── CompanySearchService
└── CompanySearchServiceApplication
```

The controller exposes the HTTP endpoint, the service contains the scraping and caching logic, the model classes represent persisted data, and the repositories provide database access through Spring Data JPA.

## Hardest Part

The hardest part was making the scraper behave reliably while interacting responsibly with Companies House.

A single search can generate many requests because each company requires separate overview, officer, and PSC requests. During development, making requests too quickly resulted in HTTP 403 responses. Adding a meaningful User-Agent, introducing a delay between requests, limiting the number of fully fetched companies, and handling failed requests individually made the behavior considerably more reliable.

Caching also introduced a useful JPA issue during testing. The companies associated with a cached search were initially lazily loaded. Accessing them after the Hibernate session had closed caused a `LazyInitializationException`. Since the companies are always needed when a cached search is returned, that relationship was changed to eager loading.

## What I Would Improve With More Time

The current solution focuses on keeping the required behavior straightforward and understandable. With more time, I would improve several areas:

- Add dedicated parsing tests using saved HTML fixtures instead of depending on live Companies House pages.
- Add structured API error responses using a global exception handler.
- Add controlled retry/backoff behavior for temporary HTTP failures.
- Move values such as the 24-hour cache lifetime, 500 ms request delay, and 100-company limit into application configuration.
- Replace the remaining console output with structured application logging.
- Add explicit input validation and URI/query encoding.
- Prevent two simultaneous requests for the same uncached query from both starting separate scraping operations.
- Add deduplication/merging if the same company appears multiple times.
- Consider PostgreSQL or another production-oriented database if the service were deployed at a larger scale.

I intentionally kept these outside the current implementation rather than adding additional complexity that was not necessary for the scope of the task.

## Known Limitations

The application scrapes Companies House HTML, so changes to the website's HTML structure or selectors may require updates to the parser.

Fresh searches can take some time because requests are intentionally throttled and each company may require several separate page requests.

Cached data can be up to 24 hours old unless `forceRefresh=true` is used.

Two simultaneous requests for the same uncached query can currently both begin scraping before either request has written its result to the cache.

## AI Assistance

I used ChatGPT as a learning and development assistant during the project, mainly to discuss Spring/JPA concepts, debug issues, review implementation choices, and improve documentation.

I implemented and tested the code myself and made sure I understood the components and decisions included in the final solution.

## Summary

The service provides a small Spring Boot API for searching UK company data while combining information from several Companies House pages into structured JSON.

The implementation focuses on the core requirements of the task while adding persistent caching, freshness control, failure handling, and a manual refresh option without introducing unnecessary infrastructure.
