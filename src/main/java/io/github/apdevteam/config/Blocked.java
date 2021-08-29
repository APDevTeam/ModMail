package io.github.apdevteam.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileConfigBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;

public class Blocked {
    @Nullable
    public static List<String> BLOCKED_IDS = null;
    public static String APPEAL_LINK = null;

    public static boolean load() {
        // Define config specification
        ConfigSpec spec = new ConfigSpec();
        spec.define("Blocked.Appeal", "");
        spec.define("Blocked.Users", new ArrayList<>(), Blocked.snowflakeListValidator());

        // Load config
        File configFile = new File("blocked.toml");
        FileConfigBuilder builder = FileConfig.builder(configFile);
        FileConfig config = builder.defaultResource("/blocked.toml").sync().build();
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

        // Load and verify BLOCKED_IDs
        Collection<Object> collection = config.getOrElse("Blocked.Users", () -> null);
        if(collection == null)
            return false;
        Blocked.BLOCKED_IDS = new ArrayList<>();
        for(Object o : collection) {
            if(snowflakeValidator().test(o))
                Blocked.BLOCKED_IDS.add((String) o);
        }

        // Load config into Blocked
        Blocked.APPEAL_LINK = config.getOrElse("Blocked.Appeal", "");

        config.save();
        config.close();

        // Verify config
        return !"".equals(Blocked.APPEAL_LINK);
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
    private static @NotNull Predicate<Object> snowflakeListValidator() {
        return o -> {
            if(!(o instanceof List<?> l))
                return false;

            for(Object s : l) {
                if(!(s instanceof String))
                    return false;

                try {
                    MiscUtil.parseSnowflake((String) s);
                }
                catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        };
    }

    public static boolean block(@NotNull User u) {
        if(BLOCKED_IDS == null)
            return false;

        String userID = u.getId();

        File configFile = new File("blocked.toml");
        FileConfigBuilder builder = FileConfig.builder(configFile);
        FileConfig config = builder.defaultResource("/blocked.toml").sync().build();
        config.load();
        BLOCKED_IDS.add(userID);
        config.set("Blocked.Users", BLOCKED_IDS);

        config.save();
        config.close();

        return true;
    }

    public static boolean unblock(@NotNull User u) {
        if(BLOCKED_IDS == null)
            return false;

        String userID = u.getId();

        File configFile = new File("blocked.toml");
        FileConfigBuilder builder = FileConfig.builder(configFile);
        FileConfig config = builder.defaultResource("/blocked.toml").sync().build();
        config.load();
        BLOCKED_IDS.remove(userID);
        config.set("Blocked.Users", BLOCKED_IDS);

        config.save();
        config.close();

        return true;
    }

    // TODO: Implement a config spec and validator for blocked users
}
