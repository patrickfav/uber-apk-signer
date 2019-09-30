package at.favre.tools.apksigner.ui;

import org.apache.tools.ant.types.Commandline;
import org.junit.Test;

import java.util.Collections;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class CLIParserTest {
    @Test
    public void testSimpleWithOnlyApkFile() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, false, false, false, false, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithOut() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ -" + CLIParser.ARG_APK_OUT + " ./test"));
        Arg expectedArg = new Arg(new String[]{"./"}, "./test", Collections.emptyList(), false, false, false, false, false, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithKeystoreAndAlias() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ --ks my-keystore.jks --ksAlias debugAlias"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.singletonList(new Arg.SignArgs(0, "my-keystore.jks", "debugAlias", null, null)), false, false, false, false, false, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithDebugKeystore() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ --ksDebug my-debug-keystore.jks"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.singletonList(new Arg.SignArgs(0, "my-debug-keystore.jks", null, null, null)), false, false, false, false, false, false, null, true, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithDebugKeystoreAndNormalKeystore() {
        assertNull(CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ --ksDebug my-debug-keystore.jks --ks my-keystore.jks")));
    }

    @Test
    public void testWithKeystoreAndAliasAndPws() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ --ks my-keystore.jks --ksAlias debugAlias --ksPass secret --ksKeyPass secret2"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.singletonList(new Arg.SignArgs(0, "my-keystore.jks", "debugAlias", "secret", "secret2")), false, false, false, false, false, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithOnlyVerify() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ --" + CLIParser.ARG_VERIFY));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, false, false, false, false, true, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithDebug() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --debug"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, false, false, false, true, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithSkipZipAlign() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --" + CLIParser.ARG_SKIP_ZIPALIGN));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, false, false, true, false, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithOverwrite() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --overwrite"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), true, false, false, false, false, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithDryRun() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --dryRun"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, true, false, false, false, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithVerbose() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --verbose"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, false, true, false, false, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithAllowResign() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --allowResign"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, false, false, false, false, false, null, false, true, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testMultipleFiles() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ apk1.apk apk2.apk"));
        Arg expectedArg = new Arg(new String[]{"./", "apk1.apk", "apk2.apk"}, null, Collections.emptyList(), false, false, false, false, false, false, null, false, false, null, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testProvideSha256Hashes() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --verifySha256 a18bc579adba6819a57a665cdf2bfe0b6f2a81263cb2d6860a3d35fac428999a"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, false, false, false, false, false, null, false, false, new String[]{"a18bc579adba6819a57a665cdf2bfe0b6f2a81263cb2d6860a3d35fac428999a"}, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testProvideTwoSha256Hashes() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + "./ --verifySha256 a18bc579adba6819a57a665cdf2b fe0b6f2a81263cb2d6860a3d35fac428999a"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, false, false, false, false, false, null, false, false, new String[]{"a18bc579adba6819a57a665cdf2b", "fe0b6f2a81263cb2d6860a3d35fac428999a"}, null);
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testWithKeystoreAndLineage() {
        Arg parsedArg = CLIParser.parse(asArgArray("-" + CLIParser.ARG_APK_FILE + " ./ --lineage ./file.lineage"));
        Arg expectedArg = new Arg(new String[]{"./"}, null, Collections.emptyList(), false, false, false, false, false, false, null, false, false, null, "./file.lineage");
        assertEquals(expectedArg, parsedArg);
    }

    @Test
    public void testHelp() {
        Arg parsedArg = CLIParser.parse(asArgArray("--help"));
        assertNull(parsedArg);
    }

    @Test
    public void testVersion() {
        Arg parsedArg = CLIParser.parse(asArgArray("--version"));
        assertNull(parsedArg);
    }

    public static String[] asArgArray(String cmd) {
        return Commandline.translateCommandline(cmd);
    }
}
