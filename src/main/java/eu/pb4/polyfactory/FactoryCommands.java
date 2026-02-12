package eu.pb4.polyfactory;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import eu.pb4.factorytools.api.virtualentity.BlockModel;
import eu.pb4.factorytools.api.virtualentity.LodItemDisplayElement;
import eu.pb4.polyfactory.booklet.BookletInit;
import eu.pb4.polymer.virtualentity.impl.HolderHolder;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

public class FactoryCommands {
    public static void register() {
        CommandRegistrationCallback.EVENT.register(FactoryCommands::createCommands);
    }

    private static void createCommands(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext registryAccess, Commands.CommandSelection environment) {
        dispatcher.register(literal("polyfactory")
                //.executes(FactoryCommands::about)
                .then(literal("wiki").executes(FactoryCommands::wiki))
                        .then(literal("booklet_page")
                                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                                .then(argument("id", IdentifierArgument.id()).executes(FactoryCommands::bookletPage)))
                .then(literal("debug")
                        .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
                        .then(literal("list_models").executes(FactoryCommands::listModels))
                        .then(literal("enable_lod")
                                .then(argument("enable", BoolArgumentType.bool())
                                        .executes(FactoryCommands::enableLod)
                                )
                        )
                )
        );
    }

    private static int bookletPage(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        return BookletInit.openPage(ctx.getSource().getPlayerOrException(), IdentifierArgument.getId(ctx, "id")) ? 1 : 0;
    }

    private static int about(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSystemMessage(Component.literal("PolyFactory by Patbox"));
        return 0;
    }

    private static int wiki(CommandContext<CommandSourceStack> context) {
        var url = "https://modded.wiki/w/Mod:PolyFactory";
        context.getSource().sendSystemMessage(Component.translatable("text.polyfactory.wiki_link",
                Component.literal(url)
                        .setStyle(Style.EMPTY.withUnderlined(true).withColor(ChatFormatting.BLUE).withClickEvent(new ClickEvent.OpenUrl(URI.create(url))))
        ).setStyle(Style.EMPTY.withColor(ChatFormatting.YELLOW)));
        return 0;
    }

    private static int enableLod(CommandContext<CommandSourceStack> context) {
        LodItemDisplayElement.isEnabled = BoolArgumentType.getBool(context, "enable");;
        LodItemDisplayElement.isDisabled = !LodItemDisplayElement.isEnabled;
        context.getSource().sendSuccess(() -> Component.literal("Model LOD: " + LodItemDisplayElement.isEnabled), false);
        return 0;
    }

    private static int listModels(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        var player = context.getSource().getPlayerOrException();

        var map = new HashMap<Class<?>, List<BlockModel>>();


        ((HolderHolder) player.connection).polymer$getHolders().forEach((x) -> {
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
            context.getSource().sendSuccess(() -> Component.literal(x[x.length - 1]).append(" - ").append(list.size() + " Models | " ).append(finalParts + " Parts"), false);
        }));

        return 0;
    }
}
