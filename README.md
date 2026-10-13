# JDK Fleet Assistant

An AI assistant, built in Java with Spring AI, that helps plan the migration of a fleet of Java servers from Oracle JDK
to OpenJDK — with a human approving every action.

> **Status:** work in progress — Phase 1 (LLMs from Java) started in October 2026. This README describes what the
> project will do; each section is updated as it ships.

## Why this project

Migrating hundreds of servers from one JDK to another is repetitive and risky: inventories are messy, compatibility
questions are scattered across release notes, and one wrong step can break a production application.

This project explores where an AI layer actually helps in that process, and where plain deterministic code is the better
answer. It is built the way a regulated enterprise would need it: in the JVM stack, observable, tested, and with a
person in control.

## Important: simulated data only

This is a personal learning project. **All server data is simulated** and all reference material comes from public
documentation (OpenJDK release notes and migration guides). It contains no code, data or details from any employer or
client.

## Roadmap

| Phase             | Dates        | What the assistant gains                                                                                                                     |
|-------------------|--------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| 1. LLMs from Java | Oct–Dec 2026 | Turns messy server reports (free text, logs, command output) into a validated, structured inventory, using structured output and a tool call |
| 2. RAG            | Jan–Feb 2027 | Answers JDK migration questions from public documentation, always citing the source                                                          |
| 3. Agents + MCP   | Mar–Apr 2027 | An MCP server in Java exposes fleet tools; an agent proposes a batched migration plan that only runs after human approval                    |
| 4. Evals          | May–Jun 2027 | An eval suite in CI that fails the build when quality drops, plus latency, token and cost metrics                                            |
| 5. Cloud          | Jul–Sep 2027 | Deployed on AWS, with its monthly cost documented                                                                                            |

- [x] Phase 0 — project bootstrap (Spring Boot 4, Spring AI 2.0, PostgreSQL + pgvector)
- [ ] Phase 1 — structured inventory *(in progress: validated chat endpoint, truncation detection, switchable model
  provider)*
- [ ] Phase 2 — RAG over migration docs
- [ ] Phase 3 — MCP tools and agent with human approval
- [ ] Phase 4 — evals and observability
- [ ] Phase 5 — AWS deployment

## Stack

Java 25 · Spring Boot 4 · Spring AI 2.0 · Anthropic Claude · Ollama (local models) · PostgreSQL + pgvector · Docker ·
JUnit — later MCP, GitHub Actions and AWS.

## How to run

Requirements: Java 25, Docker Desktop running, and an Anthropic API key. [Ollama](https://ollama.com) is optional, only
for the local-model profile.

```bash
export ANTHROPIC_API_KEY='your-key'
./mvnw spring-boot:run
```

Spring Boot starts PostgreSQL + pgvector automatically via Docker Compose.

| Endpoint               | What it does                                                                   |
|------------------------|--------------------------------------------------------------------------------|
| `GET /api/status`      | Confirms the service is up                                                     |
| `GET /actuator/health` | App and database health                                                        |
| `GET /api/ai/ping`     | End-to-end call to the model                                                   |
| `POST /api/chat`       | Sends a message to the model; returns `{ "reply": "...", "truncated": false }` |

Ready-to-run requests are in [`requests.http`](requests.http) (IntelliJ HTTP client). Run the tests with `./mvnw test`.

### Switching models

The chat provider is chosen by configuration, not code. Claude is the default; the `ollama` Spring profile switches to a
local model:

```bash
ollama pull llama3.1:8b
./mvnw spring-boot:run -Dspring-boot.run.profiles=ollama
```

| Profile   | Provider                  | Configuration                   |
|-----------|---------------------------|---------------------------------|
| (default) | Anthropic Claude          | `application.properties`        |
| `ollama`  | Ollama at localhost:11434 | `application-ollama.properties` |

## Design principles

1. **A human approves every action.** The assistant proposes; it never executes on its own.
2. **Deterministic first.** If plain code solves a step reliably, the LLM stays out of it.
3. **Measured, not assumed.** Every AI feature ships with evals that show whether it works.
4. **Costs are visible.** Token usage and infrastructure cost are tracked and documented.

## Lessons learned

- **Silent empty replies.** The chat endpoint returned `200` with an empty reply. The model's response contained more
  than one generation and the code read only the first one, which was empty; the real answer was also cut off by the
  token limit. Fix: read text from all generations and expose a `truncated` flag so clients know when an answer is
  incomplete. Providers name this finish reason differently (`max_tokens` in Anthropic, `length` in others), so both are
  checked.
- **Configuration keys change between versions.** Spring AI 2.0 renamed several properties (for example,
  `spring.ai.anthropic.chat.options.model` → `spring.ai.anthropic.chat.model`). Unknown keys are ignored without errors,
  so property names should be checked against the version's metadata (IDE hints), not older tutorials.
- **Secrets never belong in examples.** A real API key ended up in `.env.example`; GitHub push protection blocked the
  push. The key was rotated and the local history rewritten before publishing. `.env` is git-ignored and keys are only
  read from environment variables.
- **Hosted vs local models (same prompt, switched with a Spring profile).**

  | Model          | Where         | Time         | Quality                                                                     |
      |----------------|---------------|--------------|-----------------------------------------------------------------------------|
  | Claude (Haiku) | Anthropic API | ~11 s        | Accurate, covered the key features                                          |
  | llama3.2 3B    | Ollama, CPU   | ~23 s        | Hallucinated facts and fell into a repetition loop                          |
  | llama3.1 8B    | Ollama, CPU   | ~96 s (warm) | Fluent and well formatted, but invented release dates, codenames and features |

  All three returned HTTP 200. The 8B model also gave different answers to slightly different phrasings of the same
  question, and the first call timed out the client while the model loaded into memory. Takeaways: quality must be
  checked by evals, not status codes; open questions go to a strong hosted model; small local models are kept for
  bounded tasks such as embeddings (`mxbai-embed-large`) or extraction from context the app provides.

## Known issues

- The configured `max-tokens` value is not applied yet, so long answers may be truncated (they are flagged with
  `"truncated": true`).
- `POST /api/chat` has no authentication or rate limiting. Fine for local development; required before any deployment
  (Phase 5).

## Non-goals

- It is not a production migration tool.
- It never connects to real infrastructure.

## Author

Roviro Acuña Tafur — Senior Java Backend Engineer & Tech Lead · [LinkedIn](https://www.linkedin.com/in/roviacuna)

## License

MIT