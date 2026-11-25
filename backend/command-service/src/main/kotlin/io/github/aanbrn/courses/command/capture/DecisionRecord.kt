// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command.capture

/**
 * What one command attempt decided and why: the facts the decision saw, the condition its append asked the store to
 * check, and how that append ended (DEMO-01).
 *
 * A record is immutable and holds only text and numbers, so a later phase can store and render it without depending on
 * Axon. Every attempt of a command has its own record, including attempts that ended in a conflict and were retried.
 *
 * @property commandId the identifier of the command message, which every retry of the command keeps; `null` when the
 *   append happened outside a command, for example when a test seeds events
 * @property commandType the qualified name of the command, such as `courses.SubscribeStudent`; `null` outside a command
 * @property attempt the 1-based number of this attempt among the attempts of the same [commandId]
 * @property sourcingCriteria the event criteria the decision sourced, one entry per source call
 * @property sourcedEvents the events the decision saw, in the order the store delivered them
 * @property appendCriteria the criteria of the append condition, as the store received them; `null` when nothing was
 *   appended
 * @property consistencyMarker the consistency marker of the append condition, which is the position the decision's
 *   knowledge ends at; `null` when nothing was appended
 * @property appendedEvents the events the decision tried to append, with the tags the store indexes them by
 * @property outcome how the attempt ended
 */
data class DecisionRecord(
    val commandId: String?,
    val commandType: String?,
    val attempt: Int,
    val sourcingCriteria: List<String>,
    val sourcedEvents: List<RecordedEvent>,
    val appendCriteria: String?,
    val consistencyMarker: String?,
    val appendedEvents: List<RecordedEvent>,
    val outcome: Outcome,
) {

    /** How a command attempt ended. */
    sealed interface Outcome {

        /**
         * The append committed.
         *
         * @property markerAfterCommit the consistency marker the store reported after the commit, which is the position
         *   of the new events
         */
        data class Committed(val markerAfterCommit: String?) : Outcome

        /** The decision completed without appending an event, for example because it rejected the command. */
        data object NothingAppended : Outcome

        /**
         * The store rejected the append because an event that matches its criteria exists beyond its consistency
         * marker. This is the DCB conflict, and the command is retried.
         *
         * @property conflictingEvent the first event that made the append conflict; `null` only when the store could
         *   not be asked for it
         */
        data class Conflict(val conflictingEvent: RecordedEvent?) : Outcome

        /**
         * The attempt failed for a reason other than a conflict.
         *
         * @property reason the type and message of the failure
         */
        data class Failed(val reason: String) : Outcome
    }
}

/**
 * An event as a decision record shows it.
 *
 * @property type the stored message type with its version, such as `courses.StudentSubscribed#1`
 * @property payload the payload as text, which is JSON for an event read back from the store; `null` for an event
 *   without payload
 * @property tags the tags the event is indexed by; empty for a sourced event, because the store does not return them
 */
data class RecordedEvent(val type: String, val payload: String?, val tags: Set<RecordedTag> = emptySet())

/**
 * A DCB tag of an appended event.
 *
 * @property key the tag key, one of the registered tag keys
 * @property value the tag value, such as the TypeID of a course
 */
data class RecordedTag(val key: String, val value: String)
