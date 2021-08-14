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

import javax.annotation.Nonnull;
import java.awt.*;

public class DirectMessageListener extends ListenerAdapter {
    @Override
    public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent e) {
        if(e.getAuthor().isBot() || e.getAuthor().getId().equals(Settings.TOKEN))
            return;

        /*
        // Player is not in the guild, try to send an invite message
        if(e.getAuthor().getMutualGuilds().size() < 1) {
            invite(e.getChannel(), e.getAuthor());
            return;
        }
        */

        TextChannel modmail = ModMail.getInstance().getModMail(e.getAuthor());
        if(modmail == null) {
            try { // Try creating a channel and forwarding the message
                ModMail.getInstance().createModMail(e.getAuthor(), (
                        textChannel -> {
                            // TODO: Create beginning message for modmail
                            forward(e.getMessage(), textChannel);
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
        forward(e.getMessage(), modmail);
    }

    /*
    private void invite(@NotNull PrivateChannel channel, @NotNull User author) {
        MessageEmbed embed = EmbedUtils.buildEmbed(
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
    }*/

    private void forward(@NotNull Message msg, @NotNull TextChannel channel) {
        forwardText(msg, channel);

        forwardAttachments(msg, channel);
    }

    private void forwardText(@NotNull Message msg, @NotNull TextChannel channel) {
        MessageEmbed embed = EmbedUtils.buildEmbed(
                msg.getAuthor().getName(),
                msg.getAuthor().getAvatarUrl(),
                null,
                Color.yellow,
                msg.getContentDisplay(),
                null // TODO: timestamp
        );
        channel.sendMessageEmbeds(embed).queue(
                message -> msg.addReaction("checkbox").queue(
                        null,
                        error -> ModMail.getInstance().error("Failed to checkbox '" + msg + "' in '" + channel + "'")
                ),
                error -> ModMail.getInstance().error("Failed to send '" + embed + "' in '" + channel + "'")
        );
    }

    private void forwardAttachments(@NotNull Message msg, @NotNull TextChannel channel) {
        // TODO
        msg.getChannel().sendMessage(
                "Bot does not currently support attachments."
        ).queue(
                null,
                (error) -> ModMail.getInstance().error("Failed to send attachment warning in '" + channel + "'")
        );
    }
}
