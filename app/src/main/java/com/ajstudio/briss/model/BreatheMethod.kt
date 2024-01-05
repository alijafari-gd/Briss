package com.ajstudio.briss.model

data class BreatheMethod(
    val name: String,
    val inhaleSeconds: Int,
    val inhaleHoldSeconds: Int = 0,
    val exhaleSeconds: Int,
    val exhaleHoldSeconds: Int = 0,
    val description: String,
    val usage: String,
    val suggestion: String,
    val benefits: String
)