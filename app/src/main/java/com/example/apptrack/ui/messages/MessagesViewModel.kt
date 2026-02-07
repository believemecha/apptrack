package com.example.apptrack.ui.messages

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class MessagesViewModel : ViewModel() {

    private val _messages = MutableStateFlow<List<Message>>(emptyList())
    val messages: StateFlow<List<Message>> = _messages.asStateFlow()

    private var nextId = System.currentTimeMillis()

    fun addMessage(phoneNumber: String, contactName: String?, body: String) {
        val now = System.currentTimeMillis()
        val msg = Message(
            id = nextId++,
            phoneNumber = phoneNumber.trim(),
            contactName = contactName?.trim()?.takeIf { it.isNotBlank() },
            body = body.trim(),
            createdAt = now,
            updatedAt = now
        )
        _messages.update { it + msg }
    }

    fun updateMessage(id: Long, phoneNumber: String, contactName: String?, body: String) {
        val now = System.currentTimeMillis()
        _messages.update { list ->
            list.map { msg ->
                if (msg.id == id) msg.copy(
                    phoneNumber = phoneNumber.trim(),
                    contactName = contactName?.trim()?.takeIf { it.isNotBlank() },
                    body = body.trim(),
                    updatedAt = now
                ) else msg
            }
        }
    }

    fun deleteMessage(id: Long) {
        _messages.update { it.filter { msg -> msg.id != id } }
    }

    fun getMessageById(id: Long): Message? = _messages.value.find { it.id == id }
}
