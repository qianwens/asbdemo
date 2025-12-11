# Modernization Plan Template

Use this template to generate modernization plans for applications. Replace placeholders with actual values and customize content based on the specific modernization scenario.

**Planning Philosophy**: 
- **Focus on GOALS, not implementation**: Describe WHAT needs to be achieved from the user's perspective
- **Skills define HOW**: All implementation details (frameworks, SDKs, libraries, code patterns, authentication methods) will be determined by the referenced skills
- **No assumptions**: Do not assume or specify any technical implementation approach in the plan - let skills handle all "how" decisions

---

# Modernization Plan: [Modernization Title]

**Project**: [Application Name]  
**Date**: [Date]  
**Branch**: [Git branch name]

---

## Technical Framework

**Purpose**: Document the application's current technology stack to provide context for the migration.

**Template**:
```markdown
- **Language**: [Programming language and version, e.g., Java 11, Python 3.9, .NET 6]
- **Framework**: [Application framework and version, e.g., Spring Boot 2.7.18, Django 4.2, ASP.NET Core 6.0]
- **Build Tool**: [Build system, e.g., Maven 3.9, Gradle 8.0, npm]
- **Database**: [Current database, e.g., Oracle 19c, PostgreSQL 14, SQL Server 2019]
- **Key Dependencies**: [Major libraries/frameworks, e.g., Spring Data JPA, Hibernate, Entity Framework]
```

---

## Overview

**Purpose**: Describe the high-level modernization goals without technical details. Focus on business objectives and what will change.

**Template**:
> This migration [describe what is being migrated]. The application currently [describe current state]. The new architecture will:
> 
> - [First key change and its business benefit]
> - [Second key change and its business benefit]
> - [Third key change and its business benefit]
> 
> The migration follows [describe phased approach without technical specifics].

---

## Services

**Purpose**: List Azure services and resources required for the migration with their resource identifiers.

**Rule**: Only include this section if the modernization requires integration testing or deployment with Azure resources. Skip this section for code-only migrations or local-only verification.

**Template**:
```markdown
### [Service Name 1]
- **Resource ID**: [Azure resource ID or indicate "to be provisioned"]
- **Server Name**: [FQDN if applicable]
- **Database/Container/etc**: [specific resource details]
- **Authentication**: [auth method]
- **Purpose**: [what this service will be used for]

### [Service Name 2]
- **Resource ID**: [Azure resource ID or indicate "to be provisioned"]
- **Purpose**: [what this service will be used for]
```

---

## Architecture Design

**Purpose**: Draft a Mermaid diagram showing the new architecture with impacted components only. The diagram should illustrate both high-level application components (e.g., Web Module, Worker Module, API Layer) and the underlying Azure services they depend on (e.g., Azure Service Bus, Azure Storage, Azure SQL Database).

**Design Guidelines**:
- Show high-level application components in separate subgraphs
- Include the underlying Azure services that each component interacts with
- Use arrows to show data flow and dependencies between components and services
- Clearly distinguish between new, modified, and existing components

**Template**:
```mermaid
graph TB
    subgraph "[High-Level Component Name] - [Modified/New/Existing]"
        [Application-level components]
    end
    
    subgraph "[Azure Services] - [New/Existing]"
        [Underlying Azure services]
    end
    
    subgraph "[Existing Components]"
        [Unchanged application components and services]
    end
    
    [Define relationships and data flow between components and services]
    
    style [new-components] fill:#90EE90,stroke:#333,stroke-width:3px
    style [modified-components] fill:#FFD700,stroke:#333,stroke-width:3px
    style [azure-services] fill:#4da6ff,stroke:#333,stroke-width:2px
```

**Legend Template**:
- 🟢 **Green**: New components to be created
- 🟡 **Yellow**: Existing components to be modified
- 🔵 **Blue**: Azure services
- **Gray**: Unchanged components

---

## Code

