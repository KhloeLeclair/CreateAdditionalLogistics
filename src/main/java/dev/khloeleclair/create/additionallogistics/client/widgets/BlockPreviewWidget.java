package dev.khloeleclair.create.additionallogistics.client.widgets;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import com.mojang.math.Axis;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy.LowEntityKineticBlockEntityRenderer;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.createmod.catnip.data.Iterate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FastColor;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.InventoryMenu;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.client.RenderTypeHelper;
import net.minecraftforge.client.model.data.ModelData;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

// This is almost entirely taken from the EnderIO source code, available at:
// https://github.com/Team-EnderIO/EnderIO/blob/dev/1.21.1/enderio/src/main/java/com/enderio/enderio/client/foundation/widgets/ioconfig/IOConfigOverlay.java#L74
// which itself is based on work by XFactHD and code taken from Patchouli.
// Patchouli's license can be found here: https://github.com/VazkiiMods/Patchouli/blob/1.21.x/LICENSE
// EnderIO's license can be found here: https://github.com/Team-EnderIO/EnderIO/blob/dev/1.21.1/LICENSE.txt

public class BlockPreviewWidget extends AbstractWidget {

    private static final Quaternionf ROT_180_Z = Axis.ZP.rotation((float) Math.PI);
    private static final Vec3 RAY_ORIGIN = new Vec3(1.5, 1.5, 1.5);
    private static final Vec3 RAY_START = new Vec3(1.5, 1.5, -1);
    private static final Vec3 RAY_END = new Vec3(1.5, 1.5, 3);
    private static final BlockPos POS = new BlockPos(1, 1, 1);
    private static final int Z_OFFSET = 100;
    private static final Minecraft MINECRAFT = Minecraft.getInstance();

    private static final ResourceLocation SELECTED_ICON = CreateAdditionalLogistics.asResource("block/highlighted");

    private static MultiBufferSource.BufferSource ghostBuffers;
    private static MultiBufferSource.BufferSource solidBuffers;

    private final Vector3f worldOrigin;

    @Nullable
    private Predicate<Direction> canSelectDirection;
    @Nullable
    private BiConsumer<Direction, Integer> clickedDirection;

    private final BlockPos position;
    private final List<BlockPos> neighbors = new ArrayList<>();

    private float scale = 80;
    private float pitch;
    private float yaw;

    private boolean didClick = false;

    private boolean neighborsVisible = true;
    @Nullable
    private SelectedFace selection;

    public BlockPreviewWidget(int x, int y, int width, int height, BlockPos position) {
        super(x, y, width, height, Component.empty());

        this.position = position;
        worldOrigin = new Vector3f(position.getX() + 0.5f, position.getY() + 0.5f, position.getZ() + 0.5f);

        for(Direction dir: Iterate.directions)
            neighbors.add(position.relative(dir));

        /*for(int ix = -1; ix <= 1; ix++)
            for(int iy = -1; iy <= 1; iy++)
                for(int iz = -1; iz <= 1; iz++)
                    neighbors.add(position.offset(ix, iy, iz));*/

        pitch = MINECRAFT.player.getXRot();
        yaw = MINECRAFT.player.getYRot();

        initBuffers(MINECRAFT.renderBuffers().bufferSource());
    }

    public BlockPreviewWidget canSelectDirection(@Nullable Predicate<Direction> predicate) {
        this.canSelectDirection = predicate;
        return this;
    }

    public BlockPreviewWidget onClick(@Nullable BiConsumer<Direction, Integer> consumer) {
        this.clickedDirection = consumer;
        return this;
    }

