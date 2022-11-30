package dev.felnull.ttsvoice.core;

import dev.felnull.ttsvoice.core.config.ConfigAccess;
import dev.felnull.ttsvoice.core.config.ConfigManager;
import dev.felnull.ttsvoice.core.discord.Bot;
import org.apache.commons.lang3.concurrent.BasicThreadFactory;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class TTSVoiceRuntime {
    private final Logger logger = LogManager.getLogger(TTSVoiceRuntime.class);
    private final ExecutorService asyncWorkerExecutor = Executors.newCachedThreadPool(new BasicThreadFactory.Builder().namingPattern("async-worker-thread-%d").daemon(true).build());
    private final ConfigManager configManager;
    private final Bot bot;
    private final boolean developmentEnvironment = true;

    private TTSVoiceRuntime(@NotNull ConfigAccess configAccess) {
        this.bot = new Bot(this);
        this.configManager = new ConfigManager(this, configAccess);
    }

    public static TTSVoiceRuntime newRuntime(@NotNull ConfigAccess configAccess) {
        return new TTSVoiceRuntime(configAccess);
    }

    public void run() {
        if (!configManager.init())
            return;

        bot.init();
    }

    public Executor getAsyncWorkerExecutor() {
        return asyncWorkerExecutor;
    }

    public boolean isDevelopmentEnvironment() {
        return developmentEnvironment;
    }

    public String getVersion() {
        return "2.0.0";
    }

    public String getVersionText() {
        if (isDevelopmentEnvironment())
            return "開発環境";
        return "v" + getVersion();
    }

    public ConfigManager getConfigManager() {
        return configManager;
    }

    public Logger getLogger() {
        return logger;
    }
}