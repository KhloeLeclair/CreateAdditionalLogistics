package dev.khloeleclair.create.additionallogistics.common.registries;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.foundation.utility.AdventureUtil;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy.FlexibleShaftScreen;
import dev.khloeleclair.create.additionallogistics.client.content.logistics.cashRegister.SalesLedgerScreen;
import dev.khloeleclair.create.additionallogistics.common.utilities.IPromiseLimit;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.FlexibleShaftBlockEntity;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLazySimpleKineticBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.AbstractFlexibleShaftBlock;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.registration.HandlerThread;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class CALPackets {

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("3")
                .executesOn(HandlerThread.MAIN);

        registrar.playToServer(UpdateGaugePromiseLimit.TYPE, UpdateGaugePromiseLimit.STREAM_CODEC, UpdateGaugePromiseLimit::handle);
        registrar.playToServer(ConfigureFlexibleShaft.TYPE, ConfigureFlexibleShaft.STREAM_CODEC, ConfigureFlexibleShaft::handle);
        registrar.playToServer(FinishedConfiguringFlexibleShaft.TYPE, FinishedConfiguringFlexibleShaft.STREAM_CODEC, FinishedConfiguringFlexibleShaft::handle);

        registrar.playToClient(OpenSalesLedgerScreen.TYPE, OpenSalesLedgerScreen.STREAM_CODEC, (message, access) -> OpenSalesLedgerScreen.handle(message));
        registrar.playToClient(OpenFlexibleShaftScreen.TYPE, OpenFlexibleShaftScreen.STREAM_CODEC, (message, access) -> OpenFlexibleShaftScreen.handle(message));
        registrar.playToClient(ServerToClientEvent.TYPE, ServerToClientEvent.STREAM_CODEC, (message, access) -> ServerToClientEvent.handle(message, access));

    }

    public record ServerToClientEvent(String key) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ServerToClientEvent> TYPE = new CustomPacketPayload.Type<>(
                CreateAdditionalLogistics.asResource("server_event")
        );

        public static final StreamCodec<FriendlyByteBuf, ServerToClientEvent> STREAM_CODEC = StreamCodec.composite(
                ByteBufCodecs.STRING_UTF8,
                ServerToClientEvent::key,
                ServerToClientEvent::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static final ServerToClientEvent CLEAR_INFORMATION = new ServerToClientEvent("clear_information");

        @OnlyIn(Dist.CLIENT)
        public static void handle(ServerToClientEvent message, IPayloadContext context) {
            if (message.key.equals("clear_information"))
                AbstractLazySimpleKineticBlock.clearInformationWalkCache();
        }

        public void send(ServerLevel level) {
            PacketDistributor.sendToPlayersInDimension(level, this);
        }

        public void send(ServerLevel level, BlockPos pos) {
            PacketDistributor.sendToPlayersNear(level, null, pos.getX(), pos.getY(), pos.getZ(), 160, this);
        }
    }

    public record FinishedConfiguringFlexibleShaft(BlockPos pos) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<FinishedConfiguringFlexibleShaft> TYPE = new CustomPacketPayload.Type<>(
                CreateAdditionalLogistics.asResource("finished_configuring_flexible_shaft")
        );

        public static final StreamCodec<FriendlyByteBuf, FinishedConfiguringFlexibleShaft> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                FinishedConfiguringFlexibleShaft::pos,
                FinishedConfiguringFlexibleShaft::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(FinishedConfiguringFlexibleShaft message, IPayloadContext access) {
            var player = access.player();
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;

            var pos = message.pos;

            var level = player.level();
            if (!level.isLoaded(pos) || !(level.getBlockEntity(pos) instanceof FlexibleShaftBlockEntity fsb))
                return;

            if (!fsb.shouldBeActive())
                fsb.deactivateSelf();
        }

        public static FinishedConfiguringFlexibleShaft of(BlockPos pos) {
            return new FinishedConfiguringFlexibleShaft(pos);
        }

        public void send() {
            PacketDistributor.sendToServer(this);
        }

    }

    public record ConfigureFlexibleShaft(BlockPos pos, Direction side, byte mode) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<ConfigureFlexibleShaft> TYPE = new CustomPacketPayload.Type<>(
                CreateAdditionalLogistics.asResource("configure_flexible_shaft")
        );

        public static final StreamCodec<FriendlyByteBuf, ConfigureFlexibleShaft> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                ConfigureFlexibleShaft::pos,
                Direction.STREAM_CODEC,
                ConfigureFlexibleShaft::side,
                ByteBufCodecs.BYTE,
                ConfigureFlexibleShaft::mode,
                ConfigureFlexibleShaft::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        public static void handle(ConfigureFlexibleShaft message, IPayloadContext access) {
            var player = access.player();
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;

            var pos = message.pos;

            var level = player.level();
            if (!level.isLoaded(pos))
                return;

            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof AbstractFlexibleShaftBlock fsb))
                return;

            fsb.setSide(level, pos, message.side, message.mode);
        }

        public static ConfigureFlexibleShaft of(BlockPos pos, Direction side, byte mode) {
            return new ConfigureFlexibleShaft(pos, side, mode);
        }

        public static ConfigureFlexibleShaft of(FlexibleShaftBlockEntity be, Direction side) {
            return new ConfigureFlexibleShaft(be.getBlockPos(), side, be.getSide(side));
        }

        public void send() {
            PacketDistributor.sendToServer(this);
        }

    }

    public record OpenFlexibleShaftScreen(BlockPos pos) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<OpenFlexibleShaftScreen> TYPE = new CustomPacketPayload.Type<>(
                CreateAdditionalLogistics.asResource("open_flexible_shaft_screen")
        );

        public static final StreamCodec<FriendlyByteBuf, OpenFlexibleShaftScreen> STREAM_CODEC = StreamCodec.composite(
                BlockPos.STREAM_CODEC,
                OpenFlexibleShaftScreen::pos,
                OpenFlexibleShaftScreen::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() { return TYPE; }

        @OnlyIn(Dist.CLIENT)
        public static void handle(OpenFlexibleShaftScreen message) {
            Minecraft.getInstance().setScreen(new FlexibleShaftScreen(message.pos));
        }

        public static OpenFlexibleShaftScreen of(BlockPos pos) {
            return new OpenFlexibleShaftScreen(pos);
        }

        public void send(ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, this);
        }

    }


    public record OpenSalesLedgerScreen(ItemStack stack, Map<UUID, String> playerNames) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<OpenSalesLedgerScreen> TYPE = new CustomPacketPayload.Type<>(
                CreateAdditionalLogistics.asResource("open_sales_ledger_screen")
        );

        public static final StreamCodec<RegistryFriendlyByteBuf, OpenSalesLedgerScreen> STREAM_CODEC = StreamCodec.composite(
                ItemStack.STREAM_CODEC,
                OpenSalesLedgerScreen::stack,
                ByteBufCodecs.map(HashMap::new, UUIDUtil.STREAM_CODEC, ByteBufCodecs.STRING_UTF8),
                OpenSalesLedgerScreen::playerNames,
                OpenSalesLedgerScreen::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        @OnlyIn(Dist.CLIENT)
        public static void handle(OpenSalesLedgerScreen message) {
            Minecraft.getInstance().setScreen(new SalesLedgerScreen(message.stack, message.playerNames));
        }

        public static Optional<OpenSalesLedgerScreen> create(ItemStack stack) {
            if (stack.isEmpty() || !stack.is(CALItems.SALES_LEDGER))
                return Optional.empty();

            var history = stack.get(CALDataComponents.SALES_HISTORY);
            var server = CreateAdditionalLogistics.getServer();
            Map<UUID, String> playerNames = new HashMap<>();

            if (history != null && server != null) {
                var cache = server.getProfileCache();
                if (cache != null)
                    for (UUID id : history.playerMap().values()) {
                        cache.get(id).ifPresent(profile -> {
                            String name = profile.getName();
                            if (name != null && !name.isEmpty())
                                playerNames.put(id, name);
                        });
                    }
            }

            return Optional.of(new OpenSalesLedgerScreen(stack, playerNames));
        }

        public void send(ServerPlayer player) {
            PacketDistributor.sendToPlayer(player, this);
        }

    }


    public record UpdateGaugePromiseLimit(FactoryPanelPosition pos, int limit, int additionalStock) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<UpdateGaugePromiseLimit> TYPE = new CustomPacketPayload.Type<>(
                CreateAdditionalLogistics.asResource("update_gauge_promise_limit")
        );

        public static final StreamCodec<ByteBuf, UpdateGaugePromiseLimit> STREAM_CODEC = StreamCodec.composite(
                FactoryPanelPosition.STREAM_CODEC,
                UpdateGaugePromiseLimit::pos,
                ByteBufCodecs.INT,
                UpdateGaugePromiseLimit::limit,
                ByteBufCodecs.INT,
                UpdateGaugePromiseLimit::additionalStock,
                UpdateGaugePromiseLimit::new
        );

        @Override
        public Type<? extends CustomPacketPayload> type() {
            return TYPE;
        }

        public static void handle(UpdateGaugePromiseLimit message, IPayloadContext access) {
            var player = access.player();
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;

            var pos = message.pos;

            var level = player.level();
            if (!level.isLoaded(pos.pos()))
                return;

            if (!(level.getBlockEntity(pos.pos()) instanceof FactoryPanelBlockEntity be))
                return;

            var behavior = be.panels.get(pos.slot());
            if (!(behavior instanceof IPromiseLimit ipl))
                return;

            boolean changed = false;

            if (ipl.getCALPromiseLimit() != message.limit) {
                changed = true;
                ipl.setCALPromiseLimit(message.limit);
            }

            if (ipl.getCALAdditionalStock() != message.additionalStock) {
                changed = true;
                ipl.setCALAdditionalStock(message.additionalStock);
            }

            if (changed)
                be.notifyUpdate();
        }

        public void send() {
            PacketDistributor.sendToServer(this);
        }

    }

}
