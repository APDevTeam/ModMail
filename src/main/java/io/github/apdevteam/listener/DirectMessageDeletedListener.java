package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.EmbedUtils;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class DirectMessageDeletedListener extends ListenerAdapter {
    @Override
    public void onMessageDelete(@NotNull MessageDeleteEvent e) {
        if(!e.isFromType(ChannelType.PRIVATE))
            return;
        final User u = ((PrivateChannel) e.getChannel()).getUser();
        if(u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        String deletedID = e.getMessageId();
        String inboxMessageID = LogUtils.unmap(u.getId(), "Player", deletedID);
        if(inboxMessageID == null) {
            //ModMail.getInstance().error("Failed to get message ID for deleted message: " + u + ": " + deletedID);
            // Message was likely made before this session
            return;
        }

        TextChannel inbox = ModMail.getInstance().getModMailInbox(u);
        if(inbox == null) {
            ModMail.getInstance().error("Failed to get inbox for deleted message: " + u + ": " + deletedID);
            return;
        }

        inbox.retrieveMessageById(inboxMessageID).queue(
            message -> message.replyEmbeds(
                EmbedUtils.deleted()
            ).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send message for deleted message: " + u + ": " + deletedID)
            ),
            error -> ModMail.getInstance().error("Failed to get message for deleted message: " + u + ": " + deletedID)
        );
    }
}
