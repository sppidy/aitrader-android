package com.sppidy.janus.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

object BiometricAuth {
    private const val AUTH_FLAGS =
        BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK

    fun canAuthenticate(context: Context): Int =
        BiometricManager.from(context).canAuthenticate(AUTH_FLAGS)

    fun isAvailable(context: Context): Boolean =
        canAuthenticate(context) == BiometricManager.BIOMETRIC_SUCCESS

    fun availabilityMessage(code: Int): String = when (code) {
        BiometricManager.BIOMETRIC_SUCCESS -> "Biometric authentication is available."
        BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> "No biometric enrolled on this device."
        BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> "Biometric hardware is not available."
        BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> "Biometric hardware is currently unavailable."
        BiometricManager.BIOMETRIC_ERROR_SECURITY_UPDATE_REQUIRED -> "Security update required for biometrics."
        BiometricManager.BIOMETRIC_ERROR_UNSUPPORTED -> "Biometric authentication is unsupported."
        BiometricManager.BIOMETRIC_STATUS_UNKNOWN -> "Biometric status is unknown."
        else -> "Biometric authentication is unavailable."
    }

    fun authenticate(
        activity: FragmentActivity,
        title: String,
        subtitle: String,
        negativeButtonText: String = "Cancel",
        onSuccess: () -> Unit,
        onFailure: (String) -> Unit,
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onSuccess()
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onFailure(errString.toString())
            }
        }
        val prompt = BiometricPrompt(activity, executor, callback)
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setAllowedAuthenticators(AUTH_FLAGS)
            .setNegativeButtonText(negativeButtonText)
            .build()
        prompt.authenticate(promptInfo)
    }
}
