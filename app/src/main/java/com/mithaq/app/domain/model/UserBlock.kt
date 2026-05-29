package com.mithaq.app.domain.model

import java.util.Date

data class UserBlock(
    val blockId: String = "",
    val blockerUserId: String = "",
    val blockedUserId: String = "",
    val chatId: String = "",
    val createdAt: Date? = null,
    val updatedAt: Date? = null
)
