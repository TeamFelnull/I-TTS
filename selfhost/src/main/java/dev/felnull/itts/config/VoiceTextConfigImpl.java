package dev.felnull.itts.config;

import blue.endless.jankson.JsonObject;
import blue.endless.jankson.JsonPrimitive;
import dev.felnull.itts.core.config.voicetype.VoiceTextConfig;
import dev.felnull.itts.utils.Json5Utils;
import org.jetbrains.annotations.NotNull;

public class VoiceTextConfigImpl extends VoiceTypeConfigImpl implements VoiceTextConfig {
    private final String apiKey;

    protected VoiceTextConfigImpl(JsonObject jo) {
        super(jo);
        this.apiKey = Json5Utils.getStringOrElse(jo, "api_key", DEFAULT_API_KEY);
    }

    @Override
    protected JsonObject toJson() {
        var jo = super.toJson();
        jo.put("api_key", JsonPrimitive.of(apiKey), "APIキー");
        return jo;
    }

    @Override
    public @NotNull String getApiKey() {
        return apiKey;
    }
}