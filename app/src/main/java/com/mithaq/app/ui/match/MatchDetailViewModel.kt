package com.mithaq.app.ui.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mithaq.app.analytics.AppAnalytics
import com.mithaq.app.data.repository.BlockRepository
import com.mithaq.app.data.repository.ReportRepository
import kotlinx.coroutines.launch

class MatchDetailViewModel(
    private val blockRepository: BlockRepository = BlockRepository(),
    private val reportRepository: ReportRepository = ReportRepository()
) : ViewModel() {

    init {
        // The VM is created when the match detail screen opens, so this == "match viewed".
        AppAnalytics.matchViewed()
    }

    fun blockUser(blockerId: String, blockedId: String) {
        viewModelScope.launch {
            blockRepository.blockUserDirect(blockerId, blockedId)
        }
    }

    fun reportUser(reporterId: String, reportedId: String) {
        viewModelScope.launch {
            reportRepository.reportUserDirect(reporterId, reportedId)
        }
    }
}
