package io.github.apdevteam.listener;

import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class InboxListener extends ListenerAdapter {
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        final User u = e.getAuthor();
        if (u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();
        if (!msg.getGuild().getId().equals(Settings.INBOX_GUILD))
            return;

        String content = msg.getContentStripped();
        if(content.startsWith(Settings.PREFIX))
            return;

        final TextChannel inboxChannel = msg.getTextChannel();
        String userID = inboxChannel.getTopic();
        if(userID == null)
            return;

        try {
            User.fromId(userID);
        }
        catch (NumberFormatException ignored) {
            return;
        }

        // We are in an inbox of user 'player'
        LogUtils.log(userID, "Comment", u.getName(), u.getId(), msg.getContentDisplay());
    }
}