**Purpose**: Break down coding work into discrete migration tasks. Each task represents a user-requested migration from one service/component to another, or a specific business logic modernization.

**Breakdown Rules**:
- Create tasks ONLY based on what the user explicitly requested - do not infer or add implicit tasks
- Group related changes that serve a single user goal into one task (e.g., all changes needed to migrate to PostgreSQL)
- Each task should be independently testable with integration tests
- Do not add tests for unimpacted code or existing functionality unless user requested
- Look into the skill definitions in `.appmod-kit/skills/skill-index.json` to find relevant skills that can be used by this project
- **IMPORTANT**: Do NOT read individual skill files at this stage; Do Not include the skill detail in the tasks.

**Template**:
```markdown
### Task: [Task Name]

**Description**: [Brief description of what this task achieves from the user's perspective - focus on business/functional goals only]

**Requirements**:  Just summary the orignal migraton requirement from user input, if not provided, just leave it empty
- [List WHAT needs to be accomplished, not HOW to accomplish it]
- [Focus on outcomes and goals: "Enable message publishing to Azure Service Bus" not "Use Azure SDK to create ServiceBusClient"]
- [Specify functional constraints: "Maintain message ordering" or "Support existing message schema"]
- [Do NOT specify: frameworks, libraries, SDKs, class names, authentication methods, or code patterns - skills will define these]

**Environment Configration**: Get the applicaton deployment configuration from user input, if no input from user, left this section empty
- Configration about the environment the application will migrate to

**Task Execution**: .github/modernization/[plan-folder-name]/[task-folder-name]/task.md
- **Note (DO NOT INCLUDE IN plan.md)**: This task.md file will be generated during plan execution and will contain all implementation details (frameworks, SDKs, code patterns, etc.)
- **Path construction (DO NOT INCLUDE IN plan.md)**:
  - `[plan-folder-name]`: The folder name created for this modernization plan
  - `[task-folder-name]`: A folder name derived from this task's name (lowercase, hyphenated)
  - Example: `.github/modernization/migrate-rabbitmq-servicebus/task-01-message-producer/task.md`

**Referenced Skills**: [List skills from skill-index.json that are relevant, e.g., "rabbitmq-to-servicebus-mi"]

**Success Criteria**: Success Crtiera according to user input, if no input, user the default criteria
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


```

---

## Containerization

**Purpose**: Describe how to build the Docker container for deployment.

**Rule**: Only include this section if the target deployment requires containerization (e.g., AKS, ACA) or if the user explicitly requested containerization. Skip this section for non-containerized deployments (e.g., App Service with code deployment). Only create Dockerfile for the application. Do not include Docker Compose for testing unless user requested.

**Template**:
```markdown
**Purpose**: [One sentence describing containerization goal]

**Dockerfile Location**: [path to Dockerfile, indicate if existing or to be created]
```

---

## Deployment

**Purpose**: Describe the target Azure service for deployment and the tooling to be used.

**Rule**: Only include this section if the user explicitly requested deployment.

**Template**:
```markdown
**Target Azure Service**: [Azure service name, e.g., Azure Container Apps, AKS, App Service]

**Resource Status**: [Specify if using existing resource or will create new service]

**Deployment Tool**: [az cli / bicep / terraform - default: bicep]

**Evaluation**: Run existing integration tests against the deployed endpoint to verify the deployment works correctly.
```

---

## Clarifications

**Purpose**: Document items that were not explicitly requested by the user but may be necessary or beneficial. Ask the user to confirm or provide input.

**Rule**: Only include this section if there are implicit dependencies, nice-to-have features, or ambiguities in the user's request.

**Template**:
```markdown
The following items were not explicitly requested but may be needed for a complete implementation:

1. **[Item Name]**: [Description of what needs clarification]
   - **Why needed**: [Explain the dependency or benefit]
   - **Options**: [List possible approaches or choices]
   - **Recommendation**: [Suggest a default if user doesn't respond]

```