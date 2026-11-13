package com.example.data.repository

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.GameRoom
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class RoomRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun createRoom_authenticatedUser_success() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val repository = RoomRepository(firestore)

        val createResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repository.createRoom(hostName = "Alice", passcode = null)
        }
        assertTrue("Room creation should succeed", createResult.isSuccess)
        val roomId = createResult.getOrThrow()
        assertTrue("Room ID should not be blank", roomId.isNotBlank())
    }

    @Test
    fun joinRoom_secondUser_joinsAndSetsStatusPlaying() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = RoomRepository(firestore)
        val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createRoom(hostName = "Alice", passcode = "1234").getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = RoomRepository(firestore)
        val joinResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinRoom(roomIdInput = roomId, guestName = "Bob", passcode = "1234")
        }
        assertTrue("Join room should succeed", joinResult.isSuccess)
        val room = joinResult.getOrThrow()
        assertEquals("playing", room.status)
        assertEquals("Bob", room.guestName)
    }

    @Test
    fun makeMove_turnPlayer_updatesBoard() = runBlocking {
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = RoomRepository(firestore)
        val roomId = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.createRoom(hostName = "Alice", passcode = null).getOrThrow()
        }

        signInTestUser(BOB_EMAIL)
        val bobRepo = RoomRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            bobRepo.joinRoom(roomIdInput = roomId, guestName = "Bob", passcode = null).getOrThrow()
        }

        // Alice's turn (@)
        signInTestUser(ALICE_EMAIL)
        val newBoard = listOf("@", "", "", "", "", "", "", "", "")
        val moveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            aliceRepo.makeMove(
                roomId = roomId,
                newBoard = newBoard,
                fromIndex = -1,
                toIndex = 0,
                piece = "@",
                nextTurn = "#",
                nextPhase = "drop",
                atPlaced = 1L,
                hashPlaced = 0L,
                winner = "",
                winPattern = emptyList(),
                repeatCount = 0L
            )
        }
        assertTrue("Move should succeed", moveResult.isSuccess)
    }

    @Test
    fun observeRoom_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        val testRoomId = "TEST_" + UUID.randomUUID().toString().take(6).uppercase()
        // Alice creates room
        signInTestUser(ALICE_EMAIL)
        val aliceRepo = RoomRepository(firestore)
        aliceRepo.createRoom(hostName = "Alice", passcode = null)

        // Sign out to verify unauthenticated rejection
        auth.signOut()
        val unauthRepo = RoomRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) {
                unauthRepo.observeRoom(testRoomId).first()
            }
            fail("Expected exception due to missing auth")
        } catch (e: Exception) {
            val firestoreException = e.unwrapFirestoreException()
            assertNotNull("Expected root FirebaseFirestoreException, got: $e", firestoreException)
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, firestoreException?.code)
        }
    }

    private companion object {
        const val ALICE_EMAIL = "alice@atithash.com"
        const val BOB_EMAIL = "bob@atithash.com"
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
