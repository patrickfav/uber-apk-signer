package at.favre.tools.apksigner.util;

import at.favre.tools.apksigner.signing.SigningConfig;
import at.favre.tools.apksigner.ui.Arg;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class AndroidApkSignerUtil {
    private AndroidApkSignerUtil() {
    }

    public static String[] createApkToolArgs(Arg arguments, List<SigningConfig> list, File targetApkFile, File outFile) {
        List<String> args = new ArrayList<>();
        args.add("sign");

        if (arguments.verbose) {
            args.add("--verbose");
        }

        if (arguments.lineageFilePath != null) {
            args.add("--lineage");
            args.add(arguments.lineageFilePath);
        }

        for (int i = 0; i < list.size(); i++) {
            args.add("--ks");
            args.add(list.get(i).keystore.getAbsolutePath());
            args.add("--ks-key-alias");
            args.add(list.get(i).ksAlias);
            args.add("--ks-pass");
            args.add("pass:" + list.get(i).ksPass);
            args.add("--key-pass");
            args.add("pass:" + list.get(i).ksKeyPass);
            args.add("--out");
            args.add(outFile.getAbsolutePath());

            if (i + 1 < list.size()) {
                args.add("--next-signer");
            }
        }

        args.add("--v4-signing-enabled");

        args.add(targetApkFile.getAbsolutePath());

        return args.toArray(new String[0]);
    }
}
