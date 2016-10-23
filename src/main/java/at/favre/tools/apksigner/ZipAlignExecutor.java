package at.favre.tools.apksigner;

import at.favre.tools.apksigner.ui.Arg;

import java.io.File;

public class ZipAlignExecutor {

    private boolean needsCleanup = false;
    public File zipAlignExecutable;

    public ZipAlignExecutor(Arg arg) {
        findLocation(arg);
    }

    private void findLocation(Arg arg) {
        if (arg.zipAlignPath != null && new File(arg.zipAlignPath).exists()) {
            zipAlignExecutable = new File(arg.zipAlignPath);
        } else {
            //TODO use embedded
            needsCleanup = true;
        }
    }

    public boolean isExecutableFound() {
        return zipAlignExecutable != null && zipAlignExecutable.exists() && zipAlignExecutable.isFile();
    }

    public void cleanUp() {
        if (needsCleanup && isExecutableFound()) {
            zipAlignExecutable.delete();
        }
    }
}
