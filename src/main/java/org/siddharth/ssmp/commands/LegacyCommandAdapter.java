
package org.siddharth.ssmp.commands;

import io.papermc.paper.command.brigadier.BasicCommand;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

public final class LegacyCommandAdapter implements BasicCommand {
    private final String label;
    private final Supplier<? extends CommandExecutor> executorSupplier;
    private final Supplier<? extends TabCompleter> completerSupplier;
    private final Command command;

    public LegacyCommandAdapter(String label, Supplier<? extends CommandExecutor> executorSupplier, Supplier<? extends TabCompleter> completerSupplier) {
        this.label = label;
        this.executorSupplier = executorSupplier;
        this.completerSupplier = completerSupplier;
        this.command = new Command(label) {
            @Override
            public boolean execute(CommandSender sender, String commandLabel, String[] args) {
                CommandExecutor executor = LegacyCommandAdapter.this.executorSupplier.get();
                return executor != null && executor.onCommand(sender, this, commandLabel, args);
            }
        };
    }

    @Override
    public void execute(CommandSourceStack source, String[] args) {
        CommandExecutor executor = executorSupplier.get();
        if (executor != null) {
            executor.onCommand(source.getSender(), command, label, args);
        }
    }

    @Override
    public Collection<String> suggest(CommandSourceStack source, String[] args) {
        TabCompleter completer = completerSupplier.get();
        if (completer == null) return List.of();

        List<String> suggestions = completer.onTabComplete(source.getSender(), command, label, args);
        return suggestions == null ? List.of() : suggestions;
    }
}
