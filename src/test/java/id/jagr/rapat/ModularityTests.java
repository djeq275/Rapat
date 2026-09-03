package id.jagr.rapat;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;

class ModularityTests {

    ApplicationModules modules = ApplicationModules.of(RapatApplication.class);

    @Test
    void verifiesModularStructure() {
        modules.verify();
    }
}
