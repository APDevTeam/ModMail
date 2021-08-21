package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.EmbedUtils;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class DirectMessageCommandListener extends ListenerAdapter {
    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent e) {
        final User u = e.getAuthor();
        if(u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();
        if(Settings.DEBUG)
            ModMail.getInstance().log("Received `'" + msg.getContentDisplay()
                    + "' from '" + u.getName() + "#" + u.getDiscriminator() + "'`", Color.YELLOW);

        boolean foundGuild = false;
        for(Guild g : u.getMutualGuilds()) {
            String id = g.getId();
            if (id.equals(Settings.MAIN_GUILD) || id.equals(Settings.INBOX_GUILD)) {
                foundGuild = true;
                break;
            }
        }
        // Player is not in the guild, try to send an invite message
        if(!foundGuild) {
            invite(e.getChannel(), u);
            return;
        }

        // Check for commands
        String content = msg.getContentStripped();
        if(!content.startsWith(Settings.PREFIX)) {
            return;
        }

        TextChannel modMailInbox = ModMail.getInstance().getModMailInbox(u);
        if(modMailInbox == null) {
            msg.getChannel().sendMessageEmbeds(EmbedUtils.buildEmbed(
                null,
                null,
                null,
                Color.RED,
                "Please open a ModMail before using commands.",
                null,
                null,
                null,
                null
            )).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send command warning: " + error.getMessage())
            );
            return;
        }

        String command = content.substring(1).split(" ")[0];
        switch (command) {
            case "add":
                add(msg);
                break;
            case "close":
                close(msg);
                break;
            default:
                invalidCommand(msg);
                break;
        }
    }

    private void invite(final @NotNull PrivateChannel channel, final @NotNull User author) {
        final MessageEmbed embed = EmbedUtils.buildEmbed(
                null,
                null,
                "Please join our discord server!",
                Color.RED,
                Settings.MAIN_INVITE,
                null,
                null,
                null,
                null
        );
        channel.sendMessageEmbeds(embed).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send invite embed '" + embed + "' to '" + author + "'")
        );
    }

    private void invalidCommand(final @NotNull Message msg) {
        try {
            msg.getChannel().sendMessageEmbeds(
                EmbedUtils.buildEmbed(
                    null,
                    null,
                    null,
                    Color.RED,
                    "Invalid command",
                    null,
                    null,
                    null,
                    null
                )
            ).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send invalid DM command: " + error.getMessage())
            );
        }
        catch (InsufficientPermissionException e) {
            ModMail.getInstance().error("Failed to send invalid DM command: " + e.getMessage());
        }
    }

    private void add(final @NotNull Message msg) {
        msg.getChannel().sendMessageEmbeds(EmbedUtils.buildEmbed(
                null,
                null,
                null,
                Color.RED,
                "Adding is not yet supported",
                null,
                null,
                null,
                null
        )).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send add warning: " + error.getMessage())
        );
    }

    private void close(final @NotNull Message msg) {
        final User u = msg.getAuthor();
        TextChannel inboxChannel = ModMail.getInstance().getModMailInbox(u);
        MessageChannel privateChannel = msg.getPrivateChannel();
        if(inboxChannel == null) {
            msg.getChannel().sendMessageEmbeds(EmbedUtils.buildEmbed(
                    null,
                    null,
                    null,
                    Color.RED,
                    "Failed to close",
                    null,
                    null,
                    null,
                    null
            )).queue(
                    null,
                    error -> ModMail.getInstance().error("Failed to send close warning: " + error.getMessage())
            );
            return;
        }

        // Log closing
        LogUtils.log(u.getId(), "User", msg.getAuthor().getName(), msg.getAuthor().getId(), "[Closed thread]");

        MessageEmbed embed = EmbedUtils.buildEmbed(
                u.getName(),
                u.getAvatarUrl(),
                "Thread Closed",
                Color.RED,
                null,
                "Staff",
                msg.getTimeCreated(),
                null,
                null
        );
        // Inform inbox
        inboxChannel.sendMessageEmbeds(embed).queue(
                // Inform DM
                unused -> privateChannel.sendMessageEmbeds(embed).queue(
                        // Archive channel
                        dm -> LogUtils.archive(u.getId(), ModMail.getInstance().getArchiveChannel(),
                                // Delete channel
                                unused2 -> inboxChannel.delete().queue(
                                        null,
                                        error -> ModMail.getInstance().error("Failed to delete channel: " + error.getMessage())
                                ),
                                // Error logging
                                error -> {
                                    ModMail.getInstance().error("Failed to close ModMail of " + u.getId() + ": " + error.getMessage());
                                    privateChannel.sendMessageEmbeds(EmbedUtils.buildEmbed(
                                            null,
                                            null,
                                            "Failed to close channel.",
                                            Color.RED,
                                            null,
                                            null,
                                            null,
                                            null,
                                            null
                                    )).queue(
                                            null,
                                            bad -> ModMail.getInstance().error("Failed to inform inbox of close failure: " + bad.getMessage())
                                    );
                                }
                        ),
                        error -> ModMail.getInstance().error("Failed to inform DM of close: " + error.getMessage())
                ),
                error -> ModMail.getInstance().error("Failed to inform inbox of close: " + error.getMessage())
        );
    }
}
