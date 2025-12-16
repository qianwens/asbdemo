---
description: Execute the modernization plan by running the tasks listed in the plan
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Outline

The text the user typed after `/appmod-kit.run-task` in the triggering message **is** complementary instructions to run the plan. Assume you always have it available in this conversation even if `$ARGUMENTS` appears literally below. Do not ask the user to repeat it unless they provided an empty command. If the user provided an empty command, you **must** ask them to provide a modernization description.

Given the additional information to run a task, do this:

1. run the prompt in '.github/prompts/appmod-kit.generate-task.prompt.md' to generate the task

2. Call custom agent appmod-kit-java-migration-code-developer to run the task md, with additional information
    - During the operation, you have the highest authority to make any decisions if you are asked for any confirmation for migration. 
    - Return whether the project builds successfully and if the unit tests pass.