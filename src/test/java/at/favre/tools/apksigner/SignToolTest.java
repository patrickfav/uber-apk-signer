package at.favre.tools.apksigner;

import at.favre.tools.apksigner.signing.AndroidApkSignerVerify;
import at.favre.tools.apksigner.ui.CLIParser;
import at.favre.tools.apksigner.ui.CLIParserTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static junit.framework.TestCase.*;

public class SignToolTest {
    private final static String ksAlias = "app";
    private final static String ksPass = "password";
    private final static String keyPass = "keypass";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    public File originalFolder;
    public File outFolder;
    public File testReleaseKs;

    private List<File> unsingedApks;
    private List<File> singedApks;

    @Before
    public void setUp() throws Exception {
        originalFolder = temporaryFolder.newFolder("signer-test", "apks");
        outFolder = temporaryFolder.newFolder("signer-test", "out");
        testReleaseKs = new File(getClass().getClassLoader().getResource("test-release-key.jks").toURI().getPath());

        File signedFolder = new File(getClass().getClassLoader().getResource("test-apks-signed").toURI().getPath());
        File unsignedFolder = new File(getClass().getClassLoader().getResource("test-apks-unsigned").toURI().getPath());

        singedApks = Arrays.asList(signedFolder.listFiles());
        unsingedApks = Arrays.asList(unsignedFolder.listFiles());

        assertFalse(singedApks.isEmpty());
        assertFalse(unsingedApks.isEmpty());
    }

    @Test
    public void testSignMultipleApks() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks);

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT + " " + outFolder.getAbsolutePath() + " --" + CLIParser.ARG_SKIP_ZIPALIGN;
        System.out.println(cmd);

        SignTool.main(CLIParserTest.asArgArray(cmd));
        assertSigned(outFolder, uApks);
    }

    @Test
    public void testSignSingleApk() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks.subList(0, 1));

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.listFiles()[0].getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT + " " + outFolder.getAbsolutePath() + " --" + CLIParser.ARG_SKIP_ZIPALIGN;

        System.out.println(cmd);

        SignTool.main(CLIParserTest.asArgArray(cmd));
        assertSigned(outFolder, uApks);
    }

    @Test
    public void testSignMultipleApksCustomCert() throws Exception {
        List<File> uApks = copyToTestPath(originalFolder, unsingedApks);

        String cmd = "-" + CLIParser.ARG_APK_FILE + " " + originalFolder.getAbsolutePath() + " -" + CLIParser.ARG_APK_OUT + " " + outFolder.getAbsolutePath() + " --" + CLIParser.ARG_SKIP_ZIPALIGN + " --ks " + testReleaseKs.getAbsolutePath() + " --ksPass " + ksPass + " --ksKeyPass " + keyPass + " --ksAlias " + ksAlias;
        System.out.println(cmd);

        SignTool.main(CLIParserTest.asArgArray(cmd));
        assertSigned(outFolder, uApks);
    }

    private static void assertSigned(File outFolder, List<File> uApks) throws Exception {
        assertNotNull(outFolder);
        File[] outFiles = outFolder.listFiles();
        assertNotNull(outFiles);
        assertEquals("should be same count of apks in out folder", uApks.size(), outFiles.length);

        for (File outFile : outFiles) {
            AndroidApkSignerVerify.Result verifyResult = new AndroidApkSignerVerify().verify(outFile, null, null, false);
            assertTrue(verifyResult.verified);
            assertEquals(0, verifyResult.warning);
        }
    }


    private static List<File> copyToTestPath(File target, List<File> source) throws Exception {
        List<File> copiedFiles = new ArrayList<>();
        for (File file : source) {
            File dstFile = new File(target, file.getName());
            Files.copy(file.toPath(), dstFile.toPath());
            copiedFiles.add(dstFile);
        }
        return copiedFiles;
    }

}
