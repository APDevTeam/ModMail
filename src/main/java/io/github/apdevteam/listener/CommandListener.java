package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.Consumer;

public class CommandListener extends ListenerAdapter {
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        final User u = e.getAuthor();
        if (u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();
        if (Settings.DEBUG)
            ModMail.getInstance().log("Received `'" + msg.getContentDisplay()
                    + "' from '" + u.getName() + "#" + u.getDiscriminator()
                    + "' in '" + msg.getChannel() + "'`", Color.YELLOW);

        if (!msg.getGuild().getId().equals(Settings.INBOX_GUILD))
            return;

        String content = msg.getContentStripped();
        if (!content.startsWith(Settings.PREFIX))
            return;

        String command = content.substring(1).split(" ")[0];
        switch (command) {
            case "open":
                open(msg);
                break;
            case "reply":
                reply(msg);
                break;
            case "close":
                close(msg);
                break;
            default:
                invalidCommand(msg);
                break;
        }
    }

    private void open(final @NotNull Message msg) {
        String userID = msg.getContentStripped().substring(1).split(" ")[1];
        final User u = User.fromId(userID);

        TextChannel textChannel = ModMail.getInstance().getModMailInbox(u);
        if (textChannel != null) {
            try {
                textChannel.sendMessageEmbeds(EmbedUtils.buildEmbed(
                    null,
                    null,
                    "This user already has a ModMail channel.",
                    Color.RED,
                    null,
                    null,
                    null,
                    null,
                    null
                )).queue(
                    message -> msg.delete().queue(
                        null,
                        error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage())
                    ),
                    error -> ModMail.getInstance().error("Failed to send open ModMail warning: " + error.getMessage())
                );
            }
            catch (InsufficientPermissionException error) {
                ModMail.getInstance().error("Failed to delete message: " + error.getMessage());
            }
            return;
        }

        // Create ModMail
        try {
            ModMail.getInstance().createModMail(
                u,
                msg.getTimeCreated(),
                channel -> ModMail.getInstance().getModMail(
                    u,
                    // Inform player
                    privateChannel -> privateChannel.sendMessageEmbeds(
                        EmbedUtils.buildEmbed(
                            msg.getAuthor().getName(),
                            msg.getAuthor().getAvatarUrl(),
                            null,
                            Color.YELLOW,
                            "Opened a ModMail session.",
                            null,
                            null,
                            null,
                            null
                        )
                    ).queue(
                        // Delete command
                        message -> msg.delete().queue(
                            null,
                            error -> ModMail.getInstance().error("Failed to delete open command: " + error.getMessage())
                        ),
                        error -> ModMail.getInstance().error("Failed to send open ModMail message: " + error.getMessage())
                    )
                )
            );
        }
        catch (InsufficientPermissionException error) {
            ModMail.getInstance().error("Failed to delete message: " + error.getMessage());
        }
    }

    private void reply(final @NotNull Message msg) {
        final TextChannel inboxChannel = msg.getTextChannel();
        String userID = inboxChannel.getTopic();
        if(userID == null) {
            invalidInbox(inboxChannel, msg);
            return;
        }

        final User u = User.fromId(userID);
        if (!u.getId().equals(inboxChannel.getTopic())) {
            invalidInbox(inboxChannel, msg);
            return;
        }

        final String content = msg.getContentDisplay().substring(6);
        try {
            ModMail.getInstance().getModMail(
                u,
                // Forward text to DM
                (
                    (Consumer<PrivateChannel>) privateChannel -> EmbedUtils.forwardText(
                        msg.getAuthor(),
                        content,
                        privateChannel,
                        Color.GREEN,
                        null,
                        "Staff",
                        msg.getTimeCreated()
                    )
                    // Forward text to inbox
                ).andThen(
                    (
                        (Consumer<PrivateChannel>) privateChannel -> EmbedUtils.forwardText(
                            msg.getAuthor(),
                            content,
                            inboxChannel,
                            Color.GREEN,
                            null,
                            "Staff",
                            msg.getTimeCreated()
                        )
                        // Forward attachments to DM
                    ).andThen(
                        (
                            (Consumer<PrivateChannel>) privateChannel -> EmbedUtils.forwardAttachments(msg, Color.GREEN, privateChannel)
                            // Forward attachments to inbox
                        ).andThen(
                            (
                                (Consumer<PrivateChannel>) privateChannel -> EmbedUtils.forwardAttachments(msg, Color.GREEN, inboxChannel)
                                // Delete message
                            ).andThen(
                                privateChannel -> msg.delete().queue(
                                    null,
                                    error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage())
                                )
                            )
                        )
                    )
                )
            );
        }
        catch (InsufficientPermissionException error) {
            ModMail.getInstance().error("Failed to delete: " + error.getMessage());
        }
    }

    private void close(final @NotNull Message msg) {
        final TextChannel inboxChannel = msg.getTextChannel();
        String userID = inboxChannel.getTopic();
        if(userID == null) {
            invalidInbox(inboxChannel, msg);
            return;
        }

        final User u = User.fromId(userID);
        if (!u.getId().equals(inboxChannel.getTopic())) {
            invalidInbox(inboxChannel, msg);
            return;
        }

        inboxChannel.sendMessageEmbeds(EmbedUtils.buildEmbed(
            null,
            null,
            "Closing is not yet supported.",
            Color.RED,
            null,
            null,
            null,
            null,
            null
        )).queue(
            null,
            error -> ModMail.getInstance().error("Failed to warn: " + error.getMessage())
        );
    }

    private void invalidInbox(final @NotNull TextChannel channel, final @NotNull Message msg) {
        try {
            channel.sendMessageEmbeds(EmbedUtils.buildEmbed(
                null,
                null,
                "This is not a ModMail inbox.",
                Color.RED,
                null,
                null,
                msg.getTimeCreated(),
                null,
                null
            )).queue(
                message -> msg.delete().queue(
                    null,
                    error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage())
                ),
                error -> ModMail.getInstance().error("Failed to warn reply: " + error.getMessage())
            );
        }
        catch (InsufficientPermissionException error) {
            ModMail.getInstance().error("Failed to delete: " + error.getMessage());
        }
    }

    private void invalidCommand(final @NotNull Message msg) {
        try {
            msg.getChannel().sendMessageEmbeds(
                EmbedUtils.buildEmbed(
                    null,
                    null,
                    "Invalid command",
                    Color.RED,
                    msg.getContentDisplay(),
                    null,
                    msg.getTimeCreated(),
                    null,
                    null
                )
            ).queue(
                message -> msg.delete().queue(
                    null,
                    error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage())
                ),
                error -> ModMail.getInstance().error("Failed to send invalid command: " + error.getMessage())
            );
        }
        catch (InsufficientPermissionException e) {
            ModMail.getInstance().error("Failed to send invalid command: " + e.getMessage());
        }
    }
}
