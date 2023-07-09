package eu.pb4.polyfactory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import eu.pb4.polyfactory.util.DebugData;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.CommandRegistryAccess;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

import static net.minecraft.server.command.CommandManager.literal;

public class FactoryCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(FactoryCommands::createCommands);
    }

    private static void createCommands(CommandDispatcher<ServerCommandSource> dispatcher, CommandRegistryAccess registryAccess, CommandManager.RegistrationEnvironment environment) {
        dispatcher.register(literal("polyfactory")
                .then(literal("debug")
                        .requires((x) -> x.hasPermissionLevel(3))
                        .then(literal("packetinfo").executes(FactoryCommands::printPacketInfo))
                        
                )
        );
    }

    private static int printPacketInfo(CommandContext<ServerCommandSource> context) {
        DebugData.printPacketCalls(((aClass, integer) -> {
            var x = aClass.getName().split("\\.");
            context.getSource().sendFeedback(() -> Text.literal(x[x.length - 1]).append(" - ").append(integer.toString()), false);
        }));
        return 0;
    };
}
