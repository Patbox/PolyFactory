package eu.pb4.polyfactory.booklet;

import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

import java.util.List;

public record Booklet(Component title, List<Identifier> categories) {
}
