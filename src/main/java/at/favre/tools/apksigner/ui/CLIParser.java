package at.favre.tools.apksigner.ui;

import at.favre.tools.apksigner.util.CmdUtil;
import org.apache.commons.cli.*;

/**
 * Parses the command line input and converts it to a structured model ({@link Arg}
 */
public final class CLIParser {

    public static final String ARG_APK_FILE = "a";
    public static final String ARG_APK_OUT = "o";
    public static final String ARG_VERIFY = "onlyVerify";
    public static final String ARG_SKIP_ZIPALIGN = "skipZipAlign";

    private CLIParser() {
    }

    public static Arg parse(String[] inputArgs) {
        Options options = setupOptions();
        CommandLineParser parser = new DefaultParser();
        Arg argument = new Arg();

        try {
            CommandLine commandLine = parser.parse(options, inputArgs);

            if (commandLine.hasOption("h") || commandLine.hasOption("help")) {
                printHelp(options);
                return null;
            }

            if (commandLine.hasOption("v") || commandLine.hasOption("version")) {
                System.out.println("Version: " + CLIParser.class.getPackage().getImplementationVersion());
                return null;
            }

            argument.apkFile = commandLine.getOptionValues(ARG_APK_FILE);
            argument.zipAlignPath = commandLine.getOptionValue("zipAlignPath");
            argument.out = commandLine.getOptionValue(ARG_APK_OUT);

            if (commandLine.hasOption("ksDebug") && commandLine.hasOption("ks")) {
                throw new IllegalArgumentException("Either provide normal keystore or debug keystore location, not both.");
            }

            if (commandLine.hasOption("verifySha256")) {
                argument.checkCertSha256 = commandLine.getOptionValues("verifySha256");
            }

            argument.signArgsList = new MultiKeystoreParser().parse(commandLine);
            argument.ksIsDebug = commandLine.hasOption("ksDebug");
            argument.onlyVerify = commandLine.hasOption(ARG_VERIFY);
            argument.dryRun = commandLine.hasOption("dryRun");
            argument.debug = commandLine.hasOption("debug");
            argument.overwrite = commandLine.hasOption("overwrite");
            argument.verbose = commandLine.hasOption("verbose");
            argument.allowResign = commandLine.hasOption("allowResign");
            argument.skipZipAlign = commandLine.hasOption(ARG_SKIP_ZIPALIGN);

            if (argument.apkFile == null || argument.apkFile.length == 0) {
                throw new IllegalArgumentException("must provide apk file or folder");
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

    static Options setupOptions() {
        Options options = new Options();
        Option apkPathOpt = Option.builder(ARG_APK_FILE).longOpt("apks").argName("file/folder").hasArgs().desc("Can be a single apk or " +
                "a folder containing multiple apks. These are used as source for zipalining/signing/verifying. It is also possible to provide " +
                "multiple locations space seperated (can be mixed file folder): '/apk /apks2 my.apk'. Folder will be checked non-recursively.").build();
        Option outOpt = Option.builder(ARG_APK_OUT).longOpt("out").argName("path").hasArg().desc("Where the aligned/signed apks will be copied " +
                "to. Must be a folder. Will create, if it does not exist.").build();

        Option ksOpt = Option.builder().longOpt("ks").argName("keystore").hasArgs().desc("The keystore file. If this isn't provided, will try" +
                "to sign with a debug keystore. The debug keystore will be searched in the same dir as execution and 'user_home/.android' folder. " +
                "If it is not found there a built-in keystore will be used for convenience. It is possible to pass one or multiple keystores. " +
                "The syntax for multiple params is '<index>" + MultiKeystoreParser.sep + "<keystore>' for example: '1" + MultiKeystoreParser.sep +
                "keystore.jks'. Must match the parameters of --ksAlias.").build();
        Option ksDebugOpt = Option.builder().longOpt("ksDebug").argName("keystore").hasArg().desc("Same as --ks parameter but with a debug keystore." +
                " With this option the default keystore alias and passwords are used and any arguments relating to these parameter are ignored.").build();
        Option ksPassOpt = Option.builder().longOpt("ksPass").argName("password").hasArgs().desc("The password for the keystore. If this is " +
                "not provided, caller will get a user prompt to enter it. It is possible to pass one or multiple passwords for multiple keystore " +
                "configs. The syntax for multiple params is '<index>" + MultiKeystoreParser.sep + "<password>'. Must match the parameters of --ks.").build();
        Option ksKeyPassOpt = Option.builder().longOpt("ksKeyPass").argName("password").hasArgs().desc("The password for the key. If this is not" +
                " provided, caller will get a user prompt to enter it. It is possible to pass one or multiple passwords for multiple keystore configs." +
                " The syntax for multiple params is '<index>" + MultiKeystoreParser.sep + "<password>'. Must match the parameters of --ks.").build();
        Option ksAliasOpt = Option.builder().longOpt("ksAlias").argName("alias").hasArgs().desc("The alias of the used key in the keystore. Must be" +
                " provided if --ks is provided. It is possible to pass one or multiple aliases for multiple keystore configs. The syntax for multiple" +
                " params is '<index>" + MultiKeystoreParser.sep + "<alias>' for example: '1" + MultiKeystoreParser.sep + "my-alias'. Must match the parameters of --ks.").build();
        Option zipAlignPathOpt = Option.builder().longOpt("zipAlignPath").argName("path").hasArg().desc("Pass your own zipalign executable. If this " +
                "is omitted the built-in version is used (available for win, mac and linux)").build();

        Option checkSh256Opt = Option.builder().longOpt("verifySha256").argName("cert-sha256").hasArgs().desc("Provide one or multiple sha256 in " +
                "string hex representation (ignoring case) to let the tool check it against hashes of the APK's certificate and use it in the verify" +
                " process. All given hashes must be present in the signature to verify e.g. if 2 hashes are given the apk must have 2 signatures with" +
                " exact these hashes (providing only one hash, even if it matches one cert, will fail).").build();

        Option verifyOnlyOpt = Option.builder("y").longOpt(ARG_VERIFY).hasArg(false).desc("If this is passed, the signature and alignment is only verified.").build();
        Option dryRunOpt = Option.builder().longOpt("dryRun").hasArg(false).desc("Check what apks would be processed without actually doing anything.").build();
        Option skipZipOpt = Option.builder().longOpt(ARG_SKIP_ZIPALIGN).hasArg(false).desc("Skips zipAlign process. Also affects verify.").build();
        Option overwriteOpt = Option.builder().longOpt("overwrite").hasArg(false).desc("Will overwrite/delete the apks in-place").build();
        Option verboseOpt = Option.builder().longOpt("verbose").hasArg(false).desc("Prints more output, especially useful for sign verify.").build();
        Option debugOpt = Option.builder().longOpt("debug").hasArg(false).desc("Prints additional info for debugging.").build();
        Option resignOpt = Option.builder().longOpt("allowResign").hasArg(false).desc("If this flag is set, the tool will not show error on signed apks, but will " +
                "sign them with the new certificate (therefore removing the old one).").build();

        Option help = Option.builder("h").longOpt("help").desc("Prints help docs.").build();
        Option version = Option.builder("v").longOpt("version").desc("Prints current version.").build();

        OptionGroup mainArgs = new OptionGroup();
        mainArgs.addOption(apkPathOpt).addOption(help).addOption(version);
        mainArgs.setRequired(true);

        options.addOptionGroup(mainArgs);
        options.addOption(ksOpt).addOption(ksPassOpt).addOption(ksKeyPassOpt).addOption(ksAliasOpt).addOption(verifyOnlyOpt)
                .addOption(dryRunOpt).addOption(skipZipOpt).addOption(overwriteOpt).addOption(verboseOpt).addOption(debugOpt)
                .addOption(zipAlignPathOpt).addOption(outOpt).addOption(ksDebugOpt).addOption(resignOpt).addOption(checkSh256Opt);

        return options;
    }

    private static void printHelp(Options options) {
        HelpFormatter help = new HelpFormatter();
        help.setWidth(110);
        help.setLeftPadding(4);
        help.printHelp("uber-apk-signer", "Version: " + CmdUtil.jarVersion(), options, "", true);
    }
}
