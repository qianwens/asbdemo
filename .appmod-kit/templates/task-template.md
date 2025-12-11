# Task: [Task Title]

## Task Overview

[Brief description of what needs to be accomplished in this task - typically 1-2 sentences describing the migration/configuration goal]

## Requirements

The original requirements from user input, leave it to empty if not specified

- [Migration Requirements: Specific features, APIs, or patterns that must be migrated (e.g., "focus on message queue migration", "prioritize authentication changes")]
- [Constraints: Limitations or restrictions to be aware of (e.g., "maintain backward compatibility with version X", "avoid changes to module Y", "minimal refactoring")]
- [Target Azure Services: Specific Azure services to target or prefer (e.g., "use Azure Service Bus Premium tier", "deploy to Azure Container Apps", "use Azure Key Vault for secrets")]
- [Code Scope: Which parts of the codebase to focus on or exclude (e.g., "only migrate the API layer", "exclude legacy modules")]
- [Technical Preferences: Specific approaches or technologies to use (e.g., "use Managed Identity for authentication", "prefer async patterns", "implement circuit breaker pattern")]

## Environment Configration

[Environment Configration from user input]

## Success Criteria

Success Crtiera according to user input, if no input, user the default criteria

- [Pass Build: Yes (default) - Project must compile successfully after migration]
- [Generate New Unit Tests (Mock-based): Yes (default) - Create mock-based unit tests for newly added Azure integration code to ensure test coverage]
- [Generate New Integration Tests: No (default) - Create integration tests for Azure service interactions when requested:
  - If Azure resources are provided/accessible: Create tests that interact with actual Azure services
  - If no Azure resources are provided: Create integration tests with mocked Azure SDK clients using test containers or in-memory implementations where possible]
- [Pass Unit Tests: Yes (default) - All tests must pass; mock dependent Azure resources if not provided]
- [Pass New Integration Tests: No (default) - Integration tests must pass when generated:
  - With Azure resources: Tests validate actual Azure service connectivity and operations
  - Without Azure resources: Tests validate integration logic using mocked Azure clients, ensuring the integration layer works correctly]
- [Security Compliance: No (default) - No known CVEs exist in project dependencies]

## Migration Guideline

DO NOT change below part in this section when generate skill.md (NOT included this statement in task.md)

1. **Understand the Skills**: Understand the the instructions in section **Skills**

2. **Implement Migration Changes**: Execute all code modifications while ensuring:

   - **Buildability**: The project must compile successfully without errors
   - **Functional Equivalence**: All existing functionalities must work exactly as before migration; preserve business logic and behavior
   - **Completeness**: All required changes must be implemented; no partial migrations or missing files

3. **Ensure Test Coverage**:

   - **Pass Unit Tests** (default: required unless user opts out):
     - Verify all existing unit tests pass after migration
     - Update existing unit tests to work with new Azure implementations
     - Mock dependent Azure resources if not provided to ensure tests run successfully
   - **Generate New Unit Tests (Mock-based)** (default: required unless user opts out):
     - Create mock-based unit tests for newly added Azure integration code
     - Use mocking frameworks (e.g., Mockito, JUnit) to isolate Azure SDK dependencies
     - Ensure tests run without requiring live Azure resources
     - Generate tests for all new Azure integration code that lacks existing test coverage
   - **Generate New Integration Tests** (default: required unless user opts out):
     - **When Azure resources ARE provided/accessible**:
       - Create integration tests that interact with actual Azure services (Service Bus, Blob Storage, Cosmos DB, etc.)
       - Configure tests to use provided connection strings, credentials, or managed identities
       - Test end-to-end scenarios including authentication, data operations, and error handling
       - Document Azure resource requirements and configuration needed to run tests
     - **When Azure resources are NOT provided/accessible**:
       - Create integration tests using mocked Azure SDK clients and service responses
       - Use test containers (e.g., Testcontainers, Azurite for Storage) or in-memory implementations where applicable
       - Mock Azure client behaviors to simulate realistic service interactions and responses
       - Validate integration layer logic, error handling, and retry mechanisms without live Azure dependencies
       - Ensure tests can run in any environment without requiring Azure subscriptions or credentials
   - **Pass New Integration Tests** (default: required unless user opts out):
     - **With Azure resources**: Verify all integration tests pass against actual Azure services
     - **Without Azure resources**: Verify all integration tests pass using mocked clients and test implementations
     - In both cases, tests must validate that the integration layer functions correctly
   - **Best Practices**:
     - For unit tests: Mock external Azure dependencies at the service boundary to ensure isolated, fast tests
     - For integration tests without resources: Use realistic mock responses that mirror actual Azure service behavior
     - Leverage testing frameworks like JUnit 5, Mockito, Testcontainers, and Azure SDK test utilities
     - Structure tests to clearly separate unit tests (fast, isolated) from integration tests (broader scope)
     - Use test profiles or tags to distinguish between tests requiring live resources vs. mock-based tests
     - Ensure all tests are repeatable, deterministic, and don't depend on external state

4. **Security Compliance** (default: not required unless explicitly requested):
   - **Scan for CVEs**: Identify Common Vulnerabilities and Exposures in project dependencies
   - **Upgrade Dependencies**: Update packages to secure versions without known vulnerabilities
   - **Apply Azure Security Best Practices**: Implement secure coding patterns for Azure services (authentication, secrets management, etc.)

5. **Available Tools**

   Use these specialized **MCP tools** to assist with migration tasks:

   **Migration Validation:**
   - **appmod-completeness-validation**: Validate migration completeness by discovering old technology references that should have been migrated but remain unchanged; identifies missing removals and incomplete transformations
   - **appmod-consistency-validation**: Validate functional equivalence by analyzing git diffs for behavior changes; identifies critical and major issues that affect business logic

   **Java Environment:**
   - **list_jdks**: List all JDK versions available in the environment
   - **list_mavens**: List all Maven versions available in the environment

   **Build & Test:**
   - **build_java_project**: Build the project using Maven or Gradle to verify successful compilation
   - **run_tests_for_java**: Execute unit tests and integration tests to ensure functionality after migration

   **Security:**
   - **validate_cves_for_java**: Scan Java dependencies for known security vulnerabilities (CVEs) and provide remediation recommendations

## Skills

[To Be fullfilled by scripts]