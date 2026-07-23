package com.napxstream.ui.settings

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.napxstream.BuildConfig
import com.napxstream.R
import com.napxstream.XtreamApp
import com.napxstream.admin.AdminServerService
import com.napxstream.databinding.FragmentSettingsBinding
import com.napxstream.ui.login.LoginActivity
import com.napxstream.util.ParentalControlManager
import com.napxstream.util.PinDialogHelper
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) startAdminServer() else Toast.makeText(requireContext(), "Bildirim izni olmadan sunucu bildirimi gösterilemez", Toast.LENGTH_SHORT).show()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val app = requireActivity().application as XtreamApp
        val account = app.prefsManager.getAccount()

        binding.versionText.text = "${getString(R.string.app_name)} · v${BuildConfig.VERSION_NAME}"

        if (app.prefsManager.isM3uSource()) {
            binding.serverText.text = app.prefsManager.getM3uUrl() ?: "—"
            binding.usernameText.text = "M3U Playlist"
            binding.expiryText.text = "—"
            binding.refreshPlaylistText.visibility = View.VISIBLE
            binding.refreshPlaylistText.setOnClickListener {
                val url = app.prefsManager.getM3uUrl() ?: return@setOnClickListener
                viewLifecycleOwner.lifecycleScope.launch {
                    val result = app.m3uRepository.ensureLoaded(url, forceRefresh = true)
                    result.onSuccess {
                        Toast.makeText(requireContext(), R.string.playlist_refreshed, Toast.LENGTH_SHORT).show()
                    }.onFailure {
                        Toast.makeText(requireContext(), it.message ?: "Yenileme başarısız", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else if (account == null) {
            binding.serverText.text = "—"
            binding.usernameText.text = "—"
            binding.expiryText.text = "—"
        } else {
            binding.serverText.text = account.baseUrl
            binding.usernameText.text = account.username
            binding.expiryText.text = "…"

            // Güncel hesap durumunu (bitiş tarihi vb.) sunucudan tazele
            viewLifecycleOwner.lifecycleScope.launch {
                val result = app.repository.login(account)
                result.onSuccess { response ->
                    val expRaw = response.userInfo?.expDate
                    binding.expiryText.text = formatExpiry(expRaw)
                }.onFailure {
                    binding.expiryText.text = getString(R.string.empty_list)
                }
            }
        }

        binding.logoutButton.setOnClickListener { confirmLogout(app) }

        setupParentalControls(app)
        setupAdminServer(app)

        binding.clearCacheText.setOnClickListener {
            viewLifecycleOwner.lifecycleScope.launch {
                Glide.get(requireContext()).clearMemory()
                withContext(Dispatchers.IO) {
                    Glide.get(requireContext()).clearDiskCache()
                }
                Toast.makeText(requireContext(), R.string.cache_cleared, Toast.LENGTH_SHORT).show()
            }
        }

        binding.tmdbApiKeyInput.setText(app.prefsManager.getTmdbApiKey() ?: "")
        binding.saveTmdbKeyButton.setOnClickListener {
            val key = binding.tmdbApiKeyInput.text.toString().trim()
            if (key.isBlank()) {
                app.prefsManager.clearTmdbApiKey()
            } else {
                app.prefsManager.setTmdbApiKey(key)
            }
            Toast.makeText(requireContext(), R.string.tmdb_key_saved, Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatExpiry(expRaw: String?): String {
        val seconds = expRaw?.toLongOrNull() ?: return getString(R.string.empty_list)
        val sdf = SimpleDateFormat("dd.MM.yyyy", Locale.getDefault())
        return sdf.format(Date(seconds * 1000))
    }

    private fun setupParentalControls(app: XtreamApp) {
        val prefs = app.prefsManager

        binding.parentalLockSwitch.isChecked = prefs.isParentalLockEnabled()
        binding.showAdultSwitch.isChecked = ParentalControlManager.unlockedThisSession
        binding.showAdultSwitch.isEnabled = prefs.isParentalLockEnabled()

        binding.parentalLockSwitch.setOnCheckedChangeListener { switchView, isChecked ->
            if (isChecked) {
                if (prefs.hasParentalPin()) {
                    prefs.setParentalLockEnabled(true)
                    binding.showAdultSwitch.isEnabled = true
                } else {
                    PinDialogHelper.showSetPinDialog(
                        requireContext(), prefs,
                        onSuccess = { binding.showAdultSwitch.isEnabled = true },
                        onCancel = { switchView.isChecked = false }
                    )
                }
            } else {
                // Kilidi kapatmak için mevcut PIN'i doğrulat (çocukların kapatmasını zorlaştırır)
                if (prefs.hasParentalPin()) {
                    PinDialogHelper.showEnterPinDialog(
                        requireContext(), prefs,
                        onSuccess = {
                            prefs.setParentalLockEnabled(false)
                            binding.showAdultSwitch.isEnabled = false
                            binding.showAdultSwitch.isChecked = false
                            ParentalControlManager.lock()
                        },
                        onCancel = { switchView.isChecked = true }
                    )
                } else {
                    prefs.setParentalLockEnabled(false)
                    binding.showAdultSwitch.isEnabled = false
                }
            }
        }

        binding.showAdultSwitch.setOnCheckedChangeListener { switchView, isChecked ->
            if (isChecked) {
                PinDialogHelper.showEnterPinDialog(
                    requireContext(), prefs,
                    onSuccess = { ParentalControlManager.unlock() },
                    onCancel = { switchView.isChecked = false }
                )
            } else {
                ParentalControlManager.lock()
            }
        }
    }

    private fun setupAdminServer(app: XtreamApp) {
        val prefs = app.prefsManager

        // Sunucu varsayılan olarak etkin ve otomatik başlar (bkz. XtreamApp.onCreate).
        // Şifre opsiyoneldir: kullanıcı boş bırakırsa panel yerel ağda şifresiz çalışır.
        binding.adminPasswordInput.setText(prefs.getAdminPassword() ?: "")
        binding.adminPortInput.setText(prefs.getAdminPort().toString())
        binding.adminEnableSwitch.isChecked = prefs.isAdminServerEnabled()
        updateAdminAddressText(prefs)

        if (prefs.isAdminServerEnabled()) {
            requestNotificationPermissionThenStart()
        }

        binding.saveAdminPasswordButton.setOnClickListener {
            val password = binding.adminPasswordInput.text.toString().trim()
            val port = binding.adminPortInput.text.toString().toIntOrNull() ?: 8090
            prefs.setAdminPort(port)
            if (password.isBlank()) {
                prefs.clearAdminPassword()
                Toast.makeText(requireContext(), R.string.admin_password_removed, Toast.LENGTH_SHORT).show()
            } else {
                prefs.setAdminPassword(password)
                Toast.makeText(requireContext(), R.string.admin_password_saved, Toast.LENGTH_SHORT).show()
            }
            if (prefs.isAdminServerEnabled()) startAdminServer() // portu/şifreyi yeni değerle uygula
        }

        binding.adminEnableSwitch.setOnCheckedChangeListener { _, isChecked ->
            prefs.setAdminServerEnabled(isChecked)
            if (isChecked) {
                val port = binding.adminPortInput.text.toString().toIntOrNull() ?: 8090
                prefs.setAdminPort(port)
                requestNotificationPermissionThenStart()
            } else {
                AdminServerService.stop(requireContext())
                Toast.makeText(requireContext(), R.string.admin_stopped, Toast.LENGTH_SHORT).show()
                binding.adminAddressText.visibility = View.GONE
            }
        }
    }

    private fun requestNotificationPermissionThenStart() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startAdminServer()
        }
    }

    private fun startAdminServer() {
        val app = requireActivity().application as XtreamApp
        AdminServerService.start(requireContext())
        Toast.makeText(requireContext(), R.string.admin_started, Toast.LENGTH_SHORT).show()
        updateAdminAddressText(app.prefsManager)
    }

    private fun updateAdminAddressText(prefs: com.napxstream.util.PrefsManager) {
        if (!prefs.isAdminServerEnabled()) {
            binding.adminAddressText.visibility = View.GONE
            return
        }
        val ip = AdminServerService.getLocalIpAddress() ?: "cihaz-ip-adresi"
        binding.adminAddressText.text = "${getString(R.string.admin_address_label)} http://$ip:${prefs.getAdminPort()}"
        binding.adminAddressText.visibility = View.VISIBLE
    }

    private fun confirmLogout(app: XtreamApp) {
        AlertDialog.Builder(requireContext())
            .setTitle(R.string.logout_confirm_title)
            .setMessage(R.string.logout_confirm_message)
            .setPositiveButton(R.string.logout) { _, _ ->
                app.prefsManager.clearAccount()
                val intent = Intent(requireContext(), LoginActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                startActivity(intent)
                requireActivity().finish()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
