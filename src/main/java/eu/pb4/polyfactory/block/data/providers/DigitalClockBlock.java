package eu.pb4.polyfactory.block.data.providers;

import eu.pb4.polyfactory.block.data.DataProvider;
import eu.pb4.polyfactory.block.data.util.OrientableCabledDataBlock;
import eu.pb4.polyfactory.data.GameTimeData;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.elements.TextDisplayElement;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ARGB;
import net.minecraft.util.Brightness;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gamerules.GameRules;
import org.jetbrains.annotations.Nullable;

public class DigitalClockBlock extends OrientableCabledDataProviderBlock {
    public DigitalClockBlock(Properties settings) {
        super(settings);
    }


    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        super.onPlace(state, world, pos, oldState, notify);
        world.scheduleTick(pos, state.getBlock(), 1);
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.tick(state, level, pos, random);
        DataProvider.sendData(level, pos, GameTimeData.now(level));
        level.scheduleTick(pos, state.getBlock(), 1);
    }

    @Override
    public @Nullable ElementHolder createElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return new Model(initialBlockState, world);
    }

    @Override
    public boolean tickElementHolder(ServerLevel world, BlockPos pos, BlockState initialBlockState) {
        return true;
    }

    public static class Model extends OrientableCabledDataBlock.Model {
        private final TextDisplayElement time;
        private long nextUpdate = Long.MIN_VALUE;

        private Model(BlockState state, ServerLevel level) {
            super(state);
            this.time = new TextDisplayElement();
            this.time.setDisplaySize(1, 1);
            this.time.setYaw(this.base.getYaw());
            this.time.setBackground(0);
            this.time.setPitch(this.base.getPitch());
            this.time.setBrightness(new Brightness(15, 15));
            var mat = mat();
            mat.translate(0, 0, 0.501f);
            mat.scale(1 / 8f);
            mat.translate(-0.125f, -1.125f, 0);
            mat.scale(1 / 0.125f);
            this.time.setTransformation(mat);


            this.updateTime(level.getGameTime(), level.getDayTime(), level.getGameRules().get(GameRules.ADVANCE_TIME));
            this.addElement(this.time);
        }

        private void updateTime(long gameTime, long dayTime, boolean advanceTime) {
            var style = Style.EMPTY;
            if (advanceTime) {
                style = style.withColor(0xff6e19);
            } else {
                style = style.withItalic(true)
                        .withColor(ARGB.scaleRGB(0xff6e19, 0.5f))
                        .withShadowColor(ARGB.scaleRGB(0xFFff6e19, 0.2f))

                ;
            }

            this.time.setText(
                    Component.literal(GameTimeData.asTimeString(dayTime))
                            .setStyle(style)
            );
        }

        @Override
        protected void onTick() {
            super.onTick();
            var level = this.getAttachment().getWorld();
            this.updateTime(level.getGameTime(), level.getDayTime(), level.getGameRules().get(GameRules.ADVANCE_TIME));
        }

        @Override
        protected void updateStatePos(BlockState state) {
            super.updateStatePos(state);
            if (this.time != null) {
                this.time.setYaw(this.base.getYaw());
                this.time.setPitch(this.base.getPitch());
            }
        }

        @Override
        protected void setState(BlockState blockState) {
            super.setState(blockState);
            updateStatePos(this.blockState());
            this.base.tick();
            this.time.tick();
        }
    }
}
