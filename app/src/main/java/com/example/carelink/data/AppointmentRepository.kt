package com.example.carelink.data

import com.example.carelink.model.Appointment

interface AppointmentRepository {
    fun create(appointment: Appointment): Result<Appointment>
    fun get(id: String): Appointment?
    fun list(patientId: String): List<Appointment>
}

class InMemoryAppointmentRepository : AppointmentRepository {

    private val appointments = linkedMapOf<String, Appointment>()

    override fun create(appointment: Appointment): Result<Appointment> {
        appointments[appointment.id] = appointment
        return Result.success(appointment)
    }

    override fun get(id: String): Appointment? = appointments[id]

    override fun list(patientId: String): List<Appointment> =
        appointments.values.filter { it.patientId == patientId }
}