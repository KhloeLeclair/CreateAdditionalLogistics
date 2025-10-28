package dev.khloeleclair.create.additionallogistics.common;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.IConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

// An example config class. This is not required, but it's a good idea to have one to keep your config organized.
// Demonstrates how to use Neo's config APIs
public class Config {

    public enum CurrencyMode {
        DISABLED,
        ENABLED,
        AUTO
    }

    private static String t(String path) {
        return CALLang.key("config." + path);
    }

    public static void register(ModContainer modContainer) {
        ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, clientSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, commonSpec);
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, serverSpec);
    }

    public static class _Server {
        public final ForgeConfigSpec.BooleanValue currencyCompression;
        public final ForgeConfigSpec.BooleanValue currencyStockTicker;

        public final ForgeConfigSpec.BooleanValue trainInventoryAccess;
        public final ForgeConfigSpec.BooleanValue trainWriting;
        public final ForgeConfigSpec.BooleanValue trainArrivalEvents;

        _Server(ForgeConfigSpec.Builder builder) {
            builder.comment("Currency Conversion").push("currencyConversion");

            currencyCompression = builder
                    .comment("When enabled, items that are freely converted to and from other items (for example, 9 Diamonds equal 1 Diamond Block) will be converted automatically to make shopping easier. Otherwise, only specifically defined currencies will work.")
                    .translation(t("currency-conversion.compression"))
                    .define("compression", true);

            currencyStockTicker = builder
                    .comment("Allow Stock Tickers to perform Currency Conversion as well.")
                    .translation(t("currency-conversion.stock-tickers"))
                    .define("allowStockTickers", false);

            builder.pop();

            builder.comment("Train Network Monitor").push("trainNetworkMonitor");

            trainInventoryAccess = builder
                    .comment("When enabled, a train's inventory and fluid contents can be read using a Train Network Monitor Peripheral. When disabled, only information about packages is available.")
                    .translation(t("trains.network-monitor.allow-inventory"))
                    .define("allowInventoryAccess", false);

            trainWriting = builder
                    .comment("When enabled, trains and stations can be modified using a Train Network Monitor Peripheral. When disabled, data can only be read.")
                    .translation(t("trains.network-monitor.allow-writing"))
                    .define("allowWriting", false);

            trainArrivalEvents = builder
                    .comment("When enabled, Train Network Monitors will emit events whenever a train arrives or departs a station on the monitored train network.")
                    .translation(t("trains.network-monitor.arrival-events"))
                    .define("arrivalEvents", true);

            builder.pop();
        }
    }


    public static class _Common {
        public final ForgeConfigSpec.BooleanValue enablePromiseLimits;
        public final ForgeConfigSpec.BooleanValue enableAdditionalStock;
        public final ForgeConfigSpec.BooleanValue protectStockKeeperSeats;
        public final ForgeConfigSpec.BooleanValue globAllowRegex;
        public final ForgeConfigSpec.BooleanValue globOptimize;
        public final ForgeConfigSpec.IntValue maxStarHeight;
        public final ForgeConfigSpec.IntValue maxRepetitions;
        public final ForgeConfigSpec.BooleanValue allowBackrefs;

        public final ForgeConfigSpec.DoubleValue acceleratorStressImpact;

        _Common(ForgeConfigSpec.Builder builder) {

            enablePromiseLimits = builder
                    .comment("Adds a new configurable value to factory gauges that allows configuring how many promises each gauge can have at a time.")
                    .translation(t("enable-promise-limits"))
                    .define("enablePromiseLimits", true);

            enableAdditionalStock = builder
                    .comment("Adds a new configurable value to Restocker factory gauges that allows configuring the gauge to request additional items when restocking.")
                    .translation(t("enable-additional-stock"))
                    .define("enableAdditionalStock", true);

            protectStockKeeperSeats = builder
                    .comment("Prevent players from accidentally sitting in Seats holding a Stock Keeper.")
                    .translation(t("protect-seats"))
                    .define("protectStockKeeperSeats", true);

            builder.comment("Package Addresses").push("addresses");

            globOptimize = builder
                    .comment("Use optimized logic and caching for package address matching.")
                    .translation(t("addresses.optimize"))
                    .define("globOptimize", true);

            globAllowRegex = builder
                    .comment("Allow the user of regular expressions timestamp matching packages in Create (Frogports, Postboxes, Package Filters, etc.) with the \"RegEx:\" prefix.")
                    .translation(t("addresses.allowRegex"))
                    .define("globAllowRegex", true);

            builder.comment("Regex Safety").push("regexSafety");

            maxStarHeight = builder
                    .comment("Maximum star height to allow in one regular expression. This is intended to prevent catastrophic backtracking.")
                    .translation(t("regex.starHeight"))
                    .defineInRange("maxStarHeight", 1, 0, 6);

            maxRepetitions = builder
                    .comment("Maximum repetitions to allow in one regular expression. This is intended to minimize overall work.")
                    .translation(t("regex.maxRepetitions"))
                    .defineInRange("maxRepetitions", 1000, 0, Integer.MAX_VALUE);

            allowBackrefs = builder
                    .comment("Whether or not to allow backreferences in regular expressions.")
                    .translation(t("regex.allowBackrefs"))
                    .define("allowBackrefs", false);

            builder.pop();

            builder.pop();

            builder.comment("Kinetics").push("kinetics");

            builder.comment("Package Accelerator").push("packageAccelerator");

            acceleratorStressImpact = builder
                    .comment("The stress impact of the Package Accelerator")
                    .translation(t("kinetics.stress-impact"))
                    .defineInRange("acceleratorStressImpact", 4.0, 1, 100);

            builder.pop();

            builder.pop();

        }
    }

    public static class _Client {
        _Client(ForgeConfigSpec.Builder builder) {

        }
    }

    static final IConfigSpec commonSpec;
    public static final _Common Common;

    static {
        var pair = new ForgeConfigSpec.Builder().configure(_Common::new);
        commonSpec = pair.getRight();
        Common = pair.getLeft();
    }

    static final ForgeConfigSpec clientSpec;
    public static final _Client Client;

    static {
        var pair = new ForgeConfigSpec.Builder().configure(_Client::new);
        clientSpec = pair.getRight();
        Client = pair.getLeft();
    }

    static final ForgeConfigSpec serverSpec;
    public static final _Server Server;

    static {
        var pair = new ForgeConfigSpec.Builder().configure(_Server::new);
        serverSpec = pair.getRight();
        Server = pair.getLeft();
    }

}
