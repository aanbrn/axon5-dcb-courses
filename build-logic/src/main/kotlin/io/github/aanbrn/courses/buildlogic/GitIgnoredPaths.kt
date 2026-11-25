// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes
import javax.inject.Inject
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

/**
 * Lists the untracked paths that git ignores, as Ant exclude patterns relative to the project directory.
 *
 * Spotless formats whatever files match its target globs, so a git-ignored tool directory, such as the
 * `.playwright-mcp` directory of a Playwright MCP session, would fail the formatting gate locally while CI, which never
 * sees it, passes. The list comes from `git ls-files --others --ignored --exclude-standard --directory`, so the gate
 * follows `.gitignore` without a second hand-kept list. `--others` never lists a tracked file, so `.gitignore` can
 * never remove a tracked file from a gate, even a file that a rule of `.gitignore` matches.
 *
 * The value is empty, and so only the static exclusions apply, when git is not installed, when the project directory is
 * not the root of a git work tree, or when a git command fails. An empty list only means that more files stay
 * formatted, which is the safe side of every failure.
 *
 * This is a value source, as the Gradle best practices require for a process that runs at configuration time. The
 * entries under [Parameters.coveredDirectoryNames], which the build excludes by name already, are left out. Build
 * outputs that appear or disappear therefore never change the value, and the configuration cache stays reused, also
 * when a directory holds nothing else. The program is launched by the absolute path [Parameters.gitExecutable], never
 * by a bare name, for the reason given at [ExecutableOnPath].
 */
abstract class GitIgnoredPaths @Inject constructor(private val exec: ExecOperations) :
    ValueSource<List<String>, GitIgnoredPaths.Parameters> {

    /** The parameters of [GitIgnoredPaths]. */
    interface Parameters : ValueSourceParameters {
        /** The absolute path of the git executable, absent when git is not installed. */
        val gitExecutable: Property<String>

        /** The project directory, which must be the root of a git work tree for any path to be listed. */
        val workTree: DirectoryProperty

        /**
         * The directory names that the build excludes anyway. An ignored path that has such a name as one of its
         * segments is not listed. A name that ends in `*` matches as a prefix.
         */
        val coveredDirectoryNames: SetProperty<String>
    }

    /** Returns the sorted exclude patterns, or an empty list when no path can be derived from git. */
    override fun obtain(): List<String> {
        val git = parameters.gitExecutable.orNull
        val root = parameters.workTree.get().asFile
        if (git == null || !isWorkTreeRoot(git, root)) return emptyList()
        val listing = runGit(git, root, "ls-files", "-z", "--others", "--ignored", "--exclude-standard", "--directory")
        return listing.orEmpty().split(NUL).filter { it.isNotEmpty() }.let { toPatterns(root, it) }
    }

    private fun isWorkTreeRoot(git: String, root: File): Boolean {
        val topLevel = runGit(git, root, "rev-parse", "--show-toplevel")?.trim().orEmpty()
        // Canonical files, because a temporary directory can sit behind a symbolic link, as `/var` does on macOS.
        return topLevel.isNotEmpty() && File(topLevel).canonicalFile == root.canonicalFile
    }

    /** The standard output of `git -C [root] [args]`, or null when the command fails. */
    private fun runGit(git: String, root: File, vararg args: String): String? {
        val output = ByteArrayOutputStream()
        val result =
            // The kotlin-dsl plugin makes the receiver of a Gradle `Action` lambda `this`.
            exec.exec {
                executable = git
                args("-C", root.absolutePath, *args)
                standardOutput = output
                errorOutput = ByteArrayOutputStream()
                isIgnoreExitValue = true
            }
        return if (result.exitValue == 0) output.toString(Charsets.UTF_8) else null
    }

    private fun toPatterns(root: File, entries: List<String>): List<String> {
        val covered = parameters.coveredDirectoryNames.get()
        return entries
            .asSequence()
            .filterNot { entry -> entry.split('/').any { isCovered(covered, it) } }
            // Ant patterns have no escape, so an entry with a wildcard stays formatted.
            .filterNot { '*' in it || '?' in it }
            // A directory that holds nothing but covered directories, such as a leftover module with only a `build`
            // directory, would change the value for no gain, so it is left out.
            .filterNot { it.endsWith("/") && !holdsUncoveredFile(File(root, it), covered) }
            .map { if (it.endsWith("/")) "$it**" else it }
            .sorted()
            .toList()
    }

    private fun isCovered(covered: Set<String>, segment: String): Boolean = covered.any {
        if (it.endsWith("*")) segment.startsWith(it.removeSuffix("*")) else segment == it
    }

    /** Whether [directory] holds a file outside the covered directories. Symbolic links are not followed. */
    private fun holdsUncoveredFile(directory: File, covered: Set<String>): Boolean {
        var found = false
        val visitor =
            object : SimpleFileVisitor<Path>() {
                override fun preVisitDirectory(dir: Path, attrs: BasicFileAttributes): FileVisitResult =
                    if (dir != directory.toPath() && isCovered(covered, dir.fileName.toString())) {
                        FileVisitResult.SKIP_SUBTREE
                    } else {
                        FileVisitResult.CONTINUE
                    }

                override fun visitFile(file: Path, attrs: BasicFileAttributes): FileVisitResult {
                    found = true
                    return FileVisitResult.TERMINATE
                }
            }
        if (directory.isDirectory) Files.walkFileTree(directory.toPath(), visitor)
        return found
    }

    private companion object {
        const val NUL = '\u0000'
    }
}
