package dev.khloeleclair.create.additionallogistics.common.utilities;

import dev.khloeleclair.create.additionallogistics.CreateAdditionalLogistics;
import it.unimi.dsi.fastutil.objects.Object2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.world.level.Level;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.fml.util.thread.SidedThreadGroups;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.function.Consumer;

public class SidedCache {

    private static final Map<Class<?>, SidedCache> CLIENT_CACHE = new Object2ObjectOpenHashMap<>();
    private static final Map<Class<?>, SidedCache> SERVER_CACHE = new Object2ObjectOpenHashMap<>();

    public static <T extends SidedCache> void runOnEachCache(final Class<T> type, Consumer<T> method) {
        synchronized (CLIENT_CACHE) {
            T cached = (T) CLIENT_CACHE.get(type);
            if (cached != null)
                method.accept(cached);
        }

        synchronized (SERVER_CACHE) {
            T cached = (T) SERVER_CACHE.get(type);
            if (cached != null)
                method.accept(cached);
        }
    }

    public static <T extends SidedCache> T getCache(final Class<T> type) {
        final boolean is_server = Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER;
        final Map<Class<?>, SidedCache> map = is_server ? SERVER_CACHE : CLIENT_CACHE;

        synchronized (map) {
            T cached = (T) map.get(type);
            if (cached != null)
                return cached;

            try {
                cached = type.getDeclaredConstructor(boolean.class).newInstance(is_server);
            } catch(ReflectiveOperationException ex) {
                throw new RuntimeException("Failed to create cache for type: " + type.getName(), ex);
            }

            map.put(type, cached);
            return cached;
        }
    }

    @Nullable
    public static <T extends SidedCache> T getCacheIfInitialized(final Class<T> type) {
        final boolean is_server = Thread.currentThread().getThreadGroup() == SidedThreadGroups.SERVER;
        final Map<Class<?>, SidedCache> map = is_server ? SERVER_CACHE : CLIENT_CACHE;

        synchronized (map) {
            T cached = (T) map.get(type);
            if (cached != null)
                return cached;
        }

        return null;
    }

    public final boolean isServer;

    public SidedCache(boolean isServer) {
        this.isServer = isServer;
    }

    @Nullable
    public Level getLevel() {
        return isServer ? getServerLevel() : getClientLevel();
    }

    @Nullable
    private static Level getServerLevel() {
        var server = CreateAdditionalLogistics.getServer();
        return server == null ? null : server.overworld();
    }

    @OnlyIn(Dist.CLIENT)
    @Nullable
    private static Level getClientLevel() {
        return Minecraft.getInstance().level;
    }

}
