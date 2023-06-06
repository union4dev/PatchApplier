/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright 1997-2007 Sun Microsystems, Inc. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common
 * Development and Distribution License("CDDL") (collectively, the
 * "License"). You may not use this file except in compliance with the
 * License. You can obtain a copy of the License at
 * http://www.netbeans.org/cddl-gplv2.html
 * or nbbuild/licenses/CDDL-GPL-2-CP. See the License for the
 * specific language governing permissions and limitations under the
 * License.  When distributing the software, include this License Header
 * Notice in each file and include the License file at
 * nbbuild/licenses/CDDL-GPL-2-CP.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the GPL Version 2 section of the License file that
 * accompanied this code. If applicable, add the following below the
 * License Header, with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * Contributor(s):
 *
 * The Original Software is NetBeans. The Initial Developer of the Original
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2006 Sun
 * Microsystems, Inc. All Rights Reserved.
 *
 * If you wish your version of this file to be governed by only the CDDL
 * or only the GPL Version 2, indicate your decision by adding
 * "[Contributor] elects to include this software in this distribution
 * under the [CDDL or GPL Version 2] license." If you do not indicate a
 * single choice of license, a recipient has the option to distribute
 * your version of this file under either the CDDL, the GPL Version 2 or
 * to extend the choice of license to its licensees as provided above.
 * However, if you add GPL Version 2 code and therefore, elected the GPL
 * Version 2 license, then the option applies only if the new code is
 * made subject to such option by the copyright holder.
 */

package com.cloudbees.diff.provider;

import com.cloudbees.diff.Diff;
import com.cloudbees.diff.Difference;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The parser of an external diff utility compatible with Unix diff output.
 *
 * <p>The implementtaion is interruptible by Thread.interrupt().
 * On interrupt it kills external program and throws InterruptedIOException,
 *
 * @author  Martin Entlicher
 */
public class CmdlineDiffProvider extends DiffProvider implements java.io.Serializable {

    //private static final String REVISION_STR = "retrieving revision";
    public static final String DIFF_REGEXP = "(^[0-9]+(,[0-9]+|)[d][0-9]+$)|"+
                                              "(^[0-9]+(,[0-9]+|)[c][0-9]+(,[0-9]+|)$)|"+
                                              "(^[0-9]+[a][0-9]+(,[0-9]+|)$)";
    private static final int BUFF_LENGTH = 1024;

    private String diffCmd;
    private static final Pattern pattern = Pattern.compile(DIFF_REGEXP);
    //private transient StringBuffer firstText;
    //private transient StringBuffer secondText;
    
    static final long serialVersionUID =4101521743158176210L;
    /** Creates new CmdlineDiffProvider
     * @param diffCmd The diff command. Must contain "{0}" and "{1}", which
     * will be replaced with the files being compared.
     */
    public CmdlineDiffProvider(String diffCmd) {
        this.diffCmd = diffCmd;
        //firstText = new StringBuffer();
        //secondText = new StringBuffer();
    }
    
    public static CmdlineDiffProvider createDefault() {
        return new CmdlineDiffProvider("diff {0} {1}"); // NOI18N
    }

    /**
     * Set a new diff command.
     * @param diffCmd The diff command. Must contain "{0}" and "{1}", which
     * will be replaced with the files being compared.
     */
    public void setDiffCommand(String diffCmd) {
        this.diffCmd = diffCmd;
    }
    
    /**
     * Get the diff command being used.
     */
    public String getDiffCommand() {
        return diffCmd;
    }
    
    private static boolean checkEmpty(String str, String element) {
        if (str == null || str.length() == 0) {
            /*
            if (this.stderrListener != null) {
                String[] elements = { "Bad format of diff result: "+element }; // NOI18N
                stderrListener.match(elements);
            }
            */
            //Edeb("Bad format of diff result: "+element); // NOI18N
            return true;
        }
        return false;
    }

    /**
     * Create the differences of the content two streams.
     * @param r1 the first source
     * @param r2 the second source to be compared with the first one.
     * @return the list of differences found, instances of {@link Difference};
     *        or <code>null</code> when some error occured.
     */
    public Diff computeDiff(Reader r1, Reader r2) throws IOException {
        File f1 = null;
        File f2 = null;
        try {
            f1 = File.createTempFile("TempDiff".intern(), null);
            f2 = File.createTempFile("TempDiff".intern(), null);
            FileWriter fw1 = new FileWriter(f1);
            FileWriter fw2 = new FileWriter(f2);
            char[] buffer = new char[BUFF_LENGTH];
            int length;
            while((length = r1.read(buffer)) > 0) fw1.write(buffer, 0, length);
            while((length = r2.read(buffer)) > 0) fw2.write(buffer, 0, length);
            r1.close();
            r2.close();
            fw1.close();
            fw2.close();
            return createDiff(f1, f2);
        } finally {
            if (f1 != null) f1.delete();
            if (f2 != null) f2.delete();
        }
    }
    
