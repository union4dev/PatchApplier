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
 * Software is Sun Microsystems, Inc. Portions Copyright 1997-2007 Sun
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
package com.cloudbees.diff;

import java.io.ByteArrayOutputStream;
import java.util.*;

/**
 * Base64 utility methods.
 *
 * @author Maros Sandor
 */
class Base64 {
    
    private Base64() {
    }
    
    public static byte [] decode(List<String> ls) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        for (String s : ls) {
            decode(s, bos);
        }
        return bos.toByteArray();
    }
  
    private static void decode(String s, ByteArrayOutputStream bos) {
        int i = 0;
        int len = s.length();
        while (true) {
            while (i < len && s.charAt(i) <= ' ') i++;
            if (i == len) break;
            int tri = (decode(s.charAt(i)) << 18)
            + (decode(s.charAt(i+1)) << 12)
            + (decode(s.charAt(i+2)) << 6)
            + (decode(s.charAt(i+3)));
          
            bos.write((tri >> 16) & 255);
            if (s.charAt(i+2) == '=') break;
            bos.write((tri >> 8) & 255);
            if (s.charAt(i+3) == '=') break;
            bos.write(tri & 255);
          
            i += 4;
        }
    }

    private static int decode(char c) {
        if (c >= 'A' && c <= 'Z') return ((int) c) - 65;
        else if (c >= 'a' && c <= 'z') return ((int) c) - 97 + 26;
        else if (c >= '0' && c <= '9') return ((int) c) - 48 + 26 + 26;
        else {
            switch (c) {
                case '+': return 62;
                case '/': return 63;
                case '=': return 0;
                default:
                    throw new RuntimeException("unexpected code: " + c);
            }
        }
    }
    
}
