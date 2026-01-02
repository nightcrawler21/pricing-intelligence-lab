# Pricing Intelligence Lab

Internal pricing experiment lab for simulating and analyzing store-level price tests with guardrails, auditability, and clear impact reporting.

## Purpose

This tool enables the Pricing Analytics team to:

- **Define pricing experiments** at the store level with clear control and test groups
- **Configure guardrails** to prevent harmful pricing actions (margin floors, discount caps)
- **Run simulations** to project the impact of proposed price changes
- **Generate reports** comparing projected TEST vs CONTROL outcomes

## Important Disclaimers

> **This is a SIMULATION TOOL ONLY.**

- **No live pricing changes** are made to POS or pricing systems
- **No customer-level pricing** is supported (this is store-level only)
- **No ML/optimization** is included in v0 (projections use simplified models)
- **Experiments require approval** before simulations can be run

This is an internal prototype for the Pricing Analytics team. It is not customer-facing and should not be used for production pricing decisions without proper validation.

## Non-Goals (v0)

The following are explicitly out of scope for the initial version:

- Real-time pricing execution
- Customer segmentation or personalized pricing
- Machine learning price optimization
- Kafka/event streaming integration
- External API integrations (competitor pricing, etc.)
- Production-grade security (SSO/LDAP integration)

For a detailed list of intentionally deferred APIs (scope management, lever configuration, simulation results), see [docs/TODO.md](docs/TODO.md).

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────┐
│                    REST API Layer                           │
│  /api/experiments  │  /api/simulations  │  /api/reports    │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Service Layer                            │
│  ExperimentService │ SimulationService │ PricingRuleService│
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Domain Layer                             │
│  Experiment │ Scope │ Lever │ Guardrails │ SimulationRun   │
└─────────────────────────────────────────────────────────────┘
                              │
┌─────────────────────────────────────────────────────────────┐
│                    Data Layer                               │
│        Spring Data JPA  │  Flyway  │  PostgreSQL/H2        │
└─────────────────────────────────────────────────────────────┘
```

### Project Structure

```
src/main/java/com/example/pricinglab/
├── PricingLabApplication.java      # Main entry point
├── config/                         # Spring configuration
│   ├── SecurityConfig.java         # Authentication & authorization
│   ├── OpenApiConfig.java          # Swagger/OpenAPI setup
│   └── JpaConfig.java              # JPA auditing configuration
├── common/                         # Shared components
│   ├── audit/                      # Base auditable entity
│   ├── enums/                      # Status enums
│   └── exception/                  # Exception handlers
├── reference/                      # Reference data
│   ├── store/                      # Store entity & repository
│   └── sku/                        # SKU, BasePrice, SkuCost
├── experiment/                     # Experiment management
│   ├── controller/                 # REST endpoints
│   ├── service/                    # Business logic
│   ├── domain/                     # JPA entities
│   ├── repository/                 # Data access
│   └── dto/                        # Request/Response objects
├── simulation/                     # Simulation engine
│   ├── service/                    # Simulation logic
│   ├── domain/                     # SimulationRun, Results
│   └── repository/                 # Data access
└── audit/                          # Audit logging
    ├── AuditLog.java
    ├── AuditService.java
    └── AuditRepository.java
```

## Technology Stack

| Component | Technology |
|-----------|------------|
| Language | Java 21 |
| Framework | Spring Boot 3.2 |
| Build | Maven |
| Database | PostgreSQL (H2 for local) |
| Migrations | Flyway |
| Security | Spring Security (Basic Auth) |
| API Docs | OpenAPI 3 / Swagger UI |

## Running Locally

### Prerequisites

- Java 21+
- Maven 3.9+
- (Optional) PostgreSQL 15+ for production-like testing

### Quick Start with H2

```bash
# Clone the repository
git clone https://github.com/your-org/pricing-intelligence-lab.git
cd pricing-intelligence-lab

# Run with local profile (uses H2 in-memory database)
./mvnw spring-boot:run -Dspring-boot.run.profiles=local
```

The application will start at http://localhost:8080

### Available URLs

| URL | Description |
|-----|-------------|
| http://localhost:8080/swagger-ui.html | API Documentation |
| http://localhost:8080/h2-console | H2 Database Console (local profile only) |
| http://localhost:8080/actuator/health | Health Check |

### Default Users

| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | ADMIN, ANALYST |
| analyst | analyst123 | ANALYST |

### Running with PostgreSQL

```bash
# Set environment variables
export DB_USERNAME=pricing_lab
export DB_PASSWORD=your_password

