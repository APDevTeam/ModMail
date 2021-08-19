package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

import java.awt.*;

public class InboxListener extends ListenerAdapter {
    @Override
    public void onGuildMessageReceived(@NotNull GuildMessageReceivedEvent e) {
        final User u = e.getAuthor();
        if (u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();
        if (Settings.DEBUG)
            ModMail.getInstance().log("Received `'" + msg.getContentDisplay()
                    + "' from '" + u.getName() + "#" + u.getDiscriminator()
                    + "' in '" + msg.getChannel() + "'`", Color.YELLOW);


        final TextChannel inboxChannel = msg.getTextChannel();
        String userID = inboxChannel.getTopic();
        if(userID == null) {
            return;
        }

        final User player;
        try {
            player = User.fromId(userID);
        }
        catch (NumberFormatException exception) {
            return;
        }

        if (!player.getId().equals(inboxChannel.getTopic())) {
            return;
        }

        // We are in an inbox of user 'player'
        LogUtils.log("Comment", player.getId(), u.getName(), msg.getContentDisplay());
    }
}