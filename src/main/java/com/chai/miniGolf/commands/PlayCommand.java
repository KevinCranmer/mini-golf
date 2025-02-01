package com.chai.miniGolf.commands;

import com.chai.miniGolf.events.CoursePlayRequestedEvent;
import com.chai.miniGolf.models.Course;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.chai.miniGolf.Main.getPlugin;

public class PlayCommand implements CommandExecutor, TabCompleter
{
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (hasIssues(sender, args)) {
            return true;
        }
        int courseNameEndIndex = getCourseNameEndIndex(args);
        Player golfer = getGolfer(sender, args);
        String courseName = String.join(" ", Arrays.copyOfRange(args, 0, courseNameEndIndex));
        Optional<Course> maybeCourse = getPlugin().config().getCourse(courseName);
        if (maybeCourse.isEmpty()) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + courseName + " is not a course that exists.");
            return true;
        } else {
            Bukkit.getPluginManager().callEvent(new CoursePlayRequestedEvent(golfer, maybeCourse.get()));
        }
        return true;
    }

    private int getCourseNameEndIndex(String[] args) {
        Optional<Player> maybePlayer = Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.getName().equals(args[args.length-1]))
            .map(p -> (Player) p)
            .findFirst();
        if (maybePlayer.isPresent() || "@p".equals(args[args.length - 1])) {
            return args.length - 1;
        }
        return args.length;
    }

    private Player getGolfer(CommandSender sender, String[] args) {
        if (sender instanceof BlockCommandSender bcs) {
            return findNearestPlayer(bcs);
        }
        Optional<Player> maybePlayer = Bukkit.getOnlinePlayers().stream()
            .filter(p -> p.getName().equals(args[args.length-1]))
            .map(p -> (Player) p)
            .findFirst();
        //Assume the player making the command is the player that will play the course
        return maybePlayer.orElseGet(() -> (Player) sender);
    }

    private Player findNearestPlayer(BlockCommandSender bcs) {
        Location senderLoc = bcs.getBlock().getLocation();
        return Bukkit.getServer().getOnlinePlayers().stream()
            .min((p1, p2) -> (int) (p1.getLocation().distanceSquared(senderLoc) - p2.getLocation().distanceSquared(senderLoc)))
            .orElse(null);
    }

    private boolean hasIssues(CommandSender sender, String[] args) {
        if (!sender.hasPermission("mgop")) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You do not have permission to run this command.");
            return true;
        }
        if (args.length < 1) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You need to provide a course name. Like: \"/mgplay Rolling Greens\".");
            return true;
        }
        return false;
    }

    @Nullable
    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length > 0) {
            List<String> courseNames = getPlugin().config().courses().stream().map(Course::getName)
                .filter(courseName -> courseName.toLowerCase().startsWith(String.join(" ", args).toLowerCase()))
                .map(courseName -> courseName.split(" "))
                .filter(courseNameArray -> courseNameArray.length >= args.length)
                .map(courseNameArray -> Arrays.copyOfRange(courseNameArray, args.length-1, courseNameArray.length))
                .map(courseNameArray -> String.join(" ", courseNameArray))
                .toList();
            List<String> playerNames = Bukkit.getOnlinePlayers().stream().map(Player::getName)
                .filter(playerName -> args.length > 1 && playerName.toLowerCase().startsWith(args[args.length-1].toLowerCase()))
                .toList();
            return Stream.of(courseNames, playerNames)
                .flatMap(Collection::stream)
                .toList();
        }
        return null;
    }
}