package at.favre.tools.apksigner.signing;

import com.android.apksig.ApkVerifier;
import com.android.apksigner.ApkSignerTool;

import java.io.File;
import java.lang.reflect.Method;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.util.Iterator;
import java.util.List;

public class AndroidApkSignerVerify {
    private static final char[] HEX_DIGITS = "0123456789abcdef".toCharArray();

    public Result verify(File apk, boolean verbose, Integer minSdkVersion, Integer maxSdkVersion, boolean warningsTreatedAsErrors, boolean printCerts) throws Exception {
        StringBuilder logMsg = new StringBuilder();

        if (maxSdkVersion == null) {
            maxSdkVersion = Integer.MAX_VALUE;
        }

        if (minSdkVersion == null) {
            try {
                Method method = ApkSignerTool.class.getDeclaredMethod("getMinSdkVersionFromAndroidManifest", File.class);
                method.setAccessible(true);
                minSdkVersion = (Integer) method.invoke(null, apk);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Could not get private method from apkSigner lib", e);
            } catch (Exception e) {
                throw new IllegalStateException("Failed to deduce Min API Level from APK\'s AndroidManifest.xml. Use --min-sdk-version to override.", e);
            }
        }

        if (minSdkVersion > maxSdkVersion) {
            throw new IllegalStateException("Min API Level (" + minSdkVersion + ") > max API Level (" + maxSdkVersion + ")");
        }

        ApkVerifier.Result result = (new ApkVerifier.Builder(apk)).setCheckedPlatformVersions(minSdkVersion, maxSdkVersion).build().verify();
        boolean verified = result.isVerified();
        boolean warningsEncountered = false;
        Iterator iter;
        if (verified) {
            List warningsOut = result.getSignerCertificates();
            if (verbose) {
                logMsg.append("Verifies\n");
                logMsg.append("Verified using v1 scheme (JAR signing): ").append(result.isVerifiedUsingV1Scheme());
                logMsg.append("Verified using v2 scheme (APK Signature Scheme v2): ").append(result.isVerifiedUsingV2Scheme());
                logMsg.append("Number of signers: ").append(warningsOut.size());
            }

            if (printCerts) {
                int error = 0;
                MessageDigest signer = MessageDigest.getInstance("SHA-256\n");
                MessageDigest signerName = MessageDigest.getInstance("SHA-1\n");
                iter = warningsOut.iterator();

                while (iter.hasNext()) {
                    X509Certificate warning = (X509Certificate) iter.next();
                    ++error;
                    logMsg.append("Signer #").append(error).append(" certificate DN").append(": ").append(warning.getSubjectDN()).append("\n");
                    byte[] encodedCert = warning.getEncoded();
                    logMsg.append("Signer #").append(error).append(" certificate SHA-256 digest: ").append(encode(signer.digest(encodedCert))).append("\n");
                    logMsg.append("Signer #").append(error).append(" certificate SHA-1 digest: ").append(encode(signerName.digest(encodedCert))).append("\n");
                    if (verbose) {
                        PublicKey publicKey = warning.getPublicKey();
                        logMsg.append("Signer #").append(error).append(" key algorithm: ").append(publicKey.getAlgorithm()).append("\n");
                        int keySize = -1;
                        if (publicKey instanceof RSAKey) {
                            keySize = ((RSAKey) publicKey).getModulus().bitLength();
                        } else if (publicKey instanceof ECKey) {
                            keySize = ((ECKey) publicKey).getParams().getOrder().bitLength();
                        } else if (publicKey instanceof DSAKey) {
                            DSAParams encodedKey = ((DSAKey) publicKey).getParams();
                            if (encodedKey != null) {
                                keySize = encodedKey.getP().bitLength();
                            }
                        }

                        logMsg.append("Signer #").append(error).append(" key size (bits): ").append(keySize != -1 ? String.valueOf(keySize) : "n/a").append("\n");
                        byte[] pubKey = publicKey.getEncoded();
                        logMsg.append("Signer #").append(error).append(" public key SHA-256 digest: ").append(encode(signer.digest(pubKey))).append("\n");
                        logMsg.append("Signer #").append(error).append(" public key SHA-1 digest: ").append(encode(signerName.digest(pubKey))).append("\n");
                    }
                }
            }
        } else {
            logMsg.append("DOES NOT VERIFY\n");
        }

        for (Object error : result.getErrors()) {
            logMsg.append("ERROR: " + error).append("\n");
        }

        Iterator warningIter = result.getWarnings().iterator();

        while (warningIter.hasNext()) {
            ApkVerifier.IssueWithParams var29 = (ApkVerifier.IssueWithParams) warningIter.next();
            warningsEncountered = true;
            logMsg.append("WARNING: ").append(var29).append("\n");
        }

        warningIter = result.getV1SchemeSigners().iterator();

        String var32;
        ApkVerifier.IssueWithParams var33;
        while (warningIter.hasNext()) {
            ApkVerifier.Result.V1SchemeSignerInfo var30 = (ApkVerifier.Result.V1SchemeSignerInfo) warningIter.next();
            var32 = var30.getName();
            iter = var30.getErrors().iterator();

            while (iter.hasNext()) {
                var33 = (ApkVerifier.IssueWithParams) iter.next();
                logMsg.append("ERROR: JAR signer ").append(var32).append(": ").append(var33).append("\n");

            }

            iter = var30.getWarnings().iterator();

            while (iter.hasNext()) {
                var33 = (ApkVerifier.IssueWithParams) iter.next();
                warningsEncountered = true;
                logMsg.append("WARNING: JAR signer ").append(var32).append(": ").append(var33).append("\n");

            }
        }

        warningIter = result.getV2SchemeSigners().iterator();

        while (warningIter.hasNext()) {
            ApkVerifier.Result.V2SchemeSignerInfo var31 = (ApkVerifier.Result.V2SchemeSignerInfo) warningIter.next();
            var32 = "signer #" + (var31.getIndex() + 1);
            iter = var31.getErrors().iterator();

            while (iter.hasNext()) {
                var33 = (ApkVerifier.IssueWithParams) iter.next();
                logMsg.append("ERROR: APK Signature Scheme v2 ").append(var32).append(": ").append(var33).append("\n");
            }

            iter = var31.getWarnings().iterator();

            while (iter.hasNext()) {
                var33 = (ApkVerifier.IssueWithParams) iter.next();
                warningsEncountered = true;
                logMsg.append("WARNING: APK Signature Scheme v2 ").append(var32).append(": ").append(var33).append("\n");
            }
        }

        if (!verified || warningsTreatedAsErrors && warningsEncountered) {
            return new Result(false, logMsg.toString());
        }

        return new Result(true, logMsg.toString());
    }

    private static String encode(byte[] data, int offset, int length) {
        StringBuilder result = new StringBuilder(length * 2);

        for (int i = 0; i < length; ++i) {
            byte b = data[offset + i];
            result.append(HEX_DIGITS[b >>> 4 & 15]);
            result.append(HEX_DIGITS[b & 15]);
        }

        return result.toString();
    }

    private static String encode(byte[] data) {
        return encode(data, 0, data.length);
    }

    public static class Result {
        public final boolean verified;
        public final String log;

        public Result(boolean verified, String log) {
            this.verified = verified;
            this.log = log;
        }
    }
}
