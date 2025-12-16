---
description: Execute the modernization plan by running the tasks listed in the plan
---

## User Input

```text
$ARGUMENTS
```

You **MUST** consider the user input before proceeding (if not empty).

## Outline

The text the user typed after `/appmod-kit.generate-task` in the triggering message **is** complementary instructions to run the plan. Assume you always have it available in this conversation even if `$ARGUMENTS` appears literally below. Do not ask the user to repeat it unless they provided an empty command. If the user provided an empty command, you **must** ask them to provide a modernization description.

Given the additional information to generate a task, do this:

1. Call script `.appmod-kit/scripts/powershell/skill-index.ps1` to build the skill index file `.appmod-kit/skills/skill-index.json`.
2. Find the right skill md files described in `.appmod-kit/skills/skill-index.json`.
3. Pickup the right skill according to the index file, you need to think about below rules to pickup the right skills, **DO NOT** read the skill files in the this phase:
    - Source resource
    - Frameworks and libraries to use
    - Target resource
4. Using the template file ".appmod-kit/templates/task-template.md", generate a task file and save the task file in the .github/modernization folder with the filename task-%sequence-from-0%.md
5. Run `.appmod-kit/scripts/powershell/copy-skills.ps1` with args -SkillFileNames @("[skill-name-1-path]", "[skill-name-2-path]") -TaskFilePath [task-file-path] to copy reference skills from skill-index.json to the task md file