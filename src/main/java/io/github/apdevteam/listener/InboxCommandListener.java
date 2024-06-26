package io.github.apdevteam.listener;

import io.github.apdevteam.ModMail;
import io.github.apdevteam.config.Blocked;
import io.github.apdevteam.config.Settings;
import io.github.apdevteam.utils.ColorUtils;
import io.github.apdevteam.utils.EmbedUtils;
import io.github.apdevteam.utils.LogUtils;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.UserSnowflake;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.exceptions.InsufficientPermissionException;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class InboxCommandListener extends ListenerAdapter {
    @Override
    public void onMessageReceived(@NotNull MessageReceivedEvent e) {
        if (!e.isFromType(ChannelType.TEXT))
            return;
        final User u = e.getAuthor();
        if (u.isBot() || u.getId().equals(Settings.TOKEN))
            return;

        final Message msg = e.getMessage();
        if (Settings.DEBUG)
            ModMail.getInstance().log("Received `'" + msg.getContentDisplay()
                + "' from '" + u.getName() + "#" + u.getDiscriminator()
                + "' in '" + msg.getChannel() + "'`", ColorUtils.debug());

        if (!msg.getGuild().getId().equals(Settings.INBOX_GUILD))
            return;

        String content = msg.getContentStripped();
        if (!content.startsWith(Settings.PREFIX))
            return;

        String command = content.substring(1).split(" ")[0].toLowerCase(Locale.ROOT);
        switch (command) {
            case "open" -> open(msg);
            case "reply" -> reply(msg);
            case "close" -> close(msg);
            case "forceclose" -> forceClose(msg);
            case "block" -> block(msg);
            case "unblock" -> unblock(msg);
            case "add" -> add(msg);
            case "remindme" -> remindMe(msg, false);
            case "remind" -> remindMe(msg, true);
            default -> invalidCommand(msg);
        }
    }

    private void open(final @NotNull Message msg) {
        String userID = msg.getContentStripped().substring(1).split(" ")[1];

        ModMail.getInstance().getUserbyID(
            userID,
            u -> {
                TextChannel textChannel = ModMail.getInstance().getModMailInbox(u);
                if (textChannel != null) {
                    try {
                        msg.getChannel().sendMessageEmbeds(
                            EmbedUtils.existingModMail()
                        ).queue(
                            message -> msg.delete().queue(
                                null,
                                error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage(), error)
                            ),
                            error -> ModMail.getInstance().error("Failed to send open ModMail warning: " + error.getMessage(), error)
                        );
                    }
                    catch (InsufficientPermissionException error) {
                        ModMail.getInstance().error("Failed to delete message: " + error.getMessage(), error);
                    }
                    return;
                }

                final User author = msg.getAuthor();

                // Create ModMail
                try {
                    ModMail.getInstance().createModMail(
                        u,
                        msg.getTimeCreated(),
                        channel -> ModMail.getInstance().getModMail(
                            u,
                            privateChannel -> {
                                // Log message
                                if(!LogUtils.log(u.getId(), "Staff", author.getName(), author.getId(), "[Opened thread]"))
                                    ModMail.getInstance().error("Failed to log message '" + u + ": " + msg.getContentDisplay() + "'", null);

                                // Inform player
                                privateChannel.sendMessageEmbeds(
                                    EmbedUtils.adminOpened(author)
                                ).queue(
                                    // Delete command
                                    message -> msg.delete().queue(
                                        null,
                                        error -> ModMail.getInstance().error("Failed to delete open command: " + error.getMessage(), error)
                                    ),
                                    error -> ModMail.getInstance().error("Failed to send open ModMail message: " + error.getMessage(), error)
                                );
                            }
                        )
                    );
                }
                catch (InsufficientPermissionException error) {
                    ModMail.getInstance().error("Failed to delete message: " + error.getMessage(), error);
                }
            },
            ignored -> msg.getChannel().sendMessageEmbeds(
                EmbedUtils.invalidID()
            ).queue(
                null,
                error -> ModMail.getInstance().error("Failed warn invalid ID: " + error.getMessage(), error)
            )
        );
    }

    private void reply(final @NotNull Message msg) {
        final TextChannel inboxChannel = (TextChannel) msg.getChannel();
        String userID = inboxChannel.getTopic();
        if(userID == null) {
            invalidInbox(inboxChannel, msg);
            return;
        }

        ModMail.getInstance().getUserbyID(
            userID,
            u -> {
                if (!u.getId().equals(inboxChannel.getTopic())) {
                    invalidInbox(inboxChannel, msg);
                    return;
                }

                final String content = msg.getContentDisplay().substring(6).trim();
                try {
                    if(!LogUtils.log(u.getId(), "Reply", msg.getAuthor().getName(), msg.getAuthor().getId(), content))
                        ModMail.getInstance().error("Failed to log message '" + u + ": " + msg + "'", null);
                    for(Message.Attachment a : msg.getAttachments()) {
                        if (!LogUtils.log(u.getId(), "Reply", msg.getAuthor().getName(), msg.getAuthor().getId(), "Attachment <" + a.getContentType() + ">: " + a.getUrl()))
                            ModMail.getInstance().error("Failed to log attachment '" + u + ": " + a.getUrl() + "'", null);
                    }

                    // Get private ModMail channel
                    ModMail.getInstance().getModMail(
                        u,
                        // Forward text to private ModMail channel
                        privateChannel -> EmbedUtils.forwardText(
                            msg.getAuthor(),
                            content,
                            privateChannel,
                            ColorUtils.forwardToUser(),
                            // Forward attachments to private ModMail channel
                            ((Consumer<Message>) privateMessage -> EmbedUtils.forwardAttachments(
                                privateMessage,
                                msg.getAuthor(),
                                privateChannel,
                                msg.getAttachments(),
                                ColorUtils.forwardToUser(),
                                "Staff",
                                msg.getTimeCreated(),
                                null
                            )).andThen(
                                // Forward text to inbox ModMail channel
                                ((Consumer<Message>) message -> EmbedUtils.forwardText(
                                    msg.getAuthor(),
                                    content,
                                    inboxChannel,
                                    ColorUtils.forwardToUser(),
                                    // Forward attachments to inbox ModMail channel
                                    inboxMessage -> EmbedUtils.forwardAttachments(
                                        inboxMessage,
                                        msg.getAuthor(),
                                        inboxChannel,
                                        msg.getAttachments(),
                                        ColorUtils.forwardToUser(),
                                        "Staff",
                                        msg.getTimeCreated(),
                                        null
                                    ),
                                    "Staff",
                                    msg.getTimeCreated()
                                )).andThen(
                                    // Delete original message
                                    message -> msg.delete().queue(
                                        null,
                                        error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage(), error)
                                    )
                                )
                            ),
                            "Staff",
                            msg.getTimeCreated()
                        )
                    );
                }
                catch (InsufficientPermissionException error) {
                    ModMail.getInstance().error("Failed to delete: " + error.getMessage(), error);
                }
            },
            ignored -> msg.getChannel().sendMessageEmbeds(
                EmbedUtils.invalidID()
            ).queue(
                null,
                error -> ModMail.getInstance().error("Failed warn invalid ID: " + error.getMessage(), error)
            )
        );
    }

    private void close(final @NotNull Message msg) {
        final TextChannel inboxChannel = (TextChannel) msg.getChannel();
        String userID = inboxChannel.getTopic();
        if(userID == null) {
            invalidInbox(inboxChannel, msg);
            return;
        }

        ModMail.getInstance().getUserbyID(
            userID,
            u -> {
                if (!u.getId().equals(inboxChannel.getTopic())) {
                    invalidInbox(inboxChannel, msg);
                    return;
                }

                // Log closing
                LogUtils.log(u.getId(), "Staff", msg.getAuthor().getName(), msg.getAuthor().getId(), "[Closed thread]");

                MessageEmbed embed = EmbedUtils.close(msg.getAuthor(), "Staff", msg.getTimeCreated());
                // Inform inbox
                inboxChannel.sendMessageEmbeds(embed).queue(
                    message -> ModMail.getInstance().getModMail(
                        u,
                        // Inform DM
                        privateChannel -> privateChannel.sendMessageEmbeds(embed).queue(
                            // Archive channel
                            dm -> LogUtils.archive(u, ModMail.getInstance().getArchiveChannel(),
                                // Delete channel
                                unused -> inboxChannel.delete().queue(
                                    unused1 -> {},
                                    error -> ModMail.getInstance().error("Failed to delete channel: " + error.getMessage(), error)
                                ),
                                // Error logging
                                error1 -> {
                                    ModMail.getInstance().error("Failed to close ModMail of " + u.getId() + ": " + error1.getMessage(), error1);
                                    inboxChannel.sendMessageEmbeds(
                                        EmbedUtils.closeFailed()
                                    ).queue(
                                        null,
                                        error2 -> ModMail.getInstance().error("Failed to inform inbox of close failure: " + error2.getMessage(), error2)
                                    );
                                }
                            ),
                            error -> ModMail.getInstance().error("Failed to inform DM of close: " + error.getMessage(), error)
                        )
                    ),
                    error -> ModMail.getInstance().error("Failed to inform inbox of close: " + error.getMessage(), error)
                );
            },
            error -> ModMail.getInstance().error("Failed to get user for close: " + error.getMessage(), error)
        );
    }

    private void forceClose(final @NotNull Message msg) {
        final TextChannel inboxChannel = (TextChannel) msg.getChannel();
        String userID = inboxChannel.getTopic();
        if(userID == null) {
            invalidInbox(inboxChannel, msg);
            return;
        }

        ModMail.getInstance().getUserbyID(
            userID,
            u -> {
                if (!u.getId().equals(inboxChannel.getTopic())) {
                    invalidInbox(inboxChannel, msg);
                    return;
                }

                // Log closing
                LogUtils.log(u.getId(), "Staff", msg.getAuthor().getName(), msg.getAuthor().getId(), "[Force Closed thread]");

                MessageEmbed embed = EmbedUtils.close(msg.getAuthor(), "Staff", msg.getTimeCreated());
                // Inform inbox
                inboxChannel.sendMessageEmbeds(embed).queue(
                    message -> ModMail.getInstance().getModMail(
                        u,
                        // Archive channel
                        privateChannel -> LogUtils.archive(u, ModMail.getInstance().getArchiveChannel(),
                            // Delete channel
                            unused -> inboxChannel.delete().queue(
                                unused1 -> privateChannel.sendMessageEmbeds(embed).queue(
                                    null,
                                    error -> ModMail.getInstance().warn("Failed to inform DM of close: " + error.getMessage())
                                ),
                                error -> ModMail.getInstance().error("Failed to delete channel: " + error.getMessage(), error)
                            ),
                            // Error logging
                            error1 -> {
                                ModMail.getInstance().error("Failed to close ModMail of " + u.getId() + ": " + error1.getMessage(), error1);
                                inboxChannel.sendMessageEmbeds(
                                    EmbedUtils.closeFailed()
                                ).queue(
                                    null,
                                    error2 -> ModMail.getInstance().error("Failed to inform inbox of close failure: " + error2.getMessage(), error2)
                                );
                            }
                        )
                    ),
                    error -> ModMail.getInstance().error("Failed to inform inbox of close: " + error.getMessage(), error)
                );
            },
            error -> ModMail.getInstance().error("Failed to get user for close: " + error.getMessage(), error)
        );
    }

    private void block(final @NotNull Message msg) {
        Member author = msg.getMember();
        if(author == null) {
            ModMail.getInstance().error("Null author of: " + msg, null);
            return;
        }
        if(Settings.MODERATOR_ROLES == null) {
            ModMail.getInstance().error("Null blocked users: " + msg, null);
            return;
        }
        if(!isModerator(author, Settings.MODERATOR_ROLES)) {
            msg.delete().queue(
                null,
                error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage(), error)
            );
            return;
        }
        // Author is a moderator with permission to block a user

        String userID;
        try {
            userID = msg.getContentStripped().substring(1).split(" ")[1];
        } catch (ArrayIndexOutOfBoundsException e) {
            // No argument, try checking if this is an inbox
            userID = ((TextChannel) msg.getChannel()).getTopic();
            if(userID == null) {
                // Not an inbox, failed
                blockFailed(msg, null);
                return;
            }

            // Block user of the inbox
            block(msg, userID);
            return;
        }
        if(userID == null) {
            // Some sort of invalid issue here
            blockFailed(msg, null);
            return;
        }

        block(msg, userID);
    }

    private void block(final @NotNull Message msg, final @NotNull String userID) {
        // Try to retrieve the user via the Discord API
        ModMail.getInstance().getUserbyID(
            userID,
            u -> {
                if(Blocked.block(u))
                    block(msg, u);
                else
                    blockFailed(msg, u);
            },
            // If that fails, try constructing it from the ID and blocking
            unused -> {
                UserSnowflake userSnowflake;
                try {
                    userSnowflake = User.fromId(userID);
                } catch (NumberFormatException e) {
                    blockFailed(msg, null);
                    return;
                }

                if(Blocked.block(userSnowflake))
                    block(msg, userSnowflake);
                else
                    blockFailed(msg, userSnowflake);
            }
        );
    }

    private void unblock(final @NotNull Message msg) {
        Member author = msg.getMember();
        if(author == null) {
            ModMail.getInstance().error("Null author of: " + msg, null);
            return;
        }
        if(Settings.MODERATOR_ROLES == null) {
            ModMail.getInstance().error("Null blocked users: " + msg, null);
            return;
        }
        if(!isModerator(author, Settings.MODERATOR_ROLES)) {
            msg.delete().queue(
                null,
                error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage(), error)
            );
            return;
        }
        // Author is a moderator with permission to unblock a user

        String userID = msg.getContentStripped().substring(1).split(" ")[1];
        ModMail.getInstance().getUserbyID(
            userID,
            u -> {
                if(Blocked.unblock(u))
                    unblock(msg, u);
                else
                    unblockFailed(msg, u);
            },
            unused -> {
                UserSnowflake userSnowflake = User.fromId(userID);

                if(Blocked.unblock(userSnowflake))
                    unblock(msg, userSnowflake);
                else
                    unblockFailed(msg, userSnowflake);
            }
        );
    }

    private void add(final @NotNull Message msg) {
        msg.getChannel().sendMessageEmbeds(EmbedUtils.addError()).queue(
            null,
            error -> ModMail.getInstance().error("Failed to send add warning: " + error.getMessage(), error)
        );
    }

    private boolean isModerator(@NotNull Member m, @NotNull List<String> modRoles) {
        for(Role r : m.getRoles()) {
            if(modRoles.contains(r.getId()))
                return true;
        }
        return false;
    }

    private void invalidInbox(final @NotNull TextChannel channel, final @NotNull Message msg) {
        try {
            channel.sendMessageEmbeds(EmbedUtils.notInbox(msg.getTimeCreated())).queue(
                message -> msg.delete().queue(
                    null,
                    error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage(), error)
                ),
                error -> ModMail.getInstance().error("Failed to warn reply: " + error.getMessage(), error)
            );
        }
        catch (InsufficientPermissionException error) {
            ModMail.getInstance().error("Failed to delete: " + error.getMessage(), error);
        }
    }

    private void invalidCommand(final @NotNull Message msg) {
        try {
            msg.getChannel().sendMessageEmbeds(EmbedUtils.invalidCmd(msg)).queue(
                message -> msg.delete().queue(
                    null,
                    error -> ModMail.getInstance().error("Failed to delete: " + error.getMessage(), error)
                ),
                error -> ModMail.getInstance().error("Failed to send invalid command: " + error.getMessage(), error)
            );
        }
        catch (InsufficientPermissionException e) {
            ModMail.getInstance().error("Failed to send invalid command: " + e.getMessage(), e);
        }
    }

    private void unblock(final @NotNull Message msg, final @NotNull UserSnowflake unblocked) {
        // Send unblock message
        msg.getChannel().sendMessageEmbeds(
            EmbedUtils.unblocked(msg.getAuthor(), unblocked)
        ).queue(
            // Delete command
            message -> msg.delete().queue(
                null,
                error -> ModMail.getInstance().error("Failed to delete unblocked: " + unblocked.getId(), error)
            ),
            error -> ModMail.getInstance().error("Failed to send unblocked: " + msg, error)
        );
    }

    private void unblockFailed(final @NotNull Message msg, final @NotNull UserSnowflake unblocked) {
        // Send unblock failed message
        msg.getChannel().sendMessageEmbeds(
            EmbedUtils.unblockFailed(msg.getAuthor(), unblocked)
        ).queue(
            // Delete command
            message -> msg.delete().queue(
                null,
                error -> ModMail.getInstance().error("Failed to delete unblocked: " + unblocked.getId(), error)
            ),
            error -> ModMail.getInstance().error("Failed to send unblocked: " + msg, error)
        );
    }

    private void block(final @NotNull Message msg, final @NotNull UserSnowflake blocked) {
        msg.getChannel().sendMessageEmbeds(
            EmbedUtils.blocked(msg.getAuthor(), blocked)
        ).queue(
            message -> msg.delete().queue(
                null,
                error -> ModMail.getInstance().error("Failed to delete blocked: " + blocked.getId(), error)
            ),
            error -> ModMail.getInstance().error("Failed to send blocked: " + msg, error)
        );
    }

    private void blockFailed(final @NotNull Message msg, final @Nullable UserSnowflake blocked) {
        msg.getChannel().sendMessageEmbeds(
            EmbedUtils.blockFailed(msg.getAuthor(), blocked)
        ).queue(
            message -> msg.delete().queue(
                null,
                error -> ModMail.getInstance().error("Failed to delete blocked: " + msg, error)
            ),
            error -> ModMail.getInstance().error("Failed to send blocked: " + msg, error)
        );
    }

    private void remindMe(final @NotNull Message msg, boolean ping) {
        try {
            String[] content = msg.getContentStripped().substring(1).split(" ");
            long time = Long.parseLong(content[1]);
            if (time < 1)
                throw new IllegalArgumentException("Invalid time");
            TimeUnit unit;
            switch (content[2].toLowerCase()) {
                case "w", "week", "weeks" -> {
                    unit = TimeUnit.DAYS;
                    time *= 7;
                }
                case "d", "day", "days" -> unit = TimeUnit.DAYS;
                case "h", "hr", "hour", "hours" -> unit = TimeUnit.HOURS;
                case "m", "min", "mins", "minute", "minutes" -> unit = TimeUnit.MINUTES;
                case "s", "sec", "secs", "second", "seconds" -> unit = TimeUnit.SECONDS;
                default -> throw new IllegalArgumentException("Invalid unit");
            }
            String unitName = unit.name().toLowerCase();
            if (time == 1)  // Remove plural
                unitName = unitName.substring(0, unitName.length() - 1);
            msg.reply(
                    new MessageCreateBuilder().setEmbeds(EmbedUtils.remind(
                            "You will be reminded in " + time + " " + unitName + "."
                    )).build()
            ).mentionRepliedUser(false).queue(
                    null,
                    error -> ModMail.getInstance().error("Failed to send remind msg: " + error.getMessage(), error)
            );
            msg.reply(
                    new MessageCreateBuilder().setEmbeds(EmbedUtils.remind("Reminder!")).build()
            ).mentionRepliedUser(ping).queueAfter(time, unit,
                    null,
                    error -> ModMail.getInstance().error("Failed to send reminder: " + error.getMessage(), error)
            );
        }
        catch (ArrayIndexOutOfBoundsException | IllegalArgumentException e) {
            msg.reply(
                    new MessageCreateBuilder().setEmbeds(EmbedUtils.remindFailed()).build()
            ).mentionRepliedUser(false).queue(
                null,
                error -> ModMail.getInstance().error("Failed to send remind failed: " + error.getMessage(), error)
            );
        }
    }
}
