package io.github.apdevteam;

import io.github.apdevteam.config.Settings;
import io.github.apdevteam.listener.CommandListener;
import io.github.apdevteam.listener.DirectMessageListener;
import io.github.apdevteam.utils.EmbedUtils;
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
import java.awt.*;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

public class ModMail {
    private static ModMail instance = null;

    @NotNull
    public static ModMail getInstance() {
        return instance;
    }

    public static void main(String @NotNull [] args) {
        for(String arg : args) {
            if(arg.startsWith("-token=")) { // Required
                Settings.TOKEN = arg.split("=")[1];
            }
            else if(arg.startsWith("-inbox=")) { // Required
                Settings.INBOX_GUILD = arg.split("=")[1];
            }
            else if(arg.startsWith("-category=")) { // Required
                Settings.INBOX_CATEGORY = arg.split("=")[1];
            }
            else if(arg.startsWith("-main=")) { // Required
                Settings.MAIN_GUILD = arg.split("=")[1];
            }
            else if(arg.startsWith("-invite=")) { // Required
                Settings.MAIN_INVITE = arg.split("=")[1];
            }
            else if(arg.startsWith("-log=")) { // Required
                Settings.LOG_CHANNEL = arg.split("=")[1];
            }
            else if(arg.startsWith("-prefix=")) { // Optional
                Settings.PREFIX = arg.split("=")[1];
            }
            else if(arg.startsWith("-debug")) { // Optional
                Settings.DEBUG = true;
            }
        }

        if("".equals(Settings.TOKEN) || "".equals(Settings.PREFIX)
                || "".equals(Settings.INBOX_GUILD) || "".equals(Settings.INBOX_CATEGORY)
                || "".equals(Settings.MAIN_GUILD) || "".equals(Settings.MAIN_INVITE)
                || "".equals(Settings.LOG_CHANNEL)
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
    @Nullable
    private TextChannel logChannel = null;

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

        for(TextChannel ch : inboxGuild.getTextChannels()) {
            if(ch.getId().equals(Settings.LOG_CHANNEL)) {
                logChannel = ch;
                System.out.println("Found channel: " + ch.getName());
                break;
            }
        }
        if(logChannel == null) {
            System.err.println("Failed to find Channel!");
            return;
        }

        jda.addEventListener(new DirectMessageListener());
        jda.addEventListener(new CommandListener());

        instance = this;
        log("Successfully booted!" + (Settings.DEBUG ? "\n        DEBUG ENABLED" : ""), Color.GREEN);
    }

    public void shutdown() {
        if(jda == null)
            return;

        log("Shutting down..." + (Settings.DEBUG ? "\n        DEBUG ENABLED" : ""), Color.RED);
        try {
            Thread.sleep(5000);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        jda.shutdown();
        jda = null;
        System.out.println("Shut down.");
    }

    public void log(String message) {
        log(message, Color.BLUE);
    }

    public void log(String message, Color color) {
        System.out.println(message);

        if(logChannel == null)
            error("JDA is in an invalid state");

        logChannel.sendMessageEmbeds(EmbedUtils.buildEmbed(
                null,
                null,
                null,
                color,
                message,
                null,
                null,
                null,
                null
        )).queue(
                null,
                error -> error(error.getMessage())
        );
    }

    public void error(String message) {
        System.err.println(message);

        if(logChannel == null)
            System.err.println("JDA is in an invalid state");

        logChannel.sendMessageEmbeds(EmbedUtils.buildEmbed(
            null,
            null,
            null,
            Color.RED,
            message,
            null,
            null,
            null,
            null
        )).queue(
            null,
            error -> System.err.println(error.getMessage())
        );
    }


    @Nullable
    public TextChannel getModMailInbox(@NotNull User user) {
        if(jda == null || inboxGuild == null)
            throw new IllegalStateException("JDA is in an invalid state");

        String userID = user.getId();
        for(TextChannel ch : inboxGuild.getTextChannels()) {
            if(userID.equals(ch.getTopic()))
                return ch;
        }
        return null;
    }

    public void getModMail(@NotNull User user, @Nullable Consumer<PrivateChannel> callback) {
        if(jda == null)
            throw new IllegalStateException("JDA is in an invalid state");

        jda.openPrivateChannelById(user.getId()).queue(
            callback,
            error -> ModMail.getInstance().error("Failed to get ModMail for: '" + user.getName() + "#" + user.getDiscriminator() + "'")
        );
    }

    public void createModMail(
            @NotNull User user,
            @NotNull OffsetDateTime timestamp,
            @NotNull Consumer<TextChannel> callback
    ) throws InsufficientPermissionException {

        if(jda == null || inboxGuild == null || inboxCategory == null)
            throw new IllegalStateException("JDA is in an invalid state");

        // Create channel
        inboxGuild.createTextChannel(user.getName(), inboxCategory).queue(
            (
                // Then set topic
                (Consumer<TextChannel>) channel -> channel.getManager().setTopic(user.getId()).queue(
                    null,
                    error -> ModMail.getInstance().error("Failed to set topic for '" + channel + "'")
                )
            ).andThen(
                (
                    // Then add initial message
                    (Consumer<TextChannel>) channel -> channel.sendMessageEmbeds(
                        EmbedUtils.buildEmbed(
                            user.getName(),
                            user.getAvatarUrl(),
                            null,
                            Color.CYAN,
                            "ModMail thread started.",
                            "User ID: " + user.getId(),
                            timestamp,
                            user.getAvatarUrl(),
                            null
                        )
                    ).queue(
                        null,
                        error -> ModMail.getInstance().error("Failed to send initial message for '" + channel + "'")
                    )
                ).andThen(
                    // Then call callback
                    callback
                )
            ),
            (error) -> error("Failed to create channel for '" + user + "'")
        );
    }
}
