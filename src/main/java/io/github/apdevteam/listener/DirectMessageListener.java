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

public class DirectMessageListener extends ListenerAdapter {
    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent e) {
        final User u = e.getAuthor();
        if(u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();
        if(Settings.DEBUG)
            System.out.println("Received '" + msg.getContentDisplay() + "' from '" + u.getName() + "#" + u.getDiscriminator() + "'");

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

        TextChannel modmail = ModMail.getInstance().getModMail(u);
        if(modmail == null) {
            try { // Try creating a channel and forwarding the message
                ModMail.getInstance().createModMail(u, (
                        textChannel -> {
                            textChannel.getManager().setTopic(u.getId()).queue(
                                    null,
                                    error -> ModMail.getInstance().error("Failed to set topic for '" + textChannel + "'")
                            );
                            // TODO: Create beginning message for modmail
                            forward(msg, textChannel);
                        }
                ));
            }
            catch (InsufficientPermissionException exception) { // Catch exception for no permissions
                ModMail.getInstance().error(exception.getMessage());
                //exception.printStackTrace();
            }
            return;
        }

        // Forward message
        forward(msg, modmail);
    }

    private void invite(final @NotNull PrivateChannel channel, final @NotNull User author) {
        final MessageEmbed embed = EmbedUtils.buildEmbed(
                null,
                null,
                "Please join our discord server!",
                Color.RED,
                Settings.MAIN_INVITE,
                null
        );
        channel.sendMessageEmbeds(embed).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send invite embed '" + embed + "' to '" + author + "'")
        );
    }

    private void forward(final @NotNull Message msg, final @NotNull TextChannel channel) {
        forwardText(msg, channel);

        forwardAttachments(msg, channel);
    }

    private void forwardText(final @NotNull Message msg, final @NotNull TextChannel channel) {
        MessageEmbed embed = EmbedUtils.buildEmbed(
                msg.getAuthor().getName(),
                msg.getAuthor().getAvatarUrl(),
                null,
                Color.yellow,
                msg.getContentDisplay(),
                null // TODO: timestamp
        );
        channel.sendMessageEmbeds(embed).queue(
                message -> msg.addReaction("U+2705").queue(
                        null,
                        error -> ModMail.getInstance().error("Failed to checkbox '" + msg + "' in '" + channel + "'")
                ),
                error -> ModMail.getInstance().error("Failed to send '" + embed + "' in '" + channel + "'")
        );
    }

    private void forwardAttachments(final @NotNull Message msg, final @NotNull TextChannel channel) {
        // TODO
        msg.getChannel().sendMessage(
                "Bot does not currently support attachments."
        ).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send attachment warning in '" + channel + "'")
        );
    }
}
