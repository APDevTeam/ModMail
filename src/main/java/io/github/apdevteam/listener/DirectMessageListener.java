package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Blocked;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.EmbedUtils;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.Collections;
import java.util.function.Consumer;

public class DirectMessageListener extends ListenerAdapter {
    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent e) {
        final User u = e.getAuthor();
        if(u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

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

        final Message msg = e.getMessage();
        TextChannel modMailInbox = ModMail.getInstance().getModMailInbox(u);
        if(modMailInbox == null) {
            // Check for blocked
            if(Blocked.BLOCKED_IDS != null && Blocked.BLOCKED_IDS.contains(u.getId())) {
                blocked(e.getChannel(), u);
                return;
            }

            try { // Try creating a channel and forwarding the message
                ModMail.getInstance().createModMail(
                    u,
                    msg.getTimeCreated(),
                    (
                        // Forward message to inbox
                        (Consumer<TextChannel>) channel -> forward(msg, channel)
                    ).andThen(
                        channel -> {
                            // Let player know
                            msg.getChannel().sendMessageEmbeds(
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
                            );

                            // Log message
                            if(!LogUtils.log(u.getId(), "Player", u.getName(), u.getId(), msg.getContentDisplay()))
                                ModMail.getInstance().error("Failed to log message '" + u + ": " + msg.getContentDisplay() + "'");
                            for(Message.Attachment a : msg.getAttachments()) {
                                if (!LogUtils.log(u.getId(), "Player", u.getName(), u.getId(), "Attachment <" + a.getContentType() + ">: " + a.getUrl()))
                                    ModMail.getInstance().error("Failed to log attachment '" + u + ": " + a.getUrl() + "'");
                            }
                        }
                    )
                );
            }
            catch (InsufficientPermissionException exception) { // Catch exception for no permissions
                ModMail.getInstance().error(exception.getMessage());
                //exception.printStackTrace();
            }
            return;
        }

        // Check for commands
        String content = msg.getContentStripped();
        if(content.startsWith(Settings.PREFIX)) {
            return;
        }

        // Check for blocked
        if(Blocked.BLOCKED_IDS != null && Blocked.BLOCKED_IDS.contains(u.getId())) {
            blocked(e.getChannel(), u);
            return;
        }

        // Log message
        if(!LogUtils.log(u.getId(), "Player", u.getName(), u.getId(), msg.getContentDisplay()))
            ModMail.getInstance().error("Failed to log message '" + u + ": " + msg.getContentDisplay() + "'");
        for(Message.Attachment a : msg.getAttachments()) {
            if (!LogUtils.log(u.getId(), "Player", u.getName(), u.getId(), "Attachment <" + a.getContentType() + ">: " + a.getUrl()))
                ModMail.getInstance().error("Failed to log attachment '" + u + ": " + a.getUrl() + "'");
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

    private void blocked(final @NotNull PrivateChannel channel, final @NotNull User author) {
        final MessageEmbed embed = EmbedUtils.buildEmbed(
            null,
            null,
            "You are blocked from using the ModMail bot.",
            Color.RED,
            "Please appeal on our site: " + Blocked.APPEAL_LINK,
            null,
            null,
            null,
            null
        );
        channel.sendMessageEmbeds(embed).queue(
            null,
            error -> ModMail.getInstance().error("Failed to send blocked embed '" + embed + "' to '" + author + "'")
        );
    }

    private void forward(final @NotNull Message msg, final @NotNull TextChannel channel) {
        EmbedUtils.forwardText(
            msg.getAuthor(),
            msg.getContentDisplay(),
            channel,
            Color.YELLOW,
            message -> msg.addReaction("U+2705").queue(
                unused -> EmbedUtils.forwardAttachments(
                    message,
                    msg.getAuthor(),
                    Collections.singletonList(channel),
                    msg.getAttachments(),
                    Color.YELLOW,
                    "User",
                    msg.getTimeCreated()
                ),
                error -> ModMail.getInstance().error("Failed to checkbox '" + msg + "' in '" + channel + "'")
            ),
            "User",
            msg.getTimeCreated()
        );
    }
}
