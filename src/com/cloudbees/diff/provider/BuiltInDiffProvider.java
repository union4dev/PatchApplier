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

import java.io.IOException;
import java.io.Reader;

/**
 *
 * @author  Martin Entlicher
 */
public class BuiltInDiffProvider extends DiffProvider implements java.io.Serializable {

    /**
     * Holds value of property trimLines.
     */
    private boolean trimLines = true;

    static final long serialVersionUID = 1L;

    /** Creates a new instance of BuiltInDiffProvider */
    public BuiltInDiffProvider() {
    }
    
    /**
     * Create the differences of the content two streams.
     * @param r1 the first source
     * @param r2 the second source to be compared with the first one.
     * @return the list of differences found, instances of {@link Difference};
     *        or <code>null</code> when some error occured.
     */
    public Diff computeDiff(Reader r1, Reader r2) throws IOException {
        return Diff.diff(r1, r2, trimLines);
    }
    
    /** On true all lines are trimmed before passing to diff engine. */
    public boolean isTrimLines() {
        return this.trimLines;
    }

    /**
     * Setter for property trimLines.
     * @param trimLines New value of property trimLines.
     */
    public void setTrimLines(boolean trimLines) {
        this.trimLines = trimLines;
    }


    
}
