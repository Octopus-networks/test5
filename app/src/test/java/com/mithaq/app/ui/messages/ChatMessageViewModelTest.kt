package com.mithaq.app.ui.messages

import com.google.firebase.firestore.ListenerRegistration
import com.mithaq.app.data.repository.ChatMessageRepository
import com.mithaq.app.data.repository.MessagePage
import com.mithaq.app.domain.model.ChatMessage
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.Date

/**
 * Tests the pagination/accumulation logic added in the chat-pagination change: the live window is
 * merged with paged-in older history, deduped, and kept ordered by createdAt — including the case
 * where a new message arrives after the user has paged older history.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatMessageViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun msg(id: String, epochMillis: Long): ChatMessage =
        ChatMessage(messageId = id, chatId = CHAT_ID, senderId = "u", text = id, createdAt = Date(epochMillis))

    private fun fullWindow(): List<ChatMessage> =
        (1..ChatMessageRepository.PAGE_SIZE).map { msg("m$it", it * 1000L) }

    private fun viewModelWith(repo: ChatMessageRepository): ChatMessageViewModel =
        ChatMessageViewModel(repo)

    @Test
    fun liveWindowIsExposedOldestFirstAndStopsLoading() {
        val repo = mockk<ChatMessageRepository>(relaxed = true)
        val onMessages = slot<(List<ChatMessage>) -> Unit>()
        every { repo.listenToMessages(any(), capture(onMessages), any()) } returns mockk<ListenerRegistration>(relaxed = true)

        val vm = viewModelWith(repo)
        vm.listenToMessages(CHAT_ID)
        // Repository may emit in any order; the VM must sort ascending by createdAt.
        onMessages.captured.invoke(listOf(msg("m2", 2000), msg("m1", 1000)))

        val state = vm.state.value
        assertEquals(listOf("m1", "m2"), state.messages.map { it.messageId })
        assertFalse(state.isLoading)
    }

    @Test
    fun shortFirstWindowDisablesOlderPaging() {
        val repo = mockk<ChatMessageRepository>(relaxed = true)
        val onMessages = slot<(List<ChatMessage>) -> Unit>()
        every { repo.listenToMessages(any(), capture(onMessages), any()) } returns mockk<ListenerRegistration>(relaxed = true)

        val vm = viewModelWith(repo)
        vm.listenToMessages(CHAT_ID)
        onMessages.captured.invoke(listOf(msg("m1", 1000)))

        assertFalse(vm.state.value.hasMoreOlder)
    }

    @Test
    fun loadOlderPrependsHistoryAndUpdatesHasMore() = runTest(dispatcher) {
        val repo = mockk<ChatMessageRepository>(relaxed = true)
        val onMessages = slot<(List<ChatMessage>) -> Unit>()
        every { repo.listenToMessages(any(), capture(onMessages), any()) } returns mockk<ListenerRegistration>(relaxed = true)
        coEvery { repo.loadOlderMessages(CHAT_ID, "m1") } returns MessagePage(listOf(msg("m0", 500)), hasMore = false)

        val vm = viewModelWith(repo)
        vm.listenToMessages(CHAT_ID)
        onMessages.captured.invoke(fullWindow()) // full page -> hasMoreOlder = true

        vm.loadOlderMessages(CHAT_ID)
        advanceUntilIdle()

        val ids = vm.state.value.messages.map { it.messageId }
        assertEquals("m0", ids.first())
        assertFalse(vm.state.value.hasMoreOlder)
        assertFalse(vm.state.value.isLoadingOlder)
    }

    @Test
    fun newMessageAfterPaginationDoesNotDropEarlierMessages() = runTest(dispatcher) {
        val repo = mockk<ChatMessageRepository>(relaxed = true)
        val onMessages = slot<(List<ChatMessage>) -> Unit>()
        every { repo.listenToMessages(any(), capture(onMessages), any()) } returns mockk<ListenerRegistration>(relaxed = true)
        coEvery { repo.loadOlderMessages(CHAT_ID, "m1") } returns MessagePage(listOf(msg("m0", 500)), hasMore = false)

        val vm = viewModelWith(repo)
        vm.listenToMessages(CHAT_ID)
        onMessages.captured.invoke(fullWindow())   // m1..mN
        vm.loadOlderMessages(CHAT_ID)              // adds m0
        advanceUntilIdle()

        // The realtime window shifts forward: m1 leaves the newest page, a new message arrives.
        val size = ChatMessageRepository.PAGE_SIZE
        val shifted = (2..size).map { msg("m$it", it * 1000L) } + msg("m${size + 1}", (size + 1) * 1000L)
        onMessages.captured.invoke(shifted)

        val ids = vm.state.value.messages.map { it.messageId }
        assertTrue("m0 (paged older) retained", "m0" in ids)
        assertTrue("m1 must not be dropped when it leaves the live window", "m1" in ids)
        assertTrue("new message m${size + 1} arrives", "m${size + 1}" in ids)
        // Still ordered oldest-first by createdAt.
        assertEquals(ids.sortedBy { it.removePrefix("m").toInt() }, ids)
    }

    private companion object {
        const val CHAT_ID = "chat-1"
    }
}
