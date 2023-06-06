package org.union4dev.patcher.patch;

import com.cloudbees.diff.Diff;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MakePatches {

    private final Map<String, File> originMap = new HashMap<>();
    private final Map<String, File> targetMap = new HashMap<>();

    private final File target;
    private final File origin;
    private final File patches;
    private final boolean ignoreWhitespace;

    private final String originalPrefix = "a/";
    private final String modifiedPrefix = "b/";

    public MakePatches(File origin, File target, File patches, boolean ignoreWhitespace) {
        this.origin = origin;
        this.target = target;
        this.patches = patches;
        this.ignoreWhitespace = ignoreWhitespace;
    }

    public void run() throws IOException {
        if (patches.exists()) {
            System.out.println("INFO >> Clean patches folder");
            if (!deleteDirectory(patches)) {
                System.err.println("ERROR >> Failed to reset patches folder");
                return;
            }
        }
        if (!patches.mkdirs()) {
            System.err.println("ERROR >> Failed to create patches folder");
            return;
        }

        process(target, origin);
    }

    private void process(File origin, File target) throws IOException {
        final List<File> origin_files = new ArrayList<>();
        getJavaFiles(origin_files, origin);
        for (File originFile : origin_files) {
            if (originFile.getName().endsWith(".java")) {
                final String path = originFile.getAbsolutePath().replace(origin.getAbsolutePath(), "").replace("\\", "/").substring(1);
                System.out.println("Load Origin >> " + path);
                originMap.put(path, originFile);
            }
        }

        System.out.println("INFO >> Original Files loaded");

        final List<File> target_files = new ArrayList<>();
        getJavaFiles(target_files, target);
        for (File targetFile : target_files) {
            if (targetFile.getName().endsWith(".java")) {
                final String path = targetFile.getAbsolutePath().replace(target.getAbsolutePath(), "").replace("\\", "/").substring(1);
                System.out.println("Load Target >> " + path);
                targetMap.put(path, targetFile);
            }
        }

        System.out.println("INFO >> Target Files loaded");
        makePatch(originMap, targetMap, false);
        makePatch(targetMap, originMap, true);
    }

    private void makePatch(Map<String, File> form, Map<String, File> to, boolean reversed) throws IOException {
        System.out.println(reversed ? "INFO >> Start recheck patches" : "INFO >> Start making patches");
        for (String originKey : form.keySet()) {
            final FileInputStream originStream = new FileInputStream(form.get(originKey));
            final String originData = new String(originStream.readAllBytes(), StandardCharsets.UTF_8);
            originStream.close();
            String targetData;
            if (to.containsKey(originKey)) {
                final FileInputStream targetStream = new FileInputStream(to.get(originKey));
                targetData = new String(targetStream.readAllBytes(), StandardCharsets.UTF_8);
                targetStream.close();
            } else {
                targetData = "";
                System.out.println("WARNING >> Cannot find " + originKey + " in target folder, empty file instead.");
            }
            final Diff diff = Diff.diff(new StringReader(originData), new StringReader(targetData), ignoreWhitespace);
            if (!diff.isEmpty()) {
                final File patchFile = new File(patches, "/" + originKey + ".patch");
                if (!patchFile.exists()) {
                    if (!patchFile.getParentFile().exists()) {
                        if (!patchFile.getParentFile().mkdirs()) {
                            System.err.println("ERROR >> Failed to create folder " + patchFile.getParentFile().getAbsolutePath());
                            continue;
                        }
                    }
                    if (!patchFile.createNewFile()) {
                        System.err.println("ERROR >> Failed to create file " + patchFile.getAbsolutePath());
                        continue;
                    }
                }

                final String unifiedDiff = diff.toUnifiedDiff(originalPrefix + originKey, modifiedPrefix + originKey, new StringReader(originData), new StringReader(targetData), 3).replace("\r?\n", System.lineSeparator());

                final BufferedWriter writer = new BufferedWriter(new FileWriter(patchFile));
                writer.write(unifiedDiff.replace("\\ No newline at end of file", ""));
                writer.close();
            }
        }

        System.out.println(reversed ? "OKAY >> Recheck patches done..." : "OKAY >> Making patches done...");
    }

    private boolean deleteDirectory(File directory){
        if(directory.isDirectory()){
            File[] files = directory.listFiles();
            if(files != null){
                for(File file : files){
                    if(file.isDirectory()){
                        deleteDirectory(file);
                    }else{
                        if (!file.delete())
                            return false;
                    }
                }
            }
        }
        return directory.delete();
    }

    private void getJavaFiles(List<File> patchFiles, File directory) {
        final File[] files = directory.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isFile()) {
                    patchFiles.add(file);
                } else if (file.isDirectory()) {
                    getJavaFiles(patchFiles, file);
                }
            }
        }
    }
}
