// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.contracts.kernel

import com.fasterxml.uuid.Generators
import com.fasterxml.uuid.impl.TimeBasedEpochGenerator
import java.util.UUID

/**
 * The common shape of every entity ID: a UUID that is written as a TypeID with a fixed prefix.
 *
 * Implementations are value classes whose `toString()` returns the canonical TypeID.
 */
interface TypedId {
    /** The UUID the TypeID suffix encodes. */
    val uuid: UUID
}

/**
 * Companion base class that gives a [TypedId] its factory and parse API.
 *
 * @param T the ID type
 * @property prefix the TypeID prefix of [T]
 * @param wrap creates an ID from a UUID
 * @throws IllegalArgumentException when [prefix] is not a valid TypeID prefix
 */
abstract class TypedIdCompanion<T : TypedId>(val prefix: String, private val wrap: (UUID) -> T) {

    init {
        require(TypeId.isValidPrefix(prefix)) { "Malformed TypeID prefix '$prefix'" }
    }

    /**
     * Wraps [uuid] in an ID.
     *
     * @param uuid the UUID the ID stands for
     * @return the ID over [uuid]
     */
    fun of(uuid: UUID): T = wrap(uuid)

    /**
     * Generates a new ID over a UUIDv7, which sorts by creation time.
     *
     * @return a new, unique ID
     */
    fun random(): T = wrap(uuidV7.generate())

    /**
     * Parses the canonical TypeID text of this ID type.
     *
     * @param value the TypeID text, such as `course_01h455vb4pex5vsknk084sn02q`
     * @return the ID the text stands for
     * @throws IllegalArgumentException when [value] is malformed or carries a prefix other than [prefix]
     */
    fun parse(value: String): T {
        val (actualPrefix, uuid) = TypeId.decode(value)
        require(actualPrefix == prefix) {
            "Wrong TypeID prefix in '$value': expected '$prefix', got '$actualPrefix'"
        }
        return wrap(uuid)
    }

    /**
     * Parses [value] like [parse], and returns `null` instead of throwing.
     *
     * @param value the TypeID text
     * @return the ID, or `null` when [value] is malformed or carries another prefix
     */
    fun parseOrNull(value: String): T? =
        try {
            parse(value)
        } catch (@Suppress("SwallowedException") ignored: IllegalArgumentException) {
            null
        }

    private companion object {
        /** One generator for all IDs: it is thread-safe and keeps the UUIDs it hands out ordered. */
        val uuidV7: TimeBasedEpochGenerator = Generators.timeBasedEpochGenerator()
    }
}
