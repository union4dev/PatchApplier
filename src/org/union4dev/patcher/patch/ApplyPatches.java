package org.union4dev.patcher.patch;

import com.cloudbees.diff.ContextualPatch;
import com.cloudbees.diff.PatchException;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ApplyPatches {

    private final File target;
    private final File patches;

    public ApplyPatches(File target, File patches) {
        this.target = target;
        this.patches = patches;
    }

    public void run() throws PatchException, IOException {
        if (!patches.exists()) {
            if (!patches.mkdirs()) {
                System.out.println("Failed to make directory.");
                return;
            }
        }

        boolean failed = false;
        final List<File> patchFiles = new ArrayList<>();
        getPatchesFiles(patchFiles, patches);
        for (File file : patchFiles) {
            if (file.getName().endsWith(".patch")) {
                final ContextualPatch patch = ContextualPatch.create(file, target);
                for (ContextualPatch.PatchReport patchReport : patch.patch(false)) {
                    if (patchReport.getStatus() == ContextualPatch.PatchStatus.Patched) {
                        if (!patchReport.getOriginalBackupFile().delete()) //lets delete the backup because spam
                            System.out.println("Failed to delete: " + patchReport.getOriginalBackupFile().getAbsolutePath());
                    } else {
                        failed = true;
                        System.out.println("Failed to apply: " + file);
                        if (patchReport.getFailure() instanceof PatchException) {
                            System.out.println("    " + patchReport.getFailure().getMessage());
                        } else {
                            patchReport.getFailure().printStackTrace();
                        }
                    }
                }
            }
        }

        // Patches should always use /dev/null rather than any platform-specific locations, it should
        // be standardized across systems.
        // To that effect, we should clean up our messes - so delete any directories we make on Windows.
        final File NUL = new File("/dev/null");
        if (System.getProperty("os.name").toLowerCase().contains("win") && NUL.exists()) {
            if (!NUL.delete()) {
                System.out.println("Failed to delete: " + NUL.getAbsolutePath());
            }
        }

        if (failed) {
            throw new RuntimeException("One or more patches failed to apply, see log for details");
        }
    }

    private void getPatchesFiles(List<File> patchFiles, File directory) {
        final File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    patchFiles.add(file);
                } else if (file.isDirectory()) {
                    getPatchesFiles(patchFiles, file);
                }
            }
        }
    }
}
