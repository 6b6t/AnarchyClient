"""Exercise push selection against real Git repositories without compiling projects."""

import importlib.util
import os
from pathlib import Path
import subprocess
import tempfile
import unittest
from unittest.mock import patch

SPEC = importlib.util.spec_from_file_location(
    "check_push", Path(__file__).with_name("check-push.py")
)
hooks = importlib.util.module_from_spec(SPEC)
SPEC.loader.exec_module(hooks)
ENV = hooks.environment()


class PushChecksTest(unittest.TestCase):
    def setUp(self):
        self.temp = tempfile.TemporaryDirectory()
        self.addCleanup(self.temp.cleanup)
        self.root = Path(self.temp.name)
        self.git("init", "-q")
        self.git("config", "user.name", "Hook Test")
        self.git("config", "user.email", "hooks@example.invalid")
        self.write("README.md", "initial\n")
        self.base = self.commit()
        self.patch_root = patch.object(hooks, "ROOT", self.root)
        self.patch_root.start()
        self.addCleanup(self.patch_root.stop)

    def git(self, *args):
        return (
            subprocess.check_output(
                ["git", *args], cwd=self.root, env=ENV, stderr=subprocess.DEVNULL
            )
            .decode()
            .strip()
        )

    def write(self, name, content):
        path = self.root / name
        path.parent.mkdir(parents=True, exist_ok=True)
        path.write_text(content)

    def commit(self):
        self.git("add", ".")
        self.git("commit", "-qm", "test: fixture")
        return self.git("rev-parse", "HEAD")

    def update(self, tip=None, base=None):
        return f"refs/heads/main {tip or self.git('rev-parse', 'HEAD')} refs/heads/main {base or self.base}\n"

    def test_exact_remote_range_and_unusual_filenames(self):
        self.write("src/has $dollar and spaces.rs", "first\n")
        middle = self.commit()
        self.write("src/later.rs", "second\n")
        self.commit()
        self.assertEqual(
            hooks.pushed_files([self.update(base=middle)]), {"src/later.rs"}
        )
        self.assertEqual(
            hooks.pushed_files([self.update()]),
            {"src/has $dollar and spaces.rs", "src/later.rs"},
        )

    def test_deleted_and_renamed_files_are_included(self):
        self.git("mv", "README.md", "renamed.md")
        self.commit()
        self.assertEqual(
            hooks.pushed_files([self.update()]), {"README.md", "renamed.md"}
        )

    def test_new_remote_ref_checks_complete_tree(self):
        self.assertEqual(
            hooks.pushed_files([self.update(base="0" * 40)]), {"README.md"}
        )

    def test_unavailable_remote_commit_checks_complete_tree(self):
        self.assertEqual(
            hooks.pushed_files([self.update(base="1" * 40)]), {"README.md"}
        )

    def test_delete_only_and_empty_push_do_not_check_worktree(self):
        self.write("untracked.txt", "local\n")
        self.assertEqual(hooks.pushed_files([self.update(tip="0" * 40)]), set())
        self.assertEqual(hooks.pushed_files([]), set())

    def test_multiple_refs_union_their_ranges(self):
        self.write("first.rs", "first\n")
        middle = self.commit()
        self.write("second.rs", "second\n")
        self.commit()
        self.assertEqual(
            hooks.pushed_files([self.update(base=middle), self.update()]),
            {"first.rs", "second.rs"},
        )

    def test_annotated_tag_resolves_to_checked_out_commit(self):
        self.git("tag", "-a", "fixture", "-m", "fixture")
        tag = self.git("rev-parse", "fixture")
        self.assertEqual(
            hooks.pushed_files([self.update(tip=tag, base="0" * 40)]), {"README.md"}
        )

    def test_non_head_push_fails(self):
        self.write("next.rs", "next\n")
        self.commit()
        with self.assertRaisesRegex(ValueError, "Check out the pushed commit"):
            hooks.pushed_files([self.update(tip=self.base)])

    def test_dirty_tracked_staged_and_untracked_files_fail(self):
        self.write("README.md", "modified\n")
        with self.assertRaisesRegex(ValueError, "Commit or stash"):
            hooks.pushed_files([self.update()])
        self.git("add", "README.md")
        with self.assertRaisesRegex(ValueError, "Commit or stash"):
            hooks.pushed_files([self.update()])
        self.commit()
        self.write("untracked.txt", "local\n")
        with self.assertRaisesRegex(ValueError, "Commit or stash"):
            hooks.pushed_files([self.update()])

    def test_malformed_hook_input_fails(self):
        with self.assertRaisesRegex(ValueError, "Invalid pre-push input"):
            hooks.pushed_files(["not a ref update"])

    def test_hook_environment_does_not_leak_git_paths(self):
        with patch.dict(
            os.environ,
            {
                "GIT_DIR": str(self.root / ".git"),
                "GIT_INDEX_FILE": str(self.root / "alternate-index"),
            },
        ):
            env = hooks.environment()
        self.assertNotIn("GIT_DIR", env)
        self.assertNotIn("GIT_INDEX_FILE", env)

    def test_linked_worktree_with_git_hook_environment(self):
        with tempfile.TemporaryDirectory() as directory:
            linked = Path(directory) / "linked"
            self.git("worktree", "add", "--detach", "-q", str(linked), "HEAD")
            git_dir = subprocess.check_output(
                ["git", "rev-parse", "--absolute-git-dir"],
                cwd=linked,
                env=ENV,
                text=True,
            ).strip()
            with patch.object(hooks, "ROOT", linked), patch.dict(
                os.environ,
                {"GIT_DIR": git_dir, "GIT_INDEX_FILE": str(Path(git_dir) / "index")},
            ):
                self.assertEqual(
                    hooks.pushed_files([self.update(base="0" * 40)]), {"README.md"}
                )
            self.git("worktree", "remove", str(linked))

    def test_docs_only_changes_do_not_compile_projects(self):
        commands = hooks.checks_for({"README.md"})
        self.assertEqual(len(commands), 1)
        self.assertIn("test_check_push.py", commands[0][1])

    def test_source_change_selects_checks_and_consumers(self):
        selected = hooks.checks_for(
            {"src/main/java/net/blockhost/anarchyclient/Example.java"}
        )
        self.assertGreater(len(selected), 1)
        for directory in ["."]:
            self.assertTrue(any(cwd == directory for cwd, command in selected[1:]))

    def test_dry_run_never_runs_build_commands(self):
        with patch("sys.argv", ["check-push.py", "--all", "--dry-run"]), patch.object(
            hooks, "git", return_value=b"README.md\0"
        ), patch.object(hooks.subprocess, "run") as run:
            hooks.main()
        run.assert_not_called()

    def test_failed_check_stops_the_push(self):
        with patch("sys.argv", ["check-push.py", "--all"]), patch.object(
            hooks, "git", return_value=b"README.md\0"
        ), patch.object(hooks, "environment", return_value={}), patch.object(
            hooks.subprocess,
            "run",
            side_effect=subprocess.CalledProcessError(1, "fixture"),
        ):
            with self.assertRaises(subprocess.CalledProcessError):
                hooks.main()


if __name__ == "__main__":
    unittest.main()
