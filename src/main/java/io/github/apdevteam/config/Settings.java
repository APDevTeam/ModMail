package io.github.apdevteam.config;

import net.dv8tion.jda.api.utils.MiscUtil;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

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

    @Contract(pure = true)
    public static @NotNull Predicate<Object> snowflakeValidator() {
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
    public static @NotNull Predicate<Object> prefixValidator() {
        return o -> { // Prefix Validator
            if(!(o instanceof String))
                return false;

            String s = (String) o;
            if(s.length() < 1)
                return false;

            for(char ch : s.toCharArray()) {
                if(Character.isLetterOrDigit(ch))
                    return false;
            }
            return true;
        };
    }
}
