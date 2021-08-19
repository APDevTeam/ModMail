package io.github.apdevteam.utils;

import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import org.jetbrains.annotations.NotNull;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;

public class LogUtils {
    public final static String folder = "OpenModMails";
    public final static String extension = "log";

    public static boolean create(@NotNull User u) {
        File f = new File(".", folder + "/" + u.getId() + "." + extension);
        try {
            if(!f.createNewFile())
                return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean log(@NotNull User u, @NotNull String username, @NotNull String message) {
        File f = new File(".", folder + "/" + u.getId() + "." + extension);
        if(!f.exists() || !f.canRead() || !f.canWrite() || f.isDirectory())
            return false;

        try {
            BufferedWriter buffer = new BufferedWriter(new FileWriter(f, true));
            buffer.write(new SimpleDateFormat("[MM/dd/yyyy HH:mm:ss] ").format(new Date()));
            buffer.write(username);
            buffer.write(": ");
            buffer.write(message);
            buffer.write("\n");
            buffer.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void archive(
        @NotNull User u,
        @NotNull TextChannel channel,
        @NotNull Consumer<Message> success,
        @NotNull Consumer<Throwable> failure
    ) {
        File f = new File(".", folder + "/" + u.getId() + "." + extension);
        if(!f.exists() || !f.canRead() || !f.canWrite() || f.isDirectory())
            failure.accept(new Throwable("Does not exist / can't read / can't write / is directory").fillInStackTrace());

        channel.sendFile(f).queue(
                ((Consumer<Message>) message -> {
                    if (!f.delete())
                        failure.accept(new Throwable("Failed to delete file").fillInStackTrace());
                }).andThen(success),
                failure
        );
    }
}
