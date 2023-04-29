package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.EmbedUtils;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageUpdateEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.time.OffsetDateTime;

public class DirectMessageEditedListener extends ListenerAdapter {
    @Override
    public void onMessageUpdate(@NotNull MessageUpdateEvent e) {
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
            message -> {
                OffsetDateTime time = message.getTimeEdited();
                if(time == null)
                    time = message.getTimeCreated();

                EmbedUtils.edited(
                    u,
                    e.getMessage().getContentDisplay(),
                    message,
                    null,
                    "User",
                    time
                );
            },
            null
        );
    }
}
