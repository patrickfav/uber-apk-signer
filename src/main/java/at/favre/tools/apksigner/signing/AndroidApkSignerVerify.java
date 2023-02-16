package at.favre.tools.apksigner.signing;

import at.favre.lib.bytes.Bytes;
import com.android.apksig.ApkVerifier;

import java.io.File;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAKey;
import java.security.interfaces.DSAParams;
import java.security.interfaces.ECKey;
import java.security.interfaces.RSAKey;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Mirrors the logic of the apksigner.jar from Google, but provides more structural log output.
 */
public class AndroidApkSignerVerify {

    public Result verify(File apk, Integer minSdkVersion, Integer maxSdkVersion, File v4SchemeSignatureFile, boolean warningsTreatedAsErrors) throws Exception {
        StringBuilder logMsg = new StringBuilder();
        List<CertInfo> certInfoList = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        ApkVerifier.Builder builder = new ApkVerifier.Builder(apk);
        if (minSdkVersion != null) {
            builder.setMinCheckedPlatformVersion(minSdkVersion);
        }
        if (maxSdkVersion != null) {
            builder.setMaxCheckedPlatformVersion(maxSdkVersion);
        }
        if (v4SchemeSignatureFile != null) {
            builder.setV4SignatureFile(v4SchemeSignatureFile);
        }

        ApkVerifier.Result apkVerifierResult = builder.build().verify();
        boolean verified = apkVerifierResult.isVerified();
        Iterator iter;
        if (verified) {
            List signerCertificates = apkVerifierResult.getSignerCertificates();
            logMsg.append("Verifies\n");
            logMsg.append("Verified using v1 scheme (JAR signing): ").append(apkVerifierResult.isVerifiedUsingV1Scheme());
            logMsg.append("Verified using v2 scheme (APK Signature Scheme v2): ").append(apkVerifierResult.isVerifiedUsingV2Scheme());
            logMsg.append("Verified using v3 scheme (APK Signature Scheme v3): ").append(apkVerifierResult.isVerifiedUsingV3Scheme());
            logMsg.append("Verified using v3.1 scheme (APK Signature Scheme v3.1): ").append(apkVerifierResult.isVerifiedUsingV31Scheme());
            logMsg.append("Verified using v4 scheme (APK Signature Scheme v4): ").append(apkVerifierResult.isVerifiedUsingV4Scheme());
            logMsg.append("Number of signers: ").append(signerCertificates.size());

            MessageDigest sha256Digest = MessageDigest.getInstance("SHA-256");
            MessageDigest sha1Digest = MessageDigest.getInstance("SHA-1");
            iter = signerCertificates.iterator();

            while (iter.hasNext()) {
                CertInfo certInfo = new CertInfo();

                X509Certificate x509Certificate = (X509Certificate) iter.next();
                byte[] encodedCert = x509Certificate.getEncoded();

                certInfo.subjectDn = "Subject: " + x509Certificate.getSubjectDN().toString();
                certInfo.issuerDn = "Issuer: " + x509Certificate.getIssuerDN().toString();
                certInfo.sigAlgo = x509Certificate.getSigAlgName();
                certInfo.certSha1 = Bytes.wrap(sha1Digest.digest(encodedCert)).encodeHex();
                certInfo.certSha256 = Bytes.wrap(sha256Digest.digest(encodedCert)).encodeHex();
                certInfo.expiry = x509Certificate.getNotAfter();
                certInfo.beginValidity = x509Certificate.getNotBefore();

                PublicKey publicKey = x509Certificate.getPublicKey();

                certInfo.pubAlgo = publicKey.getAlgorithm();
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

                certInfo.pubKeysize = keySize;
                byte[] pubKey = publicKey.getEncoded();
                certInfo.pubSha1 = Bytes.wrap(sha1Digest.digest(pubKey)).encodeHex();
                certInfo.pubSha256 = Bytes.wrap(sha256Digest.digest(pubKey)).encodeHex();
                certInfoList.add(certInfo);
            }
        } else {
            logMsg.append("DOES NOT VERIFY\n");
        }

        List<String> errors = apkVerifierResult.getErrors().stream().map(error -> "ERROR: " + error).collect(Collectors.toList());

        for (ApkVerifier.IssueWithParams issues : apkVerifierResult.getWarnings()) {
            warnings.add("WARNING: " + issues);
        }

        extractV1SchemeIssues(warnings, apkVerifierResult, errors);

        extractV2SchemeIssues(warnings, apkVerifierResult, errors);

        extractV3SchemeIssues(warnings, apkVerifierResult, errors);

        extractV31SchemeIssues(warnings, apkVerifierResult, errors);

        extractV4SchemeIssues(warnings, apkVerifierResult, errors);

        if (!verified || warningsTreatedAsErrors && !warnings.isEmpty()) {
            return new Result(
                    false,
                    warnings,
                    errors,
                    logMsg.toString(),
                    apkVerifierResult.isVerifiedUsingV1Scheme(),
                    apkVerifierResult.isVerifiedUsingV2Scheme(),
                    apkVerifierResult.isVerifiedUsingV3Scheme(),
                    apkVerifierResult.isVerifiedUsingV31Scheme(),
                    apkVerifierResult.isVerifiedUsingV4Scheme(),
                    certInfoList
            );
        }

        return new Result(
                true,
                warnings,
                errors,
                logMsg.toString(),
                apkVerifierResult.isVerifiedUsingV1Scheme(),
                apkVerifierResult.isVerifiedUsingV2Scheme(),
                apkVerifierResult.isVerifiedUsingV3Scheme(),
                apkVerifierResult.isVerifiedUsingV31Scheme(),
                apkVerifierResult.isVerifiedUsingV4Scheme(),
                certInfoList
        );
    }

