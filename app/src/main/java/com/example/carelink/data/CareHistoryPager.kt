package com.example.carelink.data

import com.example.carelink.model.CareActivityType
import com.example.carelink.model.CareHistoryEntry
import com.example.carelink.model.DoseRecord
import com.example.carelink.model.DoseStatus
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.Source

data class CareHistoryPage(
    val entries: List<CareHistoryEntry>,
    val hasMore: Boolean
)

/**
 * One paging session for one patient.
 * Create a new instance to refresh the history.
 */
class CareHistoryPager(
    firestore: FirebaseFirestore,
    private val patientId: String,
    private val pageSize: Int = 20
) {
    private data class StreamState(
        val cursor: DocumentSnapshot? = null,
        val buffered: List<CareHistoryEntry> = emptyList(),
        val exhausted: Boolean = false
    )

    private val queries: List<Query>

    private var states = List(3) { StreamState() }
    private var busy = false

    init {
        require(patientId.isNotBlank() && '/' !in patientId)
        require(pageSize in 1..100)

        val patient = firestore.collection("users").document(patientId)

        queries = listOf(
            patient.collection("doseRecords")
                .orderBy(
                    "completionTimeMillis",
                    Query.Direction.DESCENDING
                )
                .orderBy(
                    FieldPath.documentId(),
                    Query.Direction.DESCENDING
                ),

            patient.collection("careHistory")
                .orderBy(
                    "occurredAtMillis",
                    Query.Direction.DESCENDING
                )
                .orderBy(
                    FieldPath.documentId(),
                    Query.Direction.DESCENDING
                ),

            patient.collection("careHistory")
                .orderBy(
                    "occurredAt",
                    Query.Direction.DESCENDING
                )
                .orderBy(
                    FieldPath.documentId(),
                    Query.Direction.DESCENDING
                )
        )
    }

    fun loadNext(
        completed: (Result<CareHistoryPage>) -> Unit
    ) {
        if (busy) {
            completed(
                Result.failure(
                    IllegalStateException("History is already loading.")
                )
            )
            return
        }

        busy = true

        val working = states.toMutableList()
        val output = mutableListOf<CareHistoryEntry>()
        var finished = false

        fun fail(exception: Exception) {
            if (finished) return
            finished = true
            busy = false
            completed(Result.failure(exception))
        }

        fun succeed() {
            if (finished) return
            finished = true
            states = working.toList()
            busy = false

            completed(
                Result.success(
                    CareHistoryPage(
                        entries = output.toList(),
                        hasMore = working.any {
                            it.buffered.isNotEmpty() || !it.exhausted
                        }
                    )
                )
            )
        }

        fun ensureHead(index: Int, ready: () -> Unit) {
            if (finished) return

            val state = working[index]

            if (state.buffered.isNotEmpty() || state.exhausted) {
                ready()
                return
            }

            var query = queries[index].limit(pageSize.toLong())

            state.cursor?.let {
                query = query.startAfter(it)
            }

            query.get(Source.SERVER)
                .addOnSuccessListener { snapshot ->
                    if (!finished) {
                        val documents = snapshot.documents

                        val entries = documents.mapNotNull { document ->
                            runCatching {
                                decode(index, document)
                            }.getOrNull()
                        }

                        working[index] = state.copy(
                            cursor = documents.lastOrNull() ?: state.cursor,
                            buffered = entries,
                            exhausted = documents.size < pageSize
                        )

                        ensureHead(index, ready)
                    }
                }
                .addOnFailureListener { exception ->
                    fail(exception)
                }
        }

        fun ensureHeads(index: Int, ready: () -> Unit) {
            if (finished) return

            if (index == queries.size) {
                ready()
            } else {
                ensureHead(index) {
                    ensureHeads(index + 1, ready)
                }
            }
        }

        fun collectNext() {
            ensureHeads(0) {
                if (output.size >= pageSize) {
                    succeed()
                } else {
                    val winner = working.indices
                        .filter { working[it].buffered.isNotEmpty() }
                        .minWithOrNull(
                            compareByDescending<Int> {
                                working[it].buffered.first().occurredAtMillis
                            }.thenByDescending {
                                working[it].buffered.first().id
                            }
                        )

                    if (winner == null) {
                        succeed()
                    } else {
                        val state = working[winner]
                        output.add(state.buffered.first())

                        working[winner] = state.copy(
                            buffered = state.buffered.drop(1)
                        )

                        collectNext()
                    }
                }
            }
        }

        collectNext()
    }

    private fun decode(
        stream: Int,
        document: DocumentSnapshot
    ): CareHistoryEntry? {
        val data = document.data ?: return null

        return when (stream) {
            0 -> {
                val record = DoseRecord.fromFirestore(
                    document.id,
                    data
                ) ?: return null

                if (record.status == DoseStatus.SCHEDULED) {
                    return null
                }

                val name = data["medicationName"] as? String
                    ?: "Medication unavailable"

                val dosage = data["dosage"] as? String
                    ?: "Dosage unavailable"

                val status = record.status.name.lowercase()
                    .replaceFirstChar(Char::uppercase)

                CareHistoryEntry(
                    id = "dose:${record.id}",
                    patientId = patientId,
                    occurredAtMillis = record.completionTimeMillis
                        ?: record.scheduledTimeMillis,
                    type = CareActivityType.MEDICATION,
                    summary = "$name • $dosage • $status",
                    actorId = (data["actorId"] as? String)
                        ?.takeIf { it.isNotBlank() },
                    relatedRecordId = record.medicationId
                )
            }

            1 -> {
                CareHistoryEntry.fromFirestore(document.id, data)
                    ?.takeIf { it.patientId == patientId }
            }

            else -> {
                if (CareHistoryEntry.fromFirestore(document.id, data) != null) {
                    null
                } else {
                    caregiverHistoryEntry(document.id, patientId, data)
                }
            }
        }
    }
}