# Issue tracker: GitHub

Issues and PRDs for this repo live as GitHub issues. Use the `gh` CLI for all operations.

> **Setup note.** This repo has no Git remote yet. A GitHub repository must be created and pushed
> (and the five triage labels created — see `triage-labels.md`) before issues can be published.
> Until then, `gh` issue operations will fail. PRDs remain documents in `docs/prd/` regardless
> (a PRD is a document, never a tracker issue).

## Conventions

- **Create an issue**: `gh issue create --title "..." --body "..."`. Use a heredoc for multi-line bodies.
- **Read an issue**: `gh issue view <number> --comments`, filtering comments by `jq` and also fetching labels.
- **List issues**: `gh issue list --state open --json number,title,body,labels,comments --jq '[.[] | {number, title, body, labels: [.labels[].name], comments: [.comments[].body]}]'` with appropriate `--label` and `--state` filters.
- **Comment on an issue**: `gh issue comment <number> --body "..."`
- **Apply / remove labels**: `gh issue edit <number> --add-label "..."` / `--remove-label "..."`
- **Close**: `gh issue close <number> --comment "..."`

Infer the repo from `git remote -v` — `gh` does this automatically when run inside a clone.

Derived implementation issues should link back to their source PRD path (e.g.
`docs/prd/v1-scheduling-intake-vertical-slice.md`) and be grouped under a milestone named for the
PRD version (e.g. `v1`).

## When a skill says "publish to the issue tracker"

Create a GitHub issue.

## When a skill says "fetch the relevant ticket"

Run `gh issue view <number> --comments`.
