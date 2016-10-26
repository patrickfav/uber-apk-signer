package at.favre.tools.apksigner.ui;

import org.apache.tools.ant.types.Commandline;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CLIParserTest {
    @Test
    public void testSimpleWithOnlyApkFile() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./"));
        Arg expectedArg = new Arg("./", null, null, null, null, null, false, false, false, false, false, false, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithOut() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ -" + CLIParser.ARG_APK_OUT + " ./test"));
        Arg expectedArg = new Arg("./", "./test", null, null, null, null, false, false, false, false, false, false, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithKeystoreAndAlias() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ --ks my-keystore.jks --ksAlias debugAlias"));
        Arg expectedArg = new Arg("./", null, "my-keystore.jks", null, null, "debugAlias", false, false, false, false, false, false, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithKeystoreAndAliasAndPws() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ --ks my-keystore.jks --ksAlias debugAlias --ksPass secret --ksKeyPass secret2"));
        Arg expectedArg = new Arg("./", null, "my-keystore.jks", "secret", "secret2", "debugAlias", false, false, false, false, false, false, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithOnlyVerify() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ --" + CLIParser.ARG_VERIFY));
        Arg expectedArg = new Arg("./", null, null, null, null, null, false, false, false, false, false, true, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithDebug() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --debug"));
        Arg expectedArg = new Arg("./", null, null, null, null, null, false, false, false, false, true, false, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithSkipZipAlign() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --" + CLIParser.ARG_SKIP_ZIPALIGN));
        Arg expectedArg = new Arg("./", null, null, null, null, null, false, false, false, true, false, false, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithOverwrite() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --overwrite"));
        Arg expectedArg = new Arg("./", null, null, null, null, null, true, false, false, false, false, false, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithDryRun() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --dryRun"));
        Arg expectedArg = new Arg("./", null, null, null, null, null, false, true, false, false, false, false, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithVerbose() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --verbose"));
        Arg expectedArg = new Arg("./", null, null, null, null, null, false, false, true, false, false, false, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testHelp() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("--help"));
        assertNull(parsedArg);
    }

    @Test
    public void testVersion() throws Exception {
        Arg parsedArg = CLIParser.parse(asArgArray("--version"));
        assertNull(parsedArg);
    }

    public static String[] asArgArray(String cmd) {
        return Commandline.translateCommandline(cmd);
    }
}
