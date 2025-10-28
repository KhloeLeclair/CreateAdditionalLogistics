package dev.khloeleclair.create.additionallogistics.common.registries;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.foundation.utility.AdventureUtil;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.client.content.kinetics.lazy.FlexibleShaftScreen;
import dev.khloeleclair.create.additionallogistics.client.content.logistics.cashRegister.SalesLedgerScreen;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.base.AbstractLazySimpleKineticBlock;
import dev.khloeleclair.create.additionallogistics.common.content.kinetics.lazy.flexible.AbstractFlexibleShaftBlock;
import dev.khloeleclair.create.additionallogistics.common.content.logistics.cashRegister.SalesHistoryData;
import dev.khloeleclair.create.additionallogistics.common.utilities.IPromiseLimit;
import net.createmod.catnip.net.ClientboundPacket;
import net.createmod.catnip.net.ServerboundPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public class CALPackets {

    public interface CALPacket {
        default boolean runOnMainThread() {
            return true;
        }
    }

    private static int index = 0;

    public static final String PROTOCOL_VERSION = "3";

    public static final SimpleChannel INSTANCE = NetworkRegistry.ChannelBuilder
            .named(CreateAdditionalLogistics.asResource("main"))
            .serverAcceptedVersions(PROTOCOL_VERSION::equals)
            .clientAcceptedVersions(PROTOCOL_VERSION::equals)
            .simpleChannel();

    protected static <T extends ClientboundPacket> void registerClientbound(Class<T> type, Function<FriendlyByteBuf, T> factory, Consumer<T> handler) {
        INSTANCE.messageBuilder(type, index++, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(T::write)
                .decoder(factory)
                .consumerNetworkThread(clientHandler(handler))
                .add();
    }

    private static <T extends ClientboundPacket>BiConsumer<T, Supplier<NetworkEvent.Context>> clientHandler(Consumer<T> handler) {
        return (t, contextSupplier) -> {
            if (t instanceof CALPacket cp && cp.runOnMainThread()) {
                Minecraft.getInstance().execute(() -> handler.accept(t));
            } else
                handler.accept(t);
            contextSupplier.get().setPacketHandled(true);
        };
    }

    private static <T extends ServerboundPacket> void registerServerbound(Class<T> type, Function<FriendlyByteBuf, T> factory) {
        INSTANCE.messageBuilder(type, index++, NetworkDirection.PLAY_TO_SERVER)
                .encoder(T::write)
                .decoder(factory)
                .consumerNetworkThread(serverHandler())
                .add();
    }

    private static <T extends ServerboundPacket> BiConsumer<T, Supplier<NetworkEvent.Context>> serverHandler() {
        return (t, contextSupplier) -> {
            ServerPlayer sender = contextSupplier.get().getSender();
            var server = sender == null ? null : sender.getServer();
            if (t instanceof CALPacket cp && cp.runOnMainThread()) {
                if (server != null)
                    server.execute(() -> t.handle(server, sender));
            } else
                t.handle(sender != null ? sender.getServer() : null, sender);

            contextSupplier.get().setPacketHandled(true);
        };
    }

    public static void register() {

        registerClientbound(ServerToClientEvent.class, ServerToClientEvent::new, ServerToClientEvent.Handler::handle);
        registerClientbound(OpenFlexibleShaftScreen.class, OpenFlexibleShaftScreen::new, OpenFlexibleShaftScreen.Handler::handle);
        registerClientbound(OpenSalesLedgerScreen.class, OpenSalesLedgerScreen::new, OpenSalesLedgerScreen.Handler::handle);

        registerServerbound(ConfigureFlexibleShaft.class, ConfigureFlexibleShaft::new);
        registerServerbound(UpdateGaugePromiseLimit.class, UpdateGaugePromiseLimit::new);

    }

    public static class ServerToClientEvent implements ClientboundPacket, CALPacket {
        public static final ResourceLocation ID = CreateAdditionalLogistics.asResource("server_to_client_event");
        private final String key;

        public static final ServerToClientEvent CLEAR_INFORMATION = new ServerToClientEvent("clear_information");

        public ServerToClientEvent(String key) {
            this.key = key;
        }

        public ServerToClientEvent(FriendlyByteBuf buffer) {
            key = buffer.readUtf(32767);
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeUtf(key);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        public void send(ServerLevel level) {
            INSTANCE.send(PacketDistributor.DIMENSION.with(level::dimension), this);
        }

        public void send(ServerLevel level, BlockPos pos) {
            INSTANCE.send(PacketDistributor.NEAR.with(PacketDistributor.TargetPoint.p(pos.getX(), pos.getY(), pos.getZ(), 160, level.dimension())), this);
        }

        public static class Handler {
            public static void handle(ServerToClientEvent packet) {
                if (packet.key.equals("clear_information"))
                    AbstractLazySimpleKineticBlock.clearInformationWalkCache();
            }
        }
    }

    public static class ConfigureFlexibleShaft implements ServerboundPacket, CALPacket {

        public static final ResourceLocation ID = CreateAdditionalLogistics.asResource("configure_flexible_shaft");

        public final BlockPos pos;
        public final Direction side;
        public final byte mode;

        public static ConfigureFlexibleShaft of(BlockPos pos, Direction side, byte mode) {
            return new ConfigureFlexibleShaft(pos, side, mode);
        }

        public ConfigureFlexibleShaft(BlockPos pos, Direction side, byte mode) {
            this.pos = pos;
            this.side = side;
            this.mode = mode;
        }

        public ConfigureFlexibleShaft(FriendlyByteBuf buffer) {
            pos = buffer.readBlockPos();
            side = buffer.readEnum(Direction.class);
            mode = buffer.readByte();
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
            buffer.writeEnum(side);
            buffer.writeByte(mode);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public void handle(@Nullable MinecraftServer server, @Nullable ServerPlayer player) {
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;

            var level = player.serverLevel();
            if (level == null || !level.isLoaded(pos))
                return;

            var state = level.getBlockState(pos);
            if (!(state.getBlock() instanceof AbstractFlexibleShaftBlock fsb))
                return;

            fsb.setSide(level, pos, side, mode);
        }

        public void send() {
            INSTANCE.sendToServer(this);
        }

    }


    public static class OpenFlexibleShaftScreen implements ClientboundPacket, CALPacket {

        public static final ResourceLocation ID = CreateAdditionalLogistics.asResource("open_flexible_shaft_screen");

        public final BlockPos pos;

        public static OpenFlexibleShaftScreen of(BlockPos pos) {
            return new OpenFlexibleShaftScreen(pos);
        }

        public OpenFlexibleShaftScreen(BlockPos pos) {
            this.pos = pos;
        }

        public OpenFlexibleShaftScreen(FriendlyByteBuf buffer) {
            pos = buffer.readBlockPos();
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeBlockPos(pos);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        public void send(ServerPlayer player) {
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), this);
        }

        public static class Handler {
            public static void handle(OpenFlexibleShaftScreen packet) {
                if (Minecraft.getInstance().player == null)
                    return;

                Minecraft.getInstance().setScreen(new FlexibleShaftScreen(packet.pos));
            }
        }

    }

    public static class OpenSalesLedgerScreen implements ClientboundPacket, CALPacket {

        public static final ResourceLocation ID = CreateAdditionalLogistics.asResource("open_sales_ledger_screen");

        public final ItemStack stack;
        public final Map<UUID, String> playerNames;

        public OpenSalesLedgerScreen(ItemStack stack, Map<UUID, String> playerNames) {
            this.stack = stack;
            this.playerNames = playerNames;
        }

        public OpenSalesLedgerScreen(FriendlyByteBuf buffer) {
            stack = buffer.readItem();
            playerNames = buffer.readMap(HashMap::new, FriendlyByteBuf::readUUID, FriendlyByteBuf::readUtf);
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            buffer.writeItem(stack);
            buffer.writeMap(playerNames, FriendlyByteBuf::writeUUID, FriendlyByteBuf::writeUtf);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        public static class Handler {
            public static void handle(OpenSalesLedgerScreen packet) {
                if (Minecraft.getInstance().player == null)
                    return;

                Minecraft.getInstance().setScreen(new SalesLedgerScreen(packet.stack, packet.playerNames));
            }
        }

        public static Optional<OpenSalesLedgerScreen> create(ItemStack stack) {
            if (stack.isEmpty() || !stack.is(CALItems.SALES_LEDGER.get()))
                return Optional.empty();

            var history = SalesHistoryData.get(stack);
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
            INSTANCE.send(PacketDistributor.PLAYER.with(() -> player), this);
        }
    }

    public static class UpdateGaugePromiseLimit implements ServerboundPacket, CALPacket {

        public static final ResourceLocation ID = CreateAdditionalLogistics.asResource("update_gauge_promise_limit");

        public final FactoryPanelPosition pos;
        public final int limit;
        public final int additionalStock;

        public UpdateGaugePromiseLimit(FactoryPanelPosition pos, int limit, int additionalStock) {
            this.pos = pos;
            this.limit = limit;
            this.additionalStock = additionalStock;
        }

        public UpdateGaugePromiseLimit(FriendlyByteBuf buffer) {
            pos = FactoryPanelPosition.receive(buffer);
            limit = buffer.readInt();
            additionalStock = buffer.readInt();
        }

        @Override
        public void write(FriendlyByteBuf buffer) {
            pos.send(buffer);
            buffer.writeInt(limit);
            buffer.writeInt(additionalStock);
        }

        @Override
        public ResourceLocation getId() {
            return ID;
        }

        @Override
        public void handle(@Nullable MinecraftServer server, @Nullable ServerPlayer player) {
            if (player == null || player.isSpectator() || AdventureUtil.isAdventure(player))
                return;

            var level = player.serverLevel();
            if (!level.isLoaded(pos.pos()))
                return;

            if (!(level.getBlockEntity(pos.pos()) instanceof FactoryPanelBlockEntity be))
                return;

            var behavior = be.panels.get(pos.slot());
            if (!(behavior instanceof IPromiseLimit ipl))
                return;

            boolean changed = false;

            if (ipl.getCALPromiseLimit() != limit) {
                changed = true;
                ipl.setCALPromiseLimit(limit);
            }

            if (ipl.getCALAdditionalStock() != additionalStock) {
                changed = true;
                ipl.setCALAdditionalStock(additionalStock);
            }

            if (changed)
                be.notifyUpdate();
        }

        public void send() {
            INSTANCE.sendToServer(this);
        }

    }

}
