package com.chai.miniGolf.commands;

import com.chai.miniGolf.events.CoursePlayRequestedEvent;
import com.chai.miniGolf.models.Course;
import com.chai.miniGolf.models.Score;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.chai.miniGolf.Main.getPlugin;

public class RankCommand implements CommandExecutor, TabCompleter {
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You need to provide the number of ranks to return. Like: \"/mgrank 10 Rolling Greens\".");
            return true;
        }
        if (args.length < 2) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + " You need to provide a course name. Like: \"/mgrank 10 Rolling Greens\".");
            return true;
        }
        int maxRanks;
        try {
            maxRanks = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + args[0] + " is not a valid number of ranks.");
            return true;
        }
        String courseName = String.join(" ", Arrays.copyOfRange(args, 1, args.length));
        Optional<Course> maybeCourse = getPlugin().config().getCourse(courseName);
        if (maybeCourse.isEmpty()) {
            sender.sendMessage(ChatColor.WHITE + "[MiniGolf]" + ChatColor.RED + courseName + " is not a course that exists.");
            return true;
        } else {
            sender.sendMessage(rankMessage(maybeCourse.get(), maxRanks));
        }
        return true;
    }

    private String rankMessage(Course course, int maxRanks) {
        Comparator<Map.Entry<String, Score>> scoreComparator = Comparator.comparingInt(e -> e.getValue().getScore());
        Comparator<Map.Entry<String, Score>> thenComparator = Comparator.comparingLong(e -> e.getValue().getTimestamp());
        List<String> ranks = course.getLeaderboards().entrySet().stream()
            .sorted(scoreComparator.thenComparing(thenComparator))
            .map(e -> String.format("%s : %s", e.getKey(), e.getValue().getScore()))
            .toList();
        StringBuilder rankString = new StringBuilder();
        for (int i = 1; i <= ranks.size(); i++) {
            if (i > maxRanks) {
                break;
            }
            if (i != 1) {
                rankString.append("\n");
            }
            rankString.append(String.format("%s. %s", i, ranks.get(i - 1)));
        }
        if (ranks.isEmpty()) {
            return "No scores yet";
        }
        return rankString.toString();
    }

    @Override
    public List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] args) {
        if (args.length > 1) {
            var courseNameArgs = Arrays.copyOfRange(args, 1, args.length);
            return getPlugin().config().courses().stream().map(Course::getName)
                .filter(courseName -> courseName.toLowerCase().startsWith(String.join(" ", courseNameArgs).toLowerCase()))
                .map(courseName -> courseName.split(" "))
                .filter(courseNameArray -> courseNameArray.length >= courseNameArgs.length)
                .map(courseNameArray -> Arrays.copyOfRange(courseNameArray, courseNameArgs.length - 1, courseNameArray.length))
                .map(courseNameArray -> String.join(" ", courseNameArray))
                .toList();
        }
        return List.of();
    }
}
