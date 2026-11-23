// SPDX-License-Identifier: MIT
package io.github.aanbrn.courses.command.capture

/**
 * Receives the [DecisionRecord] of every command attempt.
 *
 * The command service requires exactly one recorder component, so that capture is never silently off. Phase 2 ships
 * only this interface; storing the records is the work of the phase that adds the first decisions. An implementation is
 * called from the threads that handle commands, possibly several at once, so it must be thread-safe and must not block,
 * and an exception it throws is logged and swallowed because capture must never change the outcome of a decision.
 */
fun interface DecisionRecorder {

    /**
     * Receives the record of one finished attempt.
     *
     * @param record the immutable record of the attempt
     */
    fun record(record: DecisionRecord)
}
