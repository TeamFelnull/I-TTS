package dev.felnull.itts.core.audio;

import com.sedmelluq.discord.lavaplayer.container.MediaContainerRegistry;
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager;
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers;
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager;

/**
 * 音声オーディオマネージャー
 *
 * @author MORIMORI0317
 */
public class VoiceAudioManager {
    /**
     * オーディオプレイヤーマネージャー
     */
    private final AudioPlayerManager audioPlayerManager;

    /**
     * コンストラクタ
     */
    public VoiceAudioManager() {
        this.audioPlayerManager = new DefaultAudioPlayerManager();
        AudioSourceManagers.registerLocalSource(audioPlayerManager);
        audioPlayerManager.registerSourceManager(new HttpAudioSourceManager(MediaContainerRegistry.DEFAULT_REGISTRY));
    }

    public AudioPlayerManager getAudioPlayerManager() {
        return audioPlayerManager;
    }
}
