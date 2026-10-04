package com.example.carelink.repositories

import com.example.carelink.model.Appointment
import com.example.carelink.model.AppointmentStatus
import com.google.firebase.firestore.FirebaseFirestore

class AppointmentRepository (
    private val firestore: FirebaseFirestore
){
    fun getAppointments(
        userId: String,
        onSuccess: (List<Appointment>) -> Unit,
        onError: (String) -> Unit
    ) {
        firestore
            .collection("users")
            .document(userId)
            .collection("appointments")
            .get()
            .addOnSuccessListener { snapshot ->
                val appointments =
                    snapshot.documents.map { document ->
                        Appointment.fromFirestore(
                            id = document.id,
                            data = document.data.orEmpty()
                        )
                    }
                onSuccess(appointments)
            }
            .addOnFailureListener {
                onError(
                    "We couldn't load your appointments. Please try again."
                )
            }
    }

    fun saveAppointment(
        userId: String,
        appointment: Appointment,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val appointmentsRef = firestore
            .collection("users")
            .document(userId)
            .collection("appointments")

        val appointmentId = appointment.id.ifBlank {
            appointmentsRef.document().id
        }

        val appointmentToSave = appointment.copy(
            id = appointmentId,
            patientId = userId
        )

        appointmentsRef
            .document(appointmentId)
            .set(appointmentToSave.toFirestore())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(
                    "Unable to save appointment."
                )
            }
    }

    fun cancelAppointment(
        userId: String,
        appointment: Appointment,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cancelledAppointment = appointment.copy(
            status = AppointmentStatus.CANCELLED
        )

        firestore
            .collection("users")
            .document(userId)
            .collection("appointments")
            .document(appointment.id)
            .set(cancelledAppointment.toFirestore())
            .addOnSuccessListener {
                onSuccess()
            }
            .addOnFailureListener {
                onError(
                    "Unable to cancel appointment."
                )
            }
    }
}