package io.github.apdevteam.utils;

import io.github.apdevteam.ModMail;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.function.Consumer;

public class EmbedUtils {
    public static @NotNull MessageEmbed buildEmbed(@Nullable String author,
                                                   @Nullable String authorIcon,
                                                   @Nullable String title,
                                                   @NotNull Color color,
                                                   @Nullable String text,
                                                   @Nullable String footer,
                                                   @Nullable OffsetDateTime timestamp,
                                                   @Nullable String thumbnail,
                                                   @Nullable String image
    ) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(author, null, authorIcon);
        eb.setTitle(title);
        eb.setColor(color);
        eb.setDescription(text);
        eb.setFooter(footer);
        eb.setTimestamp(timestamp);
        eb.setThumbnail(thumbnail);
        eb.setImage(image);
        return eb.build();
    }

    public static void forwardText(final @NotNull User user,
                                   final @NotNull String msg,
                                   final @NotNull MessageChannel channel,
                                   @NotNull Color color,
                                   final Consumer<Message> callback,
                                   @NotNull String footer,
                                   @NotNull OffsetDateTime timestamp
    ) {
        MessageEmbed embed = EmbedUtils.buildEmbed(
                user.getName(),
                user.getAvatarUrl(),
                null,
                color,
                msg,
                footer,
                timestamp,
                null,
                null
        );
        channel.sendMessageEmbeds(embed).queue(
                callback,
                error -> ModMail.getInstance().error("Failed to send '" + embed + "' in '" + channel + "'")
        );
    }

    public static void forwardAttachments(
            final @NotNull Message msg,
            @NotNull Color color,
            final @NotNull MessageChannel channel
    ) {
        // TODO
        if(msg.getAttachments().size() > 0) {
            msg.getChannel().sendMessage(
                    "Bot does not currently support attachments."
            ).queue(
                    null,
                    error -> ModMail.getInstance().error("Failed to send attachment warning in '" + channel + "'")
            );
        }
    }
}
