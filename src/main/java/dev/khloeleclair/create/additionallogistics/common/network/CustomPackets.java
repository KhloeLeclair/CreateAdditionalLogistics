package dev.khloeleclair.create.additionallogistics.common.network;

import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelBlockEntity;
import com.simibubi.create.content.logistics.factoryBoard.FactoryPanelPosition;
import com.simibubi.create.foundation.utility.AdventureUtil;
import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import dev.khloeleclair.create.additionallogistics.client.screen.SalesLedgerScreen;
import dev.khloeleclair.create.additionallogistics.common.IPromiseLimit;
import dev.khloeleclair.create.additionallogistics.common.data.CustomComponents;
import dev.khloeleclair.create.additionallogistics.common.registries.CALItems;
import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
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

public class CustomPackets {

    public static void register(final RegisterPayloadHandlersEvent event) {
        final PayloadRegistrar registrar = event.registrar("1")
                .executesOn(HandlerThread.MAIN);

        registrar.playToServer(UpdateGaugePromiseLimit.TYPE, UpdateGaugePromiseLimit.STREAM_CODEC, UpdateGaugePromiseLimit::handle);

        registrar.playToClient(OpenSalesLedgerScreen.TYPE, OpenSalesLedgerScreen.STREAM_CODEC, (message, access) -> OpenSalesLedgerScreen.handle(message));

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

            var history = stack.get(CustomComponents.SALES_HISTORY);
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


    public record UpdateGaugePromiseLimit(FactoryPanelPosition pos, int limit) implements CustomPacketPayload {

        public static final CustomPacketPayload.Type<UpdateGaugePromiseLimit> TYPE = new CustomPacketPayload.Type<>(
                CreateAdditionalLogistics.asResource("update_gauge_promise_limit")
        );

        public static final StreamCodec<ByteBuf, UpdateGaugePromiseLimit> STREAM_CODEC = StreamCodec.composite(
                FactoryPanelPosition.STREAM_CODEC,
                UpdateGaugePromiseLimit::pos,
                ByteBufCodecs.INT,
                UpdateGaugePromiseLimit::limit,
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

            if (ipl.getPromiseLimit() == message.limit)
                return;

            ipl.setPromiseLimit(message.limit);
            be.notifyUpdate();
        }

        public void send() {
            PacketDistributor.sendToServer(this);
        }

    }

}
