package dev.felnull.itts.core.tts;

import com.google.common.collect.ImmutableList;
import dev.felnull.itts.core.TTSVoiceRuntime;
import dev.felnull.itts.core.audio.LoadedSaidText;
import dev.felnull.itts.core.audio.VoiceAudioScheduler;
import dev.felnull.itts.core.tts.saidtext.SaidText;
import net.dv8tion.jda.api.entities.Guild;

import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

public final class TTSInstance {
    private static final int MAX_COUNT = 150;
    private static final int LOAD_COUNT = 10;
    private static final int NEXT_WAIT_TIME = 1500;
    private final ConcurrentLinkedQueue<SaidText> saidTextQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<LoadedSaidTextEntry> loadSaidTextQueue = new ConcurrentLinkedQueue<>();
    private final AtomicReference<LoadedSaidTextEntry> currentSaidText = new AtomicReference<>();
    private final AtomicBoolean next = new AtomicBoolean(true);
    private final AtomicBoolean destroyed = new AtomicBoolean();
    private final Object updateLock = new Object();
    private final VoiceAudioScheduler voiceAudioScheduler;
    private final long audioChannel;
    private final long textChannel;
    private final boolean overwriteAloud;

    public TTSInstance(Guild guild, long audioChannel, long textChannel, boolean overwriteAloud) {
        this.voiceAudioScheduler = new VoiceAudioScheduler(guild.getAudioManager(), TTSVoiceRuntime.getInstance().getVoiceAudioManager());
        this.audioChannel = audioChannel;
        this.textChannel = textChannel;
        this.overwriteAloud = overwriteAloud;
    }

    public long getAudioChannel() {
        return audioChannel;
    }

    public long getTextChannel() {
        return textChannel;
    }


    public void dispose() {
        destroyed.set(true);

        saidTextQueue.clear();

        while (!loadSaidTextQueue.isEmpty())
            loadSaidTextQueue.poll().dispose();

        var cst = currentSaidText.get();
        if (cst != null)
            cst.dispose();

        voiceAudioScheduler.dispose();
    }

    public void sayText(SaidText saidText) {
        if (saidTextQueue.size() >= MAX_COUNT)
            return;

        if (overwriteAloud) {
            updateAloud(saidText);
        } else {
            saidTextQueue.add(saidText);
            updateQueue();
        }
    }

    private List<SaidText> getCurrentQueue() {
        ImmutableList.Builder<SaidText> cq = ImmutableList.builder();

        var crntST = currentSaidText.get();
        if (crntST != null)
            cq.add(crntST.saidText);

        cq.addAll(loadSaidTextQueue.stream().map(r -> r.saidText).toList());
        cq.addAll(saidTextQueue);

        return cq.build();
    }

    private void updateAloud(SaidText saidText) {
        synchronized (updateLock) {
            if (destroyed.get())
                return;

            voiceAudioScheduler.stop();

            var cst = currentSaidText.getAndSet(new LoadedSaidTextEntry(saidText));
            if (cst != null)
                cst.dispose();

            sayStart();
        }
    }

    private void updateQueue() {
        synchronized (updateLock) {
            if (destroyed.get() || overwriteAloud)
                return;

            loadSaidTextQueue.removeIf(r -> {
                if (r.isFailure()) {
                    r.dispose();
                    return true;
                }
                return false;
            });

            while (loadSaidTextQueue.size() < LOAD_COUNT && !saidTextQueue.isEmpty())
                loadSaidTextQueue.add(new LoadedSaidTextEntry(saidTextQueue.poll()));

            var cst = currentSaidText.get();

            if ((cst == null || cst.isFailure() || cst.isAlreadyUsed()) && next.get()) {

                if (cst != null)
                    cst.dispose();

                if (!loadSaidTextQueue.isEmpty()) {
                    currentSaidText.set(loadSaidTextQueue.poll());
                    sayStart();
                } else {
                    currentSaidText.set(null);
                }

                while (!saidTextQueue.isEmpty())
                    loadSaidTextQueue.add(new LoadedSaidTextEntry(saidTextQueue.poll()));
            }

            var cq = getCurrentQueue();

            saidTextQueue.removeIf(r -> !r.updateSurvive(cq));
            loadSaidTextQueue.removeIf(r -> {
                if (!r.saidText.updateSurvive(cq)) {
                    r.dispose();
                    return true;
                }
                return false;
            });
        }
    }

    private void sayStart() {
        currentSaidText.get().completableFuture.whenCompleteAsync((loadedSaidText, throwable) -> {

            if (throwable != null) {
                if (!(throwable instanceof CancellationException))
                    TTSVoiceRuntime.getInstance().getLogger().error("Failed to load voice audio", throwable);

                if (!overwriteAloud) {
                    updateQueue();
                }

                return;
            }

            if (loadedSaidText.isFailure()) {
                updateQueue();
            } else {
                next.set(false);
                voiceAudioScheduler.play(loadedSaidText, () -> {
                    if (overwriteAloud)
                        loadedSaidText.dispose();

                    if (!overwriteAloud) {

                        CompletableFuture.runAsync(() -> {
                            try {
                                Thread.sleep(NEXT_WAIT_TIME);
                            } catch (InterruptedException ignored) {
                            }
                            next.set(true);
                            updateQueue();
                        }, getExecutor());

                        updateQueue();
                    }
                });
            }

        }, getExecutor());
    }

    private Executor getExecutor() {
        return TTSVoiceRuntime.getInstance().getAsyncWorkerExecutor();
    }

    private class LoadedSaidTextEntry {
        private final SaidText saidText;
        private final CompletableFuture<LoadedSaidText> completableFuture;
        private final AtomicBoolean failure = new AtomicBoolean();

        private LoadedSaidTextEntry(SaidText saidText) {
            this.saidText = saidText;
            this.completableFuture = voiceAudioScheduler.load(saidText);
            this.completableFuture.whenCompleteAsync((loadedSaidText, throwable) -> {
                failure.set(throwable != null);
            }, getExecutor());
        }

        private void dispose() {
            completableFuture.thenAcceptAsync(LoadedSaidText::dispose, getExecutor());
        }

        private boolean isFailure() {
            if (completableFuture.isDone()) {
                try {
                    return completableFuture.get().isFailure();
                } catch (InterruptedException | ExecutionException ignored) {
                }
            }
            return failure.get();
        }

        private boolean isAlreadyUsed() {
            if (completableFuture.isDone()) {
                try {
                    return completableFuture.get().isAlreadyUsed();
                } catch (InterruptedException | ExecutionException ignored) {
                }
            }
            return false;
        }
    }
}
