package com.maxrave.data.loginsync

import com.maxrave.domain.data.model.loginsync.DesktopInfo
import com.maxrave.domain.data.model.loginsync.LoginSyncService
import com.maxrave.domain.repository.LoginSyncSenderRepository

internal class LoginSyncSenderRepositoryImpl : LoginSyncSenderRepository {
    override suspend fun signedInServices(): List<LoginSyncService> = emptyList()
    override fun isInvite(qrText: String): Boolean = false
    override suspend fun connect(qrText: String): DesktopInfo = DesktopInfo("", "")
    override suspend fun send(services: Set<LoginSyncService>): List<LoginSyncService> = emptyList()
    override fun disconnect() {}
}