    private void initBuffers(MultiBufferSource.BufferSource original) {
        BufferBuilder fallback = original.builder;
        Map<RenderType, BufferBuilder> layerBuffers = original.fixedBuffers;
        Map<RenderType, BufferBuilder> ghostLayers = new Object2ObjectLinkedOpenHashMap<>();
        Map<RenderType, BufferBuilder> solidLayers = new Object2ObjectLinkedOpenHashMap<>();

        for (Map.Entry<RenderType, BufferBuilder> e : layerBuffers.entrySet()) {
            ghostLayers.put(GhostRenderLayer.remap(e.getKey()), e.getValue());
            solidLayers.put(SolidRenderLayer.remap(e.getKey()), e.getValue());
        }
        ghostBuffers = new GhostBuffers(fallback, ghostLayers);
        solidBuffers = new SolidBuffers(fallback, solidLayers);
    }

    private static Vec3 transform(Vec3 vec, Matrix4f transform) {
        // Move vector to a (0,0,0) origin as the transformation matrix expects
        Vector4f vec4 = new Vector4f((float) (vec.x - RAY_ORIGIN.x), (float) (vec.y - RAY_ORIGIN.y),
                (float) (vec.z - RAY_ORIGIN.z), 1F);
        // Apply the transformation matrix
        vec4.mul(transform);
        // Move transformed vector back to the actual origin
        return new Vec3(vec4.x() + RAY_ORIGIN.x, vec4.y() + RAY_ORIGIN.y, vec4.z() + RAY_ORIGIN.z);
    }

    @Nullable
    private BlockHitResult raycast(BlockPos pos, BlockState state, float diffX, float diffY, Matrix4f transform) {
        // Add mouse offset to start and end vectors
        Vec3 start = RAY_START.add(diffX, diffY, 0);
        Vec3 end = RAY_END.add(diffX, diffY, 0);

        // Rotate start and end vectors around the block
        start = transform(start, transform);
        end = transform(end, transform);

        // Get block's shape and cast a ray through it
        VoxelShape shape = Shapes.block();
        //VoxelShape shape = state.getShape(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        Vector3f centerPos = new Vector3f(pos.getX() + 0.5f, pos.getY() + 0.5f, pos.getZ() + 0.5f).sub(worldOrigin);
        shape = shape.move(centerPos.x(), centerPos.y(), centerPos.z());
        return shape.clip(start, end, POS);
    }

    public void toggleNeighborVisibility() {
        neighborsVisible = !neighborsVisible;
    }

    @Override
    public void mouseMoved(double mouseX, double mouseY) {
        didClick = false;
        super.mouseMoved(mouseX, mouseY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (active && visible && didClick) {
            if (selection != null && clickedDirection != null)
                clickedDirection.accept(selection.side, button);
        }

        didClick = false;
        return false;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        didClick = true;
        return false;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        didClick = false;

        if (visible && isValidClickButton(button) && isMouseOver(mouseX, mouseY)) {
            double dx = dragX / (double) MINECRAFT.getWindow().getGuiScaledWidth();
            double dy = dragY / (double) MINECRAFT.getWindow().getGuiScaledHeight();

            yaw += 4 * (float) dx * 180;
            pitch += 2 * (float) dy * 180;

            pitch = Math.min(80, Math.max(-80, pitch));
        }

        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double delta) {
        if (visible) {
            scale -= (float) delta;
            scale = Math.min(160, Math.max(10, scale));
            return true;
        }

        return false;
    }

    @Override
    protected void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        if (!visible)
            return;

        int x = getX();
        int y = getY();

        LowEntityKineticBlockEntityRenderer.overrideVisualization = true;

        try {
            guiGraphics.enableScissor(x, y, x + width, y + height);

            // Calculate widget center
            int centerX = getX() + (width / 2);
            int centerY = getY() + (height / 2);
            // Calculate mouse offset from center and scale to the block space
            float diffX = (mouseX - centerX) / scale;
            float diffY = (mouseY - centerY) / scale;

            Quaternionf rotPitch = Axis.XN.rotationDegrees(pitch);
            Quaternionf rotYaw = Axis.YP.rotationDegrees(yaw);

            // Build block transformation matrix
            // Rotate 180 around Z, otherwise the block is upside down
            Quaternionf blockTransform = new Quaternionf(ROT_180_Z);
            // Rotate around X (pitch) in negative direction
            blockTransform.mul(rotPitch);
            // Rotate around Y (yaw)
            blockTransform.mul(rotYaw);

            // Draw block
            renderWorld(guiGraphics, centerX, centerY, blockTransform, partialTick);

            // Build ray transformation matrix
            // Rotate 180 around Z, otherwise the block is upside down
            Matrix4f rayTransform = new Matrix4f();
            rayTransform.set(ROT_180_Z);
            // Rotate around Y (yaw)
            rayTransform.rotate(rotYaw);
            // Rotate around X (pitch) in negative direction
            rayTransform.rotate(rotPitch);

            // Ray-cast hit on block shape
            Map<BlockHitResult, BlockPos> hits = new HashMap<>();

            BlockState state = MINECRAFT.level.getBlockState(position);
            BlockHitResult hit = raycast(position, state, diffX, diffY, rayTransform);
            if (hit != null && hit.getType() != HitResult.Type.MISS) {
                if (canSelectDirection == null || canSelectDirection.test(hit.getDirection()))
                    hits.put(hit, position);
            }

            Vec3 eyePosition = transform(RAY_START, rayTransform).add(worldOrigin.x, worldOrigin.y, worldOrigin.z);
            selection = hits.entrySet()
                    .stream()
                    .min(Comparator.comparingDouble(entry -> entry.getValue().distToCenterSqr(eyePosition))) // find
                    // closest
                    // to eye
                    .map(closest -> new SelectedFace(closest.getValue(), closest.getKey().getDirection()))
                    .orElse(null);

            renderSelection(guiGraphics, centerX, centerY, blockTransform);

            guiGraphics.disableScissor();

        } finally {
            LowEntityKineticBlockEntityRenderer.overrideVisualization = false;
        }

    }

