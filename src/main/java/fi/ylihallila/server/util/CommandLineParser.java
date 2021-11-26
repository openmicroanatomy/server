package fi.ylihallila.server.util;

import java.util.*;

public class CommandLineParser {

    private final List<String> args;
    private final HashMap<String, List<String>> map = new HashMap<>();
    private final Collection<String> flags = new HashSet<>();

    public CommandLineParser(String[] arguments) {
        this.args = Arrays.asList(arguments);
        map();
    }

    public Set<String> getArgumentNames() {
        Set<String> argumentNames = new HashSet<>();
        argumentNames.addAll(flags);
        argumentNames.addAll(map.keySet());

        return argumentNames;
    }

    public boolean hasFlag(String flag) {
        return flags.contains(flag);
    }

    public boolean hasArgument(String argument) {
        return map.containsKey(argument);
    }

    public String getString(String argument) {
        if (hasArgument(argument)) {
            return String.join(" ", map.get(argument));
        }

        throw new RuntimeException("Argument \"" + argument + "\" not present");
    }

    public Integer getInt(String argument) {
        try {
            return Integer.parseInt(getString(argument));
        } catch (NumberFormatException e) {
            throw new RuntimeException("Argument \"" + argument + "\" not a integer");
        }
    }

    public void map() {
        for (String arg : args) {
            if (arg.startsWith("-")) {
                if (args.indexOf(arg) == (args.size() - 1)) {
                    flags.add(arg.replace("-", ""));
                } else if (args.get(args.indexOf(arg) + 1).startsWith("-")) {
                    flags.add(arg.replace("-", ""));
                } else {
                    List<String> argumentValues = new ArrayList<>();
                    int i = 1;
                    while (args.indexOf(arg) + i != args.size() && !args.get(args.indexOf(arg) + i).startsWith("-")) {
                        argumentValues.add(args.get(args.indexOf(arg) + i));
                        i++;
                    }

                    map.put(arg.replace("-", ""), argumentValues);
                }
            }
        }
    }
}

