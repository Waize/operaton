# LLM Integration for BPMN Service Tasks

- status: Draft (not yet proposed)
- date: 2026-10-23
- decision-makers: TBD
- consulted: TBD
- informed: TBD
- discussion pad: TBD
- issue: TBD
- code references are pinned to commit cf6b92d

## Context and Problem Statement

Process models increasingly contain steps that are best handled by a large language model (LLM): classifying an incoming message, extracting structured data from free text, summarising a case for a human task, or letting a model decide which follow-up action to take. Operaton has no first-class way to model such a step today.

Operaton's extension point for calling external systems from a service task is Connect. A connector implements the [Connector](https://github.com/operaton/operaton/blob/cf6b92d/connect/core/src/main/java/org/operaton/connect/spi/Connector.java#L38) SPI, is registered through a [ConnectorProvider](https://github.com/operaton/operaton/blob/cf6b92d/connect/core/src/main/java/org/operaton/connect/spi/ConnectorProvider.java#L22) found by [Connectors.registerConnectors()](https://github.com/operaton/operaton/blob/cf6b92d/connect/core/src/main/java/org/operaton/connect/Connectors.java#L169), and can be configured engine-wide by a [ConnectorConfigurator](https://github.com/operaton/operaton/blob/cf6b92d/connect/core/src/main/java/org/operaton/connect/spi/ConnectorConfigurator.java#L26). The only connectors shipped are the HTTP and SOAP connectors bundled in [connectors-all](https://github.com/operaton/operaton/blob/cf6b92d/connect/connectors-all/pom.xml#L17).

Users who want an LLM step today have three workarounds, each with drawbacks:

- **HTTP connector against a provider REST API.** Every model must hand-craft provider-specific JSON payloads, authentication headers and response parsing in scripts. Switching from one provider to another means rewriting every task. Function calling (the model asking the caller to run a tool) is practically impossible to express.
- **Custom JavaDelegate.** Works, but every project re-implements provider clients, prompt handling, retries and secret management.
- **External task worker.** Moves the call out of the engine, which is good for latency, but again every project builds the client logic itself.