# Run with default profile
./mvnw spring-boot:run
```

### Sample Data

The migration `V2__sample_reference_data_LOCAL_ONLY.sql` inserts demo stores, SKUs, and prices for local development. This migration:

- **Is intended for local/demo use only**
- **Must NOT be executed in production**

For local development, the sample data loads automatically with Flyway. For production deployments, either:
1. Delete the migration file before deployment
2. Configure Flyway to skip it via `flyway.ignoreMigrationPatterns`
3. Use your organization's data import process instead

## API Overview

### Experiments

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | /api/experiments | Create new experiment |
| GET | /api/experiments | List all experiments |
| GET | /api/experiments/{id} | Get experiment details |
| POST | /api/experiments/{id}/submit | Submit for approval |
| POST | /api/experiments/{id}/approve | Approve/reject (ADMIN only) |
| POST | /api/experiments/{id}/run-simulation | Start simulation |
| POST | /api/experiments/{id}/simulate | Run synchronous simulation (v0) |

### Simulation Results

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | /api/experiments/{experimentId}/simulation-runs | List runs for experiment |
| GET | /api/simulation-runs/{runId}/status | Get run status |
| GET | /api/simulation-runs/{runId}/summary | Get summary metrics |
| GET | /api/simulation-runs/{runId}/timeseries | Get timeseries data |
| GET | /api/simulation-runs/{runId}/breakdown | Get breakdown by dimension |
| GET | /api/simulation-runs/{runId}/export.csv | Download CSV export |

### Experiment Workflow

```
DRAFT → PENDING_APPROVAL → APPROVED → RUNNING → COMPLETED
                         ↘ REJECTED
```

### Submit Requirements

Before an experiment can be submitted for approval, it must pass all validation checks:

1. **Status**: Experiment must be in `DRAFT` status
2. **Dates**: `endDate` must be after `startDate` (both required)
3. **Scope**: At least one store+SKU pair must be defined
4. **Lever**: A pricing lever must be configured
5. **Guardrails**: Guardrails must be configured and valid

Validation failures return HTTP 400 with human-readable error messages.

## Security Model

- **ANALYST**: Can create experiments, view all data, run simulations
- **ADMIN**: All ANALYST permissions + approve/reject experiments

Current implementation uses in-memory authentication for prototype phase. Production deployment should integrate with corporate SSO/LDAP.

## Development Guidelines

### Running Tests

```bash
./mvnw test
```

### Building

```bash
./mvnw clean package
```

### Code Style

- Follow standard Java conventions
- Use meaningful variable and method names
- Add Javadoc for public APIs
- Keep methods focused and small

## Simulation Results API Examples

After running a simulation, use these endpoints to retrieve results.

### List simulation runs for an experiment

```bash
curl -u analyst:analyst123 \
  "http://localhost:8080/api/experiments/{experimentId}/simulation-runs?limit=10"
```

### Get simulation run status

```bash
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/status"
```

### Get summary metrics (control vs test)

```bash
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/summary"
```

Response includes control/test totals and deltas for units, revenue, and margin.

### Get timeseries data (for charts)

```bash
# All data grouped by date
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/timeseries"

# Filter by store
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/timeseries?storeId={storeId}"

# Filter by SKU
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/timeseries?skuId={skuId}"
```

### Get breakdown by dimension

```bash
# Breakdown by store
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/breakdown?by=STORE"

# Breakdown by SKU
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/breakdown?by=SKU"

# Breakdown by date
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/breakdown?by=DATE"

# Breakdown by store, filtered to specific SKU
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/breakdown?by=STORE&skuId={skuId}"
```

### Export results as CSV

```bash
curl -u analyst:analyst123 \
  "http://localhost:8080/api/simulation-runs/{runId}/export.csv" \
  -o simulation-results.csv
```

CSV columns: `runId,experimentId,date,storeId,skuId,variant,basePrice,price,unitCost,units,revenue,margin`

## Contact

For questions or issues, contact the Pricing Analytics team.

---

*This is an internal tool. Do not share outside the organization.*
