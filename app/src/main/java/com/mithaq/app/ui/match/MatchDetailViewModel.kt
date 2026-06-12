package com.mithaq.app.ui.match

import androidx.lifecycle.ViewModel
import com.mithaq.app.analytics.AppAnalytics
import com.mithaq.app.data.repository.BlockRepository
import com.mithaq.app.data.repository.ReportRepository
import com.mithaq.app.data.repository.SafetyWriteResult

class MatchDetailViewModel(
    private val blockRepository: BlockRepository = BlockRepository(),
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    init {
        // The VM is created when the match detail screen opens, so this == "match viewed".
        AppAnalytics.matchViewed()
    }

    suspend fun blockUser(blockerId: String, blockedId: String): SafetyWriteResult =
        blockRepository.blockUser(blockerId, blockedId)

    suspend fun reportUser(reporterId: String, reportedId: String): SafetyWriteResult =
        reportRepository.reportUser(
            reporterUserId = reporterId,
            reportedUserId = reportedId,
            chatId = "",
            reason = "Spam / Inappropriate behavior",
            details = ""
        )
}
