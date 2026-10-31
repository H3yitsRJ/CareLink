package com.example.carelink.screens

import com.example.carelink.model.ConcernSeverity

internal val ConcernSeverity.displayLabel: String
    get() = when (this) {
        ConcernSeverity.LOW -> "Mild"
        ConcernSeverity.MEDIUM -> "Moderate"
        ConcernSeverity.HIGH -> "Severe"
    }
