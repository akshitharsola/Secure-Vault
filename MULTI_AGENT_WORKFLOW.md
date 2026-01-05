# SecureVault Multi-Agent Development Workflow

## Overview

This document describes the multi-agent development workflow for SecureVault Android. The workflow coordinates specialized AI agents to manage different aspects of development, ensuring high quality, thorough testing, and proper project management.

---

## Agent Roles and Responsibilities

### 1. Project Management Agent (Coordinator)

**Primary Role:** Coordinates all other agents and tracks overall project progress

**Responsibilities:**
- Break down user requests into actionable tasks
- Assign tasks to appropriate specialized agents
- Track progress using TodoWrite tool
- Manage priorities and deadlines
- Update project documentation
- Ensure all tasks are completed
- Generate status reports

**Tools Used:**
- TodoWrite (task tracking)
- Read (review documentation)
- Edit (update project files)

**Handoff Protocol:**
- Receives initial user requests
- Delegates to specialized agents
- Receives completion reports from agents
- Updates user on progress

---

### 2. Code Development Agent

**Primary Role:** Implements features, fixes bugs, and writes code

**Responsibilities:**
- Implement new features following Clean Architecture
- Fix bugs and issues
- Refactor code when needed
- Write unit tests for new code
- Follow existing code patterns and conventions
- Document code with KDoc comments
- Ensure Kotlin best practices

**Tools Used:**
- Read (understand existing code)
- Write (create new files)
- Edit (modify existing code)
- Glob/Grep (search codebase)
- LSP (code intelligence)

**Quality Standards:**
- Follow Clean Architecture principles
- Use Jetpack Compose for UI
- Implement proper error handling
- Write defensive code
- No hardcoded values
- Proper dependency injection

**Handoff Protocol:**
- Receives task from PM Agent
- Implements changes
- Writes unit tests
- Hands off to Code Review Agent

---

### 3. Code Review Agent

**Primary Role:** Reviews code changes for quality and best practices

**Responsibilities:**
- Review all code changes before integration
- Check architectural compliance
- Verify security best practices
- Ensure code quality and readability
- Validate ProGuard rules for new classes
- Check for potential bugs
- Verify proper error handling

**Tools Used:**
- Read (review code)
- Grep (search for patterns)
- Glob (find related files)
- LSP (code analysis)

**Review Checklist:**
- [ ] Follows Clean Architecture
- [ ] Proper separation of concerns
- [ ] No security vulnerabilities
- [ ] Error handling implemented
- [ ] Code is readable and maintainable
- [ ] ProGuard rules added for new classes
- [ ] No hardcoded secrets or sensitive data
- [ ] Kotlin best practices followed
- [ ] Documentation present where needed

**Handoff Protocol:**
- Receives code from Dev Agent
- Reviews changes
- If issues found: Returns to Dev Agent with feedback
- If approved: Hands off to Testing Agent

---

### 4. Testing & QA Agent

**Primary Role:** Ensures code quality through comprehensive testing

**Responsibilities:**
- Write unit tests for new features
- Write instrumented tests for Android-specific code
- Run test suites
- Verify APK functionality
- Performance testing
- Regression testing
- Report bugs and failures

**Tools Used:**
- Bash (run gradlew test commands)
- Read (understand test requirements)
- Write (create test files)
- Edit (update tests)

**Testing Strategy:**
- **Unit Tests:** Test business logic in isolation
- **Instrumented Tests:** Test Android components with device context
- **Integration Tests:** Test feature workflows end-to-end
- **Manual Testing:** Verify UI/UX on real devices

**Test Coverage Goals:**
- Core security functions: 100%
- Business logic (UseCases): 90%+
- ViewModels: 80%+
- UI Components: Manual testing

**Handoff Protocol:**
- Receives approved code from Review Agent
- Writes/updates tests
- Runs all tests
- If tests fail: Returns to Dev Agent
- If tests pass: Hands off to CI/CD Agent

---

### 5. CI/CD & Release Agent

**Primary Role:** Manages automated builds, releases, and deployment

**Responsibilities:**
- Manage GitHub Actions workflows
- Configure GitHub Secrets
- Create git commits with proper messages
- Create version tags
- Trigger releases
- Monitor build status
- Handle signing configuration
- Manage ProGuard mappings

**Tools Used:**
- Bash (git, gh CLI commands)
- Read (check configurations)
- Edit (update workflows)

**Release Process:**
1. Receive approval from Testing Agent
2. Create git commit with descriptive message
3. Push to GitHub
4. Bump version number (if releasing)
5. Create and push version tag (e.g., v1.2.5)
6. Monitor GitHub Actions build
7. Verify release created successfully
8. Download and verify APKs
9. Report completion to PM Agent

**Commit Message Format:**
```
<type>: <subject>

<body>

🤖 Generated with Claude Code

Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>
```

