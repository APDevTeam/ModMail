package io.github.apdevteam.config;

import com.electronwill.nightconfig.core.ConfigSpec;
import com.electronwill.nightconfig.core.file.FileConfig;
import com.electronwill.nightconfig.core.file.FileConfigBuilder;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.Collection;
import java.util.Set;
import java.util.function.Predicate;

public class Blocked {
    @Nullable
    public static Set<String> BLOCKED_IDs = null;

    public static void load() {
        ConfigSpec spec = new ConfigSpec();
        spec.define("Blocked Users.Blocked", null, snowflakeCollectionValidator());

        File configFile = new File("blocked.toml");
        FileConfigBuilder builder = FileConfig.builder(configFile);
        FileConfig config = builder.defaultResource("/blocked.toml").sync().build();
        config.load();

        // do things

        config.close();
    }

    @Contract(pure = true)
    private static @NotNull Predicate<Object> snowflakeCollectionValidator() {
        return o -> {
            if(!(o instanceof Collection<?>))
                return false;

            Collection<String> cs = (Collection<?>) o;
            for(String s : cs) {
                try {
                    MiscUtil.parseSnowflake(s);
                } catch (NumberFormatException e) {
                    return false;
                }
            }
            return true;
        };
    }
}
