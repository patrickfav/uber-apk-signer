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
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Mirrors the logic of the apksigner.jar from google, but provides more structural log output.
 */
public class AndroidApkSignerVerify {

    public Result verify(File apk, Integer minSdkVersion, Integer maxSdkVersion, boolean warningsTreatedAsErrors) throws Exception {
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

        ApkVerifier.Result apkVerifierResult = builder.build().verify();
        boolean verified = apkVerifierResult.isVerified();
        Iterator iter;
        if (verified) {
            List signerCertificates = apkVerifierResult.getSignerCertificates();
            logMsg.append("Verifies\n");
            logMsg.append("Verified using v1 scheme (JAR signing): ").append(apkVerifierResult.isVerifiedUsingV1Scheme());
            logMsg.append("Verified using v2 scheme (APK Signature Scheme v2): ").append(apkVerifierResult.isVerifiedUsingV2Scheme());
            logMsg.append("Verified using v3 scheme (APK Signature Scheme v3): ").append(apkVerifierResult.isVerifiedUsingV3Scheme());
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

        Iterator resultIter = apkVerifierResult.getWarnings().iterator();

        while (resultIter.hasNext()) {
            ApkVerifier.IssueWithParams var29 = (ApkVerifier.IssueWithParams) resultIter.next();
            warnings.add("WARNING: " + var29);
        }

        resultIter = apkVerifierResult.getV1SchemeSigners().iterator();

        String name;
        ApkVerifier.IssueWithParams issueWithParams;
        while (resultIter.hasNext()) {
            ApkVerifier.Result.V1SchemeSignerInfo signerInfo = (ApkVerifier.Result.V1SchemeSignerInfo) resultIter.next();
            name = signerInfo.getName();
            iter = signerInfo.getErrors().iterator();

            while (iter.hasNext()) {
                issueWithParams = (ApkVerifier.IssueWithParams) iter.next();
                errors.add("ERROR: JAR signer " + name + ": " + issueWithParams);
            }

            iter = signerInfo.getWarnings().iterator();

            while (iter.hasNext()) {
                issueWithParams = (ApkVerifier.IssueWithParams) iter.next();
                warnings.add("WARNING: JAR signer " + issueWithParams);
            }
        }

        resultIter = apkVerifierResult.getV2SchemeSigners().iterator();

        while (resultIter.hasNext()) {
            ApkVerifier.Result.V2SchemeSignerInfo signerInfo = (ApkVerifier.Result.V2SchemeSignerInfo) resultIter.next();
            name = "signer #" + (signerInfo.getIndex() + 1);
            iter = signerInfo.getErrors().iterator();

            while (iter.hasNext()) {
                issueWithParams = (ApkVerifier.IssueWithParams) iter.next();
                errors.add("ERROR: APK Signature Scheme v2 " + name + ": " + issueWithParams);
            }

            iter = signerInfo.getWarnings().iterator();

            while (iter.hasNext()) {
                issueWithParams = (ApkVerifier.IssueWithParams) iter.next();
                warnings.add("WARNING: APK Signature Scheme v2  " + name + ": " + issueWithParams);
            }
        }

        resultIter = apkVerifierResult.getV3SchemeSigners().iterator();
        while (resultIter.hasNext()) {
            ApkVerifier.Result.V3SchemeSignerInfo signerInfo = (ApkVerifier.Result.V3SchemeSignerInfo) resultIter.next();
            name = "signer #" + (signerInfo.getIndex() + 1);
            iter = signerInfo.getErrors().iterator();

            while (iter.hasNext()) {
                issueWithParams = (ApkVerifier.IssueWithParams) iter.next();
                errors.add("ERROR: APK Signature Scheme v3 " + name + ": " + issueWithParams);
            }

            iter = signerInfo.getWarnings().iterator();

            while (iter.hasNext()) {
                issueWithParams = (ApkVerifier.IssueWithParams) iter.next();
                warnings.add("WARNING: APK Signature Scheme v3  " + name + ": " + issueWithParams);
            }
        }

        if (!verified || warningsTreatedAsErrors && !warnings.isEmpty()) {
            return new Result(false, warnings, errors, logMsg.toString(), apkVerifierResult.isVerifiedUsingV1Scheme(), apkVerifierResult.isVerifiedUsingV2Scheme(), apkVerifierResult.isVerifiedUsingV3Scheme(), certInfoList);
        }

        return new Result(true, warnings, errors, logMsg.toString(), apkVerifierResult.isVerifiedUsingV1Scheme(), apkVerifierResult.isVerifiedUsingV2Scheme(), apkVerifierResult.isVerifiedUsingV3Scheme(), certInfoList);
    }

    public static class Result {
        public final boolean verified;
        public final List<String> warnings;
        public final List<String> errors;
        public final String log;
        public final boolean v1Schema;
        public final boolean v2Schema;
        public final boolean v3Schema;
        public final List<CertInfo> certInfoList;

        public Result(boolean verified, List<String> warnings, List<String> errors, String log, boolean v1Schema, boolean v2Schema, boolean v3Schema, List<CertInfo> certInfoList) {
            this.verified = verified;
            this.warnings = warnings;
            this.errors = errors;
            this.log = log;
            this.v1Schema = v1Schema;
            this.v2Schema = v2Schema;
            this.v3Schema = v3Schema;
            this.certInfoList = certInfoList;
        }

        public String getSchemaVersionInfoString() {
            return "[" + (v1Schema ? "v1" : "")
                    + (v1Schema && v2Schema ? ", " : "") + (v2Schema ? "v2" : "")
                    + (v2Schema && v3Schema ? ", " : "") + (v3Schema ? "v3" : "") + "] ";
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
