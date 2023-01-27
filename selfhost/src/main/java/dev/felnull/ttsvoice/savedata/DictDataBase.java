package dev.felnull.ttsvoice.savedata;

import com.google.common.collect.ImmutableList;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dev.felnull.ttsvoice.core.savedata.DictData;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class DictDataBase extends SaveDataBase {
    private final Map<String, DictData> dictEntries = new ConcurrentHashMap<>();

    protected DictDataBase(File saveFile) {
        super(saveFile);
    }

    @Override
    protected int getVersion() {
        return DictData.VERSION;
    }

    @Override
    protected void loadFromJson(@NotNull JsonObject jo) {
        dictEntries.clear();
        for (Map.Entry<String, JsonElement> entry : jo.entrySet()) {
            if (entry.getValue().isJsonPrimitive() && entry.getValue().getAsJsonPrimitive().isString())
                dictEntries.put(entry.getKey(), new DictDataImpl(entry.getKey(), entry.getValue().getAsString()));
        }
    }

    @Override
    protected void saveToJson(@NotNull JsonObject jo) {
        dictEntries.values().forEach(dict -> jo.addProperty(dict.getTarget(), dict.getRead()));
    }

    @NotNull
    @Unmodifiable
    public List<DictData> getAllDictData() {
        return ImmutableList.copyOf(dictEntries.values());
    }

    @Nullable
    public DictData getDictData(@NotNull String target) {
        return dictEntries.get(target);
    }

    public void addDictData(@NotNull String target, @NotNull String read) {
        dictEntries.put(target, new DictDataImpl(target, read));
    }

    public void removeDictData(@NotNull String target) {
        dictEntries.remove(target);
    }

    private record DictDataImpl(String target, String read) implements DictData {
        @Override
        public @NotNull String getTarget() {
            return target;
        }

        @Override
        public @NotNull String getRead() {
            return read;
        }
    }
}