Comparable Camunda 7-compatible distributions already ship an AI connector; CIB seven documents an [AI agent connector](https://docs.cibseven.org/manual/latest/reference/connect/ai-agent-connector/). Users migrating between Camunda 7-compatible engines will expect an equivalent capability.

The CIB seven connector is useful prior art because it shows both what works and what is hard in this design space:

- **What it confirms.** It is built on LangChain4j 1.14 and registers through the unchanged Connect SPI, so no engine changes were needed. It ships as a removable overlay: without the overlay jars the engine loads no AI dependencies. Every invocation writes an audit timeline into a process variable and can mark results as AI-generated, which it explicitly maps to the record-keeping and transparency duties of the EU AI Act. Chat memory is held in a replaceable, JVM-local store.
- **What it leaves open.** It only speaks OpenAI-compatible APIs. API keys, database passwords and custom headers are input parameters and therefore end up in process variables and history. Any class named in the toolClasses parameter is loaded and exposed to the model, without an allowlist; MCP servers have no tool allowlist either. The answer is always a String, so structured output has to be parsed downstream. The built-in process starter tool takes its polling budget from the model without a server-side limit and commits started processes in a separate transaction. Its documentation lists these points itself under known limitations.

The general question is therefore: **should Operaton provide built-in LLM support for BPMN service tasks, and if so, on which technical foundation and with which scope?**

Two properties of the existing execution model shape the answer. First, [ServiceTaskConnectorActivityBehavior.execute()](https://github.com/operaton/operaton/blob/cf6b92d/engine-plugins/connect-plugin/src/main/java/org/operaton/connect/plugin/impl/ServiceTaskConnectorActivityBehavior.java#L67) invokes the connector synchronously inside the engine transaction, so a slow LLM call holds a database transaction open unless the task is marked asynchronous. Second, connectors are loaded with the classloader of the plugin in [ConnectProcessEnginePlugin.preInit()](https://github.com/operaton/operaton/blob/cf6b92d/engine-plugins/connect-plugin/src/main/java/org/operaton/connect/plugin/impl/ConnectProcessEnginePlugin.java#L34), so any new connector and its dependencies must work in every distribution (Spring Boot, Quarkus, Tomcat, WildFly, Operaton Run).

## Decision Drivers

- **Provider neutrality.** Process models must not be tied to a single vendor. Switching between Ollama, OpenAI, Azure OpenAI, Anthropic, Google Gemini and Mistral should be a configuration change, not a model change.
- **Fit with the Connect SPI.** The solution should be usable via the existing operaton:connector element, input/output parameters and the Java fluent API, like the HTTP connector.
- **Runtime coverage.** Must run in all Operaton distributions, not only in Spring Boot.
- **Dependency footprint and classpath conflicts.** New libraries must not clash with Jackson, HTTP client or Spring versions already managed in the Operaton BOMs.
- **Secret handling.** API keys must not be stored in process variables, BPMN files or history; they come from engine-wide configuration or environment variables.
- **Security of tool calling.** If a model may trigger actions, the set of callable actions must be explicitly allowed by the process author or operator, because model output is untrusted (prompt injection). Naming arbitrary Java classes in a BPMN file must not be enough to expose them to a model.
- **Traceability and AI transparency.** Every model interaction (prompt, model, parameters, tool calls, result, duration, caller) must be traceable after the fact, and AI-generated values must be distinguishable from human input. This supports the record-keeping and transparency obligations of the EU AI Act for deployers in regulated sectors.
- **Non-invasiveness.** Users who do not use AI must not pay for it: without the connector on the classpath the engine must behave exactly as before and load no AI dependencies.
- **Data volume.** Audit records and conversation history can grow large; their storage must not silently inflate the runtime and history tables.
- **Transaction and latency behaviour.** LLM calls take seconds; the design must make asynchronous execution the documented default and surface timeouts clearly.
- **Testability.** The connector must be testable without paid APIs, both with mocks and against a real local model (Ollama in Testcontainers).
- **Maintenance effort.** The LLM ecosystem changes quickly; Operaton should not maintain its own provider clients.

## Considered Options

1. Status quo: no built-in support, document the HTTP connector, JavaDelegate and external task workarounds
2. New Connect module based on LangChain4j
3. New Connect module based on Spring AI
4. New Connect module with Operaton-maintained provider clients (no LLM framework)
5. Separate AI worker based on the external task client, outside the engine

Sub-questions that apply to options 2 to 4 and must be agreed together with the foundation:

- **Scope of version 1.** Plain chat (system and user prompt to text), structured JSON output based on a JSON schema, tool calling, chat memory, an audit trail and an AI-generated marker on results. Embeddings and retrieval-augmented generation are out of scope for version 1.
- **What a tool is.** Candidates: other registered connectors, Spring beans or CDI beans, or BPMN-native constructs (for example, the agent selects one of several outgoing message events). Tools are registered engine-wide through an SPI or bean registry; a task can only select from that registered set. Fully qualified class names in BPMN are not accepted. Tools that act on the engine, such as starting a process, need server-side limits and documented transaction semantics.
- **Configuration.** Named provider profiles (for example "default", "local-ollama") defined engine-wide via plugin properties, Spring Boot properties under operaton.bpm.ai and Quarkus config; the task references a profile by name and may override non-secret options such as model, temperature or max tokens. Secrets are never accepted as task input parameters.
- **Audit trail.** Each invocation produces structured audit events (request, response, tool call, error) with process correlation, model, parameters and caller identity. The events are returned as an output of the connector response, so the process author decides via an output parameter whether and under which name they are stored as a process variable; the connector itself never writes variables implicitly. Because an unmapped output or a failed invocation (no output mapping happens on error) would otherwise leave no trace, the events are additionally published to a pluggable engine-wide sink (for example logging or the history event handler). Content can be redacted to a hash plus length. The serialized audit trail regularly exceeds the 4000 character limit of string variables, so it is returned as a JSON object value rather than a String.
- **AI-generated marker.** Results can be accompanied by a provenance map (AI-generated flag, run id, provider, model, timestamp) that downstream gateways, forms and history consumers can evaluate.
- **Chat memory storage.** Conversation history is kept in a pluggable store based on the LangChain4j ChatMemoryStore interface, with a JVM-local in-memory default. Storing it as a process variable was considered and rejected as the default: every turn would rewrite a growing serialized value, and with full history level every revision is kept, so data volume grows roughly quadratically with conversation length. The accepted trade-off is that the default store does not survive restarts and is not shared across cluster nodes; operators who need that plug in a persistent store.
- **Element templates.** Modeler element templates for the connector are needed for usability, but the Operaton Modeler lives outside this repository. This ADR only fixes the connector id and parameter names that the templates bind to; the templates themselves are delivered with the Modeler.

## Decision Outcome

> **This ADR is a draft. No decision has been made yet.**
> The decision is taken when this draft is proposed, after the discussion in the
> pad and issue linked below has converged. Until then this section stays as is.

## Pros and Cons of the Options

### Status quo: document workarounds only

- **Good**, because there is no new dependency and no new maintenance surface.
- **Good**, because users keep full control over provider-specific features.
- **Bad**, because every project re-implements provider clients, secret handling and response parsing.
- **Bad**, because process models become provider-specific, which conflicts with provider neutrality.
- **Bad**, because Operaton lags behind comparable Camunda 7-compatible distributions.

### New Connect module based on LangChain4j

LangChain4j is an Apache 2.0 licensed Java library that offers a common ChatModel abstraction with one module per provider (for example langchain4j-ollama, langchain4j-open-ai, langchain4j-azure-open-ai, langchain4j-anthropic, langchain4j-google-ai-gemini, langchain4j-mistral-ai), plus building blocks for chat memory, tool specifications, structured output and agents. The connector would live in a new module operaton-connect-ai next to the HTTP and SOAP connectors and map request parameters onto a LangChain4j chat request.

- **Good**, because one abstraction covers all required providers; provider modules can be optional dependencies so users only pull what they use.
- **Good**, because it is framework-agnostic plain Java and therefore runs in every distribution; integrations exist for Spring Boot and, through the Quarkiverse extension, for Quarkus.
- **Good**, because tool calling and JSON-schema based structured output are part of the core API, so Operaton does not have to implement them per provider.
- **Good**, because a local Ollama model in Testcontainers allows realistic integration tests without paid APIs.
- **Good**, because a Camunda 7-compatible engine has already shipped a LangChain4j-based connector through the Connect SPI, which lowers the integration risk.
- **Good**, because LangChain4j already provides the seams needed for audit (chat model listeners) and chat memory (ChatMemoryStore), so Operaton only adds the engine-specific correlation and storage.
- **Neutral**, because the library is young and releases often; Operaton must track minor releases through the existing dependency update process.
- **Bad**, because the provider modules bring transitive dependencies (HTTP clients, Jackson) that must be aligned with the Operaton BOMs and checked against the shaded Jackson used by Spin.
- **Bad**, because the full feature set of the library is much larger than what Operaton exposes; the connector API must deliberately expose only a stable subset.

### New Connect module based on Spring AI

- **Good**, because Spring AI offers a comparable provider abstraction and integrates naturally with Spring Boot configuration.
- **Bad**, because it requires Spring and in practice Spring Boot, which excludes the Quarkus, Tomcat and WildFly distributions or forces Spring onto them.
- **Bad**, because Spring AI versions are coupled to Spring Boot versions, which couples the connector to the Spring Boot upgrade cadence of the engine.

### New Connect module with Operaton-maintained provider clients

- **Good**, because there is no third-party LLM framework and full control over the dependency footprint.
- **Bad**, because Operaton would maintain six or more provider clients, including tool calling and structured output, in a fast-moving ecosystem.
- **Bad**, because this duplicates work that LangChain4j and Spring AI already do and slows down support for new providers.

### Separate AI worker based on the external task client

- **Good**, because LLM latency never holds an engine transaction, and the worker scales independently.
- **Good**, because heavy dependencies stay out of the engine classpath.
- **Neutral**, because it can reuse the same LangChain4j foundation as option 2 internally.
- **Bad**, because it requires an additional deployable component, which is a poor fit for embedded engine users.
- **Bad**, because it does not integrate with the operaton:connector element, so it is not a connector in the sense of the Connect SPI.

## More Information

- Connect SPI: [Connector](https://github.com/operaton/operaton/blob/cf6b92d/connect/core/src/main/java/org/operaton/connect/spi/Connector.java#L38), [ConnectorProvider](https://github.com/operaton/operaton/blob/cf6b92d/connect/core/src/main/java/org/operaton/connect/spi/ConnectorProvider.java#L22), [ConnectorConfigurator](https://github.com/operaton/operaton/blob/cf6b92d/connect/core/src/main/java/org/operaton/connect/spi/ConnectorConfigurator.java#L26)
- Connector execution inside the engine transaction: [ServiceTaskConnectorActivityBehavior.execute()](https://github.com/operaton/operaton/blob/cf6b92d/engine-plugins/connect-plugin/src/main/java/org/operaton/connect/plugin/impl/ServiceTaskConnectorActivityBehavior.java#L67)
- Spring Boot registration of the Connect plugin: [OperatonBpmPluginConfiguration.ConnectConfiguration](https://github.com/operaton/operaton/blob/cf6b92d/spring-boot-starter/starter/src/main/java/org/operaton/bpm/spring/boot/starter/OperatonBpmPluginConfiguration.java#L107)
- Prior art: [CIB seven AI agent connector](https://docs.cibseven.org/manual/latest/reference/connect/ai-agent-connector/), in particular its [audit trail](https://docs.cibseven.org/manual/latest/reference/connect/ai-agent-connector/audit-trail/), [chat memory](https://docs.cibseven.org/manual/latest/reference/connect/ai-agent-connector/chat-memory/), [security and data handling](https://docs.cibseven.org/manual/latest/reference/connect/ai-agent-connector/security-and-data-handling/) and [known limitations](https://docs.cibseven.org/manual/latest/reference/connect/ai-agent-connector/troubleshooting/)
- [EU AI Act, Regulation (EU) 2024/1689](https://eur-lex.europa.eu/eli/reg/2024/1689/oj)
- [LangChain4j](https://github.com/langchain4j/langchain4j)
- [Testcontainers Ollama module](https://testcontainers.com/modules/ollama/)
