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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class EmbedUtils {
    public static @NotNull MessageEmbed buildEmbed(
        @Nullable String author,
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

    public static void forwardText(
        final @NotNull User user,
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
        final @NotNull User user,
        final @NotNull Collection<MessageChannel> channels,
        final @NotNull List<Message.Attachment> attachments,
        @NotNull Color color,
        @NotNull String footer,
        @NotNull OffsetDateTime timestamp
    ) {
        if(attachments.size() < 1)
            return;

        ArrayList<MessageEmbed> embeds = new ArrayList<>();
        for(Message.Attachment a : attachments) {
            embeds.add(formatAttachment(user, a, color, footer, timestamp));
        }

        for(MessageChannel channel : channels) {
            channel.sendMessageEmbeds(embeds).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send attachments: " + error.getMessage())
            );
        }
    }

    private static @NotNull MessageEmbed formatAttachment(
        final @NotNull User user,
        @NotNull Message.Attachment attachment,
        @NotNull Color color,
        @NotNull String footer,
        @NotNull OffsetDateTime timestamp
    ) {
        if(attachment.isImage())
            return formatImage(user, attachment, color, footer, timestamp);
        if(attachment.isVideo())
            return formatVideo(user, attachment, color, footer, timestamp);

        String contentType = attachment.getContentType();
        if(contentType == null)
            contentType = "null";

        return formatOther(user, attachment, color, footer, timestamp, contentType);
    }

    private static @NotNull MessageEmbed formatImage(
        final @NotNull User user,
        @NotNull Message.Attachment attachment,
        @NotNull Color color,
        @NotNull String footer,
        @NotNull OffsetDateTime timestamp
    ) {
        return buildEmbed(
            user.getName(),
            user.getAvatarUrl(),
            null,
            color,
            null,
            footer,
            timestamp,
            null,
            attachment.getUrl()
        );
    }

    private static @NotNull MessageEmbed formatVideo(
            final @NotNull User user,
            @NotNull Message.Attachment attachment,
            @NotNull Color color,
            @NotNull String footer,
            @NotNull OffsetDateTime timestamp
    ) {
        return buildEmbed(
            user.getName(),
            user.getAvatarUrl(),
            "Video",
            color,
            attachment.getUrl(),
            footer,
            timestamp,
            null,
            null
        );
    }

    private static @NotNull MessageEmbed formatOther(
            final @NotNull User user,
            @NotNull Message.Attachment attachment,
            @NotNull Color color,
            @NotNull String footer,
            @NotNull OffsetDateTime timestamp,
            @NotNull String contentType
    ) {
        return buildEmbed(
            user.getName(),
            user.getAvatarUrl(),
            contentType,
            color,
            attachment.getUrl(),
            footer,
            timestamp,
            null,
            null
        );
    }
}
