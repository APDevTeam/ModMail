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
    public final static String baseFolder = "OpenModMails";
    public final static String logExtension = "log";
    public final static String mapExtension = "map";

    public static boolean create(@NotNull String userID) {
        File log = new File(".", baseFolder + "/" + userID + "." + logExtension);
        try {
            if(!log.createNewFile())
                return false;
        } catch (IOException e) {
            return false;
        }
        File map = new File(".", baseFolder + "/" + userID + "." + mapExtension);
        try {
            if(!map.createNewFile())
                return false;
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean log(
        @NotNull String userID,
        @NotNull String prefix,
        @NotNull String author,
        @NotNull String authorID,
        @NotNull String message
    ) {
        File log = new File(".", baseFolder + "/" + userID + "." + logExtension);
        if(!log.exists() || !log.canRead() || !log.canWrite() || log.isDirectory())
            return false;

        try {
            BufferedWriter buffer = new BufferedWriter(new FileWriter(log, true));
            buffer.write(prefix);
            buffer.write(new SimpleDateFormat("\t[MM/dd/yyyy HH:mm:ss] ").format(new Date()));
            buffer.write(author);
            buffer.write(" <");
            buffer.write(authorID);
            buffer.write(">: ");
            buffer.write(message);
            buffer.write("\n");
            buffer.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static boolean map(
        @NotNull String userID,
        @NotNull String prefix,
        @NotNull String sourceID,
        @NotNull String destinationID
    ) {
        File map = new File(".", baseFolder + "/" + userID + "." + mapExtension);
        if(!map.exists() || !map.canRead() || !map.canWrite() || map.isDirectory())
            return false;

        try {
            BufferedWriter buffer = new BufferedWriter(new FileWriter(map, true));
            buffer.write(prefix);
            buffer.write(new SimpleDateFormat("\t[MM/dd/yyyy HH:mm:ss] ").format(new Date()));
            buffer.write(sourceID);
            buffer.write(":");
            buffer.write(destinationID);
            buffer.write("\n");
            buffer.close();
        } catch (IOException e) {
            return false;
        }
        return true;
    }

    public static void archive(
        @NotNull User user,
        @NotNull TextChannel channel,
        @NotNull Consumer<Message> success,
        @NotNull Consumer<Throwable> failure
    ) {
        // Check for log file
        File log = new File(".", baseFolder + "/" + user.getId() + "." + logExtension);
        if(!log.exists() || !log.canRead() || !log.canWrite() || log.isDirectory())
            failure.accept(new Throwable("Log does not exist / can't read / can't write / is directory").fillInStackTrace());

        // Check for map file
        File map = new File(".", baseFolder + "/" + user.getId() + "." + mapExtension);
        if(!map.exists() || !map.canRead() || !map.canWrite() || map.isDirectory())
            failure.accept(new Throwable("Map does not exist / can't read / can't write / is directory").fillInStackTrace());
        // Delete map file
        if (!map.delete())
            failure.accept(new Throwable("Failed to delete map file").fillInStackTrace());

        // Upload log file to discord
        String msg = user.getName() + "#" + user.getDiscriminator() + " <" + user.getId() + ">";
        channel.sendMessage(msg).addFile(log).queue(
            ((Consumer<Message>) message -> {
                // Delete log file locally
                if (!log.delete())
                    failure.accept(new Throwable("Failed to delete log file").fillInStackTrace());
            }).andThen(success),
            failure
        );
    }
}