    /**
     * Executes (possibly broken) external program.
     */
    private Diff createDiff(File f1, File f2) throws IOException {
        final StringBuffer firstText = new StringBuffer();
        final StringBuffer secondText = new StringBuffer();
        diffCmd = diffCmd.replace("\"{0}\"", "{0}").replace("\"{1}\"", "{1}");  // compatibility // NOI18N
        String firstPath;
        String secondPath;
        if (isWindows()) {
            firstPath = "\"" + f1.getAbsolutePath() + "\""; // NOI18N
            secondPath = "\"" + f2.getAbsolutePath() + "\""; // NOI18N
        } else {
            firstPath = f1.getAbsolutePath();
            secondPath = f2.getAbsolutePath();
        }
        final String cmd = java.text.MessageFormat.format(diffCmd, firstPath, secondPath);

        final Process p[] = new Process[1];
        final Object[] ret = new Object[1];
        Runnable cancellableProcessWrapper = new Runnable() {
            public void run() {
                try {
                    LOGGER.fine("#69616 CDP: executing: " + cmd); // NOI18N
                    synchronized(p) {
                        p[0] = Runtime.getRuntime().exec(cmd);
                    }
                    Reader stdout = new InputStreamReader(p[0].getInputStream());
                    char[] buffer = new char[BUFF_LENGTH];
                    StringBuilder outBuffer = new StringBuilder();
                    int length;
                    Diff differences = new Diff();
                    while ((length = stdout.read(buffer)) > 0) {
                        for (int i = 0; i < length; i++) {
                            if (buffer[i] == '\n') {
                                //stdoutNextLine(outBuffer.toString(), differences);
                                outputLine(outBuffer.toString(), pattern, differences,
                                           firstText, secondText);
                                outBuffer.delete(0, outBuffer.length());
                            } else {
                                if (buffer[i] != 13) {
                                    outBuffer.append(buffer[i]);
                                }
                            }
                        }
                    }
                    if (outBuffer.length() > 0) outputLine(outBuffer.toString(), pattern, differences,
                                                           firstText, secondText);
                    setTextOnLastDifference(differences, firstText, secondText);
                    ret[0] =  differences;
                } catch (IOException ioex) {
                    ret[0] = new IOException("Failed to execute: "+cmd).initCause(ioex);
                }
            }
        };

        Thread t = new Thread(cancellableProcessWrapper, "Diff.exec()"); // NOI18N
        t.start();
        try {
            t.join();
            synchronized(ret) {
                if (ret[0] instanceof IOException) {
                    throw (IOException) ret[0];
                }
                return (Diff) ret[0];
            }
        } catch (InterruptedException e) {
            synchronized(p[0]) {
                p[0].destroy();
            }
            throw new InterruptedIOException();
        }

    }

    private boolean isWindows() {
        return File.pathSeparatorChar==';';
    }

    public static void setTextOnLastDifference(List<Difference> differences,
        StringBuffer firstText, StringBuffer secondText) {
        if (differences.size() > 0) {
            String t1 = firstText.toString();
            if (t1.length() == 0) t1 = null;
            String t2 = secondText.toString();
            if (t2.length() == 0) t2 = null;
            Difference d = differences.remove(differences.size() - 1);
            differences.add(new Difference(d.getType(), d.getFirstStart(), d.getFirstEnd(),
            d.getSecondStart(), d.getSecondEnd(), t1, t2));
            firstText.delete(0, firstText.length());
            secondText.delete(0, secondText.length());
        }
    }
    
