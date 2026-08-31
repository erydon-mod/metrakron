package com.oliver.metrakron.overlay;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OverlayStateFileTest {
    @TempDir
    Path temporaryDirectory;

    @Test
    void roundTripsACompleteLocalizedStateAtomically() throws Exception {
        Path stateFile = temporaryDirectory.resolve("metrakron").resolve("window-state.properties");
        OverlayState expected = new OverlayState(
                OverlayState.CURRENT_PROTOCOL,
                "test-session",
                1234L,
                8L,
                true,
                2L,
                7_500L,
                11_000L,
                "WELT BEREIT IN",
                "LADEZEIT WIRD NEU GELERNT",
                "WELTDATEN UND PAKETE WERDEN GELESEN",
                12,
                34,
                432,
                152
        );

        OverlayStateFile.write(stateFile, expected);

        assertEquals(expected, OverlayStateFile.read(stateFile));
    }
}
