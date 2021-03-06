/*
 * Copyright 2006 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

/* @test
   @bug 6176051
   @summary Check isFile's handling of Windows device names
 */

import java.io.File;

public class WinDeviceName {
    private static String devnames[] = {
        "CON", "PRN", "AUX", "NUL", "COM1", "COM2", "COM3", "COM4",
        "COM5", "COM6", "COM7", "COM8", "COM9", "LPT1", "LPT2",
        "LPT3", "LPT4", "LPT5", "LPT6", "LPT7", "LPT8", "LPT9",
        "CLOCK$"
    };
    public static void main(String[] args) throws Exception {
        String osName = System.getProperty("os.name");
        if (!osName.startsWith("Windows")) {
            return;
        }
        for (int i = 0; i < devnames.length; i++) {
            if (new File(devnames[i]).isFile() ||
                new File(devnames[i] + ".txt").isFile()) {
                if ("CLOCK$".equals(devnames[i]) &&
                    (osName.startsWith("Windows 9") ||
                     osName.startsWith("Windows Me"))) {
                    //"CLOCK$" is a reserved device name for NT
                    continue;
                }
                throw new Exception("isFile() returns true for Device name "
                                    +  devnames[i]);
            }
        }
    }
}
