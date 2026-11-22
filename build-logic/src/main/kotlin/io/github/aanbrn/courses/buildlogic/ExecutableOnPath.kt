// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.buildlogic

import java.io.File
import org.gradle.api.provider.Property
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters

/**
 * Resolves the absolute path of an executable from the `PATH` of the current build.
 *
 * The JDK resolves a program name without a slash against a `PATH` it cached when the daemon JVM
 * launched its first process (JDK-6413313), while Gradle installs the environment of every build
 * for `System.getenv`. A warm daemon would therefore run the program of the client that started it.
 * So build-logic resolves programs here and launches the absolute path, never a bare name. As a
 * value source this is a configuration cache input that invalidates the cache only when the
 * resolved path changes, unlike a direct read of `PATH`, which invalidates it on every `PATH` edit.
 *
 * Only absolute directories are searched. Gradle moves the daemon's working directory to the
 * directory of each build, so the file checks of a relative entry would follow the build, while its
 * absolute path would not. Skipping relative and empty entries keeps every result an existing
 * absolute file.
 */
abstract class ExecutableOnPath : ValueSource<String, ExecutableOnPath.Parameters> {

    /** The parameters of [ExecutableOnPath]. */
    interface Parameters : ValueSourceParameters {
        /** The file name looked up in each `PATH` directory, for example `node`. */
        val executableName: Property<String>
    }

    /**
     * Returns the absolute path of the first match on the build's `PATH`, or null when there is
     * none.
     */
    override fun obtain(): String? = firstExecutable(parameters.executableName.get())

    private fun firstExecutable(name: String): String? =
        System.getenv("PATH")
            ?.split(File.pathSeparator)
            .orEmpty()
            .filter { it.isNotEmpty() && File(it).isAbsolute }
            .map { File(it, name) }
            .firstOrNull { it.isFile && it.canExecute() }
            ?.absolutePath
}
