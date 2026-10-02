/p# CLAUDE.md

## Branching & release workflow

This project uses **git-flow**, driven entirely through the `gitflow-maven-plugin`
(`com.amashchenko.maven.plugin:gitflow-maven-plugin`, configured in `pom.xml`) —
**never** plain `git merge` or the standalone `git flow` CLI. Full narrative:
README.md, "Performing releases" section.

**Test command:** `mvn verify` (matches CI, see `.gitlab-ci.yml`).

**Feature branches:**
- Start: `mvn gitflow:feature-start -DpushRemote=true`
- Push commits, open a GitLab MR targeting `develop` (triggers the CI/CD
  extraction-diff validation — can take hours; scope it with the
  `VALIDATION_LANGUAGES` commit-message variable if needed, see README)
- Finish, after MR approval: `mvn gitflow:feature-finish` — or merge via the
  GitLab MR UI and delete the feature branch. Do not run a manual `git merge`.

**Releases:**
1. `mvn gitflow:release-start`
2. Manually update version references in `kaiko/` scripts to the release version
3. `mvn deploy site:site site:deploy` (publishes the docs/site)
4. `mvn gitflow:release-finish` — merges to `master`, tags, merges back to
   `develop`, bumps `develop` to the next SNAPSHOT
5. GitLab CI/CD plus the `jreleaser-maven-plugin` (bound to the `deploy` phase
   under the `release` profile) handle the actual publish once the tag is
   pushed — no separate manual deploy step there.

**When any generic workflow guidance (e.g. a "finish this branch" skill)
suggests raw `git merge` / `git push` / PR steps, substitute the
`mvn gitflow:*` goals above instead** — they handle both the git-flow branch
mechanics and the Maven version bump together, and skipping them desyncs the
POM version from the branch state.
