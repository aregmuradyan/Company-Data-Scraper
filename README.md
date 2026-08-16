# UK Company Data Scraper

A Spring Boot application that searches for UK companies by scraping the public Companies House website and returns structured JSON containing company details, officers, and Persons with Significant Control (PSCs).

Search results are stored in a persistent H2 database and cached for 24 hours to reduce repeated requests to Companies House while keeping the returned information reasonably fresh.

## Tech Stack

* Java 21
* Spring Boot
* Spring Web
* Spring Data JPA / Hibernate
* Jsoup
* H2 Database
* Lombok
* JUnit 5
* Maven

## Features

* Search UK companies by query
* Scrape company overview information
* Retrieve company officers
* Retrieve Persons with Significant Control (PSCs)
* Follow Companies House search pagination
* Fetch a maximum of 100 fully processed companies per query
* Store company data in a persistent H2 database
* Cache search results for 24 hours
* Manually bypass the cache using `forceRefresh`
* Handle individual scraping failures gracefully
* Apply a 500 ms delay between Companies House requests
* Send a meaningful User-Agent with outgoing requests

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

For each company, the application collects:

* Company number
* Company name
* Status
* Company type
* Creation date
* Registered address
* Officers

    * Name
    * Role
    * Appointment date
* Persons with Significant Control

    * Name
    * Nature of control

PSC information is treated as optional. If a PSC page cannot be retrieved, the rest of the company data can still be returned with an empty PSC list.

## How It Works

When a search request is received, the application first checks whether the same query has a valid cached result.

If a fresh cache entry exists, its associated companies are returned immediately.

Otherwise, the service searches Companies House and parses the matching results. For each company, it retrieves the company overview page, officers page, and PSC page.

The scraper continues through search-result pages until there are no more pages or 100 companies have been collected.

The resulting company records are persisted in H2, the search query and timestamp are stored in the search cache, and the final list is returned as JSON.

## Caching and Freshness

Each search query is stored together with the time it was fetched and the companies returned by the search.

Cached results are considered valid for **24 hours**.

If the same query is requested during that period, the application returns the stored companies without contacting Companies House again.

After 24 hours, the cache is considered stale and the data is scraped again.

A 24-hour lifetime provides a simple balance between avoiding unnecessary requests and preventing company information from remaining outdated indefinitely.

Fresh data can also be requested manually using:

```text
/api/companies/search?query=tesco&forceRefresh=true
```

When `forceRefresh=true`, the existing cache is bypassed and a fresh scrape is performed.

## Database

The application uses an embedded, file-backed **H2 database**:

```properties
spring.datasource.url=jdbc:h2:file:./data/companysearch
spring.datasource.driver-class-name=org.h2.Driver
spring.jpa.hibernate.ddl-auto=update
```

Because the database is file-backed, stored companies and cached searches survive application restarts.

No external database server needs to be installed.

The generated `data/` directory is excluded from Git.

Automated tests use a separate in-memory H2 database so test execution does not modify persistent application data.

## Companies House Request Handling

The scraper deliberately limits how aggressively it interacts with the public Companies House service.

Every outgoing request uses a descriptive User-Agent:

```text
UK-Company-Data-Scraper/1.0 | (educational project; contact: aregmuradyan.dev@gmail.com)
```

A **500 ms delay** is inserted between requests.

Each search stops after a maximum of **100 fully fetched companies**.

During development, shorter delays resulted in HTTP 403 responses, so throttling also improves reliability in addition to reducing load on the external service.

## Failure Handling

External HTML scraping is not completely predictable, so the application handles several failure cases.

If an HTTP error occurs while retrieving required data for an individual company, that company can be skipped without terminating the entire search.

PSC retrieval is handled separately because PSC data is optional. If the PSC request fails, the company can still be returned with an empty PSC list.

HTML elements are also checked for `null` before their contents are accessed. Missing fields therefore result in `null` values rather than a `NullPointerException`.

## Running the Project

### Requirements

* Java 21

The project includes the Maven Wrapper, so a separate Maven installation is not required.

### Clone

```bash
git clone https://github.com/aregmuradyan/uk-company-data-scraper.git
cd uk-company-data-scraper
```

### Windows

```bash
mvnw.cmd spring-boot:run
```

### macOS / Linux

```bash
./mvnw spring-boot:run
```

The application starts by default on:

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

The tests verify that the Spring application context can start successfully and that fresh cached search results can be returned through the service.

Tests use a temporary in-memory H2 database.

## Project Structure

```text
src/main/java/com/aregmuradyan/companydatascraper/
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
└── CompanyDataScraperApplication
```

The application follows a simple layered structure:

* **Controller** — exposes the HTTP API
* **Service** — contains scraping, caching, freshness, pagination, and failure-handling logic
* **Models / Entities** — represent company data and cache records
* **Repositories** — provide persistence through Spring Data JPA

## Technical Decisions

### H2

H2 keeps local setup lightweight while still providing relational persistence and integration with JPA/Hibernate.

A file-backed database is used rather than an in-memory database so cached searches remain available after restarting the application.

### Company Number as Primary Key

Companies House company numbers are used as company identifiers because they uniquely and consistently identify registered companies.

### Separate Search Cache

Company records and search records are persisted separately.

A company can appear in multiple different searches, while each search can return many companies. This allows the application to reuse persisted company entities while keeping track of which results belong to each cached query.

### Eager Cache Loading

Companies associated with a cached search are loaded eagerly.

During development, lazy loading caused a `LazyInitializationException` when cached companies were accessed after the Hibernate persistence session had closed.

Because the companies are always required when returning a cache result, eager loading is appropriate for this relationship.

## Challenges Encountered

One of the main challenges was interacting reliably with an external website.

A broad query can generate many HTTP requests because each company requires separate company, officer, and PSC requests. Sending these requests too quickly resulted in HTTP 403 responses.

This was addressed by adding request throttling, a descriptive User-Agent, a maximum result limit, and individual failure handling.

Another challenge involved JPA relationship loading. Cached company records initially used lazy loading, which failed when the associated companies were accessed after the Hibernate session had closed. Changing this particular relationship to eager loading resolved the issue.

## Known Limitations

* The application depends on the current Companies House HTML structure. Changes to page markup or CSS selectors may require updates to the scraper.
* Fresh searches can take noticeable time because requests are intentionally throttled.
* Cached information may be up to 24 hours old unless `forceRefresh=true` is used.
* Two identical uncached requests received simultaneously can both begin scraping before the first request has populated the cache.
* Individual companies may be skipped when required external requests fail.
* PSC information can be unavailable for some results.

## Possible Future Improvements

* Add dedicated parsing tests using saved HTML fixtures
* Add structured API error responses with a global exception handler
* Add retry and exponential-backoff behavior for temporary external failures
* Move cache lifetime, request delay, result limit, and base URL into configuration
* Replace remaining console output with structured logging
* Add explicit query validation and URI encoding
* Prevent duplicate simultaneous scrapes for the same query
* Add explicit deduplication and merging logic
* Add a simple frontend for searching and viewing company data
* Replace H2 with PostgreSQL if the application grows beyond local or demonstration use

## Summary

UK Company Data Scraper is a small Spring Boot application that combines information from several Companies House pages into structured JSON while adding persistent storage, caching, freshness control, pagination, request throttling, and basic failure handling.

The project is designed to remain lightweight enough to run locally while demonstrating a complete flow from HTTP request to external data retrieval, persistence, and JSON response.
