package at.favre.tools.apksigner.ui;

import at.favre.tools.apksigner.signing.AndroidApkSignerVerify;
import at.favre.tools.apksigner.signing.CertHashChecker;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static junit.framework.TestCase.*;

public class CertHashCheckerTest {

    public static final String sha256 = "3b9e8ae8fadc373d4fff5da150c2e94cc0ad642e7886ffeb9d0fc9327bc66388";
    public static final String sha256_2 = "29728d7bffedbc3a8e3e3a9cbd1959cc724ae7c178cacf01547f0831fe64c3f1";
    public static final String sha256_3 = "99728d7bffedbc3a8e3e3a9cbd1959cc724ae7c178cacf01547f0831fe64c3f1";
    public static final String sha256_3_upper = "99728D7bffedbc3A8e3e3a9Cbd1959cc724ae7c178cacf01547f0831fe64c3f1";

    @Test
    public void testSingleHash() {
        assertTrue(new CertHashChecker().check(getVerifyResult(sha256), new String[]{sha256}).verified);
    }

    @Test
    public void testTwoHash() {
        assertTrue(new CertHashChecker().check(getVerifyResult(sha256, sha256_2), new String[]{sha256_2, sha256}).verified);
    }

    @Test
    public void testCountDoesNotMathc() {
        assertFalse(new CertHashChecker().check(getVerifyResult(sha256, sha256_2), new String[]{sha256_2, sha256, sha256_3}).verified);
    }

    @Test
    public void testHashNotMatch() {
        assertFalse(new CertHashChecker().check(getVerifyResult(sha256, sha256_3), new String[]{sha256_2, sha256}).verified);
    }

    @Test
    public void testHashNotMatchSingle() {
        assertFalse(new CertHashChecker().check(getVerifyResult(sha256_3), new String[]{sha256_2}).verified);
    }

    @Test
    public void testSingleHashIgnoreCase() {
        assertTrue(new CertHashChecker().check(getVerifyResult(sha256_3), new String[]{sha256_3_upper}).verified);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullVerify_ShouldThrowException() {
        new CertHashChecker().check(null, new String[]{sha256});
    }

    @Test
    public void testNullProvided() {
        assertNull(new CertHashChecker().check(getVerifyResult(sha256_3), null));
    }

    @Test
    public void testNotVerified() {
        assertNull(new CertHashChecker().check(new AndroidApkSignerVerify.Result(false, null, null, null, true, true, true, true, true, null), null));
    }

    private static AndroidApkSignerVerify.Result getVerifyResult(String... shas256) {
        List<AndroidApkSignerVerify.CertInfo> certInfos = new ArrayList<>();
        for (String s : shas256) {
            AndroidApkSignerVerify.CertInfo info = new AndroidApkSignerVerify.CertInfo();
            info.certSha256 = s;
            certInfos.add(info);
        }

        return new AndroidApkSignerVerify.Result(true, null, null, null, true, true, true, true, true, certInfos);
    }
}
