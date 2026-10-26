package com.lukr99.workout.data.document

import com.lukr99.workout.domain.nowMillis
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * The web edition's storage logic, over any [DocumentStore]. It keeps the whole [WorkoutDocument]
 * in memory, and every change saves the whole document.
 *
 * - **No lost updates.** A save names the revision this copy is based on. When another tab saved
 *   in between, the newer document is loaded, the changes not saved yet are applied to it again,
 *   and the save is tried again.
 * - **Loud failures.** A failed save keeps the change in memory and sets [status] to
 *   [StorageState.SaveFailed]; the next change or [retrySave] tries again. When the browser refuses
 *   storage, or a newer build wrote the data, [status] says [StorageState.MemoryOnly] and nothing is
 *   written.
 * - **Quarantine.** Stored text that cannot be read is set aside once, under a key made from its
 *   revision, before anything replaces it. If it cannot be set aside, nothing is ever written.
 * - **Transactions.** [transaction] collects the writes of its block and saves them in one go, or
 *   not at all when the block throws. Writes are serialized with a [Mutex].
 */
class DocumentDatabase(
    private val store: DocumentStore,
    private val now: () -> Long = ::nowMillis,
    private val maxSaveAttempts: Int = 5,
) {
    private val lock = Mutex()
    private val committed = MutableStateFlow(WorkoutDocument())
    private val statusFlow = MutableStateFlow(StorageStatus())

    private var opened = false
    private var memoryOnly = false
    private var revision = 0L
    private var unsaved: List<DocumentChange> = emptyList()

    /** What the UI shows about storage. */
    val status: StateFlow<StorageStatus> = statusFlow.asStateFlow()

    /** The document as committed in this tab; opens the store on first collection. */
    val document: Flow<WorkoutDocument> = flow {
        open()
        emitAll(committed)
    }

    /** Reads the stored document. Safe to call more than once; later calls do nothing. */
    suspend fun open() {
        if (opened) return
        lock.withLock { openLocked() }
    }

    /** The document as this coroutine sees it: the working copy inside a transaction. */
    suspend fun current(): WorkoutDocument {
        activeTransaction()?.let { return it.working }
        open()
        return committed.value
    }

    /** Applies [change] and saves, or records it when called inside a [transaction]. */
    suspend fun update(change: DocumentChange) {
        val transaction = activeTransaction()
        if (transaction != null) {
            transaction.apply(change)
            return
        }
        lock.withLock {
            openLocked()
            val before = committed.value
            commitLocked(listOf(change), before, change(before))
        }
    }

    /** Runs [block] with all its writes saved together at the end, or dropped if it throws. */
    suspend fun <T> transaction(block: suspend () -> T): T {
        if (activeTransaction() != null) return block()
        return lock.withLock {
            openLocked()
            val before = committed.value
            val transaction = DocumentTransaction(this, before)
            val result = withContext(transaction) { block() }
            commitLocked(transaction.changes, before, transaction.working)
            result
        }
    }

    /**
     * Takes in what another tab saved. Changes this tab has not saved yet are applied on top and
     * saved again.
     */
    suspend fun reload() {
        lock.withLock {
            if (!opened) {
                openLocked()
                return
            }
            if (memoryOnly) return
            val load = loadSafely()
            if (load is DocumentLoad.Found && load.revision == revision) return
            if (load is DocumentLoad.Empty && revision == 0L) return
            if (!rebaseLocked(load)) return
            if (unsaved.isNotEmpty()) persistLocked()
        }
    }

    /** Tries again to save the changes a failed save left in memory. */
    suspend fun retrySave() {
        lock.withLock {
            if (opened && unsaved.isNotEmpty()) persistLocked()
        }
    }

    /** The data set aside so far, for a download in Settings. */
    suspend fun quarantined(): List<QuarantineEntry> = store.quarantined()

    private suspend fun activeTransaction(): DocumentTransaction? =
        currentCoroutineContext()[DocumentTransaction]?.takeIf { it.owner === this }

    private suspend fun openLocked() {
        if (opened) return
        opened = true
        when (val load = loadSafely()) {
            DocumentLoad.Empty -> setState(StorageState.Saved)
            is DocumentLoad.Unavailable -> goMemoryOnly(unavailableReason(load.reason))
            is DocumentLoad.Found -> {
                val decoded = decode(load.text)
                val document = decoded.getOrNull()
                if (document == null) {
                    setAsideLocked(load, describe(decoded.exceptionOrNull()))
                } else {
                    committed.value = document
                    revision = load.revision
                    if (document.isNewerThanThisBuild) goMemoryOnly(NEWER_BUILD) else setState(StorageState.Saved)
                }
            }
        }
    }

    private suspend fun setAsideLocked(load: DocumentLoad.Found, reason: String) {
        val entry = QuarantineEntry(key = "revision-${load.revision}", text = load.text, reason = reason, atUtc = now())
        val result = try {
            store.quarantine(entry)
        } catch (cancel: CancellationException) {
            throw cancel
        } catch (failure: Throwable) {
            QuarantineResult.Failed(describe(failure))
        }
        when (result) {
            QuarantineResult.Added, QuarantineResult.AlreadyThere -> {
                // The unreadable record is safe in the quarantine, so the next save may replace it.
                revision = load.revision
                committed.value = WorkoutDocument()
                statusFlow.value = StorageStatus(StorageState.Saved, SetAside(entry.key, reason))
            }
            is QuarantineResult.Failed -> goMemoryOnly(
                "Ember could not read the saved data and could not set it aside (${result.reason}). " +
                    "To keep that data safe, nothing is saved.",
            )
        }
    }

    private suspend fun commitLocked(changes: List<DocumentChange>, before: WorkoutDocument, after: WorkoutDocument) {
        if (changes.isEmpty() || after == before) return
        unsaved = unsaved + changes
        committed.value = after
        persistLocked()
    }

    private suspend fun persistLocked() {
        if (memoryOnly) return
        var attempts = 0
        while (true) {
            val text = try {
                DocumentCodec.encode(committed.value)
            } catch (failure: Throwable) {
                setState(StorageState.SaveFailed("The data could not be prepared for saving (${describe(failure)})."))
                return
            }
            when (val result = saveSafely(text)) {
                is DocumentSave.Saved -> {
                    revision = result.revision
                    unsaved = emptyList()
                    setState(StorageState.Saved)
                    return
                }
                is DocumentSave.Failed -> {
                    setState(StorageState.SaveFailed(result.reason))
                    return
                }
                is DocumentSave.Conflict -> {
                    attempts++
                    if (attempts >= maxSaveAttempts) {
                        setState(StorageState.SaveFailed("Another tab keeps changing the data. Close the other Ember tabs."))
                        return
                    }
                    if (!rebaseLocked(loadSafely())) return
                    if (unsaved.isEmpty()) {
                        setState(StorageState.Saved)
                        return
                    }
                }
            }
        }
    }

    /**
     * Moves this copy onto what [load] found and applies the unsaved changes again. Returns false
     * when it switched to memory only instead.
     */
    private fun rebaseLocked(load: DocumentLoad): Boolean {
        when (load) {
            // The record is gone (site data cleared elsewhere): keep everything here and save it anew.
            DocumentLoad.Empty -> {
                revision = 0
                if (unsaved.isEmpty() && committed.value != WorkoutDocument()) unsaved = listOf<DocumentChange> { it }
            }
            is DocumentLoad.Unavailable -> {
                goMemoryOnly(unavailableReason(load.reason))
                return false
            }
            is DocumentLoad.Found -> {
                val newer = decode(load.text).getOrNull()
                if (newer == null) {
                    goMemoryOnly("Another tab saved data this tab cannot read. Reload the page; changes made here are not saved.")
                    return false
                }
                if (newer.isNewerThanThisBuild) {
                    goMemoryOnly(NEWER_BUILD)
                    return false
                }
                val replayed = unsaved.fold(newer) { document, change -> change(document) }
                if (replayed == newer) unsaved = emptyList()
                revision = load.revision
                committed.value = replayed
            }
        }
        return true
    }

    private fun decode(text: String): Result<WorkoutDocument> = runCatching { DocumentCodec.decode(text) }

    private suspend fun loadSafely(): DocumentLoad = try {
        store.load()
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (failure: Throwable) {
        DocumentLoad.Unavailable(describe(failure))
    }

    private suspend fun saveSafely(text: String): DocumentSave = try {
        store.save(text, revision)
    } catch (cancel: CancellationException) {
        throw cancel
    } catch (failure: Throwable) {
        DocumentSave.Failed(describe(failure))
    }

    private fun goMemoryOnly(reason: String) {
        memoryOnly = true
        setState(StorageState.MemoryOnly(reason))
    }

    private fun setState(state: StorageState) {
        statusFlow.value = statusFlow.value.copy(state = state)
    }

    private fun describe(failure: Throwable?): String =
        failure?.message?.lineSequence()?.firstOrNull()?.takeIf(String::isNotBlank)
            ?: failure?.toString()
            ?: "unknown error"

    private fun unavailableReason(detail: String) =
        "This browser does not let Ember store data ($detail). Your data lives in this page only until it closes."

    private companion object {
        const val NEWER_BUILD =
            "A newer version of Ember saved data in another tab. Reload this page; changes made here are not saved."
    }
}
