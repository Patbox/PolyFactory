package eu.pb4.polyfactory.block.mechanical.conveyor;

import com.kneelawk.graphlib.graph.BlockNode;
import eu.pb4.polyfactory.ModInit;
import eu.pb4.polyfactory.block.FactoryBlockEntities;
import eu.pb4.polyfactory.block.FactoryBlockTags;
import eu.pb4.polyfactory.block.FactoryBlocks;
import eu.pb4.polyfactory.block.mechanical.RotationalSource;
import eu.pb4.polyfactory.display.LodElementHolder;
import eu.pb4.polyfactory.util.FactoryUtil;
import eu.pb4.polyfactory.util.VirtualDestroyStage;
import eu.pb4.polyfactory.util.movingitem.ContainerHolder;
import eu.pb4.polyfactory.util.movingitem.MovingItem;
import eu.pb4.polyfactory.util.movingitem.MovingItemConsumer;
import eu.pb4.polymer.core.api.block.PolymerBlock;
import eu.pb4.polyfactory.block.network.NetworkBlock;
import eu.pb4.polyfactory.nodes.mechanical.ConveyorNode;
import eu.pb4.polymer.resourcepack.api.PolymerResourcePackUtils;
import eu.pb4.polymer.resourcepack.api.ResourcePackBuilder;
import eu.pb4.polymer.virtualentity.api.BlockWithElementHolder;
import eu.pb4.polymer.virtualentity.api.ElementHolder;
import eu.pb4.polymer.virtualentity.api.attachment.BlockBoundAttachment;
import eu.pb4.polymer.virtualentity.api.attachment.HolderAttachment;
import eu.pb4.polymer.virtualentity.api.elements.ItemDisplayElement;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.BlockHalf;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.DirectionProperty;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.ItemScatterer;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldEvents;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class ConveyorBlock extends NetworkBlock implements PolymerBlock, BlockWithElementHolder, BlockEntityProvider, ConveyorLikeDirectional, MovingItemConsumer, VirtualDestroyStage.Marker {
    public static final int FRAMES = 20;
    public static final ItemStack[] ANIMATION_REGULAR = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_10 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_01 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_00 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_STICKY = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_STICKY_10 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_STICKY_01 = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_REGULAR_STICKY_00 = new ItemStack[1 + FRAMES];
    public static final ItemStack[][] ANIMATION_REGULAR_STICKY_X = new ItemStack[][] { ANIMATION_REGULAR_STICKY, ANIMATION_REGULAR_STICKY_10, ANIMATION_REGULAR_STICKY_01, ANIMATION_REGULAR_STICKY_00 } ;
    public static final ItemStack[][] ANIMATION_REGULAR_X = new ItemStack[][] { ANIMATION_REGULAR, ANIMATION_REGULAR_10, ANIMATION_REGULAR_01, ANIMATION_REGULAR_00 } ;
    public static final ItemStack[] ANIMATION_UP = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_UP_STICKY = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_DOWN = new ItemStack[1 + FRAMES];
    public static final ItemStack[] ANIMATION_DOWN_STICKY = new ItemStack[1 + FRAMES];

    public static DirectionProperty DIRECTION = DirectionProperty.of("direction", Direction.NORTH, Direction.SOUTH, Direction.EAST, Direction.WEST);
    public static EnumProperty<DirectionValue> VERTICAL = EnumProperty.of("vertical", DirectionValue.class);
    public static BooleanProperty NEXT_CONVEYOR = BooleanProperty.of("next");
    public static BooleanProperty PREVIOUS_CONVEYOR = BooleanProperty.of("previous");
    public static BooleanProperty HAS_OUTPUT_TOP = BooleanProperty.of("has_top_output");

    private static final String MODEL_JSON = """
                {
                  "parent": "polyfactory:block/|PREFIX|conveyor|TYPE|",
                  "textures": {
                    "1": "polyfactory:block/gen/|PREFIX|conveyor_top_|ID|"
                  }
                }
                """;
    private static final String MODEL_JSON_UP = """
                {
                  "parent": "polyfactory:block/|PREFIX|conveyor_up",
                  "textures": {
                    "1": "polyfactory:block/gen/|PREFIX|conveyor_top_|ID|"
                  }
                }
                """;

    private static final String MODEL_JSON_DOWN = """
                {
                  "parent": "polyfactory:block/|PREFIX|conveyor_down",
                  "textures": {
                    "1": "polyfactory:block/gen/|PREFIX|conveyor_top_|ID|"
                  }
                }
                """;

    public ConveyorBlock(Settings settings) {
        super(settings);
        this.setDefaultState(this.getDefaultState().with(VERTICAL, DirectionValue.NONE).with(DIRECTION, Direction.NORTH).with(HAS_OUTPUT_TOP, false).with(NEXT_CONVEYOR, false).with(PREVIOUS_CONVEYOR, false));
    }

    private static void createItemModel(ItemStack[] array, String path, int i) {
        var model = PolymerResourcePackUtils.requestModel(Items.PURPLE_CANDLE, FactoryUtil.id(path + "_" + i));
        var stack = new ItemStack(Items.PURPLE_CANDLE);
        stack.getOrCreateNbt().putInt("CustomModelData", model.value());
        array[array.length - i] = stack;
    }

    private static void createItemModelZero(ItemStack[] array, String path) {
        var model = PolymerResourcePackUtils.requestModel(Items.PURPLE_CANDLE, FactoryUtil.id(path));
        var stack = new ItemStack(Items.PURPLE_CANDLE);
        stack.getOrCreateNbt().putInt("CustomModelData", model.value());
        array[0] = stack;
    }

    public static void registerAssetsEvents() {
        for (int i = 1; i <= FRAMES; i++) {
            createItemModel(ANIMATION_REGULAR, "block/conveyor", i);
            createItemModel(ANIMATION_REGULAR_00, "block/conveyor_00", i);
            createItemModel(ANIMATION_REGULAR_01, "block/conveyor_01", i);
            createItemModel(ANIMATION_REGULAR_10, "block/conveyor_10", i);
            createItemModel(ANIMATION_UP, "block/conveyor_up", i);
            createItemModel(ANIMATION_DOWN, "block/conveyor_down", i);
            createItemModel(ANIMATION_REGULAR_STICKY, "block/sticky_conveyor", i);
            createItemModel(ANIMATION_REGULAR_STICKY_00, "block/sticky_conveyor_00", i);
            createItemModel(ANIMATION_REGULAR_STICKY_01, "block/sticky_conveyor_01", i);
            createItemModel(ANIMATION_REGULAR_STICKY_10, "block/sticky_conveyor_10", i);
            createItemModel(ANIMATION_UP_STICKY, "block/sticky_conveyor_up", i);
            createItemModel(ANIMATION_DOWN_STICKY, "block/sticky_conveyor_down", i);
        }

        createItemModelZero(ANIMATION_REGULAR_00, "block/conveyor_00");
        createItemModelZero(ANIMATION_REGULAR_01, "block/conveyor_01");
        createItemModelZero(ANIMATION_REGULAR_10, "block/conveyor_10");
        createItemModelZero(ANIMATION_REGULAR_STICKY_00, "block/sticky_conveyor_00");
        createItemModelZero(ANIMATION_REGULAR_STICKY_01, "block/sticky_conveyor_01");
        createItemModelZero(ANIMATION_REGULAR_STICKY_10, "block/sticky_conveyor_10");
        createItemModelZero(ANIMATION_UP, "block/conveyor_up");
        createItemModelZero(ANIMATION_UP_STICKY, "block/sticky_conveyor_up");
        createItemModelZero(ANIMATION_DOWN, "block/conveyor_down");
        createItemModelZero(ANIMATION_DOWN_STICKY, "block/sticky_conveyor_down");

        PolymerResourcePackUtils.RESOURCE_PACK_CREATION_EVENT.register(ConveyorBlock::generateModels);
    }

    private static void generateModels(ResourcePackBuilder resourcePackBuilder) {
        var animJson = """
                {
                  "animation": {
                    "interpolate": false,
                    "frametime": |SPEED|
                  }
                }
                """;
        byte[] textureTop = new byte[0];
        byte[] textureTopSticky = new byte[0];
        for (var path : FabricLoader.getInstance().getModContainer(ModInit.ID).get().getRootPaths()) {
            path = path.resolve("assets/polyfactory/textures/block/conveyor_top.png");
            if (Files.exists(path)) {
                try {
                    textureTop = createMovingTexture(path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            path = path.getParent().resolve("sticky_conveyor_top.png");
            if (Files.exists(path)) {
                try {
                    textureTopSticky = createMovingTexture(path);
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }

            if (textureTop.length != 0 && textureTopSticky.length != 0) {
                break;
            }
        }


        for (int i = 1; i <= FRAMES; i++) {
            byte[] bytes = animJson.replace("|SPEED|", "" + i).getBytes(StandardCharsets.UTF_8);
            createVariations(resourcePackBuilder, i, bytes, textureTop, "");
            createVariations(resourcePackBuilder, i, bytes, textureTopSticky, "sticky_");
        }
    }

    private static void createVariations(ResourcePackBuilder resourcePackBuilder, int i, byte[] mcmeta, byte[] texture, String prefix) {
        resourcePackBuilder.addData("assets/polyfactory/textures/block/gen/" + prefix + "conveyor_top_" + i + ".png.mcmeta", mcmeta);
        resourcePackBuilder.addData("assets/polyfactory/textures/block/gen/" + prefix + "conveyor_top_" + i + ".png", texture);
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_" + i + ".json", MODEL_JSON.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", "").getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_00_" + i + ".json", MODEL_JSON.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", "_00").getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_01_" + i + ".json", MODEL_JSON.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", "_01").getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_10_" + i + ".json", MODEL_JSON.replace("|PREFIX|", prefix).replace("|ID|", "" + i).replace("|TYPE|", "_10").getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_up_" + i + ".json", MODEL_JSON_UP.replace("|PREFIX|", prefix).replace("|ID|", "" + i).getBytes(StandardCharsets.UTF_8));
        resourcePackBuilder.addData("assets/polyfactory/models/block/" + prefix + "conveyor_down_" + i + ".json", MODEL_JSON_DOWN.replace("|PREFIX|", prefix).replace("|ID|", "" + i).getBytes(StandardCharsets.UTF_8));

    }

    private static byte[] createMovingTexture(Path path) throws IOException {
        var image = ImageIO.read(Files.newInputStream(path));
        var imageSize = 32;
        var textureXSize = 16;
        var textureYSize = 18;

        BufferedImage output;
        if (image.getColorModel() instanceof IndexColorModel colorModel) {
            output = new BufferedImage(imageSize, imageSize * imageSize, image.getType(), colorModel);
        } else {
           output = new BufferedImage(imageSize, imageSize * imageSize, image.getType());
        }

        var scale = imageSize / image.getWidth();

        for (var i = 0; i < imageSize; i++) {
            var position = i * imageSize;

            for (var x = 0; x < textureXSize; x++) {
                for (var y = 0; y < textureYSize; y++) {
                    var pixel = image.getRGB(((x + i) % textureXSize) / scale, y / scale);

                    output.setRGB(x, y + position, pixel);
                    output.setRGB(x + 16, y + position, pixel);
                }
            }
        }

        var out = new ByteArrayOutputStream();
        ImageIO.write(output, "png", out);
        return out.toByteArray();
    }
    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        super.appendProperties(builder);
        builder.add(DIRECTION);
        builder.add(VERTICAL);
        builder.add(HAS_OUTPUT_TOP);
        builder.add(NEXT_CONVEYOR);
        builder.add(PREVIOUS_CONVEYOR);
    }

    @Override
    public ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, Hand hand, BlockHitResult hit) {
        var x = player.getStackInHand(hand);
        if (x.isOf(Items.SLIME_BALL) && this == FactoryBlocks.CONVEYOR) {
            var delta = 0d;
            MovingItem itemContainer = null;

            if (world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                itemContainer = conveyor.pullAndRemove();
                delta = conveyor.delta;
            }

            world.setBlockState(pos, FactoryBlocks.STICKY_CONVEYOR.getStateWithProperties(state));
            world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(Blocks.SLIME_BLOCK.getDefaultState()));
            if (!player.isCreative()) {
                x.decrement(1);
            }

            if (itemContainer != null && world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                conveyor.pushAndAttach(itemContainer);
                conveyor.setDelta(delta);
            }

            return ActionResult.SUCCESS;
        } else if (x.isOf(Items.WET_SPONGE) && this == FactoryBlocks.STICKY_CONVEYOR) {
            var delta = 0d;
            MovingItem itemContainer = null;

            if (world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                itemContainer = conveyor.pullAndRemove();
                delta = conveyor.delta;
            }

            world.setBlockState(pos, FactoryBlocks.CONVEYOR.getStateWithProperties(state));
            world.syncWorldEvent(WorldEvents.BLOCK_BROKEN, pos, Block.getRawIdFromState(Blocks.SLIME_BLOCK.getDefaultState()));
            if (!player.isCreative()) {
                player.getInventory().offerOrDrop(Items.SLIME_BALL.getDefaultStack());
            }

            if (itemContainer != null && world.getBlockEntity(pos) instanceof ConveyorBlockEntity conveyor) {
                conveyor.pushAndAttach(itemContainer);
                conveyor.setDelta(delta);
            }

            return ActionResult.SUCCESS;
        }


        if (hand == Hand.MAIN_HAND) {
            if (x.isEmpty()) {
                var be = world.getBlockEntity(pos);

                if (be instanceof ConveyorBlockEntity conveyor && !conveyor.getStack(0).isEmpty()) {
                    player.setStackInHand(hand, conveyor.getStack(0));
                    conveyor.setStack(0, ItemStack.EMPTY);
                    conveyor.setDelta(0);
                    return ActionResult.SUCCESS;
                }
            }
        }

        return super.onUse(state, world, pos, player, hand, hit);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction, BlockState neighborState, WorldAccess world, BlockPos pos, BlockPos neighborPos) {
        super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
        if (direction.getAxis() == Direction.Axis.Y) {
            var vert = state.get(VERTICAL);
            if (vert.value != 0 && direction == Direction.UP) {
                if (neighborState.isOf(this)) {
                    state = state.with(VERTICAL, switch (vert) {
                        case POSITIVE -> DirectionValue.POSITIVE_STACK;
                        case NEGATIVE -> DirectionValue.NEGATIVE_STACK;
                        default -> vert;
                    });
                } else {
                    state = state.with(VERTICAL, switch (vert) {
                        case POSITIVE_STACK -> DirectionValue.POSITIVE;
                        case NEGATIVE_STACK -> DirectionValue.NEGATIVE;
                        default -> vert;
                    });
                }
            }

            return state.with(HAS_OUTPUT_TOP, world.getBlockState(pos.up()).isIn(FactoryBlockTags.CONVEYOR_TOP_OUTPUT));
        } else if (direction.getAxis() == state.get(DIRECTION).getAxis()) {
            return state.with(direction == state.get(DIRECTION) ? NEXT_CONVEYOR : PREVIOUS_CONVEYOR, neighborState.isIn(FactoryBlockTags.CONVEYORS));
        }
        return state;
    }

    @Override
    public void onSteppedOn(World world, BlockPos pos, BlockState state, Entity entity) {
        if (world instanceof ServerWorld serverWorld) {
            pushEntity(serverWorld, state, pos, entity);
        }
    }

    @Override
    public void onEntityCollision(BlockState state, World world, BlockPos pos, Entity entity) {
        if (world instanceof ServerWorld serverWorld) {
            var box = entity.getBoundingBox().offset(0, -0.1, 0);

            boolean match = false;
            for (var shape : state.getCollisionShape(world, pos).getBoundingBoxes()) {
                if (shape.offset(pos).intersects(box)) {
                    match = true;
                    break;
                }
            }

            if (match) {
                this.pushEntity(serverWorld, state, pos, entity);
            }
        }
    }

    @Override
    public boolean hasComparatorOutput(BlockState state) {
        return true;
    }

    @Override
    public int getComparatorOutput(BlockState state, World world, BlockPos pos) {
        var be = world.getBlockEntity(pos);
        return be.getClass() == ConveyorBlockEntity.class && !((ConveyorBlockEntity) be).getStack(0).isEmpty() ? 15 : 0;
    }

    private void pushEntity(ServerWorld world, BlockState state, BlockPos pos, Entity entity) {
        if (entity instanceof ItemEntity itemEntity) {
            var be = world.getBlockEntity(pos);
            if (be instanceof ConveyorBlockEntity conveyorBlockEntity && conveyorBlockEntity.tryAdding(itemEntity.getStack())) {
                conveyorBlockEntity.setDelta(0.5);
                if (itemEntity.getStack().isEmpty()) {
                    entity.discard();
                }
            }
            return;
        }

        var dir = state.get(DIRECTION);

        var next = entity.getBlockPos().offset(dir);

        var speed = RotationalSource.getNetworkSpeed(world, pos) * 0.9;

        if (speed == 0) {
            return;
        }
        var vert = state.get(VERTICAL);
        if (vert != ConveyorBlock.DirectionValue.NONE) {
            speed = speed / MathHelper.SQUARE_ROOT_OF_TWO;
        }
        var vec = Vec3d.of(dir.getVector()).multiply(speed);
        if (entity.getStepHeight() < 0.51) {
            var box = entity.getBoundingBox().offset(vec);
            if (vert == DirectionValue.POSITIVE) {
                for (var shape : state.getCollisionShape(world, pos).getBoundingBoxes()) {
                    if (shape.offset(pos).intersects(box)) {
                        entity.move(MovementType.SELF, new Vec3d(0, 0.51, 0));
                        entity.move(MovementType.SELF, vec);
                        return;
                    }
                }
            }

            var nextState = world.getBlockState(next);
            if (nextState.isOf(this) && (nextState.get(VERTICAL) == DirectionValue.POSITIVE)) {
                for (var shape : state.getCollisionShape(world, pos).getBoundingBoxes()) {
                    if (shape.offset(next).intersects(box)) {
                        entity.move(MovementType.SELF, new Vec3d(0, 0.51, 0));
                        entity.move(MovementType.SELF, vec);
                        return;
                    }
                }
            }
        }

        entity.addVelocity(vec);
        entity.velocityModified = true;
    }

    @Nullable
    @Override
    public BlockState getPlacementState(ItemPlacementContext ctx) {
        var against = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(ctx.getSide().getOpposite()));
        BlockState state = null;
        Direction direction = Direction.NORTH;
        if (against.isOf(this)) {
            if (ctx.getSide().getAxis() == Direction.Axis.Y) {
                var behind = ctx.getWorld().getBlockState(ctx.getBlockPos().offset(against.get(DIRECTION).getOpposite()));

                state = this.getDefaultState().with(DIRECTION, against.get(DIRECTION)).with(VERTICAL, behind.isOf(this) ? DirectionValue.NEGATIVE : DirectionValue.POSITIVE);
            } else if (ctx.getSide().getAxis() == against.get(DIRECTION).getAxis()) {
                state = this.getDefaultState().with(DIRECTION, against.get(DIRECTION));
            }
            direction = against.get(DIRECTION);
        }


        if (state == null) {
            if (ctx.getSide().getAxis() != Direction.Axis.Y) {
                direction = ctx.getSide().getOpposite();
                state = this.getDefaultState().with(DIRECTION, ctx.getSide().getOpposite());
            } else {
                direction =  ctx.getHorizontalPlayerFacing();
                state = this.getDefaultState().with(DIRECTION, ctx.getHorizontalPlayerFacing());
            }
        }

        var upState = ctx.getWorld().getBlockState(ctx.getBlockPos().up());

        if (upState.isOf(this) && upState.get(VERTICAL).value != 0) {
            var vert = state.get(VERTICAL);
            state = state.with(VERTICAL, switch (vert) {
                case POSITIVE -> DirectionValue.POSITIVE_STACK;
                case NEGATIVE -> DirectionValue.NEGATIVE_STACK;
                default -> vert;
            });
        }



        return state
                .with(NEXT_CONVEYOR, ctx.getWorld().getBlockState(ctx.getBlockPos().offset(direction)).isIn(FactoryBlockTags.CONVEYORS))
                .with(PREVIOUS_CONVEYOR, ctx.getWorld().getBlockState(ctx.getBlockPos().offset(direction, -1)).isIn(FactoryBlockTags.CONVEYORS))

                .with(HAS_OUTPUT_TOP, ctx.getWorld().getBlockState(ctx.getBlockPos().up()).isIn(FactoryBlockTags.CONVEYOR_TOP_OUTPUT));
    }

    @Override
    public Block getPolymerBlock(BlockState state) {
        return Blocks.DEEPSLATE_TILE_STAIRS;
    }

    @Override
    public BlockState getPolymerBlockState(BlockState state) {
        if (state.get(VERTICAL) == DirectionValue.NONE) {
            return Blocks.BARRIER.getDefaultState();
        } else {
            return Blocks.ANDESITE_STAIRS.getDefaultState().with(StairsBlock.HALF, BlockHalf.BOTTOM).with(StairsBlock.FACING, state.get(VERTICAL).value == 1 ? state.get(DIRECTION) : state.get(DIRECTION).getOpposite());
        }
    }

    @Override
    public Collection<BlockNode> createNodes(BlockState state, ServerWorld world, BlockPos pos) {
        return List.of(new ConveyorNode(state.get(DIRECTION), state.get(VERTICAL)));
    }

    @Nullable
    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new ConveyorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(World world, BlockState state, BlockEntityType<T> type) {
        return world instanceof ServerWorld && type == FactoryBlockEntities.CONVEYOR ? ConveyorBlockEntity::tick : null;
    }

    @Override
    public TransferMode getTransferMode(BlockState selfState, Direction direction) {
        var dir = selfState.get(DIRECTION);

        if (dir == direction) {
            return TransferMode.TO_CONVEYOR;
        }

        return TransferMode.FROM_CONVEYOR;
    }

    public void onStateReplaced(BlockState state, World world, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            BlockEntity blockEntity = world.getBlockEntity(pos);
            if (blockEntity instanceof Inventory) {
                ItemScatterer.spawn(world, pos, (Inventory)blockEntity);
                world.updateComparators(pos, this);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }

    @Override
    public ElementHolder createElementHolder(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return new Model(world, initialBlockState);
    }

    @Override
    public Vec3d getElementHolderOffset(ServerWorld world, BlockPos pos, BlockState initialBlockState) {
        return FactoryUtil.HALF_BELOW;
    }

    @Override
    public boolean pushItemTo(BlockPointer self, Direction pushDirection, Direction relative, BlockPos conveyorPos, ContainerHolder conveyor) {
        var state = self.getBlockState();
        var vert = state.get(VERTICAL);
        if (!state.isOf(FactoryBlocks.STICKY_CONVEYOR) && vert.stack) {
            return true;
        }

        if (self.getBlockEntity() instanceof ConveyorBlockEntity be && be.isContainerEmpty()) {
            var selfDir = be.getCachedState().get(DIRECTION);

            if (selfDir == pushDirection) {
                be.setDelta(0);
            } else if (selfDir == pushDirection.getOpposite()) {
                be.setDelta(0.8);
            } else {
                be.setDelta(0.5);
            }

            be.pushAndAttach(conveyor.pullAndRemove());
            return true;
        }

        return true;
    }



    public enum DirectionValue implements StringIdentifiable {
        NONE(0, false),
        POSITIVE(1, false),
        POSITIVE_STACK(1, true),
        NEGATIVE(-1, false),
        NEGATIVE_STACK(-1, true);

        public final int value;
        public final boolean stack;

        DirectionValue(int value, boolean isStack) {
            this.value = value;
            this.stack = isStack;
        }

        @Override
        public String asString() {
            return this.name().toLowerCase(Locale.ROOT);
        }
    }

    public final class Model extends LodElementHolder implements ContainerHolder {
        private final Matrix4f mat = new Matrix4f();
        private final ItemDisplayElement base;
        private double speed;
        private Direction direction;
        private MovingItem movingItemContainer;
        private DirectionValue value;
        private double delta;

        private Model(ServerWorld world, BlockState state) {
            var type = state.get(ConveyorBlock.VERTICAL);
            this.base = new ItemDisplayElement(getModelForSpeed(0, type, state.isOf(FactoryBlocks.STICKY_CONVEYOR), state.get(PREVIOUS_CONVEYOR), state.get(NEXT_CONVEYOR)));
            this.base.setDisplaySize(1, 1);
            this.base.setModelTransformation(ModelTransformationMode.FIXED);
            this.addElement(this.base);
            this.updateAnimation(state.get(ConveyorBlock.DIRECTION), state.get(ConveyorBlock.VERTICAL));
        }

        private ItemStack getModelForSpeed(double speed, DirectionValue directionValue, boolean sticky, boolean prev, boolean next) {
            return (switch (directionValue) {
                case NONE -> (sticky ? ANIMATION_REGULAR_STICKY_X : ANIMATION_REGULAR_X)[(prev ? 1 : 0) + (next ? 2 : 0)];
                case POSITIVE, POSITIVE_STACK-> sticky ? ANIMATION_UP_STICKY :ANIMATION_UP;
                case NEGATIVE, NEGATIVE_STACK -> sticky ? ANIMATION_DOWN_STICKY :ANIMATION_DOWN;
            })[(int) Math.ceil(MathHelper.clamp(speed * FRAMES * 15, 0, FRAMES))];
        }

        private void updateAnimation(Direction dir, DirectionValue value) {
            if (dir != this.direction || value != this.value) {
                mat.identity().translate(0, 0.5f, 0).rotateY((90 - dir.asRotation()) * MathHelper.RADIANS_PER_DEGREE);
                if (value.value == -1) {
                    mat.rotateY(MathHelper.PI);
                };
                var x = value == DirectionValue.NONE;
                var v = x ? 0 : 0.0015f;
                mat.translate(v, v, 0);
                mat.scale(x ? 2.001f : 2.015f);
                this.base.setTransformation(mat);
            }
            this.direction = dir;
            this.value = value;
        }

        @Override
        protected void onTick() {
            var bs = BlockBoundAttachment.get(this).getBlockState();
            if (this.movingItemContainer != null) {
                this.movingItemContainer.checkItems();
            }
            this.updateAnimation(bs.get(ConveyorBlock.DIRECTION), bs.get(ConveyorBlock.VERTICAL));
        }

        @Override
        public void notifyUpdate(HolderAttachment.UpdateType updateType) {
            if (updateType == BlockBoundAttachment.BLOCK_STATE_UPDATE) {
                var state = BlockBoundAttachment.get(this).getBlockState();
                this.base.setItem(getModelForSpeed(speed, state.get(ConveyorBlock.VERTICAL), state.isOf(FactoryBlocks.STICKY_CONVEYOR), state.get(PREVIOUS_CONVEYOR), state.get(NEXT_CONVEYOR)));

                this.tick();
            }
        }

        public boolean updateSpeed(double speed) {
            if (this.speed != speed) {
                var state = BlockBoundAttachment.get(this).getBlockState();
                this.base.setItem(getModelForSpeed(speed, state.get(ConveyorBlock.VERTICAL), state.isOf(FactoryBlocks.STICKY_CONVEYOR), state.get(PREVIOUS_CONVEYOR), state.get(NEXT_CONVEYOR)));
                this.speed = speed;
                return true;
            }
            return false;
        }

        public void updateDelta(double oldDelta, double newDelta) {
            if (oldDelta == newDelta) {
                return;
            }
            this.delta = newDelta;

            if (movingItemContainer != null) {
                movingItemContainer.setPos(calculatePos(newDelta));
                var base = new Quaternionf().rotateY(MathHelper.HALF_PI * this.direction.getHorizontal());
                if (this.value.stack) {
                    base.rotateX(MathHelper.HALF_PI);
                } else if (this.value.value != 0) {
                    base.rotateX(MathHelper.HALF_PI / 2 * -this.value.value);
                }
                movingItemContainer.setRotation(base);
            }
        }

        private Vec3d calculatePos(double delta) {
            if (this.value.stack) {
                var visualDelta = MathHelper.clamp(delta, 0, 1);
                Vec3i vec3i = this.direction.getVector();

                return new Vec3d(
                        (-vec3i.getX() * this.value.value * 0.52),
                        this.value.value == -1 ? 1 - visualDelta : visualDelta,
                        (-vec3i.getZ() * this.value.value * 0.52)
                ).add(this.getPos());

            } else {
                var visualDelta = MathHelper.clamp(delta - 0.5, -0.5, 0.5);
                Vec3i vec3i = this.direction.getVector();

                return new Vec3d(
                        (vec3i.getX() * visualDelta),
                        MathHelper.clamp(this.value.value * visualDelta, -1f, 0f) + 1.05,
                        (vec3i.getZ() * visualDelta)
                ).add(this.getPos());
            }
        }

        @Override
        public MovingItem getContainer() {
            return this.movingItemContainer;
        }

        @Override
        public void setContainer(MovingItem container) {
            if (this.movingItemContainer != null) {
                this.removeElement(this.movingItemContainer);
            }

            this.movingItemContainer = container;
            if (container != null) {
                container.setPos(calculatePos(this.delta));
                updateDelta(-1, this.delta);
                this.addElement(container);
            }
        }

        @Override
        public void clearContainer() {
            if (this.movingItemContainer != null) {
                this.removeElement(this.movingItemContainer);
            }
            this.movingItemContainer = null;
        }

        @Override
        public MovingItem pullAndRemove() {
            var x = this.movingItemContainer;
            this.movingItemContainer = null;
            if (x != null) {
                this.removeElementWithoutUpdates(x);
            }

            return x;
        }

        @Override
        public void pushAndAttach(MovingItem container) {
            if (this.movingItemContainer != null) {
                this.removeElement(this.movingItemContainer);
            }

            this.movingItemContainer = container;
            updateDelta(-1, this.delta);
            this.addElementWithoutUpdates(container);
        }
    }
}