    private static void extractV4SchemeIssues(List<String> warnings, ApkVerifier.Result apkVerifierResult, List<String> errors) {
        ApkVerifier.IssueWithParams issueWithParams;
        Iterator<ApkVerifier.IssueWithParams> iter;
        for (ApkVerifier.Result.V4SchemeSignerInfo signerInfo : apkVerifierResult.getV4SchemeSigners()) {
            String name = "signer #" + (signerInfo.getIndex() + 1);
            iter = signerInfo.getErrors().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                errors.add("ERROR: APK Signature Scheme v4 " + name + ": " + issueWithParams);
            }

            iter = signerInfo.getWarnings().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                warnings.add("WARNING: APK Signature Scheme v4  " + name + ": " + issueWithParams);
            }
        }
    }

    private static void extractV31SchemeIssues(List<String> warnings, ApkVerifier.Result apkVerifierResult, List<String> errors) {
        ApkVerifier.IssueWithParams issueWithParams;
        Iterator<ApkVerifier.IssueWithParams> iter;
        for (ApkVerifier.Result.V3SchemeSignerInfo signerInfo : apkVerifierResult.getV31SchemeSigners()) {
            String name = "signer #" + (signerInfo.getIndex() + 1);
            iter = signerInfo.getErrors().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                errors.add("ERROR: APK Signature Scheme v3.1 " + name + ": " + issueWithParams);
            }

            iter = signerInfo.getWarnings().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                warnings.add("WARNING: APK Signature Scheme v3.1  " + name + ": " + issueWithParams);
            }
        }
    }

    private static void extractV3SchemeIssues(List<String> warnings, ApkVerifier.Result apkVerifierResult, List<String> errors) {
        ApkVerifier.IssueWithParams issueWithParams;
        Iterator<ApkVerifier.IssueWithParams> iter;
        for (ApkVerifier.Result.V3SchemeSignerInfo signerInfo : apkVerifierResult.getV3SchemeSigners()) {
            String name = "signer #" + (signerInfo.getIndex() + 1);
            iter = signerInfo.getErrors().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                errors.add("ERROR: APK Signature Scheme v3 " + name + ": " + issueWithParams);
            }

            iter = signerInfo.getWarnings().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                warnings.add("WARNING: APK Signature Scheme v3  " + name + ": " + issueWithParams);
            }
        }
    }

    private static void extractV2SchemeIssues(List<String> warnings, ApkVerifier.Result apkVerifierResult, List<String> errors) {
        ApkVerifier.IssueWithParams issueWithParams;
        Iterator<ApkVerifier.IssueWithParams> iter;
        for (ApkVerifier.Result.V2SchemeSignerInfo signerInfo : apkVerifierResult.getV2SchemeSigners()) {
            String name = "signer #" + (signerInfo.getIndex() + 1);
            iter = signerInfo.getErrors().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                errors.add("ERROR: APK Signature Scheme v2 " + name + ": " + issueWithParams);
            }

            iter = signerInfo.getWarnings().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                warnings.add("WARNING: APK Signature Scheme v2  " + name + ": " + issueWithParams);
            }
        }
    }

    private static void extractV1SchemeIssues(List<String> warnings, ApkVerifier.Result apkVerifierResult, List<String> errors) {
        ApkVerifier.IssueWithParams issueWithParams;
        Iterator<ApkVerifier.IssueWithParams> iter;
        for (ApkVerifier.Result.V1SchemeSignerInfo signerInfo : apkVerifierResult.getV1SchemeSigners()) {
            String name = signerInfo.getName();
            iter = signerInfo.getErrors().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                errors.add("ERROR: JAR signer " + name + ": " + issueWithParams);
            }

            iter = signerInfo.getWarnings().iterator();

            while (iter.hasNext()) {
                issueWithParams = iter.next();
                warnings.add("WARNING: JAR signer " + issueWithParams);
            }
        }
    }

    public static class Result {
        public final boolean verified;
        public final List<String> warnings;
        public final List<String> errors;
        public final String log;
        public final boolean v1Schema;
        public final boolean v2Schema;
        public final boolean v3Schema;
        public final boolean v31Schema;
        public final boolean v4Schema;
        public final List<CertInfo> certInfoList;

        public Result(boolean verified, List<String> warnings, List<String> errors, String log, boolean v1Schema, boolean v2Schema, boolean v3Schema, boolean v31Schema, boolean v4Schema, List<CertInfo> certInfoList) {
            this.verified = verified;
            this.warnings = warnings;
            this.errors = errors;
            this.log = log;
            this.v1Schema = v1Schema;
            this.v2Schema = v2Schema;
            this.v3Schema = v3Schema;
            this.v31Schema = v31Schema;
            this.v4Schema = v4Schema;
            this.certInfoList = certInfoList;
        }

        public String getSchemaVersionInfoString() {
            StringJoiner stringJoiner = new StringJoiner(", ", "[", "]");

            if (v1Schema) {
                stringJoiner.add("v1");
            }
            if (v2Schema) {
                stringJoiner.add("v2");
            }
            if (v3Schema) {
                stringJoiner.add("v3");
            }
            if (v31Schema) {
                stringJoiner.add("v3.1");
            }
            if (v4Schema) {
                stringJoiner.add("v4");
            }

            return stringJoiner.toString();
        }

        public String getCertCountString() {
            if (certInfoList.size() > 1) {
                return "(" + certInfoList.size() + ") ";
            }
            return "";
        }
    }

    //CHECKSTYLE:OFF -- I do want a concise class with only public access
    public static class CertInfo {
        public String certSha1;
        public String certSha256;
        public String pubSha1;
        public String pubSha256;
        public String subjectDn;
        public String issuerDn;
        public String sigAlgo;
        public String pubAlgo;
        public int pubKeysize;
        public Date expiry;
        public Date beginValidity;
    }
    //CHECKSTYLE:ON
}
