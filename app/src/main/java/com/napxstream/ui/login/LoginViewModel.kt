package com.napxstream.ui.login

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.napxstream.data.api.XtreamAccount
import com.napxstream.data.model.LoginResponse
import com.napxstream.data.repository.XtreamRepository
import com.napxstream.util.Resource
import kotlinx.coroutines.launch

class LoginViewModel(private val repository: XtreamRepository) : ViewModel() {

    private val _loginState = MutableLiveData<Resource<LoginResponse>>()
    val loginState: LiveData<Resource<LoginResponse>> = _loginState

    fun login(host: String, port: String, username: String, password: String, useHttps: Boolean) {
        if (host.isBlank() || username.isBlank() || password.isBlank()) {
            _loginState.value = Resource.Error("Lütfen sunucu adresi, kullanıcı adı ve şifreyi girin")
            return
        }

        val account = XtreamAccount(host.trim(), port.trim(), username.trim(), password.trim(), useHttps)
        _loginState.value = Resource.Loading

        viewModelScope.launch {
            val result = repository.login(account)
            result.onSuccess {
                _loginState.value = Resource.Success(it)
            }.onFailure {
                _loginState.value = Resource.Error(it.message ?: "Bilinmeyen hata")
            }
        }
    }
}
