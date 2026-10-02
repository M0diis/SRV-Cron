package me.m0dii.srvcron.testsupport;

import org.bukkit.configuration.file.FileConfiguration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

public abstract class MockBukkitTest {
    protected ServerMock server;
    protected TestSRVCron plugin;

    @BeforeEach
    protected void startMockServer() {
        server = MockBukkit.mock();
        plugin = MockBukkit.load(TestSRVCron.class);
        clearJobSections(plugin.getConfig());
    }

    @AfterEach
    protected void stopMockServer() {
        MockBukkit.unmock();
    }

    protected static void clearJobSections(FileConfiguration config) {
        config.set("jobs", null);
        config.set("event-jobs", null);
        config.set("generic-event-jobs", null);
        config.set("startup.commands", null);
    }
}
