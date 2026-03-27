---
name: full-project-explorer
description: 'Deep project exploration before executing any task. Use when: implementing a feature, fixing a bug, refactoring, adding tests, or doing ANY task that requires full understanding of the codebase. Reads structure, READMEs, configs, entry points, source files, and dependencies before acting. Ensures no assumption is made without evidence from the actual code.'
argument-hint: 'Describe the task or goal to accomplish after full exploration'
---

# Full Project Explorer

## When to Use

- Implementing a new feature that touches multiple modules
- Fixing a bug without knowing its location
- Refactoring or restructuring code
- Adding tests to existing code
- Any task where partial knowledge could lead to wrong changes
- When a previous attempt failed due to missing context

## Procedure

### Phase 1 — Map the Project

1. **Read the root**: List the workspace root directory, read `readme.md`, `README.md`, `ToDo.md`, or any top-level documentation found.
2. **Identify modules**: List every top-level folder and note its purpose (backend, frontend, tests, config, etc.).
3. **Find entry points**: Look for `main.*`, `app.*`, `index.*`, `Launcher.*`, `__init__.py`, `build.gradle`, `pom.xml`, `package.json`, `requirements.txt`, `Dockerfile`, `docker-compose.yml`.
4. **Read configuration files**: Parse all dependency manifests (`requirements.txt`, `build.gradle`, `gradle.properties`, `settings.gradle`) to understand the tech stack, versions, and libraries in use.

### Phase 2 — Understand Each Module

For each module identified in Phase 1:

1. **Read the module README** if it exists.
2. **List subdirectories** and scan the file tree.
3. **Read source entry points** (the files most likely to be called first at runtime).
4. **Read shared/common utilities** (files named `common.*`, `utils.*`, `helpers.*`, `base.*`).
5. **Read interfaces and contracts** (API definitions, abstract classes, interfaces, type definitions).
6. **Scan test files** to understand expected behavior without reading every test in detail — focus on test names and structure.

### Phase 3 — Build a Mental Model

Before touching any file, write a short internal summary (do NOT output it unless asked):

- **Architecture**: How do modules communicate? (REST API, direct import, inter-process, etc.)
- **Data flow**: Where does data enter the system, how is it transformed, and where does it exit?
- **Key abstractions**: What are the main classes, functions, or services?
- **Constraints**: Any known limitations, TODOs, or fragile areas found in comments?

### Phase 4 — Scope the Task

1. Identify **which files need to change** to accomplish the goal.
2. Identify **which files must NOT change** (test contracts, public APIs, generated files).
3. Check for **existing patterns** in the codebase — naming conventions, code style, error handling approach — and match them exactly.
4. If the task is ambiguous, surface specific questions with evidence from the code before proceeding.

### Phase 5 — Execute

1. Make changes in **dependency order**: shared utilities before consumers, models before services, services before controllers.
2. After each file change, **verify no regressions** by re-reading dependent files.
3. If tests exist, **run them** after changes and report results.
4. If the task introduced new behavior, **check whether documentation** (`README`, `ToDo.md`, inline comments) needs updating.

## Quality Checklist (before declaring done)

- [ ] All files relevant to the task have been read — no assumptions made
- [ ] Changes match the existing code style and naming conventions
- [ ] No unrelated files were modified
- [ ] Tests pass (or test failures are explicitly reported with cause)
- [ ] The original goal stated in the task is fully met

## This Project's Known Structure

| Folder | Role |
|--------|------|
| `RA/` | Research activity notes and reports |
| `RA/labyrinth_gen/` | Python backend — maze generation (DFS, Kruskal, Prim) + Flask web service |
| `RA/labyrinth_gen/tests/` | Python tests for the backend |
| `RA/labyrinth_ui/` | Java LibGDX frontend — renders and plays the maze |
| `RA/labyrinth_ui/core/src/` | Core game logic: screens, player, net client, maze rendering |
| `RA/labyrinth_ui/lwjgl3/src/` | Desktop launcher |

## Anti-patterns to Avoid

- **Skipping Phase 1–3** and jumping straight to editing — leads to broken imports, mismatched APIs, and style violations.
- **Reading only the file that seems relevant** — side effects and dependencies are usually in adjacent files.
- **Guessing method signatures** — always read the actual source before calling or extending.
- **Ignoring test files** — they document intended behavior and catch regressions.
