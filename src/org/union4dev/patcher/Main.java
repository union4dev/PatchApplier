package org.union4dev.patcher;

import com.cloudbees.diff.PatchException;
import org.union4dev.patcher.patch.ApplyPatches;
import org.union4dev.patcher.patch.MakePatches;

import java.io.File;
import java.io.IOException;

public class Main {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -jar patcher.jar [mode] [origial_src] [patch_src]");
            return;
        }

        if (args[0].equals("-apply")) {
            try {
                new ApplyPatches(new File(args[1]), new File(args[2])).run();
            } catch (PatchException | IOException e) {
                e.printStackTrace();
            }
        } else if (args[0].equals("-make")) {
            try {
                new MakePatches(new File(args[1]), new File(args[2]), new File(args[3]), !args[4].startsWith("-dry=") || Boolean.parseBoolean(args[4].replace("-dry=", ""))).run();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
