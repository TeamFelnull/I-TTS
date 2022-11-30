package dev.felnull.ttsvoice.core.discord;

import dev.felnull.ttsvoice.core.TTSVoiceRuntime;
import dev.felnull.ttsvoice.core.discord.command.*;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class Bot {
    protected final List<BaseCommand> baseCommands = new ArrayList<>();
    private final TTSVoiceRuntime runtime;
    private JDA jda;

    public Bot(@NotNull TTSVoiceRuntime runtime) {
        this.runtime = runtime;
    }

    public void init() {
        registeringCommands();

        this.jda = JDABuilder.createDefault(runtime.getConfigManager().getConfig().getBotToken()).addEventListeners(new EventListener(this)).build();
        updateCommands(this.jda);
    }

    private void registeringCommands() {
        registerCommand(new JoinCommand(this.runtime));
        registerCommand(new LeaveCommand(this.runtime));
        registerCommand(new ReconnectCommand(this.runtime));
        registerCommand(new VoiceCommand(this.runtime));
        registerCommand(new VnickCommand(this.runtime));
        registerCommand(new InfoCommand(this.runtime));
        registerCommand(new ConfigCommand(this.runtime));
        registerCommand(new DenyCommand(this.runtime));
        registerCommand(new AdminCommand(this.runtime));
        registerCommand(new DictCommand(this.runtime));
    }

    private void registerCommand(BaseCommand command) {
        baseCommands.add(command);
    }

    private void updateCommands(JDA jda) {
        jda.updateCommands().addCommands(baseCommands.stream().map(BaseCommand::createSlashCommand).toList()).queue();
    }

    public JDA getJDA() {
        return jda;
    }
}