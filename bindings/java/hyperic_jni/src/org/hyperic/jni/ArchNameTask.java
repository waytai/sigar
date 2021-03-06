/*
 * Copyright (C) [2004, 2005, 2006], Hyperic, Inc.
 * This file is part of SIGAR.
 * 
 * SIGAR is free software; you can redistribute it and/or modify
 * it under the terms version 2 of the GNU General Public License as
 * published by the Free Software Foundation. This program is distributed
 * in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A
 * PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307
 * USA.
 */

package org.hyperic.jni;

import java.io.File;
import java.io.FileFilter;
import java.util.Arrays;
import java.util.Comparator;

import org.apache.tools.ant.Task;
import org.apache.tools.ant.BuildException;

public class ArchNameTask extends Task {

    public void execute() throws BuildException {
        String osArch = System.getProperty("os.arch");

        if (getProject().getProperty("jni.dmalloc") != null) {
            ArchName.useDmalloc = true;
        }

        String archName;

        try {
            archName = ArchName.getName();
        } catch (ArchNotSupportedException e) {
            //ok: can still compile *.java
            System.out.println(e.getMessage());
            return;
        }

        System.out.println(archName);
        getProject().setProperty("jni.libarch", archName);
        getProject().setProperty("jni.libpre",
                                 ArchLoader.getLibraryPrefix());
        getProject().setProperty("jni.libext",
                                 ArchLoader.getLibraryExtension());

        String compiler;
        if (ArchLoader.IS_WIN32) {
            compiler = "msvc";
        }
        else if (ArchLoader.IS_HPUX) {
            compiler = "hp";
        }
        else if (ArchLoader.IS_AIX) {
            compiler = "xlc_r";
        }
        else {
            compiler = "gcc";
            getProject().setProperty("jni.compiler.isgcc", "true");
        }

        getProject().setProperty("jni.compiler", compiler);

        if (ArchName.is64()) {
            getProject().setProperty("jni.arch64", "true");
            if (ArchLoader.IS_LINUX) {
                if (!osArch.equals("ia64")) {
                    getProject().setProperty("jni.gccm", "-m64");
                }
            }
        }
        else {
            if (ArchLoader.IS_LINUX && osArch.equals("s390")) {
                //gcc defaults to m64 on s390x platforms
                getProject().setProperty("jni.gccm", "-m31");
            }
        }

        if (ArchLoader.IS_DARWIN) {
            //default to most recent SDK
            //MacOSX10.3.9.sdk, MacOSX10.4u.sdk, MacOSX10.5.sdk,etc.
            File[] sdks =
                new File("/Developer/SDKs").listFiles(new FileFilter() {
                    public boolean accept(File file) {
                        String name = file.getName();
                        return
                            name.startsWith("MacOSX10.") &&
                            name.endsWith(".sdk");
                    }
                });
            if (sdks != null) {
                Arrays.sort(sdks, new Comparator() {
                    public int compare(Object s1, Object s2) {
                        return (int)(((File)s2).lastModified() -
                                     ((File)s1).lastModified());
                    }
                });
                final String prop = "uni.sdk";
                String sdk = getProject().getProperty(prop);
                if (sdk == null) {
                    sdk = sdks[0].getPath();
                    getProject().setProperty(prop, sdk);
                }
                System.out.println("Using SDK=" + sdk);
            }
        }
    }
}
