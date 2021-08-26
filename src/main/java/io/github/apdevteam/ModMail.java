package io.github.apdevteam;

import io.github.apdevteam.config.Settings;
import io.github.apdevteam.listener.DirectMessageCommandListener;
import io.github.apdevteam.listener.InboxCommandListener;
import io.github.apdevteam.listener.DirectMessageListener;
import io.github.apdevteam.listener.InboxListener;
import io.github.apdevteam.utils.EmbedUtils;
import io.github.apdevteam.utils.LogUtils;
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
import java.io.File;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

public class ModMail {
    private static ModMail instance = null;

    @NotNull
    public static ModMail getInstance() {
        return instance;
    }

    public static void main(String @NotNull [] args) {
        if (!Settings.load()) {
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
    @Nullable
    private TextChannel archiveChannel = null;

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
            if(g.getId().equals(Settings.MAIN_GUILD)) {
                mainGuild = g;
                System.out.println("Found main guild: " + g.getName());
                break;
            }
        }
        if(mainGuild == null) {
            System.err.println("Failed to find main guild!");
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
            System.err.println("Failed to find inbox guild!");
            return;
        }

        for(Category c : inboxGuild.getCategories()) {
            if(c.getId().equals(Settings.INBOX_DEFAULT_CATEGORY)) {
                inboxCategory = c;
                System.out.println("Found inbox category: " + c.getName());
                break;
            }
        }
        if(inboxCategory == null) {
            System.err.println("Failed to find category!");
            return;
        }

        for(TextChannel ch : inboxGuild.getTextChannels()) {
            if(ch.getId().equals(Settings.INBOX_LOG_CHANNEL)) {
                logChannel = ch;
                System.out.println("Found log channel: " + ch.getName());
                break;
            }
        }
        if(logChannel == null) {
            System.err.println("Failed to find log channel!");
            return;
        }

        for(TextChannel ch : inboxGuild.getTextChannels()) {
            if(ch.getId().equals(Settings.INBOX_ARCHIVE_CHANNEL)) {
                archiveChannel = ch;
                System.out.println("Found archive channel: " + ch.getName());
            }
        }
        if(archiveChannel == null) {
            System.err.println("Failed to find archive channel!");
            return;
        }

        File modmailFolder = new File(".", LogUtils.folder);
        if(!modmailFolder.exists())
            if(!modmailFolder.mkdirs())
                System.err.println("Failed to create modmail folder!");
        if(!modmailFolder.isDirectory())
            System.err.println("Failed to create modmail folder!");
        if(!modmailFolder.exists())
            System.err.println("Failed to create modmail folder!");

        jda.addEventListener(new DirectMessageCommandListener());
        jda.addEventListener(new DirectMessageListener());
        jda.addEventListener(new InboxCommandListener());
        jda.addEventListener(new InboxListener());

        instance = this;
        log("Successfully booted!" + (Settings.DEBUG ? "\n        DEBUG ENABLED" : ""), Color.GREEN, true);
    }

    public void shutdown() {
        if(jda == null)
            return;

        log("Shutting down..." + (Settings.DEBUG ? "\n        DEBUG ENABLED" : ""), Color.RED, true);
        try {
            Thread.sleep(3750);
        }
        catch (InterruptedException e) {
            e.printStackTrace();
        }
        jda.shutdown();
        jda = null;
        System.out.println("Shut down.");
    }

    public void log(String message) {
        log(message, Color.BLUE, false);
    }

    public void log(String message, Color color) {
        log(message, color, false);
    }

    private void log(String message, Color color, boolean bold) {
        System.out.println(message);

        if(logChannel == null)
            error("JDA is in an invalid state");

        logChannel.sendMessageEmbeds(EmbedUtils.buildEmbed(
            null,
            null,
            bold ? message : null, // Send as title if bold
            color,
            bold ? null : message, // Send as text if not bold
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
            error ->  {
                System.err.println(error.getMessage());
                // Try sending a simple message if the embed failed
                logChannel.sendMessage("Failed to send: " + message).queue(
                    null,
                    null
                );
            }
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

    public void getModMail(@NotNull User user, @NotNull Consumer<PrivateChannel> callback) {
        if(jda == null)
            throw new IllegalStateException("JDA is in an invalid state");

        jda.openPrivateChannelById(user.getId()).queue(
            callback,
            error -> ModMail.getInstance().error("Failed to get ModMail for: '" + user.getName() + "#" + user.getDiscriminator() + "'")
        );
    }

    public void getUserbyID(@NotNull String userID, @NotNull Consumer<User> success, @Nullable Consumer<Throwable> failure) {
        if(jda == null)
            throw new IllegalStateException("JDA is in an invalid state");

        jda.retrieveUserById(userID).queue(
            success,
            failure
        );
    }

    public void createModMail(
        @NotNull User user,
        @NotNull OffsetDateTime timestamp,
        @NotNull Consumer<TextChannel> callback
    ) throws InsufficientPermissionException {

        if(jda == null || inboxGuild == null || inboxCategory == null)
            throw new IllegalStateException("JDA is in an invalid state");

        // Create log
        if(!LogUtils.create(user.getId())) {
            ModMail.getInstance().error("Failed to create ModMail log for: '" + user + "'");
            return;
        }

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

    @NotNull
    public TextChannel getArchiveChannel() {
        if(archiveChannel == null)
            throw new IllegalStateException("JDA is in an invalid state");

        return archiveChannel;
    }

    /* TODO:
        - Add the ability to add staff teams (with fancy reacts maybe?)
        - Add a replay ability to replay a ModMail to a channel?
     */
}
