package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Arrays;

public class CommandListener extends ListenerAdapter {
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        final User u = e.getAuthor();
        if (u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();
        if (Settings.DEBUG)
            System.out.println("Received '" + msg.getContentDisplay()
                    + "' from '" + u.getName() + "#" + u.getDiscriminator()
                    + "' in '" + msg.getChannel() + "'");

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
                // TODO
                break;
            default:
                invalidCommand(msg);
                break;
        }
    }

    private void open(final @NotNull Message msg) {
        String userID = msg.getContentStripped().split("\\\\s+")[1];
        final User u = User.fromId(userID);

        TextChannel textChannel = ModMail.getInstance().getModMailInbox(u);
        if (textChannel != null) {
            textChannel.sendMessageEmbeds(EmbedUtils.buildEmbed(
                    null,
                    null,
                    null,
                    Color.RED,
                    "This user already has a ModMail channel.",
                    null,
                    msg.getTimeCreated(),
                    null,
                    null
            )).queue(
                    message -> msg.delete().queue(
                            null,
                            error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage())
                    ),
                    error -> ModMail.getInstance().error("Failed to send open ModMail warning: " + error.getMessage())
            );
            return;
        }

        // Create ModMail
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

    private void reply(final @NotNull Message msg) {
        String userID = msg.getContentStripped().substring(1).split(" ")[1];
        final User u = User.fromId(userID);
        final TextChannel textChannel = msg.getTextChannel();

        if (!u.getId().equals(textChannel.getTopic())) {
            textChannel.sendMessageEmbeds(EmbedUtils.buildEmbed(
                    null,
                    null,
                    null,
                    Color.RED,
                    "This is not a ModMail inbox.",
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
            return;
        }

        // Get ModMail private message
        ModMail.getInstance().getModMail(
                u,
                // Forward text
                privateChannel -> {
                    EmbedUtils.forwardText(
                            msg.getAuthor(),
                            msg.getContentDisplay(),
                            textChannel,
                            Color.GREEN,
                            null,
                            "Staff",
                            msg.getTimeCreated()
                    );
                    EmbedUtils.forwardAttachments(msg, Color.GREEN, privateChannel);
                }
        );
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
