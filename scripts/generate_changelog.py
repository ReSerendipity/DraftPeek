#!/usr/bin/env python3
"""
CHANGELOG 自动生成脚本。

从 git log 中解析 Conventional Commits 格式的提交消息，
自动生成符合 Keep a Changelog 格式的 CHANGELOG 条目。

Usage:
    python scripts/generate_changelog.py --version 1.0.31 [--from v1.0.30] [--to HEAD]

Conventional Commits 格式：
    type(scope): description
    type: description

支持的 type：
    feat     → Added
    fix      → Fixed
    perf     → Performance
    refactor → Changed
    docs     → (skip, 不纳入 CHANGELOG)
    style    → (skip)
    test     → (skip)
    chore    → (skip)
    build    → (skip)
    ci       → (skip)
    security → Security
    deprecate→ Deprecated
    remove   → Removed

Author: DraftPeek Team
Since: 1.0.31
"""

import argparse
import re
import subprocess
import sys
from collections import defaultdict
from datetime import date

# Conventional Commit type → CHANGELOG section mapping
TYPE_TO_SECTION = {
    "feat": "Added",
    "fix": "Fixed",
    "perf": "Performance",
    "refactor": "Changed",
    "security": "Security",
    "deprecate": "Deprecated",
    "remove": "Removed",
    "breaking": "Breaking Changes",
}

# Types to skip (not user-visible changes)
SKIP_TYPES = {"docs", "style", "test", "chore", "build", "ci", "merge"}

# Conventional Commit regex: type(scope)!: description
COMMIT_PATTERN = re.compile(
    r"^(?P<type>\w+)"
    r"(?:\((?P<scope>[\w\-/.]+)\))?"
    r"(?P<breaking>!)?:"
    r"\s*(?P<description>.+)$"
)


def run_git(args):
    """Run a git command and return its output."""
    try:
        result = subprocess.run(
            ["git"] + args,
            capture_output=True,
            text=True,
            check=True,
            encoding="utf-8",
        )
        return result.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Git error: {e.stderr}", file=sys.stderr)
        sys.exit(1)


def parse_commits(log_output):
    """Parse git log output into structured commits."""
    commits = []
    current_hash = None
    current_subject = ""
    current_body = ""
    in_body = False

    for line in log_output.split("\n"):
        if line.startswith("\x00"):
            # Commit separator
            if current_hash:
                commits.append((current_hash, current_subject.strip(), current_body.strip()))
            current_hash = line[1:].strip()
            current_subject = ""
            current_body = ""
            in_body = False
        elif current_hash and not in_body:
            if line.strip() == "":
                in_body = True
            else:
                current_subject = line
        elif in_body:
            current_body += line + "\n"

    if current_hash:
        commits.append((current_hash, current_subject.strip(), current_body.strip()))

    return commits


def categorize_commit(subject):
    """Categorize a commit by Conventional Commit type."""
    match = COMMIT_PATTERN.match(subject)
    if not match:
        return None

    commit_type = match.group("type").lower()
    scope = match.group("scope")
    description = match.group("description").strip()
    has_breaking = bool(match.group("breaking"))

    # Check for BREAKING CHANGE in description or body
    if has_breaking:
        return ("breaking", scope, description)

    if commit_type in SKIP_TYPES:
        return None

    section = TYPE_TO_SECTION.get(commit_type)
    if not section:
        return None

    return (section, scope, description)


def generate_changelog_entry(version, commits, from_tag):
    """Generate a CHANGELOG entry for the given version."""
    categorized = defaultdict(list)

    for _, subject, body in commits:
        result = categorize_commit(subject)
        if result:
            section, scope, description = result
            # Check for BREAKING CHANGE in body
            if "BREAKING CHANGE:" in body:
                breaking_desc = body.split("BREAKING CHANGE:")[1].strip().split("\n")[0]
                categorized["Breaking Changes"].append(breaking_desc)
            else:
                prefix = f"**{scope}**: " if scope else ""
                categorized[section].append(f"{prefix}{description}")

    # Build markdown entry
    today = date.today().isoformat()
    lines = [f"## [{version}] - {today}", ""]

    # Order sections according to Keep a Changelog
    section_order = [
        "Breaking Changes",
        "Added",
        "Changed",
        "Deprecated",
        "Removed",
        "Fixed",
        "Security",
        "Performance",
    ]

    for section in section_order:
        if section in categorized:
            lines.append(f"### {section}")
            for item in categorized[section]:
                lines.append(f"- {item}")
            lines.append("")

    return "\n".join(lines)


def main():
    parser = argparse.ArgumentParser(description="Generate CHANGELOG entry from git log")
    parser.add_argument("--version", required=True, help="Version number (e.g., 1.0.31)")
    parser.add_argument("--from", dest="from_tag", default=None, help="From tag/commit (default: last tag)")
    parser.add_argument("--to", default="HEAD", help="To tag/commit (default: HEAD)")
    parser.add_argument("--output", default=None, help="Output file (default: stdout)")
    parser.add_argument("--insert", action="store_true",
                        help="Insert into CHANGELOG.md (after [Unreleased] section)")
    args = parser.parse_args()

    # Determine from tag
    from_ref = args.from_tag
    if not from_ref:
        tags = run_git(["tag", "--sort=-creatordate"]).split("\n")
        from_ref = tags[0] if tags and tags[0] else "HEAD~50"

    # Get git log
    log_output = run_git([
        "log", f"{from_ref}..{args.to}",
        "--format=%x00%H%n%s%n%b",
    ])

    if not log_output.strip():
        print(f"No commits found between {from_ref} and {args.to}", file=sys.stderr)
        sys.exit(0)

    commits = parse_commits(log_output)
    entry = generate_changelog_entry(args.version, commits, from_ref)

    if args.insert:
        # Read existing CHANGELOG.md
        try:
            with open("CHANGELOG.md", "r", encoding="utf-8") as f:
                changelog = f.read()
        except FileNotFoundError:
            changelog = "# Changelog\n\n"

        # Insert after [Unreleased] section header
        unreleased_marker = "## [Unreleased]"
        if unreleased_marker in changelog:
            # Find the next ## section after [Unreleased]
            idx = changelog.index(unreleased_marker)
            # Find the end of [Unreleased] section (next ## or end of file)
            rest = changelog[idx + len(unreleased_marker):]
            next_section = rest.find("\n## ")
            if next_section == -1:
                # Insert at end
                insert_pos = len(changelog)
            else:
                insert_pos = idx + len(unreleased_marker) + next_section
        else:
            # Insert after the header line
            lines = changelog.split("\n", 2)
            insert_pos = len(lines[0]) + 1 if len(lines) > 1 else len(changelog)

        new_changelog = changelog[:insert_pos] + "\n" + entry + "\n" + changelog[insert_pos:]
        with open("CHANGELOG.md", "w", encoding="utf-8") as f:
            f.write(new_changelog)
        print(f"Inserted CHANGELOG entry for version {args.version}")
    elif args.output:
        with open(args.output, "w", encoding="utf-8") as f:
            f.write(entry)
        print(f"Wrote CHANGELOG entry to {args.output}")
    else:
        print(entry)


if __name__ == "__main__":
    main()
