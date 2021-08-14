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
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nonnull;
import java.awt.*;

public class DirectMessageListener extends ListenerAdapter {
    @Override
    public void onPrivateMessageReceived(@Nonnull PrivateMessageReceivedEvent e) {
        boolean foundGuild = false;
        for(Guild g : e.getAuthor().getMutualGuilds()) {
            if(g.getId().equals(Settings.INBOX_GUILD) || g.getId().equals(Settings.MAIN_GUILD)) {
                foundGuild = true;
                break;
            }
        }

        // Player is not in the guild, try to send a message to join
        if(!foundGuild) {
            invite(e.getChannel(), e.getAuthor());
            return;
        }

        TextChannel modmail = ModMail.getInstance().getModMail(e.getAuthor());
        if(modmail == null) {
            ModMail.getInstance().createModMail(e.getAuthor(), (
                    textChannel -> {
                        // do things?
                    }
            ));
            return;
        }

        forward(e.getMessage(), modmail);
    }

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
    }

    private void forward(@NotNull Message msg, @NotNull TextChannel channel) {
        forwardText(msg, channel);

        for(Message.Attachment attachment : msg.getAttachments()) {
            forwardAttachment(attachment, channel);
        }
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

    private void forwardAttachment(@NotNull Message.Attachment attachment, @NotNull TextChannel channel) {
        // TODO
    }
}
