package dev.felnull.itts.core.voice.voicevox;

import dev.felnull.itts.core.voice.CachedVoice;
import dev.felnull.itts.core.voice.VoiceType;

import java.io.IOException;
import java.io.InputStream;

/**
 * VOICEVOXの声
 *
 * @author MORIMORI0317
 */
public class VoicevoxVoice extends CachedVoice {
    /**
     * VOICEVOXマネージャー
     */
    private final VoicevoxManager manager;

    /**
     * 話者
     */
    private final VoicevoxSpeaker speaker;

    /**
     * コンストラクタ
     *
     * @param voiceType 声タイプ
     * @param manager   マネージャー
     * @param speaker   話者
     */
    protected VoicevoxVoice(VoiceType voiceType, VoicevoxManager manager, VoicevoxSpeaker speaker) {
        super(voiceType);
        this.manager = manager;
        this.speaker = speaker;
    }

    @Override
    protected InputStream openVoiceStream(String text) throws IOException, InterruptedException {
        return this.manager.openVoiceStream(text, speaker.styles().get(0).id());
    }

    @Override
    protected String createHashCodeChars() {
        return this.speaker.uuid().toString();
    }
}
