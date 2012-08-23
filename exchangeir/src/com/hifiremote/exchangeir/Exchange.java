/*
	Copyright (C) 2010 Graham Dixon, 2012 Bengt Martensson

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

package com.hifiremote.exchangeir;

import java.io.FileNotFoundException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;

/**
 * This class is an abstract class supporting the Java translation of a subset of Graham Dixon's ("mathdon") C++ library ExchangeIR.
 */
public abstract class Exchange {

    private static final String debugFilename = "debug.txt";

    /** String identifying the version of the library. */
    public final static String versionString = "0.0.8.2";

    // this is private, not protected. Derived classes should use outputDebugString.
    private PrintStream debugFile = null;

    /**
     * The <code>errlimit</code> parameter is used in determining whether
     * two times should be considered as nominally equal.  Two times are nominally equal if they are either
     * within 2.5% of one another or if their difference in microseconds does not exceed <code>errlimit</code>.
     * The <code>errlimit</code> criterion represents a variation inherent in the signal capture mechanism, the
     * percentage criterion represents natural variations in the signal.
     */
     protected int errlimit;
    
    private static boolean debug = false;

    /**
     * Turns debugging on or off, according to the argument.
     * @param dbg
     */
    public static void setDebug(boolean dbg) {
        debug = dbg;
    }

    /**
     * Returns version string.
     * @return Version number as string.
     */
    public static String getVersion() {
        return versionString;
    }

    /**
     *
     * @param s
     */
    protected void outputDebugString(String s) {
        if (debugFile != null)
            debugFile.println(s);
    }

    protected void outputDebugString(String format, Object... obj) {
        String str = String.format(format, obj);
        outputDebugString(str);
    }

    protected boolean equalTimes(int t1, int t2) {
        return !(Math.abs(t2) < Math.floor(0.975 * Math.abs(t1)) && Math.abs(t2) < Math.abs(t1) - errlimit
                || Math.abs(t2) > Math.ceil(1.025 * Math.abs(t1)) && Math.abs(t2) > Math.abs(t1) + errlimit);
    }

    protected boolean equalTimes(int[] t1, int offset1, int[] t2, int offset2, int duration_count) {
        for (int i = 0; i < duration_count; i++) {
            if (!equalTimes(t1[i + offset1], t2[i + offset2])) {
                //if (debug)
                //    debugFile.print("F");
                return false;
            }
        }
        //if (debug)
        //    debugFile.print("T");
        return true;
    }

    protected Exchange(int errlimit) {
        this.errlimit = errlimit;
        debugFile = null;
        if (debug)
            try {
                debugFile = new PrintStream(debugFilename, "US-ASCII");
            } catch (UnsupportedEncodingException ex) {
                // Never happens
                assert false;
            } catch (FileNotFoundException ex) {
                System.err.println(ex.getMessage());
            }
    }
}
