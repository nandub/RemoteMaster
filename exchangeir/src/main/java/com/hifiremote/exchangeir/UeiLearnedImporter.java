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

import java.util.ArrayList;

/**
 * This class is a translation of the ReadUEILearned function in Graham Dixon's
 * ("mathdon") C++ library ExchangeIR. It interprets a UEI learned signal,
 * producing timing data. It is sort-of the inverse function of UeiLearned.
 *
 * @see UeiLearned
 */
public class UeiLearnedImporter {

    private int iFreq;
    private int iSngl_count;
    private int iRpt_count;
    private int iExtra_count;
    private ArrayList<Integer> signal_out = new ArrayList<Integer>();
    private int[][] m_Bursts;
    private final static int romBursts[] = {
        0x01A3, 0x4A81, 0x068F, 0x0690, 0x01A3, 0x04F6, 0x01A3, 0x01A4, // 0
        0x00D2, 0xAF0C, 0x00D2, 0x4507, 0x0277, 0x00D3, 0x00D2, 0x0278, // 8
        0x0083, 0x589B, 0x0083, 0x039B, 0x0083, 0x0189, // 16
        0x0083, 0x5D6F, 0x0083, 0x5527, 0x0083, 0x039B, 0x0083, 0x0189, // 22
        0x0009, 0xFCC0, 0x0009, 0x008C, 0x0009, 0x005A, 0x0009, 0x0028, // 30
        0x270F, 0x07D0, 0x01F3, 0x0FA0, 0x07CF, 0x07D0, 0x00F9, 0x03E8, 0x00F9, 0x01F4, // 38
        0x0118, 0x60C9, 0x08C9, 0x08CA, 0x0118, 0x034D, 0x0118, 0x0119, // 48
        0x0118, 0x4F1F, 0x1193, 0x08CA, 0x0118, 0x034D, 0x0118, 0x0119, // 56
        0x0118, 0xBE1D, 0x0118, 0x5C64, 0x08C9, 0x08CA, 0x0118, 0x034D, 0x0118, 0x0119, // 64
        0x0118, 0xBCE6, 0x0118, 0x4F21, 0x1193, 0x08CA, 0x1193, 0x0465, 0x0118, 0x034D, 0x0118, 0x0119, // 74
        0x0118, 0xBCE6, 0x0118, 0x57EE, 0x1193, 0x08CA, 0x1193, 0x0465, 0x0118, 0x034D, 0x0118, 0x0119, // 86
        0x0118, 0xB895, 0x0118, 0x2E8C, 0x1193, 0x08CA, 0x0118, 0x034D, 0x0118, 0x0119, // 98
        0x010A, 0xEE3E, 0x010A, 0x283A, 0x010A, 0x0537, 0x010A, 0x0216, 0x010A, 0x010B, // 108
        0x010A, 0xEE3E, 0x010A, 0x283A, 0x010A, 0x0537, 0x0215, 0x0216, 0x010A, 0x0216, 0x010A, 0x010B, // 118
        0x010A, 0xEE3E, 0x010A, 0x283A, 0x00F2, 0x0537, 0x0215, 0x0216, 0x0215, 0x010B, 0x010A, 0x0216, 0x010A, 0x010B, // 130
        0x01BC, 0xB0FF, 0x0379, 0x01BD, 0x01BC, 0x01BD, // 144
        0x01BC, 0xB0FF, 0x0379, 0x01BD, 0x01BC, 0x037A, 0x01BC, 0x01BD, // 150
        0x01BC, 0xB0FF, 0x0379, 0x037A, 0x0379, 0x01BD, 0x01BC, 0x037A, 0x01BC, 0x01BD, // 158
        0x0009, 0xFFFF, // 168
        0x0009, 0x1E61, 0x0009, 0x184E, 0x0009, 0x1238, 0x0009, 0x0C22, 0x0009, 0x060C, // 170
        0x0009, 0x7B65, 0x0009, 0x10CC, 0x0009, 0x0B2C, // 180
        0x006B, 0xFFFF, // 186
        0x006B, 0x1DFF, 0x006B, 0x17EC, 0x006B, 0x11D6, 0x006B, 0x0BC0, 0x006B, 0x05AA, // 188
        0x0013, 0x7B5B, 0x0013, 0x10C2, 0x0013, 0x0B22 // 198
    };
    private final static int romIndex[] = {
        48, 56, 64, 74, 86, 8, 98, 16, 22, 30, 144, 150, 158, 108, 118, 130, 0,
        38, 168, 170, 180, 186, 188, 198
    };

