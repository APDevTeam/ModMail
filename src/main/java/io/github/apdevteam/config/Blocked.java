package io.github.apdevteam.config;

import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileConfigBuilder;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Predicate;

public class Blocked {
    @Nullable
    public static Set<String> BLOCKED_IDs = null;
    public static String APPEAL_LINK = null;

    public static void load() {
        File configFile = new File("blocked.toml");
        FileConfigBuilder builder = FileConfig.builder(configFile);
        FileConfig config = builder.defaultResource("/blocked.toml").sync().build();
        config.load();

        Collection<Object> collection = config.getOrElse("Blocked.Users", null);
        BLOCKED_IDs = new HashSet<>();
        for(Object o : collection) {
            if(snowflakeValidator().test(o))
                BLOCKED_IDs.add((String) o);
        }
        APPEAL_LINK = config.getOrElse("Blocked.Appeal", "");

        config.close();
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

    // TODO: Implement a config spec and validator for blocked users
}
