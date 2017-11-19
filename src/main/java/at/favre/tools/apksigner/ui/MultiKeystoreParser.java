package at.favre.tools.apksigner.ui;

import org.apache.commons.cli.CommandLine;

import java.util.*;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;

/**
 * Responsible for parsing the signing config arguments, especially if multiple configs are passed.
 */
public class MultiKeystoreParser {
    public static final String sep = "=";

    List<Arg.SignArgs> parse(CommandLine commandLine) {
        if (commandLine.hasOption("ksDebug")) {
            return singletonList(new Arg.SignArgs(0, commandLine.getOptionValue("ksDebug"), null, null, null));
        } else {
            List<Arg.SignArgs> signArgsList = new ArrayList<>();
            String[] ksArgs = commandLine.getOptionValues("ks");
            if (!commandLine.hasOption("ks")) {
                return signArgsList;
            } else if (ksArgs.length == 1) {
                signArgsList.add(new Arg.SignArgs(0, commandLine.getOptionValue("ks"), commandLine.getOptionValue("ksAlias"),
                        commandLine.getOptionValue("ksPass"), commandLine.getOptionValue("ksKeyPass")));
            } else if (ksArgs.length > 1) {
                Map<Integer, String> ksArgList = new HashMap<>();
                Map<Integer, String> ksAliasArgList = new HashMap<>();
                Map<Integer, String> ksPassArgList = new HashMap<>();
                Map<Integer, String> ksKeyPassARgList = new HashMap<>();

                for (String arg : ksArgs) {
                    Entry entry = parseEntry(arg);
                    ksArgList.put(entry.index, entry.value);
                }

                for (String arg : commandLine.getOptionValues("ksAlias")) {
                    Entry entry = parseEntry(arg);
                    ksAliasArgList.put(entry.index, entry.value);
                }

                if (commandLine.hasOption("ksPass")) {
                    for (String arg : commandLine.getOptionValues("ksPass")) {
                        Entry entry = parseEntry(arg);
                        ksPassArgList.put(entry.index, entry.value);
                    }
                }

                if (commandLine.hasOption("ksKeyPass")) {
                    for (String arg : commandLine.getOptionValues("ksKeyPass")) {
                        Entry entry = parseEntry(arg);
                        ksKeyPassARgList.put(entry.index, entry.value);
                    }
                }

                if (ksArgList.size() != ksAliasArgList.size()) {
                    throw new IllegalArgumentException("must provide the same count of --ks as --ksAlias");
                }

                signArgsList.addAll(ksArgList.entrySet().stream().map(entry -> new Arg.SignArgs(entry.getKey(), entry.getValue(), ksAliasArgList.get(entry.getKey()),
                        ksPassArgList.get(entry.getKey()), ksKeyPassARgList.get(entry.getKey()))).collect(Collectors.toList()));
            }

            for (int i = 0; i < signArgsList.size(); i++) {
                Arg.SignArgs signArgs = signArgsList.get(i);

                if (signArgs.ksFile == null && (signArgs.pass != null || signArgs.keyPass != null || signArgs.alias != null)) {
                    throw new IllegalArgumentException("must provide keystore file if any keystore config is given for sign config " + i);
                }

                if (signArgs.ksFile != null && signArgs.alias == null) {
                    throw new IllegalArgumentException("must provide alias if keystore is given for sign config " + i);
                }
            }
            Collections.sort(signArgsList);

            return signArgsList;
        }
    }

    private Entry parseEntry(String ksArgs) {
        String[] parts = ksArgs.trim().split(sep);

        if (parts.length != 2) {
            throw new IllegalArgumentException("must be of syntax <index>" + sep + "<argument> - " + ksArgs);
        }

        if (!parts[0].chars().allMatch(Character::isDigit)) {
            throw new IllegalArgumentException("first parm of <index>" + sep + "<argument> must be integer: " + parts[0]);
        }
        return new Entry(Integer.valueOf(parts[0]), parts[1]);
    }

    private static class Entry {
        final int index;
        final String value;

        Entry(int index, String value) {
            this.index = index;
            this.value = value;
        }
    }
}