Types: feat, fix, docs, test, refactor, ci, chore

**Handoff Protocol:**
- Receives green light from Testing Agent
- Commits and releases code
- Hands off to PM Agent for documentation

---

## Workflow Sequences

### Feature Implementation Workflow

```
User Request
    ↓
PM Agent (creates tasks)
    ↓
Dev Agent (implements feature)
    ↓
Code Review Agent (reviews code)
    ↓
    ├─→ Issues Found → Dev Agent (fix issues) ─┐
    │                                          │
    ↓                                          │
Testing Agent (writes & runs tests)            │
    ↓                                          │
    ├─→ Tests Failed → Dev Agent (fix bugs) ───┘
    │
    ↓
CI/CD Agent (commit & release)
    ↓
PM Agent (update docs, close task)
    ↓
User (receives completed feature)
```

### Bug Fix Workflow

```
Bug Report
    ↓
PM Agent (triages bug, creates task)
    ↓
Dev Agent (investigates & fixes)
    ↓
Testing Agent (verifies fix)
    ↓
    ├─→ Not Fixed → Dev Agent ─┐
    │                          │
    ↓                          │
Code Review Agent (quick review) │
    ↓                          │
CI/CD Agent (hotfix release)     │
    ↓                          │
PM Agent (update changelog) ─────┘
```

### Release Workflow

```
Release Decision
    ↓
PM Agent (plans release)
    ↓
Code Review Agent (final audit)
    ↓
Testing Agent (full test suite)
    ↓
CI/CD Agent (version bump)
    ↓
CI/CD Agent (create tag)
    ↓
GitHub Actions (automated build)
    ↓
Testing Agent (verify APKs)
    ↓
PM Agent (create release notes)
    ↓
CI/CD Agent (publish release)
```

---

## Parallelization Strategy

### Tasks That CAN Run in Parallel

1. **Independent Features**
   - Different features not touching same files
   - Example: Dev Agent A works on password generator while Dev Agent B works on settings

2. **Different Module Tests**
   - Unit tests for different modules
   - Example: Testing Agent runs ViewModel tests while another runs DAO tests

3. **Documentation Updates**
   - Different documentation files
   - Example: Update README while updating CHANGELOG

4. **Code Review of Independent Changes**
   - Review multiple PRs that don't conflict
   - Example: Review UI changes separate from backend changes

### Tasks That MUST Run Sequentially

1. **Development Pipeline**
   - Dev → Review → Test → Release (always sequential)

2. **Version Management**
   - Version bump → Build → Release → Tag

3. **Database Migrations**
   - Schema change → Migration code → Testing → Deployment

4. **Signing Key Changes**
   - Key generation → Configuration → Build → Verification

5. **ProGuard Rule Updates**
   - Rule changes → Build verification → Test with minification

---

## Communication Protocol

### Handoff Document Template

```yaml
---
FROM: [Agent Name]
TO: [Agent Name]
TASK: [Task ID/Description]
STATUS: [READY|BLOCKED|IN_PROGRESS|NEEDS_REVIEW]
PRIORITY: [HIGH|MEDIUM|LOW]
---

## Context
[Brief description of work done and current state]

## Deliverables
- [ ] File 1: path/to/file1.kt
- [ ] File 2: path/to/file2.kt
- [ ] Tests: path/to/test.kt

## Changes Made
1. Added feature X in ComponentY
2. Updated ViewModel to handle new state
3. Added unit tests with 95% coverage

## Testing Instructions
1. Run unit tests: ./gradlew test
2. Manual test: Navigate to screen X, tap button Y
3. Expected: Feature works as described

## Notes
- Uses new dependency: androidx.compose.material3:1.2.0
- Requires API 24+ (already our min SDK)
- ProGuard rules added for new classes

## Blockers
[None | Description of any blocking issues]

## Next Steps
[What the receiving agent should do next]
```

---

## Best Practices

### For All Agents

1. **Always use TodoWrite** to track progress
2. **Read before writing** - understand existing code
3. **Follow established patterns** in the codebase
4. **Document complex logic** with comments
5. **Ask for clarification** when requirements unclear
6. **Test your changes** before handing off
7. **Update documentation** when behavior changes

### For Code Development Agent

1. **Small, focused commits** - one feature per commit
2. **Write tests first** (TDD when possible)
3. **Use meaningful variable names**
4. **Avoid premature optimization**
5. **Handle all error cases**
6. **Follow Kotlin conventions**
7. **Use dependency injection**

### For Testing Agent

1. **Test happy path AND edge cases**
2. **Use descriptive test names**
3. **Arrange-Act-Assert pattern**
4. **Mock external dependencies**
5. **Test one thing per test**
6. **Clean up test data**

### For CI/CD Agent

1. **Never force push** to main/master
2. **Always verify builds** before releasing
3. **Use semantic versioning**
4. **Preserve ProGuard mappings**
5. **Verify APK signatures**
6. **Test on real devices** when possible

