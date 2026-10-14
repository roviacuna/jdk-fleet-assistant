# JDK Fleet Assistant

An AI assistant, built in Java with Spring AI, that helps plan the migration of a fleet of Java servers from Oracle JDK
to OpenJDK — with a human approving every action.

> **Status:** work in progress — currently in Phase 1 (LLMs from Java). This README describes what the project will
> do; each section is updated as it ships.

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

| Phase             | What the assistant gains                                                                                                                     |
|-------------------|----------------------------------------------------------------------------------------------------------------------------------------------|
| 1. LLMs from Java | Turns messy server reports (free text, logs, command output) into a validated, structured inventory, using structured output and a tool call |
| 2. RAG            | Answers JDK migration questions from public documentation, always citing the source                                                          |
| 3. Agents + MCP   | An MCP server in Java exposes fleet tools; an agent proposes a batched migration plan that only runs after human approval                    |
| 4. Evals          | An eval suite in CI that fails the build when quality drops, plus latency, token and cost metrics                                            |
| 5. Cloud          | Deployed on AWS, with its monthly cost documented                                                                                            |

- [x] Phase 0 — project bootstrap (Spring Boot 4, Spring AI 2.0, PostgreSQL + pgvector)
- [ ] Phase 1 — structured inventory *(in progress: chat and customer-email endpoints with scoped system prompts,
  truncation detection, switchable model provider)*
- [ ] Phase 2 — RAG over migration docs
- [ ] Phase 3 — MCP tools and agent with human approval
- [ ] Phase 4 — evals and observability
- [ ] Phase 5 — AWS deployment

## Stack

Java 25 · Spring Boot 4 · Spring AI 2.0 · Anthropic Claude · Ollama and Docker Model Runner (local models) ·
PostgreSQL + pgvector · Docker · JUnit — later MCP, GitHub Actions and AWS.

## How to run

Requirements: Java 25, Docker Desktop running, and an Anthropic API key. [Ollama](https://ollama.com) is optional, only
for the local-model profile.

```bash
export ANTHROPIC_API_KEY='your-key'
./mvnw spring-boot:run
```

Spring Boot starts PostgreSQL + pgvector automatically via Docker Compose.

| Endpoint               | What it does                                                                                   |
|------------------------|------------------------------------------------------------------------------------------------|
| `GET /api/status`      | Confirms the service is up                                                                     |
| `GET /actuator/health` | App and database health                                                                        |
| `GET /api/ai/ping`     | End-to-end call to the model                                                                   |
| `POST /api/chat`       | Answers JDK migration questions; returns `{ "reply": "...", "truncated": false }`              |
| `POST /api/email`      | Drafts a reply email to a customer's message; returns `{ "email": "...", "truncated": false }` |

Both AI endpoints only answer questions about Java and JDK migration and politely refuse anything else. Their system
prompts live in `src/main/resources/prompts/`. Invalid input returns `400`; if the model returns no result, the API
returns `502`.

Ready-to-run requests are in [`requests.http`](requests.http) (IntelliJ HTTP client), including off-topic and
prompt-injection cases, with assertions on status codes. Run the tests with `./mvnw test`.

### Switching models

The chat provider is chosen by configuration, not code. Claude is the default; a Spring profile switches to a local
model.

Ollama:

```bash
ollama pull llama3.1:8b
./mvnw spring-boot:run -Dspring-boot.run.profiles=ollama
```

Docker Model Runner (OpenAI-compatible API, enable it in Docker Desktop):

```bash
docker model pull ai/gemma3
./mvnw spring-boot:run -Dspring-boot.run.profiles=docker
```

| Profile   | Provider                               | Configuration                   |
|-----------|----------------------------------------|---------------------------------|
| (default) | Anthropic Claude                       | `application.properties`        |
| `ollama`  | Ollama at localhost:11434              | `application-ollama.properties` |
| `docker`  | Docker Model Runner at localhost:12434 | `application-docker.properties` |

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

  | Model          | Where         | Time         | Quality                                                                       |
      |----------------|---------------|--------------|-------------------------------------------------------------------------------|
  | Claude (Haiku) | Anthropic API | ~11 s        | Accurate, covered the key features                                            |
  | llama3.2 3B    | Ollama, CPU   | ~23 s        | Hallucinated facts and fell into a repetition loop                            |
  | llama3.1 8B    | Ollama, CPU   | ~96 s (warm) | Fluent and well formatted, but invented release dates, codenames and features |

  All three returned HTTP 200. The 8B model also gave different answers to slightly different phrasings of the same
  question, and the first call timed out the client while the model loaded into memory. Takeaways: quality must be
  checked by evals, not status codes; open questions go to a strong hosted model; small local models are kept for
  bounded tasks such as embeddings (`mxbai-embed-large`) or extraction from context the app provides.
- **A per-request `.system()` replaces the default system prompt.** A leftover `.system("You are a helpful
  assistant.")` silently overrode the `defaultSystem` set on the builder, so the assistant ignored its scope rules and
  answered off-topic questions. Removing it fixed the problem. In Spring AI's `ChatClient`, a per-request system prompt
  replaces the default, while advisors and tools are added to the defaults.
- **Shared response handling in one place.** Reading the text, detecting truncation and rejecting empty model responses
  live in a small `ModelResponses` helper, so each new AI endpoint gets the same behavior instead of copying it.
- **Customer input is data, not instructions.** The email prompt wraps the customer's message in tags and tells the
  model to treat it only as content to answer. `requests.http` includes injection attempts in both the message and the
  name fields; these are checked by reading the response for now, and will become automated evals in Phase 4.

## Known issues

- The configured `max-tokens` value is not applied yet, so long answers may be truncated (they are flagged with
  `"truncated": true`).
- `POST /api/chat` and `POST /api/email` have no authentication or rate limiting. Fine for local development; required
  before any deployment (Phase 5).

## Non-goals

- It is not a production migration tool.
- It never connects to real infrastructure.

## Author

Roviro Acuña Tafur — Senior Java Backend Engineer & Tech Lead · [LinkedIn](https://www.linkedin.com/in/roviacuna)

## License

MIT