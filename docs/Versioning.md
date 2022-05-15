# Versioning with git

If you have your JOSM plugin under version control in git, the `gradle-josm-plugin` tries to automatically
calculate the version number for you, so you don't need to explicitly set the version number in the code.

The version number is calculated very similarly to the command
[`git describe --always`](https://git-scm.com/docs/git-describe) on the command line. With just the minor difference
that a leading `v` character is removed, so a git-tag `v1.2.3` will become version `1.2.3`.

This way you can't forget updating the version number when a release happens.

## When there is no previously released version

When you didn't create a release before, the version number will just be the short commit hash
(seven hexadecimal digits, e.g. `abc1234`) of the current git-commit.
If you have any uncommitted changes in your git-repository, then `-SNAPSHOT` is appended to the version number.

## Creating a new release version

Checkout the commit that you want to release.

Then create a new tag on the command line:
```shell
git tag -a v1.2.3
```
When a text editor opens, you can enter a message for the release (like a commit message, but for a release).

It is important to use the `-a` flag (or alternatively the `-s` flag to sign the git-tag, but that requires additional setup).

From then on when you build that commit, the version should be `1.2.3`.

## When there was an earlier release version

After you created your first release, the version number will always be:
 * the name of the most recent git-tag (without leading `v`, if there was one)
 * the number of commits since that git-tag
 * the letter `g` followed by the short commit hash of the current git-commit
 * if there are uncommitted changes, then `-SNAPSHOT` is added

## Examples

1. Version `abc1234` was built from a commit with hash `abc1234` and there was no release yet.
2. Version `abc1234-SNAPSHOT` is the same as 1., but with uncommitted changes in the working directory.
And there were some additional changes that were not committed to the repository.
3. Version `1.2.3` was build from exactly the commit, which has the git-tag `v1.2.3`.
4. Version `1.2.3-4-gabc1234` was built from the commit with short hash `abc1234`, which comes four commits after the git-tag `v1.2.3`
5. Version `1.2.3-4-gabc1234-SNAPSHOT` is the same as 4., but with uncommitted changes in the working directory.

# Versioning manually

If you really want to set the version number manually, you can do it like this:

Add `version=1.2.3` to your `gradle.properties` file.

Or add `version = "1.2.3"` to your `build.gradle`/`build.gradle.kts` file.
