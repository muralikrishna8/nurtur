---
name: update-knowledge-doc
description: Updates PROJECT_KNOWLEDGE.md with a consistent feature or bugfix ledger entry. Use when implementing a feature, bug fix, refactor, validation change, or UX behavior change.
disable-model-invocation: true
---

# Update Knowledge Doc

## Purpose

Keep `PROJECT_KNOWLEDGE.md` as the single source of truth after each behavior change.

## When To Use

Use this skill after any feature, bug fix, refactor, or UX/persistence/validation change.

## Workflow

1. Open `PROJECT_KNOWLEDGE.md`.
2. Add a new subsection under **Implemented Feature Requirements Ledger**.
3. Use this template:

```markdown
### <Feature or Fix Name>

- Requirement requested: <user story or issue>
- What changed:
  - <change 1>
  - <change 2>
- Constraints/validation rules:
  - <rule 1>
  - <rule 2>
- Testing impact:
  - <tests added/updated>
```

4. Update **Open Decisions / Next Features** if priorities changed.
5. If setup or usage changed, update `README.md`.

## Guardrail

If no behavior changed, add no ledger section and state in the final response:
`No knowledge doc update required: no behavior change.`
