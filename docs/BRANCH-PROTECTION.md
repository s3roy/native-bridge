# Lock `main` on GitHub (branch protection)

Use this so **only you (maintainers) can push to `main`**, and everyone else must open a pull request.

> **Note:** On a public repo, strangers already cannot push to `main` unless you add them as collaborators. Branch protection adds a hard rule so even collaborators cannot push directly.

---

## Step-by-step (GitHub UI)

1. Open **https://github.com/s3roy/native-bridge/settings/branches**
2. Under **Branch protection rules**, click **Add branch protection rule** (or edit existing rule for `main`)
3. **Branch name pattern:** `main`
4. Enable these settings:

| Setting | Recommended | Why |
|---------|-------------|-----|
| **Require a pull request before merging** | On | No direct commits to `main` |
| **Require approvals** | On (1 approval) | Optional for solo maintainer — you can approve your own PRs or leave off if only you merge |
| **Dismiss stale pull request approvals when new commits are pushed** | On | Keeps review meaningful |
| **Require status checks to pass before merging** | Off until CI exists | Turn on when you add GitHub Actions |
| **Require conversation resolution before merging** | On | All review threads resolved |
| **Require signed commits** | Off (optional) | Enable if you use GPG/SSH commit signing |
| **Require linear history** | Off (optional) | Squash merges are fine |
| **Include administrators** | **On** | **Important:** rules apply to you too — you also use PRs |
| **Restrict who can push to matching branches** | **On** | Core lock |
| → Add allowed people/teams | **Only your GitHub user** (`s3roy`) | Nobody else can push to `main` |
| **Allow force pushes** | **Off** | Protects history |
| **Allow deletions** | **Off** | Prevents accidental branch delete |

5. Click **Create** or **Save changes**

---

## Solo maintainer (simplest setup)

If you are the only person with write access:

1. **Do not add collaborators** with Write access (Settings → Collaborators)
2. Enable **Require a pull request before merging** on `main`
3. Enable **Restrict who can push** → add only **`s3roy`**
4. Enable **Include administrators**

Result:

- You merge via PR (or temporarily disable rule for an emergency — not recommended)
- Fork contributors always use PRs from their fork
- No one else can push to `main`

---

## Workflow after protection is on

```bash
# Your day-to-day (even as owner)
git checkout -b fix/playground-url
# ... edits ...
git push origin fix/playground-url
# Open PR on GitHub → Review → Merge
```

Emergency bypass (avoid if possible): temporarily disable the rule in Settings → Branches, push, re-enable.

---

## Optional: GitHub CLI

If you install [GitHub CLI](https://cli.github.com/) and run `gh auth login`:

```bash
gh api repos/s3roy/native-bridge/branches/main/protection \
  --method PUT \
  -f required_pull_request_reviews='{"required_approving_review_count":0,"dismiss_stale_reviews":true}' \
  -F enforce_admins=true \
  -F restrictions='{"users":["s3roy"],"teams":[],"apps":[]}' \
  -F required_status_checks='null' \
  -F restrictions_users='["s3roy"]'
```

Adjust `s3roy` if your GitHub username differs. Prefer the UI for the first setup so you can see every checkbox.

---

## FAQ

**Can contributors still fork and PR?**  
Yes. Fork + PR is the intended open-source flow. Protection only blocks direct pushes to `main` on this repo.

**I merged a PR and Vercel didn’t deploy.**  
Check Vercel Git integration still points at `s3roy/native-bridge` after any repo rename.

**Someone opened a PR I don’t want.**  
Close it with a short, respectful note. You are not obligated to merge.
