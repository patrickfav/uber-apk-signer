package at.favre.tools.apksigner.ui;

import org.apache.commons.cli.DefaultParser;
import org.junit.Test;

import java.util.List;

import static junit.framework.TestCase.assertEquals;

public class MultiKeystoreParserTest {
    private MultiKeystoreParser multiKeystoreParser = new MultiKeystoreParser();

    @Test
    public void testSingleCustomReleaseKs() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks test-release.jks --ksPass ksPass --ksKeyPass keyPass --ksAlias ksAlias";
        List<Arg.SignArgs> signArgsList = multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));

        assertEquals(1, signArgsList.size());
        assertEquals(new Arg.SignArgs(0, "test-release.jks", "ksAlias", "ksPass", "keyPass"), signArgsList.get(0));
    }

    @Test
    public void testSingleCustomReleaseKsNoKsPass() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks test-release.jks --ksKeyPass keyPass --ksAlias ksAlias";
        List<Arg.SignArgs> signArgsList = multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));

        assertEquals(1, signArgsList.size());
        assertEquals(new Arg.SignArgs(0, "test-release.jks", "ksAlias", null, "keyPass"), signArgsList.get(0));
    }

    @Test
    public void testSingleCustomReleaseKsNoKsPassAndKeyPass() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks test-release.jks --ksAlias ksAlias";
        List<Arg.SignArgs> signArgsList = multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));

        assertEquals(1, signArgsList.size());
        assertEquals(new Arg.SignArgs(0, "test-release.jks", "ksAlias", null, null), signArgsList.get(0));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testSingleCustomReleaseKsNoKsPassAndKeyPassNoAlias_shouldThrowException() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks test-release.jks ";
        multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));
    }

    @Test
    public void testTwoCustomReleaseKs() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks 1" + MultiKeystoreParser.sep + "test-release.jks 2" + MultiKeystoreParser.sep + "test2-release.jks --ksPass 1" + MultiKeystoreParser.sep + "ksPass1 2" + MultiKeystoreParser.sep + "ksPass2 --ksKeyPass 1" + MultiKeystoreParser.sep + "ksKeyPass1 2" + MultiKeystoreParser.sep + "ksKeyPass2 --ksAlias 1" + MultiKeystoreParser.sep + "ksAlias1 2" + MultiKeystoreParser.sep + "ksAlias2";
        List<Arg.SignArgs> signArgsList = multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));

        assertEquals(2, signArgsList.size());
        assertEquals(new Arg.SignArgs(1, "test-release.jks", "ksAlias1", "ksPass1", "ksKeyPass1"), signArgsList.get(0));
        assertEquals(new Arg.SignArgs(2, "test2-release.jks", "ksAlias2", "ksPass2", "ksKeyPass2"), signArgsList.get(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTwoCustomReleaseKsWithMissingAlias_shouldThrowException() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks 1" + MultiKeystoreParser.sep + "test-release.jks 2" + MultiKeystoreParser.sep + "test2-release.jks --ksAlias 2" + MultiKeystoreParser.sep + "ksAlias2";
        multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testTwoCustomReleaseKsWithUnknownAlias_shouldThrowException() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks 1" + MultiKeystoreParser.sep + "test-release.jks 2" + MultiKeystoreParser.sep + "test2-release.jks --ksAlias 3" + MultiKeystoreParser.sep + "ksAlias3 2" + MultiKeystoreParser.sep + "ksAlias2";
        multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));
    }

    @Test
    public void testTwoCustomReleaseKsWithoutPw() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks 1" + MultiKeystoreParser.sep + "test-release.jks 2" + MultiKeystoreParser.sep + "test2-release.jks --ksAlias  2" + MultiKeystoreParser.sep + "ksAlias2 1" + MultiKeystoreParser.sep + "ksAlias1";
        List<Arg.SignArgs> signArgsList = multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));

        assertEquals(2, signArgsList.size());
        assertEquals(new Arg.SignArgs(1, "test-release.jks", "ksAlias1", null, null), signArgsList.get(0));
        assertEquals(new Arg.SignArgs(2, "test2-release.jks", "ksAlias2", null, null), signArgsList.get(1));
    }

    @Test
    public void testTwoCustomReleaseKsWithPartialPw() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks 2" + MultiKeystoreParser.sep + "test2-release.jks 1" + MultiKeystoreParser.sep + "test-release.jks --ksPass 2" + MultiKeystoreParser.sep + "ksPass2 --ksKeyPass 1" + MultiKeystoreParser.sep + "ksKeyPass1 --ksAlias 1" + MultiKeystoreParser.sep + "ksAlias1 2" + MultiKeystoreParser.sep + "ksAlias2";
        List<Arg.SignArgs> signArgsList = multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));

        assertEquals(2, signArgsList.size());
        assertEquals(new Arg.SignArgs(1, "test-release.jks", "ksAlias1", null, "ksKeyPass1"), signArgsList.get(0));
        assertEquals(new Arg.SignArgs(2, "test2-release.jks", "ksAlias2", "ksPass2", null), signArgsList.get(1));
    }

    @Test
    public void testThreeCustomReleaseKsWithRndIndex() throws Exception {
        String cmd = "-" + CLIParser.ARG_APK_FILE + " ./ --ks 97" + MultiKeystoreParser.sep + "test-release.jks 4" + MultiKeystoreParser.sep + "test2-release.jks 7899" + MultiKeystoreParser.sep + "test3-release.jks " +
                "--ksPass 4" + MultiKeystoreParser.sep + "ksPass2 97" + MultiKeystoreParser.sep + "ksPass1 7899" + MultiKeystoreParser.sep + "ksPass3 --ksKeyPass 4" + MultiKeystoreParser.sep + "ksKeyPass2 --ksAlias 7899" + MultiKeystoreParser.sep + "ksAlias3 97" + MultiKeystoreParser.sep + "ksAlias1 4" + MultiKeystoreParser.sep + "ksAlias2";
        List<Arg.SignArgs> signArgsList = multiKeystoreParser.parse(new DefaultParser().parse(CLIParser.setupOptions(), CLIParserTest.asArgArray(cmd)));

        assertEquals(3, signArgsList.size());
        assertEquals(new Arg.SignArgs(4, "test2-release.jks", "ksAlias2", "ksPass2", "ksKeyPass2"), signArgsList.get(0));
        assertEquals(new Arg.SignArgs(97, "test-release.jks", "ksAlias1", "ksPass1", null), signArgsList.get(1));
        assertEquals(new Arg.SignArgs(7899, "test3-release.jks", "ksAlias3", "ksPass3", null), signArgsList.get(2));
    }
}
