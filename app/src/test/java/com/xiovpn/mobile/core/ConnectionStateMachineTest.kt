package com.xiovpn.mobile.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ConnectionStateMachineTest {
    @Test
    fun connectAndDisconnectAreGuardedAndIdempotent() {
        val machine = ConnectionStateMachine()

        assertTrue(machine.tryBeginConnect())
        assertFalse(machine.tryBeginConnect())
        assertTrue(machine.markConnected())
        // Idempotent: PoW coordinator and VPN service both publish Connected;
        // a duplicate markConnected must be a no-op success, not a failure.
        assertTrue(machine.markConnected())
        assertTrue(machine.tryBeginDisconnect())
        assertFalse(machine.tryBeginDisconnect())
        machine.markDisconnected()
        assertEquals(ConnectionState.DISCONNECTED, machine.state.value)
    }

    @Test
    fun connectedCountrySwitchGoesThroughConnectingAgain() {
        val machine = ConnectionStateMachine()
        assertTrue(machine.tryBeginConnect())
        assertTrue(machine.markConnected())
        machine.markConnecting()
        assertEquals(ConnectionState.CONNECTING, machine.state.value)
        assertTrue(machine.markConnected())
        assertEquals(ConnectionState.CONNECTED, machine.state.value)
    }

    @Test
    fun errorCanRetryButCannotBecomeConnectedWithoutConnecting() {
        val machine = ConnectionStateMachine()
        machine.markError()
        assertFalse(machine.markConnected())
        assertTrue(machine.tryBeginConnect())
        assertTrue(machine.markConnected())
    }
}
