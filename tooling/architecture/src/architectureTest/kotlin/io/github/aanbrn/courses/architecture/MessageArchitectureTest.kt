// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.architecture

import com.lemonappdev.konsist.api.declaration.combined.KoClassAndInterfaceAndObjectDeclaration
import com.lemonappdev.konsist.api.verify.assertTrue
import org.junit.jupiter.api.Test

/**
 * Every message names itself explicitly (D-03) and every event tag key comes from the registry (D-10, INFRA-02).
 *
 * A message name and version are one-way after the first persisted event, so they are never derived from the class name
 * or from a default: `@Event`, `@Command` and `@Query` declare `namespace`, `name` and `version` as named arguments.
 * The rules run over all source sets of the backend, so a test message obeys them too. The module placement of
 * production messages is asserted once over the whole message scope, which is non-empty through the events and commands
 * of the contracts; no assertion targets the queries module alone, because it holds no message yet.
 */
class MessageArchitectureTest {
    private val messageClasses: List<KoClassAndInterfaceAndObjectDeclaration> =
        ArchitectureScopes.backend().classesAndInterfacesAndObjects().filter { declaration ->
            declaration.annotations.any { it.name in MODULE_OF_MESSAGE_ANNOTATION }
        }

    @Test
    fun `every message names the courses namespace, its own class name and an explicit version`() {
        messageClasses.assertTrue(strict = true) { declaration ->
            val arguments =
                declaration.annotations
                    .first { it.name in MODULE_OF_MESSAGE_ANNOTATION }
                    .arguments
                    .associateBy { it.name }
            val namespaceIsRegistered = arguments["namespace"]?.value == "MessageNamespace.COURSES"
            val nameIsClassName = arguments["name"]?.text?.replace(WHITESPACE, "") == "name=\"${declaration.name}\""
            val versionIsLiteral = arguments["version"]?.text?.let { NON_EMPTY_VERSION_LITERAL.matches(it) } == true
            if (!namespaceIsRegistered) {
                addFailureReason("${declaration.name}: namespace must be MessageNamespace.COURSES")
            }
            if (!nameIsClassName)
                addFailureReason("${declaration.name}: name must be the string \"${declaration.name}\"")
            if (!versionIsLiteral) addFailureReason("${declaration.name}: version must be a non-empty string literal")
            namespaceIsRegistered && nameIsClassName && versionIsLiteral
        }
    }

    @Test
    fun `every event tag key is a reference into the Tags registry`() {
        val eventTags =
            ArchitectureScopes.backend()
                .classes()
                .flatMap { declaration ->
                    declaration.properties().flatMap { it.annotations } +
                        declaration.primaryConstructor?.parameters.orEmpty().flatMap { it.annotations }
                }
                .filter { it.name == "EventTag" }

        eventTags.assertTrue(strict = true) { annotation ->
            val keyIsRegistered = annotation.arguments.any { it.name == "key" && it.value?.startsWith("Tags.") == true }
            if (!keyIsRegistered) addFailureReason("@EventTag must name its key as a Tags.* reference")
            keyIsRegistered
        }
    }

    @Test
    fun `production messages live in the contracts module of their kind`() {
        messageClasses.assertTrue(strict = true) { declaration ->
            val annotation = declaration.annotations.first { it.name in MODULE_OF_MESSAGE_ANNOTATION }
            val module = MODULE_OF_MESSAGE_ANNOTATION.getValue(annotation.name)
            val path = declaration.containingFile.path.replace('\\', '/')
            val isProduction = ArchitectureScopes.sourceSetOf(path) == ArchitectureScopes.PRODUCTION_SOURCE_SET
            val expectedDirectory = "/${ArchitectureScopes.CONTRACTS_ROOT}/$module/src/main/"
            val inPlace = !isProduction || path.contains(expectedDirectory)
            if (!inPlace)
                addFailureReason("${declaration.name}: a @${annotation.name} must live under $expectedDirectory")
            inPlace
        }
    }

    private companion object {
        /** The contracts module that holds production messages carrying the annotation, by annotation name. */
        val MODULE_OF_MESSAGE_ANNOTATION = mapOf("Event" to "events", "Command" to "commands", "Query" to "queries")

        /** The source text of a `version` argument that is a string literal with at least one character. */
        val NON_EMPTY_VERSION_LITERAL = Regex("version\\s*=\\s*\"[^\"]+\"")

        val WHITESPACE = Regex("\\s+")
    }
}
