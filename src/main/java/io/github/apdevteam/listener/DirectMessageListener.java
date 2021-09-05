package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Blocked;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.ColorUtils;
import io.github.apdevteam.utils.EmbedUtils;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.events.message.priv.PrivateMessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.function.Consumer;

public class DirectMessageListener extends ListenerAdapter {
    @Override
    public void onPrivateMessageReceived(@NotNull PrivateMessageReceivedEvent e) {
        final User u = e.getAuthor();
        if(u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();

        // Check for commands
        String content = msg.getContentStripped();
        if(content.startsWith(Settings.PREFIX)) {
            return;
        }

        boolean foundGuild = false;
        for(Guild g : u.getMutualGuilds()) {
            String id = g.getId();
            if (id.equals(Settings.MAIN_GUILD) || id.equals(Settings.INBOX_GUILD)) {
                foundGuild = true;
                break;
            }
        }
        // Player is not in the guild, try to send an invitation message
        if(!foundGuild) {
            invite(e.getChannel(), u);
            return;
        }

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
                                EmbedUtils.playerOpened()
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
        channel.sendMessageEmbeds(EmbedUtils.invite()).queue(
            null,
            error -> ModMail.getInstance().error("Failed to send invite embed to '" + author + "'")
        );
    }

    private void blocked(final @NotNull PrivateChannel channel, final @NotNull User author) {
        channel.sendMessageEmbeds(EmbedUtils.blocked()).queue(
            null,
            error -> ModMail.getInstance().error("Failed to send blocked embed to '" + author + "'")
        );
    }

    private void forward(final @NotNull Message msg, final @NotNull TextChannel channel) {
        EmbedUtils.forwardText(
            msg.getAuthor(),
            msg.getContentDisplay(),
            channel,
            ColorUtils.forwardFromUser(),
            message -> msg.addReaction("U+2705").queue(
                unused -> EmbedUtils.forwardAttachments(
                    message,
                    msg.getAuthor(),
                    Collections.singletonList(channel),
                    msg.getAttachments(),
                    ColorUtils.forwardFromUser(),
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
