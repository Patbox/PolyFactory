package eu.pb4.polyfactory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polymer.virtualentity.impl.HolderHolder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class FactoryCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(FactoryCommands::createCommands);
    }

    private static void createCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("polyfactory")
                //.executes(FactoryCommands::about)
                .then(literal("wiki").executes(FactoryCommands::wiki))
                .then(literal("debug")
                        .requires((x) -> x.hasPermissionLevel(3))
                        .then(literal("list_models").executes(FactoryCommands::listModels))
                        .then(literal("enable_lod")
                                .then(argument("enable", BoolArgumentType.bool())
                                        .executes(FactoryCommands::enableLod)
                                )
                        )
                )
        );
    }

    private static int about(CommandContext<ServerCommandSource> context) {
        context.getSource().sendMessage(Text.literal("PolyFactory by Patbox"));
        return 0;
    }

    private static int wiki(CommandContext<ServerCommandSource> context) {
        var url = "https://modded.wiki/w/Mod:PolyFactory";
        context.getSource().sendMessage(Text.translatable("text.polyfactory.wiki_link",
                Text.literal(url)
                        .setStyle(Style.EMPTY.withUnderline(true).withColor(Formatting.BLUE).withClickEvent(new ClickEvent(ClickEvent.Action.OPEN_URL, url)))
        ).setStyle(Style.EMPTY.withColor(Formatting.YELLOW)));
        return 0;
    }

    private static int enableLod(CommandContext<ServerCommandSource> context) {
        LodItemDisplayElement.isEnabled = BoolArgumentType.getBool(context, "enable");;
        LodItemDisplayElement.isDisabled = !LodItemDisplayElement.isEnabled;
        context.getSource().sendFeedback(() -> Text.literal("Model LOD: " + LodItemDisplayElement.isEnabled), false);
        return 0;
    }

    private static int listModels(CommandContext<ServerCommandSource> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrThrow();

        var map = new HashMap<Class<?>, List<BlockModel>>();


        ((HolderHolder) player.networkHandler).polymer$getHolders().forEach((x) -> {
            if (x instanceof BlockModel b) {
                map.computeIfAbsent(x.getClass(), (a) -> new ArrayList<>()).add(b);
            }
        });
        map.forEach(((aClass, list) -> {
            int parts = 0;
            for (var e : list) {
                parts += e.getElements().size();
            }


            var x = aClass.getName().split("\\.");
            int finalParts = parts;
            context.getSource().sendFeedback(() -> Text.literal(x[x.length - 1]).append(" - ").append(list.size() + " Models | " ).append(finalParts + " Parts"), false);
        }));

        return 0;
    }
}