    private void renderWorld(GuiGraphics guiGraphics, int centerX, int centerY, Quaternionf transform,
                             float partialTick) {
        Lighting.setupForFlatItems();
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, Z_OFFSET);
        guiGraphics.pose().scale(scale, scale, -scale);
        guiGraphics.pose().mulPose(transform);

        // RenderNeighbours
        if (neighborsVisible) {
            for (var neighbour : neighbors) {
                Vector3f pos = new Vector3f(neighbour.getX() - worldOrigin.x(), neighbour.getY() - worldOrigin.y(),
                        neighbour.getZ() - worldOrigin.z());
                renderBlock(guiGraphics, neighbour, pos, ghostBuffers, partialTick);
            }

        }
        ghostBuffers.endBatch();

        // Render our main block
        Vector3f pos = new Vector3f(position.getX() - worldOrigin.x(), position.getY() - worldOrigin.y(),
                position.getZ() - worldOrigin.z());

        renderBlock(guiGraphics, position, pos, solidBuffers, partialTick);

        solidBuffers.endBatch();

        guiGraphics.pose().popPose();
        Lighting.setupFor3DItems();
    }

    private void renderBlock(GuiGraphics guiGraphics, BlockPos blockPos, Vector3f renderPos,
                             MultiBufferSource.BufferSource buffers, float partialTick) {
        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(renderPos.x(), renderPos.y(), renderPos.z());

        ModelData modelData = Optional.ofNullable(MINECRAFT.level.getModelDataManager().getAt(blockPos))
                .orElse(ModelData.EMPTY);

        BlockState blockState = MINECRAFT.level.getBlockState(blockPos);

        RenderShape rendershape = blockState.getRenderShape();
        if (rendershape != RenderShape.INVISIBLE) {
            if (rendershape == RenderShape.MODEL) {
                var renderer = MINECRAFT.getBlockRenderer();
                BakedModel bakedmodel = renderer.getBlockModel(blockState);
                modelData = bakedmodel.getModelData(MINECRAFT.level, blockPos, blockState, modelData);
                int blockColor = MINECRAFT.getBlockColors().getColor(blockState, MINECRAFT.level, blockPos, 0);
                float r = FastColor.ARGB32.red(blockColor) / 255F;
                float g = FastColor.ARGB32.green(blockColor) / 255F;
                float b = FastColor.ARGB32.blue(blockColor) / 255F;
                for (RenderType renderType : bakedmodel.getRenderTypes(blockState, RandomSource.create(42), modelData)) {
                    renderer.getModelRenderer()
                            .renderModel(guiGraphics.pose().last(),
                                    buffers.getBuffer(RenderTypeHelper.getEntityRenderType(renderType, false)), blockState,
                                    bakedmodel, r, g, b, LightTexture.FULL_BRIGHT, OverlayTexture.NO_OVERLAY, modelData,
                                    renderType);
                }
            }
            BlockEntity blockEntity = MINECRAFT.level.getBlockEntity(blockPos);
            if (blockEntity != null) {
                var beRenderer = MINECRAFT.getBlockEntityRenderDispatcher().getRenderer(blockEntity);
                if (beRenderer != null) {
                    beRenderer.render(blockEntity, partialTick, guiGraphics.pose(), buffers, LightTexture.FULL_BRIGHT,
                            OverlayTexture.NO_OVERLAY);
                }

            }
        }
        guiGraphics.pose().popPose();
    }

    private void renderSelection(GuiGraphics guiGraphics, int centerX, int centerY, Quaternionf transform) {
        if (selection == null)
            return;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().translate(centerX, centerY, Z_OFFSET);
        guiGraphics.pose().scale(scale, scale, -scale);
        guiGraphics.pose().mulPose(transform);

        BufferBuilder bufferbuilder = Tesselator.getInstance().getBuilder();
        bufferbuilder.begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);

        TextureAtlasSprite tex = MINECRAFT.getTextureAtlas(InventoryMenu.BLOCK_ATLAS).apply(SELECTED_ICON);
        RenderSystem.setShaderTexture(0, tex.atlasLocation());
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);

        BlockPos blockPos = selection.blockPos;
        guiGraphics.pose()
                .translate(blockPos.getX() - worldOrigin.x(), blockPos.getY() - worldOrigin.y(),
                        blockPos.getZ() - worldOrigin.z());
        Vector3f[] vec = createQuadVerts(selection.side, 0, 1, 1);
        Matrix4f matrix4f = guiGraphics.pose().last().pose();
        bufferbuilder.vertex(matrix4f, vec[0].x(), vec[0].y(), vec[0].z())
                .color(1F, 1F, 1F, 1F)
                .uv(tex.getU0(), tex.getV0());
        bufferbuilder.vertex(matrix4f, vec[1].x(), vec[1].y(), vec[1].z())
                .color(1F, 1F, 1F, 1F)
                .uv(tex.getU0(), tex.getV1());
        bufferbuilder.vertex(matrix4f, vec[2].x(), vec[2].y(), vec[2].z())
                .color(1F, 1F, 1F, 1F)
                .uv(tex.getU1(), tex.getV1());
        bufferbuilder.vertex(matrix4f, vec[3].x(), vec[3].y(), vec[3].z())
                .color(1F, 1F, 1F, 1F)
                .uv(tex.getU1(), tex.getV0());
        BufferUploader.drawWithShader(bufferbuilder.end());

        guiGraphics.pose().popPose();
    }

    public static Vector3f[] createQuadVerts(Direction face, float leftEdge, float rightEdge, float elevation) {
        return switch (face) {
            case DOWN -> new Vector3f[] {
                    new Vector3f(leftEdge, 1 - elevation, leftEdge),
                    new Vector3f(rightEdge, 1 - elevation, leftEdge),
                    new Vector3f(rightEdge, 1 - elevation, rightEdge),
                    new Vector3f(leftEdge, 1 - elevation, rightEdge)
            };
            case UP -> new Vector3f[] {
                    new Vector3f(leftEdge, elevation, leftEdge),
                    new Vector3f(leftEdge, elevation, rightEdge),
                    new Vector3f(rightEdge, elevation, rightEdge),
                    new Vector3f(rightEdge, elevation, leftEdge)
            };
            case NORTH -> new Vector3f[] {
                    new Vector3f(rightEdge, rightEdge, 1 - elevation),
                    new Vector3f(rightEdge, leftEdge, 1 - elevation),
                    new Vector3f(leftEdge, leftEdge, 1 - elevation),
                    new Vector3f(leftEdge, rightEdge, 1 - elevation)
            };
            case SOUTH -> new Vector3f[] {
                    new Vector3f(leftEdge, rightEdge, elevation),
                    new Vector3f(leftEdge, leftEdge, elevation),
                    new Vector3f(rightEdge, leftEdge, elevation),
                    new Vector3f(rightEdge, rightEdge, elevation)
            };
            case WEST -> new Vector3f[] {
                    new Vector3f(1 - elevation, rightEdge, leftEdge),
                    new Vector3f(1 - elevation, leftEdge, leftEdge),
                    new Vector3f(1 - elevation, leftEdge, rightEdge),
                    new Vector3f(1 - elevation, rightEdge, rightEdge)
            };
            case EAST -> new Vector3f[] {
                    new Vector3f(elevation, rightEdge, rightEdge),
                    new Vector3f(elevation, leftEdge, rightEdge),
                    new Vector3f(elevation, leftEdge, leftEdge),
                    new Vector3f(elevation, rightEdge, leftEdge)
            };
        };
    }

    @Override
    protected void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {

    }

    private record SelectedFace(BlockPos blockPos, Direction side) { }

    private static class GhostBuffers extends MultiBufferSource.BufferSource {
        private GhostBuffers(BufferBuilder fallback, Map<RenderType, BufferBuilder> layerBuffers) {
            super(fallback, layerBuffers);
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return super.getBuffer(GhostRenderLayer.remap(type));
        }
    }

    private static class SolidBuffers extends MultiBufferSource.BufferSource {
        private SolidBuffers(BufferBuilder fallback, Map<RenderType, BufferBuilder> layerBuffers) {
            super(fallback, layerBuffers);
        }

        @Override
        public VertexConsumer getBuffer(RenderType type) {
            return super.getBuffer(SolidRenderLayer.remap(type));
        }
    }

    private static class SolidRenderLayer extends RenderType {
        private static final Map<RenderType, RenderType> REMAPPED_TYPES = new IdentityHashMap<>();

        private SolidRenderLayer(RenderType original) {
            super(String.format("%s_%s_solid", original, CreateAdditionalLogistics.MODID), original.format(), original.mode(),
                    original.bufferSize(), original.affectsCrumbling(), true, () -> {
                        original.setupRenderState();

                        //RenderSystem.disableDepthTest();
                    }, () -> {
                        //RenderSystem.enableDepthTest();

                        original.clearRenderState();
                    });
        }

        public static RenderType remap(RenderType in) {
            if (in instanceof SolidRenderLayer) {
                return in;
            } else {
                return REMAPPED_TYPES.computeIfAbsent(in, SolidRenderLayer::new);
            }
        }
    }

    private static class GhostRenderLayer extends RenderType {
        private static final Map<RenderType, RenderType> REMAPPED_TYPES = new IdentityHashMap<>();

        private GhostRenderLayer(RenderType original) {
            super(String.format("%s_%s_ghost", original, CreateAdditionalLogistics.MODID), original.format(), original.mode(),
                    original.bufferSize(), original.affectsCrumbling(), true, () -> {
                        original.setupRenderState();

                        RenderSystem.disableDepthTest();
                        RenderSystem.enableBlend();
                        RenderSystem.setShaderColor(1, 1, 1, 0.5f);
                    }, () -> {
                        RenderSystem.setShaderColor(1, 1, 1, 1);
                        RenderSystem.disableBlend();
                        RenderSystem.enableDepthTest();

                        original.clearRenderState();
                    });
        }

        public static RenderType remap(RenderType in) {
            if (in instanceof GhostRenderLayer) {
                return in;
            } else {
                return REMAPPED_TYPES.computeIfAbsent(in, GhostRenderLayer::new);
            }
        }
    }

}
