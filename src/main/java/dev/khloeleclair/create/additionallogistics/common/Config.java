package dev.khloeleclair.create.additionallogistics.common;

import net.neoforged.fml.ModContainer;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

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
        modContainer.registerConfig(ModConfig.Type.CLIENT, clientSpec);
        modContainer.registerConfig(ModConfig.Type.COMMON, commonSpec);
        modContainer.registerConfig(ModConfig.Type.SERVER, serverSpec);
    }

    public static class _Server {
        public final ModConfigSpec.BooleanValue currencyConversion;
        public final ModConfigSpec.BooleanValue stockTickersConvertToo;

        _Server(ModConfigSpec.Builder builder) {
            builder.comment("Currency Conversion").push("currencyConversion");

            currencyConversion = builder
                    .comment("When enabled, items that are freely converted to and from other items (for example, 9 Diamonds equal 1 Diamond Block) will be converted automatically to make shopping easier. Otherwise, only specifically defined currencies will work.")
                    .translation(t("currency-conversion.compression"))
                    .define("compression", true);

            stockTickersConvertToo = builder
                    .comment("Allow Stock Tickers to perform Currency Conversion as well.")
                    .translation(t("currency-conversion.stock-tickers"))
                    .define("allowStockTickers", false);

            builder.pop();
        }
    }


    public static class _Common {
        public final ModConfigSpec.BooleanValue enablePromiseLimits;
        public final ModConfigSpec.BooleanValue protectStockKeeperSeats;
        public final ModConfigSpec.BooleanValue globAllowRegex;
        public final ModConfigSpec.BooleanValue globOptimize;
        public final ModConfigSpec.IntValue maxStarHeight;
        public final ModConfigSpec.IntValue maxRepetitions;
        public final ModConfigSpec.BooleanValue allowBackrefs;

        public final ModConfigSpec.DoubleValue acceleratorStressImpact;

        _Common(ModConfigSpec.Builder builder) {

            enablePromiseLimits = builder
                    .comment("Adds a new configurable value to factory gauges that allows configuring how many promises each gauge can have at a time.")
                    .translation(t("enable-promise-limits"))
                    .define("enablePromiseLimits", true);

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
        _Client(ModConfigSpec.Builder builder) {

        }
    }

    static final ModConfigSpec commonSpec;
    public static final _Common Common;

    static {
        var pair = new ModConfigSpec.Builder().configure(_Common::new);
        commonSpec = pair.getRight();
        Common = pair.getLeft();
    }

    static final ModConfigSpec clientSpec;
    public static final _Client Client;

    static {
        var pair = new ModConfigSpec.Builder().configure(_Client::new);
        clientSpec = pair.getRight();
        Client = pair.getLeft();
    }

    static final ModConfigSpec serverSpec;
    public static final _Server Server;

    static {
        var pair = new ModConfigSpec.Builder().configure(_Server::new);
        serverSpec = pair.getRight();
        Server = pair.getLeft();
    }

}
