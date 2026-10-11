# JDK Fleet Assistant

An AI assistant, built in Java with Spring AI, that helps plan the migration of a fleet of Java servers from Oracle JDK to OpenJDK — with a human approving every action.

> **Status:** work in progress. Phase 1 of 5 starts November 2026. This README describes what the project will do; each section is updated as it ships.

## Why this project

Migrating hundreds of servers from one JDK to another is repetitive and risky: inventories are messy, compatibility questions are scattered across release notes, and one wrong step can break a production application.

This project explores where an AI layer actually helps in that process, and where plain deterministic code is the better answer. It is built the way a regulated enterprise would need it: in the JVM stack, observable, tested, and with a person in control.

## Important: simulated data only

This is a personal learning project. **All server data is simulated** and all reference material comes from public documentation (OpenJDK release notes and migration guides). It contains no code, data or details from any employer or client.

## Roadmap

| Phase | Dates | What the assistant gains |
| --- | --- | --- |
| 1. LLMs from Java | Nov–Dec 2026 | Turns messy server reports (free text, logs, command output) into a validated, structured inventory, using structured output and a tool call |
| 2. RAG | Jan–Feb 2027 | Answers JDK migration questions from public documentation, always citing the source |
| 3. Agents + MCP | Mar–Apr 2027 | An MCP server in Java exposes fleet tools; an agent proposes a batched migration plan that only runs after human approval |
| 4. Evals | May–Jun 2027 | An eval suite in CI that fails the build when quality drops, plus latency, token and cost metrics |
| 5. Cloud | Jul–Sep 2027 | Deployed on AWS, with its monthly cost documented |

- [x] Phase 0 — project bootstrap (Spring Boot 4, Spring AI 2.0, PostgreSQL + pgvector)
- [ ] Phase 1 — structured inventory
- [ ] Phase 2 — RAG over migration docs
- [ ] Phase 3 — MCP tools and agent with human approval
- [ ] Phase 4 — evals and observability
- [ ] Phase 5 — AWS deployment

## Stack

Java 21 · Spring Boot 4 · Spring AI 2.0 · Anthropic Claude · PostgreSQL + pgvector · Docker · JUnit — later MCP, GitHub Actions and AWS.

## How to run

Requirements: Java 21, Docker Desktop running, and an Anthropic API key.

```bash
export ANTHROPIC_API_KEY='your-key'
./mvnw spring-boot:run
```

Spring Boot starts PostgreSQL + pgvector automatically via Docker Compose.

| Endpoint | What it checks |
| --- | --- |
| `GET /api/status` | The service is up |
| `GET /actuator/health` | App and database health |
| `GET /api/ai/ping` | End-to-end call to the model |

Run the tests with `./mvnw test`.

## Design principles

1. **A human approves every action.** The assistant proposes; it never executes on its own.
2. **Deterministic first.** If plain code solves a step reliably, the LLM stays out of it.
3. **Measured, not assumed.** Every AI feature ships with evals that show whether it works.
4. **Costs are visible.** Token usage and infrastructure cost are tracked and documented.

## Non-goals

- It is not a production migration tool.
- It never connects to real infrastructure.

## Author

Roviro Acuña Tafur — Senior Java Backend Engineer & Tech Lead · [LinkedIn](https://www.linkedin.com/in/roviacuna)

## License

MIT
