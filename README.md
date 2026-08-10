# Company Data Search Service

A Spring Boot service that searches for UK companies by scraping the public Companies House website, combines company information with officer and Persons with Significant Control (PSC) data, stores the results in a local database, and returns structured JSON.

The service also implements persistent caching so repeated searches do not unnecessarily send requests to Companies House.

## Tech Stack

- Java 17+
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Jsoup
- H2 Database
- Lombok
- JUnit 5
- Maven

## Features

The service supports:

- Searching Companies House by company name/query
- Pagination through Companies House search results
- Fetching up to 100 fully processed companies per query
- Company overview scraping
- Officer scraping
- Persons with Significant Control (PSC) scraping
- JSON responses
- Persistent H2 database storage
- 24-hour query caching
- Manual cache bypass using `forceRefresh`
- Graceful handling of some HTTP errors during scraping
- Polite request throttling
- A meaningful User-Agent header

## API

### Search for companies

**Method**

`GET`

**Endpoint**

```text
/api/companies/search
```

### Parameters

| Parameter | Type | Required | Description |
|---|---|---|---|
| `query` | String | Yes | Company search query |
| `forceRefresh` | boolean | No | If `true`, bypasses an existing cache entry and fetches fresh data. Defaults to `false`. |

### Example request

```text
http://localhost:8080/api/companies/search?query=tesco
```

A normal request first checks whether a fresh cached result exists.

### Force refresh example

```text
http://localhost:8080/api/companies/search?query=tesco&forceRefresh=true
```

Setting `forceRefresh=true` skips the cached result and performs a new scrape even when the existing cache is still within the 24-hour freshness period.

## Response

The endpoint returns a JSON array containing matching companies.

Each company contains:

- company number
- company name
- company status
- company type
- creation date
- registered address
- officers
- persons with significant control (PSCs)

Each officer contains:

- name
- role
- appointment date

Each PSC contains:

- name
- nature of control

### Example response

