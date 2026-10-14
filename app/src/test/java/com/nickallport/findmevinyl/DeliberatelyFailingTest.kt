package com.nickallport.findmevinyl

import org.junit.Assert.fail
import org.junit.Test

// Throwaway test to check that a failing run blocks the merge. Never merge this.
class DeliberatelyFailingTest {
    @Test
    fun failsOnPurpose() {
        fail("Deliberate failure to test the required status check")
    }
}
