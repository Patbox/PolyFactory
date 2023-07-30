package eu.pb4.polyfactory.mixin;

import eu.pb4.polymer.common.impl.CommonImpl;
import eu.pb4.polymer.common.impl.CompatStatus;
import net.fabricmc.loader.api.FabricLoader;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.List;
import java.util.Set;

public class PolyFactoryMixinConfigPlugin implements IMixinConfigPlugin {
    private static final String PACKAGE_ROOT = "eu.pb4.polyfactory.mixin.";

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        var name = mixinClassName.substring(PACKAGE_ROOT.length());

        if (name.startsWith("compat.")) {
            var tmp = name.split("\\.");

            var type = tmp[tmp.length - 1].split("_")[0];

            return switch (type) {
                case "htm" -> FabricLoader.getInstance().isModLoaded("htm");
                default -> true;
            };
        }

        return true;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {

    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {

    }
}
