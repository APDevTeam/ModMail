package io.github.apdevteam.utils;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.awt.*;

public class EmbedUtils {
    public static @NotNull MessageEmbed buildEmbed(@Nullable String author,
                                                   @Nullable String authorIcon,
                                                   @Nullable String title,
                                                   @NotNull Color color,
                                                   @Nullable String text,
                                                   @Nullable String footer
    ) {
        EmbedBuilder eb = new EmbedBuilder();
        eb.setAuthor(author, null, authorIcon);
        eb.setTitle(title);
        eb.setColor(color);
        eb.setDescription(text);
        eb.setFooter(footer);
        return eb.build();
    }
}
