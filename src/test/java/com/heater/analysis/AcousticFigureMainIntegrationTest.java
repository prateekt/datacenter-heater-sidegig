package com.heater.analysis;

import org.junit.jupiter.api.Test;

class AcousticFigureMainIntegrationTest {

    @Test
    void generateAcousticFiguresCompletesWithoutReadmePatch() throws Exception {
        AcousticFigureMain.main(new String[] {"--no-readme"});
    }
}
