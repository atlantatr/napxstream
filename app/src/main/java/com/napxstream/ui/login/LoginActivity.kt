package com.napxstream.ui.login

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.napxstream.XtreamApp
import com.napxstream.admin.AdminServerService
import com.napxstream.data.api.XtreamAccount
import com.napxstream.databinding.ActivityLoginBinding
import com.napxstream.ui.main.MainActivity
import com.napxstream.util.Constants
import com.napxstream.util.QrCodeGenerator
import com.napxstream.util.Resource
import com.napxstream.util.ViewModelFactory
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private lateinit var viewModel: LoginViewModel

    /** true: M3U sekmesi seçili, false: Xtream Codes sekmesi seçili */
    private var isM3uTabSelected = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val app = application as XtreamApp

        // Zaten kayıtlı hesap varsa doğrudan ana ekrana geç
        if (app.prefsManager.isLoggedIn()) {
            goToMain()
            return
        }

        setupAdminPanelInfo(app)

        viewModel = ViewModelProvider(
            this,
            ViewModelFactory(app.repository) { LoginViewModel(it) }
        )[LoginViewModel::class.java]

        binding.sourceTabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                isM3uTabSelected = tab?.position == 1
                binding.xtreamFieldsGroup.visibility = if (isM3uTabSelected) View.GONE else View.VISIBLE
                binding.m3uFieldsGroup.visibility = if (isM3uTabSelected) View.VISIBLE else View.GONE
                binding.errorText.visibility = View.GONE
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        binding.loginButton.setOnClickListener {
            if (isM3uTabSelected) submitM3u(app) else submitXtream()
        }

        viewModel.loginState.observe(this) { state ->
            when (state) {
                is Resource.Loading -> setLoading(true)
                is Resource.Success -> {
                    setLoading(false)
                    val account = XtreamAccount(
                        host = binding.hostInput.text.toString().trim(),
                        port = binding.portInput.text.toString().trim(),
                        username = binding.usernameInput.text.toString().trim(),
                        password = binding.passwordInput.text.toString().trim(),
                        useHttps = binding.httpsCheckbox.isChecked
                    )
                    app.prefsManager.saveAccount(account)
                    goToMain()
                }
                is Resource.Error -> {
                    setLoading(false)
                    showError(state.message)
                }
            }
        }
    }

    /** Login ekranında panelin IP:port adresini ve tarayarak açılabilecek QR kodunu gösterir. */
    private fun setupAdminPanelInfo(app: XtreamApp) {
        if (!app.prefsManager.isAdminServerEnabled()) {
            binding.adminPanelSection.visibility = View.GONE
            return
        }
        val ip = AdminServerService.getLocalIpAddress()
        if (ip == null) {
            binding.adminPanelSection.visibility = View.GONE
            return
        }
        val url = "http://$ip:${app.prefsManager.getAdminPort()}"
        binding.loginAdminAddressText.text = url
        binding.loginAdminQrCode.setImageBitmap(QrCodeGenerator.generate(url))
    }

    private fun submitXtream() {
        viewModel.login(
            host = binding.hostInput.text.toString(),
            port = binding.portInput.text.toString(),
            username = binding.usernameInput.text.toString(),
            password = binding.passwordInput.text.toString(),
            useHttps = binding.httpsCheckbox.isChecked
        )
    }

    private fun submitM3u(app: XtreamApp) {
        val url = binding.m3uUrlInput.text.toString().trim()
        val epgUrl = binding.m3uEpgUrlInput.text.toString().trim()
        if (url.isBlank()) {
            showError(getString(com.napxstream.R.string.m3u_url_required))
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            val result = app.m3uRepository.ensureLoaded(url, forceRefresh = true)
            setLoading(false)
            result.onSuccess {
                app.prefsManager.saveM3uSource(url, epgUrl.ifBlank { null })
                goToMain()
            }.onFailure { error ->
                showError(error.message ?: "M3U playlist yüklenemedi")
            }
        }
    }

    private fun setLoading(loading: Boolean) {
        binding.loadingProgress.visibility = if (loading) View.VISIBLE else View.GONE
        if (loading) binding.errorText.visibility = View.GONE
        binding.loginButton.isEnabled = !loading
        binding.sourceTabs.isEnabled = !loading
    }

    private fun showError(message: String) {
        binding.errorText.visibility = View.VISIBLE
        binding.errorText.text = message
    }

    private fun goToMain() {
        startActivity(Intent(this, MainActivity::class.java).apply {
            putExtra(Constants.EXTRA_SHORTCUT_TARGET, shortcutTargetFromAction())
        })
        finish()
    }

    /** Uygulama kısayolundan (launcher'da uzun basma menüsü) gelindiyse hedef sekmeyi döndürür. */
    private fun shortcutTargetFromAction(): String? = when (intent?.action) {
        "com.napxstream.action.SHORTCUT_LIVE" -> "live"
        "com.napxstream.action.SHORTCUT_VOD" -> "vod"
        "com.napxstream.action.SHORTCUT_SEARCH" -> "search"
        else -> null
    }
}
