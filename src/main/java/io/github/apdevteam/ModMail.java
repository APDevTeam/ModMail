package io.github.apdevteam;

import io.github.apdevteam.config.Settings;
import io.github.apdevteam.listener.DirectMessageListener;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.ChunkingFilter;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.security.auth.login.LoginException;
import java.util.function.Consumer;

public class ModMail {
    private static ModMail instance = null;

    @NotNull
    public static ModMail getInstance() {
        return instance;
    }

    public static void main(String @NotNull [] args) {
        for(String arg : args) {
            if(arg.startsWith("-token=")) {
                Settings.TOKEN = arg.split("=")[1];
            }
            else if(arg.startsWith("-inbox=")) {
                Settings.INBOX_GUILD = arg.split("=")[1];
            }
            else if(arg.startsWith("-category=")) {
                Settings.INBOX_CATEGORY = arg.split("=")[1];
            }
            else if(arg.startsWith("-main=")) {
                Settings.MAIN_GUILD = arg.split("=")[1];
            }
            else if(arg.startsWith("-invite=")) {
                Settings.MAIN_INVITE = arg.split("=")[1];
            }
            else if(arg.startsWith("-debug")) {
                Settings.DEBUG = true;
            }
        }

        if("".equals(Settings.TOKEN)
                || "".equals(Settings.INBOX_GUILD) || "".equals(Settings.INBOX_CATEGORY)
                || "".equals(Settings.MAIN_GUILD) || "".equals(Settings.MAIN_INVITE)
        ) {
            System.err.println("Failed to load arguments, please read the code for 'help'.");
            return;
        }

        new ModMail();
    }

    @Nullable
    private JDA jda = null;
    @Nullable
    private Guild inboxGuild = null;
    @Nullable
    private Category inboxCategory = null;
    @Nullable
    private Guild mainGuild = null;

    public ModMail() {
        JDABuilder builder = JDABuilder.createDefault(Settings.TOKEN);
        builder.disableCache(
                CacheFlag.ACTIVITY,
                CacheFlag.CLIENT_STATUS,
                CacheFlag.EMOTE,
                CacheFlag.MEMBER_OVERRIDES,
                CacheFlag.ONLINE_STATUS,
                CacheFlag.ROLE_TAGS,
                CacheFlag.VOICE_STATE
        );
        builder.setChunkingFilter(ChunkingFilter.ALL);
        builder.setMemberCachePolicy(MemberCachePolicy.ALL);
        builder.enableIntents(
                GatewayIntent.GUILD_MEMBERS,
                //GatewayIntent.GUILD_EMOJIS,
                GatewayIntent.GUILD_MESSAGES,
                GatewayIntent.GUILD_MESSAGE_REACTIONS,
                GatewayIntent.DIRECT_MESSAGES,
                GatewayIntent.DIRECT_MESSAGE_REACTIONS
        );

        // Add shutdown hook for CTRL+C
        Runtime.getRuntime().addShutdownHook(new Thread(() -> ModMail.getInstance().shutdown()));

        try {
            jda = builder.build();
            jda.awaitReady();
        } catch (LoginException | InterruptedException e) {
            System.err.println("Failed to login to Discord!");
            e.printStackTrace();
            return;
        }

        for(Guild g : jda.getGuilds()) {
            if(g.getId().equals(Settings.INBOX_GUILD)) {
                inboxGuild = g;
                System.out.println("Found inbox guild: " + g.getName());
                break;
            }
        }
        if(inboxGuild == null) {
            System.err.println("Failed to find Guild!");
            return;
        }

        for(Guild g : jda.getGuilds()) {
            if(g.getId().equals(Settings.MAIN_GUILD)) {
                mainGuild = g;
                System.out.println("Found main guild: " + g.getName());
                break;
            }
        }
        if(mainGuild == null) {
            System.err.println("Failed to find Guild!");
            return;
        }

        for(Category c : inboxGuild.getCategories()) {
            if(c.getId().equals(Settings.INBOX_CATEGORY)) {
                inboxCategory = c;
                System.out.println("Found category: " + c.getName());
                break;
            }
        }
        if(inboxCategory == null) {
            System.err.println("Failed to find Category!");
            return;
        }

        jda.addEventListener(new DirectMessageListener());

        System.out.println("Sucessfully booted!");
        instance = this;
    }

    public void shutdown() {
        if(jda == null)
            return;

        System.out.println("Shutting down...");
        jda.shutdown();
        jda = null;
        System.out.println("Shut down.");
    }

    public void error(String error) {
        System.err.println(error);
    }


    @Nullable
    public TextChannel getModMail(@NotNull User user) {
        if(jda == null || inboxGuild == null)
            throw new IllegalStateException("JDA is in an invalid state");

        String userID = user.getId();
        for(TextChannel ch : inboxGuild.getTextChannels()) {
            if(userID.equals(ch.getTopic()))
                return ch;
        }
        return null;
    }

    public void createModMail(@NotNull User user, @NotNull Consumer<TextChannel> callback) throws InsufficientPermissionException {
        if(jda == null || inboxGuild == null || inboxCategory == null)
            throw new IllegalStateException("JDA is in an invalid state");

        inboxGuild.createTextChannel(user.getName(), inboxCategory).queue(
            callback,
            (error) -> error("Failed to create channel for '" + user + "'")
        );
    }
}
