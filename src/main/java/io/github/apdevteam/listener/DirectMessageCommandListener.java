package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Blocked;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.ColorUtils;
import io.github.apdevteam.utils.EmbedUtils;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.entities.channel.middleman.MessageChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

public class DirectMessageCommandListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if(!e.isFromType(ChannelType.PRIVATE))
            return;
        final User u = e.getAuthor();
        if(u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();

        // Check for commands
        String content = msg.getContentStripped();
        if(!content.startsWith(Settings.PREFIX)) {
            return;
        }

        if(Settings.DEBUG)
            ModMail.getInstance().log("Received cmd `'" + content
                    + "' from '" + u.getName() + "#" + u.getDiscriminator() + "'`", ColorUtils.debug());

        boolean foundGuild = false;
        for(Guild g : u.getMutualGuilds()) {
            String id = g.getId();
            if (id.equals(Settings.MAIN_GUILD) || id.equals(Settings.INBOX_GUILD)) {
                foundGuild = true;
                break;
            }
        }
        // Player is not in the guild, try to send an invitation message
        PrivateChannel privateChannel = (PrivateChannel) e.getChannel();
        if(!foundGuild) {
            invite(privateChannel, u);
            return;
        }

        // Check for blocked
        if(Blocked.BLOCKED_IDS != null && Blocked.BLOCKED_IDS.contains(u.getId())) {
            blocked(privateChannel, u);
            return;
        }

        TextChannel modMailInbox = ModMail.getInstance().getModMailInbox(u);
        if(modMailInbox == null) {
            openBeforeCmd(msg);
            return;
        }

        String command = content.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        switch (command) {
            case "add" -> add(msg);
            case "close" -> close(msg);
            default -> invalidCommand(msg);
        }
    }

    private void openBeforeCmd(final @NotNull Message msg) {
        msg.getChannel().sendMessageEmbeds(
            EmbedUtils.openBeforeCmd()
        ).queue(
            null,
            error -> ModMail.getInstance().error("Failed to send command warning: " + error.getMessage())
        );
    }

    private void invite(final @NotNull PrivateChannel channel, final @NotNull User author) {
        channel.sendMessageEmbeds(
            EmbedUtils.invite()
        ).queue(
            null,
            error -> ModMail.getInstance().error("Failed to send invite embed to '" + author + "'")
        );
    }

    private void blocked(final @NotNull PrivateChannel channel, final @NotNull User author) {
        channel.sendMessageEmbeds(
            EmbedUtils.blocked()
        ).queue(
            null,
            error -> ModMail.getInstance().error("Failed to send blocked embed to '" + author + "'")
        );
    }

    private void invalidCommand(final @NotNull Message msg) {
        try {
            msg.getChannel().sendMessageEmbeds(
                EmbedUtils.invalidCmd(msg)
            ).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send invalid DM command: " + error.getMessage())
            );
        }
        catch (InsufficientPermissionException e) {
            ModMail.getInstance().error("Failed to send invalid DM command: " + e.getMessage());
        }
    }

    private void add(final @NotNull Message msg) {
        msg.getChannel().sendMessageEmbeds(
            EmbedUtils.addError()
        ).queue(
            null,
            error -> ModMail.getInstance().error("Failed to send add warning: " + error.getMessage())
        );
    }

    private void close(final @NotNull Message msg) {
        final User u = msg.getAuthor();
        TextChannel inboxChannel = ModMail.getInstance().getModMailInbox(u);
        MessageChannel privateChannel = msg.getChannel();
        if(inboxChannel == null) {
            msg.getChannel().sendMessageEmbeds(
                EmbedUtils.closeFailed()
            ).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send close warning: " + error.getMessage())
            );
            return;
        }

        // Log closing
        LogUtils.log(u.getId(), "Player", msg.getAuthor().getName(), msg.getAuthor().getId(), "[Closed thread]");

        MessageEmbed embed = EmbedUtils.close(msg.getAuthor(), "User", msg.getTimeCreated());
        // Inform inbox
        inboxChannel.sendMessageEmbeds(embed).queue(
            // Inform DM
            unused -> privateChannel.sendMessageEmbeds(embed).queue(
                // Archive channel
                dm -> LogUtils.archive(u, ModMail.getInstance().getArchiveChannel(),
                    // Delete channel
                    unused2 -> inboxChannel.delete().queue(
                        null,
                        error -> ModMail.getInstance().error("Failed to delete channel: " + error.getMessage())
                    ),
                    // Error logging
                    error -> {
                        ModMail.getInstance().error("Failed to close ModMail of " + u.getId() + ": " + error.getMessage());
                        privateChannel.sendMessageEmbeds(
                            EmbedUtils.closeFailed()
                        ).queue(
                            null,
                            bad -> ModMail.getInstance().error("Failed to inform inbox of close failure: " + bad.getMessage())
                        );
                    }
                ),
                error -> ModMail.getInstance().error("Failed to inform DM of close: " + error.getMessage())
            ),
            error -> ModMail.getInstance().error("Failed to inform inbox of close: " + error.getMessage())
        );
    }
}
