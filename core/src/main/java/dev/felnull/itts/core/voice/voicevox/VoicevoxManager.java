package dev.felnull.itts.core.voice.voicevox;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.felnull.itts.core.ITTSRuntime;
import dev.felnull.itts.core.config.voicetype.VoicevoxConfig;
import dev.felnull.itts.core.voice.VoiceType;

import java.io.*;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * VOICEVOX系エンジンの管理
 *
 * @author MORIMORI0317
 */
public class VoicevoxManager {

    /**
     * GSON
     */
    private static final Gson GSON = new Gson();

    /**
     * VOICEVOX系の声カテゴリ
     */
    private final VoicevoxVoiceCategory category = new VoicevoxVoiceCategory(this);

    /**
     * バランサー
     */
    private final VoicevoxBalancer balancer;

    /**
     * エンジンのの前
     */
    private final String name;

    /**
     * VOICEVOX系エンジンのコンフィグ
     */
    private final Supplier<VoicevoxConfig> configSupplier;

    /**
     * コンストラクタ
     *
     * @param name           名前
     * @param enginUrls      エンジンURL
     * @param configSupplier コンフィグ
     */
    public VoicevoxManager(String name, Supplier<List<String>> enginUrls, Supplier<VoicevoxConfig> configSupplier) {
        this.name = name;
        this.configSupplier = configSupplier;
        this.balancer = new VoicevoxBalancer(this, enginUrls);
    }

    protected VoicevoxConfig getConfig() {
        return configSupplier.get();
    }

    /**
     * 初期化
     *
     * @return 初期化の非同期CompletableFuture
     */
    public CompletableFuture<?> init() {
        return balancer.init();
    }

    public VoicevoxVoiceCategory getCategory() {
        return category;
    }

    public String getName() {
        return name;
    }

    public boolean isAvailable() {
        return getConfig().isEnable() && balancer.isAvailable();
    }

    public List<VoiceType> getAvailableVoiceTypes() {
        return balancer.getAvailableSpeakers().stream()
                .map(r -> (VoiceType) new VoicevoxVoiceType(r, this))
                .toList();
    }

    protected VoicevoxBalancer getBalancer() {
        return balancer;
    }

    /**
     * エンジンのURLから話者一覧を取得
     *
     * @param vvurl VOICEVOXのURL
     * @return 話者のリスト
     * @throws IOException          IO例外
     * @throws InterruptedException 割り込み例外
     */
    protected List<VoicevoxSpeaker> requestSpeakers(VVURL vvurl) throws IOException, InterruptedException {
        HttpClient hc = ITTSRuntime.getInstance().getNetworkManager().getHttpClient();
        HttpRequest req = HttpRequest.newBuilder(vvurl.createURI("speakers"))
                .timeout(Duration.of(3000, ChronoUnit.MILLIS))
                .build();
        HttpResponse<InputStream> rep = hc.send(req, HttpResponse.BodyHandlers.ofInputStream());
        JsonArray ja;

        try (InputStream stream = new BufferedInputStream(rep.body()); Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            ja = GSON.fromJson(reader, JsonArray.class);
        }

        ImmutableList.Builder<VoicevoxSpeaker> speakerBuilder = new ImmutableList.Builder<>();

        for (JsonElement je : ja) {
            speakerBuilder.add(VoicevoxSpeaker.of(je.getAsJsonObject()));
        }

        return speakerBuilder.build();
    }

    private JsonObject getQuery(String text, int speakerId) {
        text = URLEncoder.encode(text, StandardCharsets.UTF_8);

        try (var urlUse = balancer.getUseURL()) {
            HttpClient hc = ITTSRuntime.getInstance().getNetworkManager().getHttpClient();
            HttpRequest req = HttpRequest.newBuilder(urlUse.getVVURL().createURI(String.format("audio_query?text=%s&speaker=%d", text, speakerId)))
                    .POST(HttpRequest.BodyPublishers.noBody())
                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                    .build();
            HttpResponse<InputStream> rep = hc.send(req, HttpResponse.BodyHandlers.ofInputStream());

            try (InputStream stream = new BufferedInputStream(rep.body()); Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                return GSON.fromJson(reader, JsonObject.class);
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * 読み上げ音声データのストリームを開く
     *
     * @param text      読み上げるテキスト
     * @param speakerId 話者ID
     * @return 音声データのストリーム
     * @throws IOException          IO例外
     * @throws InterruptedException 割り込み例外
     */
    protected InputStream openVoiceStream(String text, int speakerId) throws IOException, InterruptedException {
        JsonObject qry = getQuery(text, speakerId);
        try (var urlUse = balancer.getUseURL()) {
            HttpClient hc = ITTSRuntime.getInstance().getNetworkManager().getHttpClient();
            HttpRequest request = HttpRequest.newBuilder(urlUse.getVVURL().createURI(String.format("synthesis?speaker=%d", speakerId)))
                    .timeout(Duration.of(10, ChronoUnit.SECONDS))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(GSON.toJson(qry)))
                    .build();

            HttpResponse<InputStream> res = hc.send(request, HttpResponse.BodyHandlers.ofInputStream());

            Optional<String> content = res.headers().firstValue("content-type");
            int code = res.statusCode();

            if (content.isEmpty()) {
                throw new IOException("Content Type does not exist: " + code);
            }


            if (content.get().startsWith("audio/")) {
                return res.body();
            }

            throw new IOException("Not audio data: " + code);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
