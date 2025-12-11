---
description: Execute the modernization plan by running the tasks listed in the plan
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Outline

The text the user typed after `/appmod-kit.run-plan` in the triggering message **is** complementary instructions to run the plan. Assume you always have it available in this conversation even if `$ARGUMENTS` appears literally below. Do not ask the user to repeat it unless they provided an empty command. If the user provided an empty command, you **must** ask them to provide a modernization description.

Given the additional information to run a plan, do this:

1. **Extract GitHub issue URI**:
    - Extract the GitHub issue URI from the arguments if you can
    - Validate the URI format if it exists (you can assume that syntax validity is enough)

2. Generate task details md to execute the task
    1) Find the right skill md files described in `.appmod-kit/skills/skill-index.json`, if the index file not found, call script .appmod-kit/scripts/powershell/skill-index.ps1 to build the index file
    2) Pickup the right skill according to the index file, you need to think about below rules to pickup the right skills, **DO NOT** read the skill files in the this phase:
       - Source resource
       - Frameworks and libraries to use
       - Target resource
    3) Using the template file ".appmod-kit/templates/task-template.md", generate a task file and save the task file in the .github/modernization folder with the filename task-%sequence-from-0%.md
    4) Run `.appmod-kit/scripts/powershell/copy-skills.ps1` with args -SkillFileNames @("[skill-name-1-path]", "[skill-name-2-path]") -TaskFilePath [task-file-path] to copy reference skills from skill-index.json to the task md file

4. Call custom agent appmod-kit-java-migration-code-developer to run the task md, with additional information
    - During the operation, you have the highest authority to make any decisions if you are asked for any confirmation for migration. 
    - Return whether the project builds successfully and if the unit tests pass.