---

## Example: Password Generator Feature

### Iteration 1: Planning
**PM Agent:**
- Creates task: "Implement password generator feature"
- Breaks down into subtasks:
  1. Create PasswordGenerator utility class
  2. Add generator screen UI
  3. Integrate with form screen
  4. Add unit tests
  5. Add instrumented tests

### Iteration 2: Development
**Dev Agent:**
- Creates `PasswordGenerator.kt` with configurable options
- Creates `GeneratorScreen.kt` with Compose UI
- Adds navigation from FormScreen
- Writes unit tests for PasswordGenerator logic
- Hands off to Code Review Agent

### Iteration 3: Review
**Code Review Agent:**
- Reviews code structure ✅
- Checks security (SecureRandom usage) ✅
- Finds issue: No input validation for length
- Returns to Dev Agent with feedback

### Iteration 4: Fix and Re-review
**Dev Agent:**
- Adds input validation (8-32 characters)
- Adds error handling
- Updates tests
- Hands off to Code Review Agent

**Code Review Agent:**
- Re-reviews changes ✅
- Approves
- Hands off to Testing Agent

### Iteration 5: Testing
**Testing Agent:**
- Runs unit tests: ✅ All pass
- Writes instrumented test for UI
- Runs instrumented tests: ✅ All pass
- Manual testing on device: ✅ Works correctly
- Hands off to CI/CD Agent

### Iteration 6: Release
**CI/CD Agent:**
- Commits changes:
  ```
  feat: Add password generator with customizable options

  - Created PasswordGenerator utility with length/complexity options
  - Added generator screen with Material 3 UI
  - Integrated into password form
  - Added comprehensive test coverage

  🤖 Generated with Claude Code
  ```
- Bumps version to 1.2.6
- Creates tag v1.2.6
- Monitors GitHub Actions build: ✅ Success
- Hands off to PM Agent

### Iteration 7: Documentation
**PM Agent:**
- Updates README with new feature
- Updates CHANGELOG.md
- Closes task
- Notifies user: "Password generator feature complete and released in v1.2.6"

---

## Monitoring and Metrics

### Key Performance Indicators

1. **Time to Market**
   - Feature request → Production release
   - Target: < 1 day for small features

2. **Code Quality**
   - Test coverage: Target 80%+
   - Lint warnings: Target 0
   - ProGuard warnings: Target 0

3. **Build Success Rate**
   - GitHub Actions builds
   - Target: 95%+ success rate

4. **Release Frequency**
   - Patch releases: As needed (hotfixes)
   - Minor releases: Monthly
   - Major releases: Quarterly

### Health Checks

**Daily:**
- Run unit tests locally
- Check GitHub Actions status
- Monitor GitHub Issues

**Weekly:**
- Review test coverage
- Check dependency updates
- Review ProGuard mappings

**Monthly:**
- Security audit
- Performance profiling
- User feedback review

---

## Emergency Procedures

### Critical Bug in Production

1. **PM Agent:** Triages bug, assesses severity
2. **Dev Agent:** Creates hotfix branch, fixes bug
3. **Testing Agent:** Verifies fix (expedited testing)
4. **CI/CD Agent:** Creates hotfix release (v1.2.5.1)
5. **PM Agent:** Notifies users, updates documentation

### Failed Release Build

1. **CI/CD Agent:** Identifies failure, notifies PM Agent
2. **PM Agent:** Reviews build logs, assigns to Dev Agent
3. **Dev Agent:** Fixes issue
4. **Testing Agent:** Runs tests locally
5. **CI/CD Agent:** Retries build

### Signing Key Compromised

1. **PM Agent:** Emergency protocol activated
2. **CI/CD Agent:** Generates new keystore
3. **Dev Agent:** Updates package name to com.securevault.v2
4. **PM Agent:** Creates migration guide for users
5. **CI/CD Agent:** Releases as new app

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 1.0.0 | 2026-01-05 | Initial multi-agent workflow documentation |

---

## Contributing

When adding new agent types or modifying workflows:

1. Update this document
2. Test the workflow with a real feature
3. Document any issues encountered
4. Update the workflow diagram
5. Get approval from PM Agent

---

## References

- [Clean Architecture](https://blog.cleancoder.com/uncle-bob/2012/08/13/the-clean-architecture.html)
- [Kotlin Style Guide](https://kotlinlang.org/docs/coding-conventions.html)
- [Android Best Practices](https://developer.android.com/jetpack/guide)
- [Jetpack Compose Guidelines](https://developer.android.com/jetpack/compose/guidelines)
- [Git Commit Messages](https://chris.beams.io/posts/git-commit/)

---

*This workflow was used to develop SecureVault v1.2.5 and will guide all future development.*

**Maintained by:** Project Management Agent
**Last Updated:** January 5, 2026
