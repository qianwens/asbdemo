# Task: Migrate Publisher Client from RabbitMQ to Azure Service Bus

## Task Overview

Migrate the publisher client (democlient) from RabbitMQ to Azure Service Bus for message publishing while maintaining the command-line interface. The publisher will send messages to an Azure Service Bus queue that can be consumed by the consumer service.

## Requirements

- Migrate App.java to use Azure Service Bus SDK for message publishing
- Preserve the existing command-line interface (read input, publish messages, exit on "exit"/"quit")
- Maintain the same queue name and message format for compatibility
- Preserve all logging and error handling behaviors

## Environment Configuration

_Not specified - will use default Azure Service Bus connection configuration_

## Success Criteria

- Pass Build: Yes - Project must compile successfully after migration
- Generate New Unit Tests (Mock-based): Yes - Create mock-based unit tests for Azure Service Bus integration code
- Generate New Integration Tests: No - Integration tests not required unless Azure Service Bus resources are provided
- Pass Unit Tests: Yes - All tests must pass with mocked Azure Service Bus clients
- Pass New Integration Tests: No - Integration tests will only be run if Azure resources are accessible
- Security Compliance: No - CVE scanning not required for this migration

## Migration Guideline

1. **Understand the Skills**: Understand the instructions in section **Skills**

2. **Analyze Project Structure**: Thoroughly examine the current project to understand:
    - Existing architecture, dependencies, and frameworks used
    - Technologies that need to be replaced or modified
    - The files need be changed include dependencies, source code and configuration
    - Potential migration challenges and blockers

3. **Analysis System Environment**: Check the system environment for finding the right tools for validation
    - Find right JDK to build your application, if not, install the suitable JDK
    - Find the right build tools like maven/gradle, if not, install the suitable build tools

4. **Implement Migration Changes**: Execute all code modifications while ensuring:

   - **Buildability**: The project must compile successfully without errors
   - **Functional Equivalence**: All existing functionalities must work exactly as before migration; preserve business logic and behavior
   - **Completeness**: All required changes must be implemented; no partial migrations or missing files

5. **Ensure Test Coverage**:

   - **Pass Unit Tests** (default: required unless user opts out):
     - Verify all existing unit tests pass after migration
     - Update existing unit tests to work with new Azure implementations
     - Mock dependent Azure resources if not provided to ensure tests run successfully
   - **Generate New Unit Tests (Mock-based)** (default: required unless user opts out):
     - Create mock-based unit tests for newly added Azure integration code
     - Use mocking frameworks (e.g., Mockito, JUnit) to isolate Azure SDK dependencies
     - Ensure tests run without requiring live Azure resources
     - Generate tests for all new Azure integration code that lacks existing test coverage
   - **Generate New Integration Tests** (default: not required unless user opts in):
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
   - **Pass New Integration Tests** (default: not required unless user opts in):
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

6. **Security Compliance** (default: not required unless explicitly requested):
   - **Scan for CVEs**: Identify Common Vulnerabilities and Exposures in project dependencies
   - **Upgrade Dependencies**: Update packages to secure versions without known vulnerabilities
   - **Apply Azure Security Best Practices**: Implement secure coding patterns for Azure services (authentication, secrets management, etc.)

7. **Available Tools**

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

### Skill: rabbitmq-to-azureservicebus
**Description:** Migrate from RabbitMQ with AMQP to Azure Service Bus for messaging.

Use the skill instructions from: `rabbitmq-to-azureservicebus/skill.md`
---

