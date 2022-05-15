package io.github.apdevteam.utils;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Blocked;
import io.github.apdevteam.config.Settings;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageChannel;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.requests.restaction.MessageAction;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Consumer;

public class EmbedUtils {
    private static @NotNull MessageEmbed buildEmbed(
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
        if("".equals(msg)) {
            callback.accept(null);
            return; // Don't send empty text headers for attachment only messages
        }

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

    public static void edited(
            final @NotNull User user,
            final @NotNull String msg,
            final @NotNull Message source,
            final Consumer<Message> callback,
            @NotNull String footer,
            @NotNull OffsetDateTime timestamp
    ) {
        if ("".equals(msg)) {
            callback.accept(null);
            return; // Don't send empty text headers for attachment only messages
        }

        MessageEmbed embed = EmbedUtils.buildEmbed(
            user.getName(),
            user.getAvatarUrl(),
            "Edited",
            ColorUtils.messageEdited(),
            msg,
            footer,
            timestamp,
            null,
            null
        );

        source.replyEmbeds(embed).queue(
            callback,
            error -> ModMail.getInstance().error("Failed to send '" + embed + "' in '" + source + "'")
        );
    }

    public static void forwardAttachments(
        final @Nullable Message main,
        final @NotNull User user,
        final @NotNull MessageChannel channel,
        final @NotNull List<Message.Attachment> attachments,
        @NotNull Color color,
        @NotNull String footer,
        @NotNull OffsetDateTime timestamp,
        @Nullable Consumer<Message> callback
    ) {
        if(attachments.size() < 1) {
            if(callback != null)
                callback.accept(null);

            return;
        }

        ArrayList<MessageEmbed> embeds = new ArrayList<>();
        for(Message.Attachment a : attachments) {
            embeds.add(formatAttachment(user, a, color, footer, timestamp));
        }

        MessageAction action = channel.sendMessageEmbeds(embeds);
        if(main != null)
            action = action.reference(main);

        action.queue(
            callback,
            error -> ModMail.getInstance().error("Failed to send attachments: " + error.getMessage())
        );
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


    public static @NotNull MessageEmbed blocked() {
        return buildEmbed(
            null,
            null,
            "You are blocked from using the ModMail bot.",
            ColorUtils.userBlocked(),
            "Please appeal on our site: " + Blocked.APPEAL_LINK,
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed invite() {
        return buildEmbed(
            null,
            null,
            "Please join our discord server!",
            ColorUtils.invite(),
            Settings.MAIN_INVITE,
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed invalidCmd(@NotNull Message msg) {
        return buildEmbed(
            msg.getAuthor().getName(),
            msg.getAuthor().getAvatarUrl(),
            "Invalid command",
            ColorUtils.invalidCmd(),
            msg.getContentDisplay(),
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed invalidID() {
        return buildEmbed(
            null,
            null,
            "Invalid User ID",
            ColorUtils.invalidUser(),
            null,
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed notInbox(@Nullable OffsetDateTime timestamp) {
        return buildEmbed(
                null,
                null,
                "This is not a ModMail inbox.",
                ColorUtils.notInbox(),
                null,
                null,
                timestamp,
                null,
                null
        );
    }

    public static @NotNull MessageEmbed addError() {
        return buildEmbed(
            null,
            null,
            null,
            ColorUtils.addError(),
            "Adding is not yet supported",
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed existingModMail() {
        return buildEmbed(
            null,
            null,
            "This user already has a ModMail channel.",
            ColorUtils.existingModMail(),
            null,
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed adminOpened(@NotNull User author) {
        return buildEmbed(
            author.getName(),
            author.getAvatarUrl(),
            null,
            ColorUtils.initialMessage(),
            "Opened a ModMail session.",
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed playerOpened() {
        return buildEmbed(
            null,
            null,
            "Thank you for your message!",
            ColorUtils.initialMessage(),
            "The AP Admin team will get back to you as soon as possible!",
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed inboxOpened(@NotNull User user, @Nullable OffsetDateTime timestamp) {
        return buildEmbed(
            user.getName(),
            user.getAvatarUrl(),
            null,
            ColorUtils.beginModMail(),
            "ModMail thread started.",
            "User ID: " + user.getId(),
            timestamp,
            user.getAvatarUrl(),
            null
        );
    }

    public static @NotNull MessageEmbed unblocked(@NotNull User author, @NotNull UserSnowflake unblocked) {
        return buildEmbed(
            author.getName(),
            author.getAvatarUrl(),
            "Unblocked user",
            ColorUtils.unblocked(),
            "<@" + unblocked.getId() + ">",
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed unblockFailed(@NotNull User author, @NotNull UserSnowflake unblocked) {
        return buildEmbed(
            author.getName(),
            author.getAvatarUrl(),
            "Failed to unblock user",
            ColorUtils.unblockFailed(),
            "<@" + unblocked.getId() + ">",
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed blocked(@NotNull User author, @NotNull UserSnowflake blocked) {
        return buildEmbed(
            author.getName(),
            author.getAvatarUrl(),
            "Blocked user",
            ColorUtils.blocked(),
            "<@" + blocked.getId() + ">",
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed blockFailed(@NotNull User author, @Nullable UserSnowflake blocked) {
        if(blocked != null)
            return buildEmbed(
                author.getName(),
                author.getAvatarUrl(),
                "Failed to block user",
                ColorUtils.blockFailed(),
                "<@" + blocked.getId() + ">",
                null,
                null,
                null,
                null
            );
        else
            return buildEmbed(
                author.getName(),
                author.getAvatarUrl(),
                "Failed to block user",
                ColorUtils.blockFailed(),
                null,
                null,
                null,
                null,
                null
            );
    }

    public static @NotNull MessageEmbed close(@NotNull User author, @Nullable String footer, @Nullable OffsetDateTime timestamp) {
        return buildEmbed(
            author.getName(),
            author.getAvatarUrl(),
            "Thread Closed",
            ColorUtils.closeModMail(),
            null,
            footer,
            timestamp,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed closeFailed() {
        return buildEmbed(
            null,
            null,
            null,
            ColorUtils.closeFailed(),
            "Failed to close",
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed openBeforeCmd() {
        return buildEmbed(
            null,
            null,
            null,
            ColorUtils.noExistingModMail(),
            "Please open a ModMail before using commands.",
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed deleted() {
        return buildEmbed(
            null,
            null,
            null,
            ColorUtils.messageDeleted(),
            "Message deleted",
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed log(@Nullable String title, @NotNull Color color, @Nullable String message) {
        return buildEmbed(
            null,
            null,
            title,
            color,
            message,
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed warn(@NotNull String message) {
        return buildEmbed(
            null,
            null,
            null,
            ColorUtils.warn(),
            message,
            null,
            null,
            null,
            null
        );
    }

    public static @NotNull MessageEmbed error(@NotNull String message) {
        return buildEmbed(
            null,
            null,
            null,
            ColorUtils.error(),
            message,
            null,
            null,
            null,
            null
        );
    }
}
