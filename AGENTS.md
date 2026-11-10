# AGENTS.md

Behavioral guidelines to reduce common LLM coding mistakes. Merge with project-specific instructions as needed.

**Tradeoff:** These guidelines bias toward caution over speed. For trivial tasks, use judgment.

## Skills

- Before implementing, identify relevant skills in `.agents/skills/`.
- Read the `SKILL.md` files for applicable skills and follow their instructions.
- Use only skills compatible with the current stack and task.

## 1. Think Before Coding

**Don't assume. Don't hide confusion. Surface tradeoffs.**

Before implementing:
- State your assumptions explicitly. If uncertain, ask.
- If multiple interpretations exist, present them - don't pick silently.
- If a simpler approach exists, say so. Push back when warranted.
- If something is unclear, stop. Name what's confusing. Ask.

## 2. Simplicity First

**Minimum code that solves the problem. Nothing speculative.**

- No features beyond what was asked.
- No abstractions for single-use code.
- No "flexibility" or "configurability" that wasn't requested.
- No error handling for impossible scenarios.
- If you write 200 lines and it could be 50, rewrite it.

Ask yourself: "Would a senior engineer say this is overcomplicated?" If yes, simplify.

## 3. Surgical Changes

**Touch only what you must. Clean up only your own mess.**

When editing existing code:
- Don't "improve" adjacent code, comments, or formatting.
- Don't refactor things that aren't broken.
- Match existing style, even if you'd do it differently.
- If you notice unrelated dead code, mention it - don't delete it.

When your changes create orphans:
- Remove imports/variables/functions that YOUR changes made unused.
- Don't remove pre-existing dead code unless asked.

The test: Every changed line should trace directly to the user's request.

## 4. Goal-Driven Execution

**Define success criteria. Loop until verified.**

Transform tasks into verifiable goals:
- "Add validation" → "Write tests for invalid inputs, then make them pass"
- "Fix the bug" → "Write a test that reproduces it, then make it pass"
- "Refactor X" → "Ensure tests pass before and after"

For multi-step tasks, state a brief plan:
```
1. [Step] → verify: [check]
2. [Step] → verify: [check]
3. [Step] → verify: [check]
```

Strong success criteria let you loop independently. Weak criteria ("make it work") require constant clarification.

---

**These guidelines are working if:** fewer unnecessary changes in diffs, fewer rewrites due to overcomplication, and clarifying questions come before implementation rather than after mistakes.

## Project Instructions

### Language

- Respond and write project documentation in Brazilian Portuguese.
- Keep explanations concise, with clear steps for practical tasks.
- Preserve the names of commands, APIs, files, and technical identifiers.
- Follow existing conventions for naming and code comments.
- Keep third-party skills in their original language.



### Workflow

<<<<<<< HEAD
- Work on the current `US<N>` branch, where `N` is the user story number associated with the stage file.
- Preserve existing changes made by the user.
- Before editing, check whether the file already exists and whether relevant local conventions apply.
- Prefer small, straightforward changes consistent with the project's conventions.
- At the start of every task, check the repository's `docs/` directory.
- Read `docs/README.md`, when available, to identify relevant documentation.
- Before planning or editing files, read the documents relevant to the task,
  including applicable requirements, business rules, and architectural decisions.
- Follow links to additional documents when needed to understand the task.
- Treat proposals and open questions as unconfirmed; do not assume they are
  approved requirements.
- If documentation conflicts with the implementation or the user's request,
  report the discrepancy and clarify it when necessary.
- Update affected documentation when an approved change alters the documented
  behavior.
=======
## Credenciais e Segredos

- Todas as chaves de API e credenciais externas devem ficar armazenadas localmente no diretorio `.secrets/` na raiz do repositorio (ex.: `.secrets/firebase.json` para a conta de servico do Firebase e `.secrets/foursquare.txt` contendo a Service API Key pura do Foursquare).
- Nunca versione arquivos de `.secrets/` nem inclua chaves reais hardcoded no codigo ou em arquivos de configuracao versionados.
- Testes automatizados devem funcionar sem depender da presenca de arquivos reais em `.secrets/` (usando mocks/stubs).

## Verificacao
>>>>>>> e960d0254b6d3b7003cbd621815798f9a595c71e

### File Editing

- Use clear English names for new files and directories.
- Avoid refactoring outside the requested scope.
- Do not remove code, files, or configuration without a clear need.
- Add comments only when they help explain a non-obvious decision.

### Verification

- When changing code, run the available tests, lint checks, or validation commands relevant to the changes.
- If automated validation is not possible, explain what was checked manually.
- Report any limitations encountered during execution.

### Delivery

- Summarize the changes made.
- Reference modified files using clear paths.
- Suggest next steps only when they are useful.