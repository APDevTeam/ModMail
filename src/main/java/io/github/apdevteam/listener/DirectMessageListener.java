package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.EmbedUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.function.Consumer;

public class DirectMessageListener extends ListenerAdapter {
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

        TextChannel modMailInbox = ModMail.getInstance().getModMailInbox(u);
        if(modMailInbox == null) {
            try { // Try creating a channel and forwarding the message
                ModMail.getInstance().createModMail(
                    u,
                    msg.getTimeCreated(),
                    (
                        (Consumer<TextChannel>) channel -> forward(msg, channel)
                    ).andThen(
                        channel -> msg.getChannel().sendMessageEmbeds(
                            EmbedUtils.buildEmbed(
                                null,
                                null,
                                "Thank you for your message!",
                                Color.GREEN,
                                "The AP Admin team will get back to you as soon as possible!",
                                null,
                                null,
                                null,
                                null
                            )
                        ).queue(
                            null,
                            error -> ModMail.getInstance().error("Failed to send initial message for '" + channel + "'")
                        )
                    )
                );
            }
            catch (InsufficientPermissionException exception) { // Catch exception for no permissions
                ModMail.getInstance().error(exception.getMessage());
                //exception.printStackTrace();
            }
            return;
        }

        // Forward message
        forward(msg, modMailInbox);
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

    private void forward(final @NotNull Message msg, final @NotNull TextChannel channel) {
        EmbedUtils.forwardText(
            msg.getAuthor(),
            msg.getContentDisplay(),
            channel,
            Color.YELLOW,
            message -> msg.addReaction("U+2705").queue(
                    unused -> EmbedUtils.forwardAttachments(msg, Color.YELLOW, channel),
                    error -> ModMail.getInstance().error("Failed to checkbox '" + msg + "' in '" + channel + "'")
            ),
            "User",
            msg.getTimeCreated()
        );
    }
}
