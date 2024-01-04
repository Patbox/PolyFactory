package eu.pb4.polyfactory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.factorytools.api.virtualentity.BaseModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.factorytools.impl.DebugData;
import eu.pb4.polymer.virtualentity.impl.HolderHolder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

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
                .then(literal("debug")
                        .requires((x) -> x.hasPermissionLevel(3))
                        .then(literal("packetinfo")
                                .then(argument("enable", BoolArgumentType.bool())
                                        .executes(FactoryCommands::togglePacketDebug)
                                )
                                .executes(FactoryCommands::printPacketInfo)
                        )
                        .then(literal("list_models").executes(FactoryCommands::listModels))
                        .then(literal("enable_lod")
                                .then(argument("enable", BoolArgumentType.bool())
                                        .executes(FactoryCommands::enableLod)
                                )
                        )
                        //.then(literal("run_asset_generator")
                        //        .executes(FactoryCommands::assetGenerator)
                        //)
                )
        );
    }

    private static int assetGenerator(CommandContext<ServerCommandSource> serverCommandSourceCommandContext) {

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

        var map = new HashMap<Class<?>, List<BaseModel>>();


        ((HolderHolder) player.networkHandler).polymer$getHolders().forEach((x) -> {
            if (x instanceof BaseModel b) {
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

    private static int togglePacketDebug(CommandContext<ServerCommandSource> context) {
        DebugData.enabled = BoolArgumentType.getBool(context, "enable");
        context.getSource().sendFeedback(() -> Text.literal("Packet debug: " + DebugData.enabled), false);
        return 0;
    }

    private static int printPacketInfo(CommandContext<ServerCommandSource> context) {
        DebugData.printPacketCalls(((aClass, entries) -> {
            var x = aClass.getName().split("\\.");
            context.getSource().sendFeedback(() -> Text.literal(x[x.length - 1]).append(":"), false);
            for (var entry : entries) {
                var y = entry.getKey().getName().split("\\.");

                context.getSource().sendFeedback(() -> Text.literal("- ").append(y[y.length - 1]).append(" - ").append(entry.getIntValue() + ""), false);
            }
        }));
        return 0;
    };
}
