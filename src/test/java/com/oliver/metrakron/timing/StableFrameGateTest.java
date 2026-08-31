package com.oliver.metrakron.timing;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StableFrameGateTest {
    @Test
    void requiresConsecutivePlayableFrames() {
        StableFrameGate gate = new StableFrameGate(3);

        assertFalse(gate.observe(true));
        assertFalse(gate.observe(true));
        assertFalse(gate.observe(false));
        assertFalse(gate.observe(true));
        assertFalse(gate.observe(true));
        assertTrue(gate.observe(true));
    }
}
