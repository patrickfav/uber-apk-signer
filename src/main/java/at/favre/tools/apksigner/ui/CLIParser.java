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
            argument.zipAlignPath = commandLine.getOptionValue("zipAlignPath");
            argument.out = commandLine.getOptionValue("out");

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

            if (argument.overwrite && argument.out != null) {
                throw new IllegalArgumentException("either provide out path or overwrite argument, cannot process both");
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
        Option apkPathOpt = Option.builder(ARG_APK_FILE).longOpt("apks").argName("file/folder").hasArg(true).desc("Can be a single apk or a folder containing multiple apks. These are used as source for zipalining/signing/verifying").build();
        Option outOpt = Option.builder("o").longOpt("out").argName("path").hasArg(true).desc("Where the aligned/signed apks will be copied to. Must be a folder. Will generate, if not existent.").build();

        Option ksOpt = Option.builder().longOpt("ks").argName("keystore").hasArg(true).desc("The keystore file. If this isn't provided, will try to sign with a debug keystore. The debug keystore will be searched in the same dir as execution and 'user_home/.android' folder. If it is not found there a built-in keystore will be used for convenience.").build();
        Option ksPassOpt = Option.builder().longOpt("ksPass").argName("password").hasArg(true).desc("The password for the keystore. If this is not provided, caller will get an user prompt to enter it.").build();
        Option ksKeyPassOpt = Option.builder().longOpt("ksKeyPass").argName("password").hasArg(true).desc("The password for the key. If this is not provided, caller will get an user prompt to enter it.").build();
        Option ksAliasOpt = Option.builder().longOpt("ksAlias").argName("alias").hasArg(true).desc("The alias of the used key in the keystore. Must be provided if --ks is provided.").build();
        Option zipAlignPathOpt = Option.builder().longOpt("zipAlignPath").argName("path").hasArg(true).desc("Pass your own zipalign executable. If this is omitted the built-in version is used (available for win, mac and linux)").build();

        Option verifyOnlyOpt = Option.builder().longOpt(ARG_VERIFY).hasArg(false).desc("If this is passed, the signature and alignment is only verified.").build();
        Option dryRunOpt = Option.builder().longOpt("dryRun").hasArg(false).desc("Check what apks would be processed").build();
        Option skipZipOpt = Option.builder().longOpt("skipZipAlign").hasArg(false).desc("Skips zipAlign process. Also affects verify.").build();
        Option overwriteOpt = Option.builder().longOpt("overwrite").hasArg(false).desc("Will overwrite/delete the apks in-place").build();
        Option verboseOpt = Option.builder().longOpt("verbose").hasArg(false).desc("Prints more output, especially useful for sign verify.").build();
        Option debugOpt = Option.builder().longOpt("debug").hasArg(false).desc("Prints additional info for debugging.").build();

        Option help = Option.builder("h").longOpt("help").desc("Prints help docs.").build();
        Option version = Option.builder("v").longOpt("version").desc("Prints current version.").build();

        OptionGroup mainArgs = new OptionGroup();
        mainArgs.addOption(apkPathOpt).addOption(help).addOption(version);
        mainArgs.setRequired(true);

        options.addOptionGroup(mainArgs);
        options.addOption(ksOpt).addOption(ksPassOpt).addOption(ksKeyPassOpt).addOption(ksAliasOpt).addOption(verifyOnlyOpt)
                .addOption(dryRunOpt).addOption(skipZipOpt).addOption(overwriteOpt).addOption(verboseOpt).addOption(debugOpt)
                .addOption(zipAlignPathOpt).addOption(outOpt);

        return options;
    }

    private static void printHelp(Options options) {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(110);
        help.setLeftPadding(4);
        help.printHelp("uber-apk-signer", "Version: " + CLIParser.class.getPackage().getImplementationVersion(), options, "", true);
    }
}
