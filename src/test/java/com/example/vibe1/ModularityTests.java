package com.example.vibe1;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(Vibe1Application.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