    /**
     * Parses its argument which is assumed to contain hexadecimal data.
     *
     * @param data Hexadecimal data as string, separated by whitespace.
     */
    public UeiLearnedImporter(String data) {
        String[] d = data.trim().split("[^\\w]+");
        int[] array = new int[d.length];
        for (int i = 0; i < d.length; i++) {
            if (d[i].length() != 2)
                throw new IllegalArgumentException("All digits must be two hexadecimal characters.");
            array[i] = Integer.parseInt(d[i], 16);
        }

        setup(array);
    }

    /**
     * Imports a UEI learned signal.
     *
     * @param aiUEI array of numbers as present in the UEI learned form.
     */
    public UeiLearnedImporter(int[] aiUEI) {
        setup(aiUEI);
    }

    private void setup(int[] aiUEI) {
        int iUEIsize = aiUEI.length;
        if (iUEIsize < 6 || iUEIsize < aiUEI[2] + 3)
            throw new IllegalArgumentException("Invalid format for learned signal");

        int unit = ((aiUEI[3] & 0x7F) << 8) + aiUEI[4];
        iFreq = (unit == 0) ? 0 : 8000000 / unit;
        int noBursts = loadUEIBurstTable(aiUEI);
        iSngl_count = iRpt_count = iExtra_count = 0;
        int start = ((aiUEI[5] & 0x80) != 0) ? 6 : (6 + 4 * noBursts);	// start of section
        boolean bRepeat = false;
        do {
            int count = aiUEI[start] & 0x7F;			// number of nibbles in section
            int end = start + (count + 1) / 2 + 1;		// potential start of next section
            if (end > aiUEI[2] + 3)
                break;                                          // present section extends beyond data end
            if ((aiUEI[start] >> 7) != 0 && !bRepeat) {		// test for repeat section
                iRpt_count += count;				// first repeat section
                bRepeat = true;
            } else if (!bRepeat) {
                iSngl_count += count;				// not reached a repeat section
            } else {
                iExtra_count += count;				// beyond first repeat section
            }

            for (int i = 0; i < count; i++) {
                int ndx = aiUEI[start + i / 2 + 1];
                ndx = ((i & 1) != 0) ? (ndx & 0xF) : (ndx >> 4);
                if (ndx >= m_Bursts.length)
                    throw new IndexOutOfBoundsException();      // index out of range

                signal_out.add(m_Bursts[ndx][0]);
                signal_out.add(m_Bursts[ndx][1]);
            }

            start = end;
        } while (start < aiUEI[2] + 3);
    }

    private int loadUEIBurstTable(int[] ai) {
        int count;
        int burstNum = ai[5];
        if ((burstNum & 0x80) != 0) {
            int index = burstNum & 0x1F;
            if (index >= romIndex.length)
                throw new IllegalArgumentException();

            index = romIndex[index];
            count = romBursts.length - index;
            count = (count > 32) ? 16 : count / 2;
            m_Bursts = new int[count][2];
            for (int i = 0; i < count; i++) {
                m_Bursts[i][0] = 2 * romBursts[index + 2 * i];
                m_Bursts[i][1] = 2 * romBursts[index + 2 * i + 1];
            }

        } else {
            count = burstNum & 0x1F;
            if (count == 0 || count > 0xF || 4 * count + 3 > ai[2])
                throw new IllegalArgumentException("Invalid format for learned signal: bursts extend beyond data");
            m_Bursts = new int[count][2];
            for (int i = 0; i < count; i++) {
                m_Bursts[i][0] = 2 * ((ai[6 + 4 * i] << 8) + ai[7 + 4 * i]);
                m_Bursts[i][1] = 2 * ((ai[8 + 4 * i] << 8) + ai[9 + 4 * i]);
            }
        }
        return count;
    }

    public int getFrequency() {
        return iFreq;
    }

    public int getNoIntroBursts() {
        return iSngl_count;
    }

    public int getNoRepeatBursts() {
        return iRpt_count;
    }

    public int getNoEndingBursts() {
        return iExtra_count;
    }

    /**
     * Returns signal as array of durations.
     *
     * @return integer array of microsecond durations
     */
    public int[] getSignal() {
        int[] signal = new int[signal_out.size()];
        for (int i = 0; i < signal.length; i++) {
            signal[i] = signal_out.get(i).intValue();
        }

        return signal;
    }

    /**
     * Returns the computed burst pairs.
     *
     * @return Burst pairs
     */
    public int[][] getBursts() {
        return m_Bursts;
    }

    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        str.append("{ frequency = ");
        str.append(this.iFreq);
        str.append(", [");
        for (int i = 0; i < signal_out.size(); i++) {
            str.append(this.signal_out.get(i)).append(" ");
        }
        str.append("]}");
        return str.toString();
    }

    public static void main(String[] args) {
        int[] data = new int[args.length];
        for (int i = 0; i < args.length; i++) {
            data[i] = Integer.parseInt(args[i], 16);
        }

        System.out.println(new UeiLearnedImporter(data));
    }
}
