# API Roadmap - Intentionally Deferred Features

This document tracks APIs and features that are **intentionally missing** from the v0 foundation.
These are planned for future milestones and should not be considered bugs or oversights.

---

## Week 2: Experiment Configuration APIs

The current API allows creating experiments but not fully configuring them.
The following endpoints are required for a complete workflow:

### Scope Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/experiments/{id}/scopes` | Add store-SKU pairs to experiment |
| GET | `/api/experiments/{id}/scopes` | List all scopes for experiment |
| DELETE | `/api/experiments/{id}/scopes/{scopeId}` | Remove a scope entry |
| POST | `/api/experiments/{id}/scopes/bulk` | Bulk add scopes (CSV or JSON) |

**Why deferred:** Scope management requires validation logic (store/SKU existence, no duplicates, test/control balance checks). These validations add complexity beyond the v0 foundation.

### Lever Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/experiments/{id}/levers` | Add a pricing lever |
| GET | `/api/experiments/{id}/levers` | List all levers |
| PUT | `/api/experiments/{id}/levers/{leverId}` | Update lever value |
| DELETE | `/api/experiments/{id}/levers/{leverId}` | Remove a lever |

**Why deferred:** Lever creation requires guardrail pre-validation. Adding levers without guardrails could create invalid experiments.

### Guardrail Configuration

| Method | Endpoint | Description |
|--------|----------|-------------|
| PUT | `/api/experiments/{id}/guardrails` | Set/update guardrails |
| GET | `/api/experiments/{id}/guardrails` | Get current guardrails |

**Why deferred:** Guardrails should have sensible defaults and possibly org-wide templates. The data model supports guardrails, but the configuration UX needs design.

---

## Week 3: Simulation Results APIs

The current `/run-simulation` endpoint creates a simulation run but results cannot be retrieved.

### Simulation Results

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/simulations/{runId}` | Get simulation run details + summary |
| GET | `/api/simulations/{runId}/results` | Get daily result breakdown |
| GET | `/api/simulations/{runId}/results/by-store` | Results aggregated by store |
| GET | `/api/simulations/{runId}/results/by-sku` | Results aggregated by SKU |
| GET | `/api/experiments/{id}/simulations` | List all simulation runs for experiment |

**Why deferred:** The simulation engine itself is a stub. Results APIs are meaningless without actual projection logic.

---

## Week 4+: Supporting Features

### Reference Data Management

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/stores` | List stores (with filtering) |
| GET | `/api/skus` | List SKUs (with filtering) |
| GET | `/api/stores/{id}/prices` | Get base prices for a store |

**Why deferred:** Reference data is assumed to come from external master data systems. APIs for browsing may be added for convenience.

### Reporting & Export

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/experiments/{id}/report` | Generate experiment summary report |
| GET | `/api/simulations/{runId}/export` | Export results as CSV/Excel |

**Why deferred:** Report format and export requirements need stakeholder input.

### Audit Trail

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/experiments/{id}/audit` | Get audit history for experiment |
| GET | `/api/audit` | Search audit logs (admin only) |

**Why deferred:** Audit logging infrastructure exists but is not wired into services. APIs depend on that integration.

---

## Not Planned for v0

The following are explicitly **out of scope** and will not be added without a product decision:

- Pagination on list endpoints (acceptable for small internal user base)
- Bulk experiment operations
- Experiment templates/cloning
- Scheduled/recurring experiments
- Real-time notifications
- Webhook integrations

---

## How to Contribute

When implementing a deferred API:

1. Update this document to mark the item as "In Progress" or "Done"
2. Follow existing patterns in `experiment/controller/`
3. Add corresponding integration tests
4. Update `README.md` API table if adding public endpoints
