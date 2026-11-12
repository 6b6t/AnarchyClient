#!/usr/bin/env python3
"""Run local checks for the exact commits supplied by Git's pre-push hook."""

import argparse
import fnmatch
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys

ROOT = Path(__file__).resolve().parents[1]
RULES = [
    (
        [
            "src/*",
            "*.java",
            "*.kt",
            "*.kts",
            "*.gradle",
            "gradle/*",
            "gradlew*",
            "gradle.properties",
            "buildSrc/*",
        ],
        [(".", ["./gradlew", "check", "--max-workers=6", "--no-configuration-cache"])],
    )
]


def environment():
    # Hooks export Git paths. Do not leak them into Gradle or temporary test repos.
    env = os.environ.copy()
    names = subprocess.check_output(
        ["git", "rev-parse", "--local-env-vars"], cwd=ROOT, text=True
    ).splitlines()
    for name in names:
        env.pop(name, None)
    env["PYTHONDONTWRITEBYTECODE"] = "1"
    return env


def git(*args):
    return subprocess.check_output(
        ["git", *args], cwd=ROOT, env=environment(), stderr=subprocess.DEVNULL
    )


def pushed_files(lines):
    files = set()
    head = git("rev-parse", "HEAD").strip()
    updates = False
    for line in lines:
        fields = line.split()
        if len(fields) != 4:
            raise ValueError("Invalid pre-push input from Git")
        local_ref, local_sha, remote_ref, remote_sha = fields
        if any(
            not re.fullmatch(r"[0-9a-f]{40}|[0-9a-f]{64}", sha)
            for sha in (local_sha, remote_sha)
        ):
            raise ValueError("Invalid pre-push input from Git")
        if set(local_sha) == {"0"}:
            continue  # Deleting a ref has no local code to check.
        updates = True
        tip = git("rev-parse", local_sha + "^{commit}").strip()
        if tip != head:
            raise ValueError("Check out the pushed commit first: " + local_ref)
        if set(remote_sha) != {"0"}:
            try:
                git("cat-file", "-e", remote_sha + "^{commit}")
            except subprocess.CalledProcessError:
                files.update(
                    git("ls-tree", "-r", "--name-only", "-z", local_sha).split(b"\0")
                )
            else:
                files.update(
                    git(
                        "diff",
                        "--no-renames",
                        "--name-only",
                        "-z",
                        remote_sha,
                        local_sha,
                    ).split(b"\0")
                )
        else:
            # A new remote ref has no known baseline. Verify its complete tree.
            files.update(
                git("ls-tree", "-r", "--name-only", "-z", local_sha).split(b"\0")
            )
    if updates and git("status", "--porcelain", "--untracked-files=normal"):
        raise ValueError(
            "Commit or stash worktree changes before pushing so checks match the pushed commit"
        )
    return {os.fsdecode(path) for path in files if path}


def checks_for(files):
    commands = [
        (
            ".",
            [
                "python3",
                "-m",
                "unittest",
                "discover",
                "-s",
                "scripts",
                "-p",
                "test_check_push.py",
                "-v",
            ],
        )
    ]
    for patterns, checks in RULES:
        if any(
            fnmatch.fnmatchcase(path, pattern) for path in files for pattern in patterns
        ):
            for check in checks:
                if check not in commands:
                    commands.append(check)
    return commands


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument(
        "--dry-run",
        action="store_true",
        help="print the exact checks without running them",
    )
    parser.add_argument(
        "--all",
        action="store_true",
        help="check all tracked working-tree files without reading Git hook input",
    )
    parser.add_argument(
        "--verbose",
        action="store_true",
        help="list the changed paths that select checks",
    )
    args = parser.parse_args()
    files = (
        {os.fsdecode(p) for p in git("ls-files", "-z").split(b"\0") if p}
        if args.all
        else pushed_files(sys.stdin)
    )
    if args.verbose:
        for path in sorted(files):
            print(f"Changed: {path}", flush=True)
    if not files:
        print("No changed files to check.", flush=True)
        return
    for directory, command in checks_for(files):
        print(f"[{directory}] {shlex.join(command)}", flush=True)
        if not args.dry_run:
            # Rendered Helm manifests can contain configuration. Keep them out of logs.
            output = subprocess.DEVNULL if command[:2] == ["helm", "template"] else None
            subprocess.run(
                command,
                cwd=ROOT / directory,
                env=environment(),
                stdout=output,
                check=True,
            )


if __name__ == "__main__":
    try:
        main()
    except (ValueError, OSError, subprocess.CalledProcessError) as error:
        print(f"Local push checks failed: {error}", file=sys.stderr)
        sys.exit(1)