    /**
     * This method is called, with elements of the output data.
     * @param elements the elements of output data.
     */
    //private void outputData(String[] elements, List differences) {
    public static void outputLine(String elements, Pattern pattern, List<Difference> differences,
                                   StringBuffer firstText, StringBuffer secondText) {
        //diffBuffer.append(elements[0]+"\n"); // NOI18N
        //D.deb("diff match: "+elements[0]); // NOI18N
        //System.out.println("diff outputData: "+elements[0]); // NOI18N

        int index, commaIndex;
        int n1, n2, n3, n4;
        String nStr;
        if (pattern.matcher(elements).matches()) {
            setTextOnLastDifference(differences, firstText, secondText);
        } else {
            if (elements.startsWith("< ")) {
                firstText.append(elements.substring(2) + "\n");
            }
            if (elements.startsWith("> ")) {
                secondText.append(elements.substring(2) + "\n");
            }
            return ;
        }
        if ((index = elements.indexOf('a')) >= 0) {
            //DiffAction action = new DiffAction();
            try {
                n1 = Integer.parseInt(elements.substring(0, index));
                index++;
                commaIndex = elements.indexOf(',', index);
                if (commaIndex < 0) {
                    nStr = elements.substring(index, elements.length());
                    if (checkEmpty(nStr, elements)) return;
                    n3 = Integer.parseInt(nStr);
                    n4 = n3;
                } else {
                    nStr = elements.substring(index, commaIndex);
                    if (checkEmpty(nStr, elements)) return;
                    n3 = Integer.parseInt(nStr);
                    nStr = elements.substring(commaIndex+1, elements.length());
                    if (nStr == null || nStr.length() == 0) n4 = n3;
                    else n4 = Integer.parseInt(nStr);
                }
            } catch (NumberFormatException e) {
                /*
                if (this.stderrListener != null) {
                    String[] debugOut = { "NumberFormatException "+e.getMessage() }; // NOI18N
                    stderrListener.match(debugOut);
                }
                */
                //Edeb("NumberFormatException "+e.getMessage()); // NOI18N
                return;
            }
            //action.setAddAction(n1, n3, n4);
            //diffActions.add(action);
            differences.add(new Difference(Difference.ADD, n1, 0, n3, n4));
        } else if ((index = elements.indexOf('d')) >= 0) {
            //DiffAction action = new DiffAction();
            commaIndex = elements.lastIndexOf(',', index);
            try {
                if (commaIndex < 0) {
                    n1 = Integer.parseInt(elements.substring(0, index));
                    n2 = n1;
                } else {
                    nStr = elements.substring(0, commaIndex);
                    if (checkEmpty(nStr, elements)) return;
                    n1 = Integer.parseInt(nStr);
                    nStr = elements.substring(commaIndex+1, index);
                    if (checkEmpty(nStr, elements)) return;
                    n2 = Integer.parseInt(nStr);
                }
                nStr = elements.substring(index+1, elements.length());
                if (checkEmpty(nStr, elements)) return;
                n3 = Integer.parseInt(nStr);
            } catch (NumberFormatException e) {
                /*
                if (this.stderrListener != null) {
                    String[] debugOut = { "NumberFormatException "+e.getMessage() }; // NOI18N
                    stderrListener.match(debugOut);
                }
                */
                //Edeb("NumberFormatException "+e.getMessage()); // NOI18N
                return;
            }
            //action.setDeleteAction(n1, n2, n3);
            //diffActions.add(action);
            differences.add(new Difference(Difference.DELETE, n1, n2, n3, 0));
        } else if ((index = elements.indexOf('c')) >= 0) {
            //DiffAction action = new DiffAction();
            commaIndex = elements.lastIndexOf(',', index);
            try {
                if (commaIndex < 0) {
                    n1 = Integer.parseInt(elements.substring(0, index));
                    n2 = n1;
                } else {
                    nStr = elements.substring(0, commaIndex);
                    if (checkEmpty(nStr, elements)) return;
                    n1 = Integer.parseInt(nStr);
                    nStr = elements.substring(commaIndex+1, index);
                    if (checkEmpty(nStr, elements)) return;
                    n2 = Integer.parseInt(nStr);
                }
                index++;
                commaIndex = elements.indexOf(',', index);
                if (commaIndex < 0) {
                    nStr = elements.substring(index, elements.length());
                    if (checkEmpty(nStr, elements)) return;
                    n3 = Integer.parseInt(nStr);
                    n4 = n3;
                } else {
                    nStr = elements.substring(index, commaIndex);
                    if (checkEmpty(nStr, elements)) return;
                    n3 = Integer.parseInt(nStr);
                    nStr = elements.substring(commaIndex+1, elements.length());
                    if (nStr == null || nStr.length() == 0) n4 = n3;
                    else n4 = Integer.parseInt(nStr);
                }
            } catch (NumberFormatException e) {
                /*
                if (this.stderrListener != null) {
                    String[] debugOut = { "NumberFormatException "+e.getMessage() }; // NOI18N
                    stderrListener.match(debugOut);
                }
                */
                //Edeb("NumberFormatException "+e.getMessage()); // NOI18N
                return;
            }
            //action.setChangeAction(n1, n2, n3, n4);
            //diffActions.add(action);
            differences.add(new Difference(Difference.CHANGE, n1, n2, n3, n4));
        }
    }

    private static final Logger LOGGER = Logger.getLogger(CmdlineDiffProvider.class.getName());
}
