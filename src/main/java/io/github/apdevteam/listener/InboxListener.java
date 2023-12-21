package io.github.apdevteam.listener;

import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.MiscUtil;
import org.jetbrains.annotations.NotNull;

public class InboxListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if(!e.isFromType(ChannelType.TEXT))
            return;
        final User u = e.getAuthor();
        if (u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();
        if (!msg.getGuild().getId().equals(Settings.INBOX_GUILD))
            return;

        String content = msg.getContentStripped();
        if(content.startsWith(Settings.PREFIX))
            return;

        final TextChannel inboxChannel = (TextChannel) msg.getChannel();
        String userID = inboxChannel.getTopic();
        if(userID == null)
            return;

        try {
            MiscUtil.parseSnowflake(userID);
        }
        catch (NumberFormatException ignored) {
            return;
        }

        // We are in an inbox of user 'player'
        LogUtils.log(userID, "Comment", u.getName(), u.getId(), msg.getContentDisplay());
    }
}