```json
[
  {
    "companyNumber": "12345678",
    "companyName": "EXAMPLE COMPANY LTD",
    "status": "Active",
    "companyType": "Private limited Company",
    "creationDate": "1 January 2020",
    "address": "Example Address, London, United Kingdom",
    "officers": [
      {
        "name": "JOHN SMITH",
        "role": "Director",
        "appointedOn": "1 January 2020"
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

The exact data returned depends on the information currently available on Companies House.

If no companies match the query, the service returns an empty JSON array:

```json
[]
```

## How the Search Works

For a query that is not already cached, the service:

1. Sends a search request to Companies House.
2. Parses the company results from the search page.
3. Follows the link for each company.
4. Extracts company overview information.
5. Fetches and parses the company's officers page.
6. Attempts to fetch and parse the company's Persons with Significant Control page.
7. Continues through search-result pages until there are no more pages or 100 companies have been fully collected.
8. Stores the company records in H2.
9. Stores the search query, timestamp, and associated companies in the search cache.
10. Returns the collected companies as JSON.

## Caching and Freshness Strategy

Search results are cached by their search query.

Each cache record stores:

- the original search query
- the time the result was cached
- the companies associated with the search

When the same query is received again, the service checks the database first.

If the cached result is less than 24 hours old, the previously stored companies are returned immediately without scraping Companies House again.

If the cached result is older than 24 hours, the service performs the search again and replaces the cached result with fresh data.

I chose a 24-hour cache lifetime as a compromise between avoiding unnecessary requests to a free public service and preventing company information from remaining stale indefinitely.

The caller can also explicitly bypass a still-valid cache by using:

```text
forceRefresh=true
```

For example:

```text
/api/companies/search?query=tesco&forceRefresh=true
```

This performs a fresh scrape, stores the updated results, and resets the cache timestamp.

## Database

The project uses an embedded H2 relational database.

I chose H2 because the task does not require an external production database and H2 allows the project to be cloned and run without installing or configuring a separate database server.

The application uses a **file-backed H2 database**, rather than an in-memory database:

```properties
spring.datasource.url=jdbc:h2:file:./data/companysearch
```

This means cached searches and company records survive application restarts.

Hibernate manages the database schema using:

```properties
spring.jpa.hibernate.ddl-auto=update
```

The generated local database files are stored under the `data/` directory and are excluded from Git.

### Test Database

Automated tests use a separate in-memory H2 database:

```properties
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop
```

This prevents tests from modifying or depending on the application's persistent local database.

## Data Model

The main persisted entities are:

### Company

Represents a company returned from Companies House.

The Companies House company number is used as the identifier because it uniquely identifies the company.

A company contains its overview data together with its officers and PSCs.

### Officer

Represents an officer appointment belonging to a company.

A company can contain multiple officer records.

### PSC

Represents a Person with Significant Control.

PSC data is treated as optional because it may not always be available or successfully retrieved.

### SearchCache

Represents a previously executed search.

The search query itself is used as the cache identifier.

A cache entry also stores:

- `cachedAt`
- the companies returned by that query

This allows the application to distinguish between storing company information and remembering which companies belonged to a particular search.

## Persistence Relationships

Company data and search-cache data are stored separately.

A cached search can contain many companies, while the same company can appear in several different search queries. Therefore, the relationship between `SearchCache` and `Company` is modeled as many-to-many.

The cached companies are loaded eagerly because retrieving a cache entry is immediately followed by returning its associated companies to the caller.

Company officer/PSC relationships represent the data collected for each individual company.

## Companies House Politeness

Companies House is a free public service, so the scraper deliberately limits how aggressively it sends requests.

### User-Agent

Every outgoing request uses a meaningful User-Agent identifying the project:

```text
Polixis-Company-Search-Internship-Task/1.0 | (educational scraper; contact: aregmuradyan.dev@gmail.com)
```

### Request delay

A delay of 500 milliseconds is inserted between requests:

```text
500 ms
```

This reduces request frequency and avoids sending a large burst of requests to Companies House.

During development, shorter delays resulted in HTTP 403 responses, so the delay also proved important in practice.

### Result limit

The service stops after fully collecting a maximum of:

```text
100 companies
```

for a single search query.

## Failure Handling

Scraping external HTML is not completely reliable. Pages can be unavailable, return unexpected HTTP status codes, or contain missing information.

The service therefore performs basic failure handling.

If an individual company cannot be fetched because of an HTTP error, that company is skipped instead of terminating the entire search.

PSC retrieval is handled separately because PSC data is optional. If the PSC page returns an HTTP error, the company can still be returned with an empty PSC list rather than discarding otherwise valid company and officer data.

Individual HTML fields are also checked for missing elements before their text is accessed. Missing fields are represented as `null` instead of causing a null-pointer failure.

This allows a search to continue when some Companies House records are incomplete or individual requests fail.

## Optional Extensions Implemented

### Failure handling

HTTP failures during individual company scraping are handled so that one failed company does not necessarily terminate the complete search.

PSC failures are handled independently because PSC information is optional.

### forceRefresh

The optional `forceRefresh` query parameter allows the caller to bypass the normal 24-hour cache and explicitly request fresh information.

Example:

```text
/api/companies/search?query=tesco&forceRefresh=true
```

## Running the Project

### Requirements

You need:

- Java 17 or later
- Maven

No separate database installation is required because H2 is embedded in the application.

### Clone the repository

```bash
git clone <repository-url>
cd Polixis-Task-Company-Data-Search-Service
```

### Run using Maven

On Windows:

```bash
mvnw.cmd spring-boot:run
```

On macOS/Linux:

```bash
./mvnw spring-boot:run
```

Alternatively, if Maven is installed globally:

```bash
mvn spring-boot:run
```

The application starts on the default Spring Boot port:

```text
8080
```

You can then search using:

```text
http://localhost:8080/api/companies/search?query=tesco
```

## Running the Tests

Using the Maven wrapper on Windows:

```bash
mvnw.cmd test
```

On macOS/Linux:

```bash
./mvnw test
```

Or with a globally installed Maven:

```bash
mvn test
```

The project includes a Spring context test to verify that the application, JPA configuration, repositories, and dependencies can start correctly.

It also includes a cache behavior test that verifies that a fresh stored search can be returned through the service.

The tests use their own in-memory H2 database and do not depend on the persistent application database.

## Project Structure

The main application follows a simple layered structure:

```text
src/main/java/com/polixis/companysearch/
│
├── controller/
│   └── CompanyController
│
├── service/
│   └── CompanySearchService
│
├── model/
│   ├── Company
│   ├── Officer
│   ├── Psc
│   └── SearchCache
│
├── repository/
│   ├── CompanyRepository
│   └── SearchCacheRepository
│
└── CompanySearchServiceApplication
```

The responsibilities are separated as follows:

- **Controller** — exposes the HTTP API and converts request parameters into service calls.
- **Service** — contains search, scraping, parsing, caching, freshness, throttling, and failure-handling logic.
- **Models / Entities** — represent the structured company data and cache records.
- **Repositories** — use Spring Data JPA to persist and retrieve entities from H2.

## Hardest Part

The most challenging part was making the scraper reliable while respecting Companies House.

A single search can require many HTTP requests because each search-result company requires additional requests for its overview, officers, and PSC information. During development, sending requests too quickly resulted in HTTP 403 responses.

This required adding a meaningful User-Agent, introducing a delay between requests, limiting each search to 100 fully collected companies, and handling failures without unnecessarily terminating the entire search.

Caching also introduced an interesting persistence issue. The relationship between a cached search and its companies was initially lazily loaded by Hibernate. When the companies were accessed after the persistence session had closed, this caused a `LazyInitializationException`. Since cached companies are always required when a cache entry is returned, the cache-to-company relationship was changed to eager loading.

These issues were useful because they required understanding both the behavior of the external website and how JPA/Hibernate loads persisted relationships.

## What I Would Improve With More Time

The current implementation is intentionally kept relatively small and focused on the task requirements.

With more time, I would consider the following improvements:

- **More focused parsing tests.** Extract HTML parsing responsibilities into dedicated parser components and test them against stored HTML fixtures without sending requests to Companies House.
- **Better retry/backoff behavior.** HTTP 403, 429, and temporary server errors could be handled with controlled retries and exponential backoff rather than immediately skipping a request.
- **Improved error responses.** Instead of relying on Spring Boot's default error response for unexpected failures, introduce structured API error responses and a global exception handler.
- **URL encoding and input validation.** Explicitly validate incoming search queries and construct search URLs using proper URI/query encoding.
- **Concurrency control for identical searches.** If two identical uncached requests arrive simultaneously, both can currently begin scraping before either one writes the cache. A per-query lock or similar mechanism could ensure only one scrape runs for a query at a time.
- **Configuration externalization.** Move values such as the cache lifetime, request delay, maximum company count, and Companies House base URL into application configuration rather than keeping them as constants.
- **Logging.** Replace `System.out.println` with a structured logging framework and appropriate log levels.
- **More precise persistence modeling.** Officer appointments could be modeled separately from people if the application needed to identify the same individual across multiple companies.
- **Additional optional extensions.** Multiple-query requests and explicit deduplication/merging could be added if required.
- **Production database.** For a larger production service, H2 could be replaced with PostgreSQL or another production-oriented database.

I deliberately did not add these features to keep the submitted solution understandable and focused rather than adding unnecessary complexity.

## Known Limitations

The service scrapes the public Companies House HTML website rather than using a stable structured API, so changes to the site's HTML structure or CSS selectors may require updates to the scraper.

The scraper is synchronous, meaning a fresh search can take noticeable time because the service deliberately waits between requests and may need to visit several pages for each company.

A company may be skipped when required company/officer requests fail.

PSC data may be empty when the PSC page is unavailable.

Two identical uncached requests made at the same time can both begin scraping before the first request has populated the cache.

Cached data may be up to approximately 24 hours old unless the caller uses `forceRefresh=true`.

## Design Decisions

A few choices were made deliberately to keep the implementation simple and appropriate for the scope of the task:

**Jsoup** was chosen because the data source is HTML and Jsoup provides straightforward HTTP retrieval and CSS-selector-based parsing.

**Spring Data JPA** was used to avoid manually implementing standard persistence operations and to keep the database layer small.

**H2** was chosen because it provides persistent relational storage without requiring the reviewer to install a separate database.

**24-hour caching** provides a simple compromise between freshness and avoiding unnecessary repeated scraping.

**500 ms request throttling** reduces load on Companies House and helped avoid HTTP 403 responses observed with more aggressive request rates.

**100-company maximum** follows the task requirement and prevents a broad search from creating an unbounded scraping operation.

## Summary

The service provides a simple API around Companies House search data while adding structured JSON output, persistence, caching, freshness control, and basic failure handling.

The implementation prioritizes understandable code and conservative interaction with the public Companies House service rather than trying to turn the assignment into a production-scale scraping platform.
