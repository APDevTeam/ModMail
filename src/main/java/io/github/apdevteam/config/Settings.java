package io.github.apdevteam.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileConfigBuilder;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.function.Predicate;

public class Settings {
    public static boolean DEBUG = false;
    public static String PREFIX = "%";
    public static String TOKEN = "";

    public static String INBOX_GUILD = "";
    public static String INBOX_DEFAULT_CATEGORY = "";
    public static String INBOX_LOG_CHANNEL = "";
    public static String INBOX_ARCHIVE_CHANNEL = "";

    public static String MAIN_GUILD = "";
    public static String MAIN_INVITE = "";

    public static boolean load() {
        // Define config specification
        ConfigSpec spec = new ConfigSpec();
        spec.define("Debug", false);
        spec.define("Token", "", Settings.tokenValidator());
        spec.define("Prefix", "%", Settings.prefixValidator());
        spec.define("Inbox.Guild", "", Settings.snowflakeValidator());
        spec.define("Inbox.Category", "", Settings.snowflakeValidator());
        spec.define("Inbox.Log", "", Settings.snowflakeValidator());
        spec.define("Inbox.Archive", "", Settings.snowflakeValidator());
        spec.define("Main.Guild", "", Settings.snowflakeValidator());
        spec.define("Main.Invite", "", Settings.inviteValidator());

        // Load config
        File configFile = new File("config.toml");
        FileConfigBuilder builder = FileConfig.builder(configFile);
        FileConfig config = builder.defaultResource("/config.toml").sync().build();
        config.load();

        // Check config against spec
        spec.correct(config, (correctionAction, path, from, to) -> {
            String s = " config value '" + String.join(".", path) + "' from '" + from + "' to '" + to + "'";
            switch(correctionAction) {
                case ADD:
                    System.err.println("Added" + s);
                case REMOVE:
                    System.err.println("Removed" + s);
                case REPLACE:
                default:
                    System.err.println("Corrected" + s);
            }
        });

        // Load into Settings
        Settings.DEBUG = config.getOrElse("Debug", false);
        Settings.TOKEN = config.getOrElse("Token", "");
        Settings.PREFIX = config.getOrElse("Prefix", "%");

        Settings.INBOX_GUILD = config.getOrElse("Inbox.Guild", "");
        Settings.INBOX_DEFAULT_CATEGORY = config.getOrElse("Inbox.Category", "");
        Settings.INBOX_LOG_CHANNEL = config.getOrElse("Inbox.Log", "");
        Settings.INBOX_ARCHIVE_CHANNEL = config.getOrElse("Inbox.Archive", "");

        Settings.MAIN_GUILD = config.getOrElse("Main.Guild", "");
        Settings.MAIN_INVITE = config.getOrElse("Main.Invite", "");


        // Verify config
        return !"".equals(Settings.TOKEN) && !"".equals(Settings.PREFIX)
            && !"".equals(Settings.INBOX_GUILD) && !"".equals(Settings.INBOX_DEFAULT_CATEGORY)
            && !"".equals(Settings.INBOX_LOG_CHANNEL) && !"".equals(Settings.INBOX_ARCHIVE_CHANNEL)
            && !"".equals(Settings.MAIN_GUILD) && !"".equals(Settings.MAIN_INVITE);
    }

    @Contract(pure = true)
    private static @NotNull Predicate<Object> tokenValidator() {
        return o -> {
            if(!(o instanceof String s))
                return false;

            return s.length() >= 1;
        };
    }

    @Contract(pure = true)
    private static @NotNull Predicate<Object> snowflakeValidator() {
        return o -> {
            if(!(o instanceof String))
                return false;

            try {
                MiscUtil.parseSnowflake((String) o);
            }
            catch (NumberFormatException e) {
                return false;
            }
            return true;
        };
    }

    @Contract(pure = true)
    private static @NotNull Predicate<Object> prefixValidator() {
        return o -> { // Prefix Validator
            if(!(o instanceof String s))
                return false;

            if(s.length() < 1)
                return false;

            for(char ch : s.toCharArray()) {
                if(Character.isLetterOrDigit(ch))
                    return false;
            }
            return true;
        };
    }

    @Contract(pure = true)
    private static @NotNull Predicate<Object> inviteValidator() {
        return o -> {
            if(!(o instanceof String s))
                return false;

            return s.length() >= 1;
        };
    }
}
