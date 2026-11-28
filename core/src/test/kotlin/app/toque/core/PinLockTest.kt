package app.toque.core

import app.toque.core.machine.PinLock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PinLockTest {
    private var now = 1_000_000L
    private val lock = PinLock { now }
    private val stored = PinLock.create("2468")

    @Test fun onlyFourToEightDigits() {
        for (ok in listOf("1234", "00000000", "9081726")) assertTrue(ok, PinLock.isValid(ok))
        for (bad in listOf("", "123", "123456789", "12a4", "12 34", "١٢٣٤")) assertFalse(bad, PinLock.isValid(bad))
    }

    @Test fun theCodeIsNeverStoredInClear() {
        assertFalse(stored.hash.contains("2468"))
        assertEquals(64, stored.hash.length)
        // Même code, autre sel : autre empreinte.
        assertNotEquals(stored.hash, PinLock.create("2468").hash)
    }

    @Test fun withoutACodeNothingIsAsked() {
        val none = PinLock.Stored("", "")
        assertFalse(lock.needsCode(none))
        assertEquals(PinLock.Result.OK, lock.check("", none))
    }

    @Test fun theRightCodeOpensForFiveMinutes() {
        assertTrue(lock.needsCode(stored))
        assertEquals(PinLock.Result.OK, lock.check("2468", stored))
        assertFalse(lock.needsCode(stored))
        now += PinLock.UNLOCK_MS - 1
        assertFalse(lock.needsCode(stored))
        now += 1
        assertTrue(lock.needsCode(stored))
        assertEquals(PinLock.Result.OK, lock.check("2468", stored))
        lock.relock()
        assertTrue(lock.needsCode(stored))
    }

    @Test fun wrongCodesLockForLongerAndLonger() {
        repeat(PinLock.MAX_FAILURES - 1) { assertEquals(PinLock.Result.WRONG, lock.check("0000", stored)) }
        assertEquals(PinLock.Result.LOCKED, lock.check("0000", stored))
        // Même le bon code attend.
        assertEquals(PinLock.Result.LOCKED, lock.check("2468", stored))
        assertEquals(60_000L, lock.lockedForMs)
        now += 60_000
        repeat(PinLock.MAX_FAILURES - 1) { assertEquals(PinLock.Result.WRONG, lock.check("1111", stored)) }
        assertEquals(PinLock.Result.LOCKED, lock.check("1111", stored))
        assertEquals(120_000L, lock.lockedForMs)
        now += 120_000
        assertEquals(PinLock.Result.OK, lock.check("2468", stored))
        // Après un code juste, on repart de 1 min.
        lock.relock()
        repeat(PinLock.MAX_FAILURES) { lock.check("9999", stored) }
        assertEquals(60_000L, lock.lockedForMs)
    }

    @Test fun lockNeverExceedsThirtyMinutes() {
        repeat(20) {
            repeat(PinLock.MAX_FAILURES) { lock.check("0000", stored) }
            assertTrue(lock.lockedForMs <= PinLock.MAX_LOCK_MS)
            now += lock.lockedForMs
        }
    }
}
