package com.napxstream.util

import android.app.AlertDialog
import android.content.Context
import android.text.InputType
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import com.napxstream.R

/**
 * Ebeveyn kilidi PIN belirleme / doğrulama diyalogları.
 * Basit tutmak için EncryptedSharedPreferences üzerinde düz metin PIN saklanır
 * (dosyanın kendisi zaten AES-256 ile şifreli).
 */
object PinDialogHelper {

    fun showSetPinDialog(context: Context, prefs: PrefsManager, onSuccess: () -> Unit, onCancel: () -> Unit = {}) {
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 24, 48, 0)
        }
        val pinInput = EditText(context).apply {
            hint = context.getString(R.string.pin_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
        }
        val confirmInput = EditText(context).apply {
            hint = context.getString(R.string.pin_confirm_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
        }
        container.addView(pinInput)
        container.addView(confirmInput)

        AlertDialog.Builder(context)
            .setTitle(R.string.set_pin_title)
            .setMessage(R.string.set_pin_message)
            .setView(container)
            .setPositiveButton(R.string.ok) { _, _ ->
                val pin = pinInput.text.toString()
                val confirm = confirmInput.text.toString()
                when {
                    pin.length != 4 -> Toast.makeText(context, R.string.pin_too_short, Toast.LENGTH_SHORT).show()
                    pin != confirm -> Toast.makeText(context, R.string.pin_mismatch, Toast.LENGTH_SHORT).show()
                    else -> {
                        prefs.setParentalPin(pin)
                        prefs.setParentalLockEnabled(true)
                        onSuccess()
                    }
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
            .setCancelable(false)
            .show()
    }

    fun showEnterPinDialog(context: Context, prefs: PrefsManager, onSuccess: () -> Unit, onCancel: () -> Unit = {}) {
        val pinInput = EditText(context).apply {
            hint = context.getString(R.string.pin_hint)
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            filters = arrayOf(android.text.InputFilter.LengthFilter(4))
            setPadding(48, 24, 48, 0)
        }

        AlertDialog.Builder(context)
            .setTitle(R.string.enter_pin_title)
            .setView(pinInput)
            .setPositiveButton(R.string.ok) { _, _ ->
                if (prefs.verifyParentalPin(pinInput.text.toString())) {
                    onSuccess()
                } else {
                    Toast.makeText(context, R.string.pin_incorrect, Toast.LENGTH_SHORT).show()
                    onCancel()
                }
            }
            .setNegativeButton(R.string.cancel) { _, _ -> onCancel() }
            .setCancelable(false)
            .show()
    }
}
