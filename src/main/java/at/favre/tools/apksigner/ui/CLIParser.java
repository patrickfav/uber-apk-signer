package at.favre.tools.apksigner.ui;

import org.apache.commons.cli.*;

public class CLIParser {

    static final String ARG_APK_FILE = "a";
    static final String ARG_VERIFY = "onlyVerify";

    public static Arg parse(String[] args) {
        Options options = setupOptions();
        CommandLineParser parser = new DefaultParser();
        Arg argument = new Arg();

        try {
            CommandLine commandLine = parser.parse(options, args);

            if (commandLine.hasOption("h") || commandLine.hasOption("help")) {
                printHelp(options);
                return null;
            }

            if (commandLine.hasOption("v") || commandLine.hasOption("version")) {
                System.out.println("Version: " + CLIParser.class.getPackage().getImplementationVersion());
                return null;
            }

            argument.apkFile = commandLine.getOptionValue(ARG_APK_FILE);
            argument.zipAlignPath = commandLine.getOptionValue("zipalignPath");

            argument.ksFile = commandLine.getOptionValue("ks");
            argument.ksPass = commandLine.getOptionValue("ksPass");
            argument.ksAliasName = commandLine.getOptionValue("ksAlias");
            argument.ksKeyPass = commandLine.getOptionValue("ksKeyPass");

            argument.onlyVerify = commandLine.hasOption(ARG_VERIFY);
            argument.dryRun = commandLine.hasOption("dryRun");
            argument.debug = commandLine.hasOption("debug");
            argument.overwrite = commandLine.hasOption("overwrite");
            argument.verbose = commandLine.hasOption("verbose");
            argument.skipZipAlign = commandLine.hasOption("skipZipAlign");

            if (argument.apkFile == null || argument.apkFile.isEmpty()) {
                throw new IllegalArgumentException("must provide apk file or folder");
            }

            if (argument.ksFile == null && (argument.ksPass != null || argument.ksKeyPass != null || argument.ksAliasName != null)) {
                throw new IllegalArgumentException("must provide keystore file if any keystore config is given");
            }

        } catch (Exception e) {
            System.err.println(e.getMessage());

            CLIParser.printHelp(options);

            argument = null;
        }

        return argument;
    }

    private static Options setupOptions() {
        Options options = new Options();
        Option apkPathOpt = Option.builder(ARG_APK_FILE).longOpt("apk").argName("package name").hasArg(true).desc("Filter string that has to be a package name or part of it containing wildcards '*'. Can be multiple filter Strings comma separated. Example: 'com.android.*' or 'com.android.*,com.google.*'").build();
        Option ksOpt = Option.builder().longOpt("ks").argName("keystore").hasArg(true).desc("").build();
        Option ksPassOpt = Option.builder().longOpt("ksPass").argName("password").hasArg(true).desc("").build();
        Option ksKeyPassOpt = Option.builder().longOpt("ksKeyPass").argName("password").hasArg(true).desc("").build();
        Option ksAliasOpt = Option.builder().longOpt("ksAlias").argName("alias").hasArg(true).desc("").build();

        Option verifyOnlyOpt = Option.builder().longOpt(ARG_VERIFY).hasArg(false).desc("").build();
        Option dryRunOpt = Option.builder().longOpt("dryRun").hasArg(false).desc("").build();
        Option skipZipOpt = Option.builder().longOpt("skipZipAlign").hasArg(false).desc("Skips zipAlign process.").build();
        Option overwriteOpt = Option.builder().longOpt("overwrite").hasArg(false).desc("Will overwrite/delete the unsigned apks").build();
        Option verboseOpt = Option.builder().longOpt("verbose").hasArg(false).desc("Prints more output, especially useful for sign verify.").build();
        Option debugOpt = Option.builder().longOpt("debug").hasArg(false).desc("Prints additional info for debugging.").build();

        Option help = Option.builder("h").longOpt("help").desc("Prints docs").build();
        Option version = Option.builder("v").longOpt("version").desc("Prints current version.").build();

        OptionGroup mainArgs = new OptionGroup();
        mainArgs.addOption(apkPathOpt).addOption(help).addOption(version);
        mainArgs.setRequired(true);

        options.addOptionGroup(mainArgs);
        options.addOption(ksOpt).addOption(ksPassOpt).addOption(ksKeyPassOpt).addOption(ksAliasOpt).addOption(verifyOnlyOpt)
                .addOption(dryRunOpt).addOption(skipZipOpt).addOption(overwriteOpt).addOption(verboseOpt).addOption(debugOpt);

        return options;
    }

    private static void printHelp(Options options) {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(110);
        help.setLeftPadding(4);
        help.printHelp("uber-apk-signer", "Version: " + CLIParser.class.getPackage().getImplementationVersion(), options, "", true);
    }
}
