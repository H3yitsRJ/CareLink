package repositories

import com.example.carelink.model.CareTask
import com.google.firebase.firestore.FirebaseFirestore

class CareTaskRepository(
    private val firestore: FirebaseFirestore
) {
    fun getCareTask(
        patientId: String,
        onSuccess: (List<CareTask>) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(patientId)
            .collection("careTasks")
            .get()
            .addOnSuccessListener { snapshot ->
                val tasks =
                    snapshot.documents.mapNotNull { document ->
                        CareTask.fromFirestore(
                            document.id,
                            document.data.orEmpty() +
                                    ("patientId" to patientId)
                        )
                    }

                onSuccess(tasks)
            }
            .addOnFailureListener {
                onError("We couldn't load care tasks. Please try agian.")
            }
    }

    fun createCareTask(
        task: CareTask,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(task.patientId)
            .collection("careTasks")
            .document(task.id)
            .set(task.toFirestore())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("We couldn't create your care task. Please try again.")
            }
    }

    fun updateCareTask(
        task: CareTask,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(task.patientId)
            .collection("careTasks")
            .document(task.id)
            .set(task.toFirestore())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("We couldn't update your care task. Please try again.")
            }
    }

    fun setTaskCompleted(
        patientId: String,
        taskId: String,
        completed: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(patientId)
            .collection("careTasks")
            .document(taskId)
            .update(
                mapOf(
                    "completed" to completed,
                    "status" to if (completed) "COMPLETED"
                    else "PENDING"
                )
            )
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("We couldn't update your task status. Please try again.")
            }
    }

    fun deleteCareTask(
        patientId: String,
        taskId: String,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(patientId)
            .collection("careTasks")
            .document(taskId)
            .delete()
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError("We couldn't delete your care task. Please try again.")
            }
    }
}