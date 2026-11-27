package org.siddharth.ssmp.mechanics;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.keys.DialogKeys;
import io.papermc.paper.registry.keys.tags.DialogTagKeys;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.tag.PostFlattenTagRegistrar;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import java.util.List;

public class Bootstrap implements PluginBootstrap {
    // thx flower for telling me to write this in capital, it truly doesnt make ANY difference
    private static final Key PLAYER_SETTINGS = Key.key("ssmp:player_settings");
    @Override
    public void bootstrap(BootstrapContext context) {
        context.getLifecycleManager().registerEventHandler(
                RegistryEvents.DIALOG.compose(),
                event -> event.registry().register(
                        DialogKeys.create(PLAYER_SETTINGS),
                        builder -> builder.base(DialogBase.builder(Component.text("Siddy's Simple Plugin Settings"))
                                        .externalTitle(Component.text("Siddy's Simple Plugin")).canCloseWithEscape(true).build()
                                ).type(DialogType.notice(ActionButton.builder(Component.text("Open Player Settings")).width(200)
                                        .action(DialogAction.staticAction(ClickEvent.runCommand("/playerinfo"))).build()))));
        context.getLifecycleManager().registerEventHandler(
                LifecycleEvents.TAGS.postFlatten(RegistryKey.DIALOG),
                event -> event.registrar().addToTag(
                        DialogTagKeys.PAUSE_SCREEN_ADDITIONS,
                        List.of(DialogKeys.create(PLAYER_SETTINGS))));
    }
}