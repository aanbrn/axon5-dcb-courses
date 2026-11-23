// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command.fixtures

import io.github.aanbrn.courses.command.capture.DecisionRecord
import io.github.aanbrn.courses.command.capture.DecisionRecorder
import java.util.concurrent.CopyOnWriteArrayList

/**
 * A [DecisionRecorder] that keeps every record in memory, in the order the records arrived, for tests to inspect.
 *
 * It is thread-safe, because attempts of concurrent commands finish on different threads. Each query returns a
 * snapshot, so a test can read it while commands are still running.
 */
class CollectingDecisionRecorder : DecisionRecorder {
    private val collected = CopyOnWriteArrayList<DecisionRecord>()

    /** Every record received so far, in arrival order. */
    val records: List<DecisionRecord>
        get() = collected.toList()

    override fun record(record: DecisionRecord) {
        collected += record
    }

    /** Forgets every record received so far, for example the records of seeding events before the real test starts. */
    fun clear() {
        collected.clear()
    }

    /** The records of the attempts of the command with [commandId], in arrival order. */
    fun ofCommand(commandId: String): List<DecisionRecord> = records.filter { it.commandId == commandId }

    /** The records of attempts whose append committed. */
    fun committed(): List<DecisionRecord> = records.filter { it.outcome is DecisionRecord.Outcome.Committed }

    /** The records of attempts whose append the store rejected as a DCB conflict. */
    fun conflicts(): List<DecisionRecord> = records.filter { it.outcome is DecisionRecord.Outcome.Conflict }
}
