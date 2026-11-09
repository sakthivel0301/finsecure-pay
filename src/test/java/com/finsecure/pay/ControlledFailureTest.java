package com.finsecure.pay;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.fail;
class ControlledFailureTest {
    @Test void controlledFailure() { fail("Deliberate failure to prove the pipeline blocks deployment"); }
}
