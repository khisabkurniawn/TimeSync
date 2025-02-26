package com.arknesia.timesync;

import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.scheduler.ScheduledTask;
import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import com.moandjiezana.toml.TomlWriter;

import java.io.*;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import com.velocitypowered.api.proxy.server.RegisteredServer;

@Plugin(
        id = "timesync",
        name = "TimeSync",
        version = "${project.version}",
        url = "https://arknesia.com",
        description = "Sync Minecraft World Time",
        authors = {"Lexivale"}
)
public class TimeSync {
    private final ProxyServer proxy;
    private final Logger logger;
    private final File configFile;
    private final TimeManager timeManager;
    private ScheduledTask syncTask;
    private static final long TICKS_PER_DAY = 24000;
    private static final long TICKS_PER_REAL_SECOND = 20;
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from("timesync:time");

    @Inject
    public TimeSync(ProxyServer proxy, Logger logger) {
        this.proxy = proxy;
        this.logger = logger;
        this.configFile = new File("plugins/TimeSync/time.toml");
        this.timeManager = new TimeManager(this.configFile, this.logger);
    }

    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("[TimeSync] Plugin aktif! Sinkronisasi waktu dimulai...");

        proxy.getChannelRegistrar().register(CHANNEL);

        timeManager.loadTime();
        logger.info("Waktu dimulai pada tick: " + timeManager.getCurrentTicks());

        syncTask = proxy.getScheduler()
                .buildTask(this, new SyncTask(proxy, timeManager, logger))
                .repeat(1, TimeUnit.SECONDS)
                .schedule();
    }

    private static class TimeManager {
        private final File configFile;
        private final Logger logger;
        private long currentTick;
        private long totalDays;
        private int currentYear;
        private int currentMonth;
        private int currentDay;

        public TimeManager(File configFile, Logger logger) {
            this.configFile = configFile;
            this.logger = logger;
            this.totalDays = 0;
            this.currentYear = 1;
            this.currentMonth = 1;
            this.currentDay = 1;
            loadTime();
        }

        public void advanceTime() {
            long adjustedTicks = (this.currentTick + 6000) % TICKS_PER_DAY;
            this.currentTick = (this.currentTick + TICKS_PER_REAL_SECOND) % TICKS_PER_DAY;

            if (adjustedTicks == 0) {
                this.totalDays++;
                updateDate();
            }

            saveTime();
        }

        private void updateDate() {
            this.currentDay++;
            if (this.currentDay > 30) {
                this.currentDay = 1;
                this.currentMonth++;
                if (this.currentMonth > 12) {
                    this.currentMonth = 1;
                    this.currentYear++;
                }
            }
        }

        public long getCurrentTicks() {
            return currentTick;
        }

        public String getCurrentDate() {
            return String.format("Day %d, Month %d, Year %d", currentDay, currentMonth, currentYear);
        }

        public void loadTime() {
            if (configFile.exists()) {
                Toml toml = new Toml().read(configFile);
                this.currentTick = toml.getLong("time.ticks", 0L);
                this.totalDays = toml.getLong("time.totalDays", 0L);
                this.currentYear = toml.getLong("time.year", 1L).intValue();
                this.currentMonth = toml.getLong("time.month", 1L).intValue();
                this.currentDay = toml.getLong("time.day", 1L).intValue();
            } else {
                this.currentTick = 0;
                this.totalDays = 0;
                this.currentYear = 1;
                this.currentMonth = 1;
                this.currentDay = 1;
                saveTime();
            }
        }

        public void saveTime() {
            try {
                TomlWriter writer = new TomlWriter();
                TimeData data = new TimeData(this.currentTick, this.totalDays, this.currentYear, this.currentMonth, this.currentDay);
                configFile.getParentFile().mkdirs();
                writer.write(data, configFile);
            } catch (IOException e) {
                logger.warning("[TimeSync] Gagal menyimpan waktu: " + e.getMessage());
            }
        }
    }

    private static class TimeData {
        public TimeSection time = new TimeSection();

        public TimeData(long ticks, long totalDays, int year, int month, int day) {
            this.time.ticks = ticks;
            this.time.totalDays = totalDays;
            this.time.year = year;
            this.time.month = month;
            this.time.day = day;
        }

        private static class TimeSection {
            public long ticks;
            public long totalDays;
            public long year;
            public long month;
            public long day;
        }
    }

    private static class SyncTask implements Runnable {
        private final ProxyServer proxy;
        private final TimeManager timeManager;
        private final Logger logger;

        public SyncTask(ProxyServer proxy, TimeManager timeManager, Logger logger) {
            this.proxy = proxy;
            this.timeManager = timeManager;
            this.logger = logger;
        }

        @Override
        public void run() {
            timeManager.advanceTime();
            long ticks = timeManager.getCurrentTicks();
            String date = timeManager.getCurrentDate();

            for (RegisteredServer server : proxy.getAllServers()) {
                sendTimeUpdate(server, ticks, date);
            }
        }

        private void sendTimeUpdate(RegisteredServer server, long ticks, String date) {
            try (ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
                 DataOutputStream out = new DataOutputStream(byteOut)) {

                out.writeLong(ticks);
                out.writeUTF(date);
                server.sendPluginMessage(CHANNEL, byteOut.toByteArray());

            } catch (IOException e) {
                logger.warning("[TimeSync] Gagal mengirim data ke server: " + e.getMessage());
            }
        }
    }
}
