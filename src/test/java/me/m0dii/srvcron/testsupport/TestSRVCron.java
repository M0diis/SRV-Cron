package me.m0dii.srvcron.testsupport;

import me.m0dii.srvcron.SRVCron;

/** A plugin fixture that avoids production startup work during unit tests. */
public class TestSRVCron extends SRVCron {
    @Override
    public void onEnable() {
        // Tests configure jobs and instantiate managers explicitly.
    }
}
