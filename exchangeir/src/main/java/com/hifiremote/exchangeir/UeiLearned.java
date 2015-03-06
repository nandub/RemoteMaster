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
 * This class is a translation of the MakeUEILearned function in Graham Dixon's ("mathdon") C++ library ExchangeIR.
 * It constructs a UEI learned signal from timing data.
 *
 * @see Analyzer
 * @see RepeatFinder
 * @see UeiLearnedImporter
 */
public class UeiLearned {

    /** Maximal number of different burst pairs. */
    public final static int maxBursts = 16;
    
    /** Maximal length of UEI signal. */
    public final static int maxLength = 128;

    private final static double frequencyConstant = 8000000D;
    
    private final static int maxUnit = 0x7FFF;
    
    private ArrayList<Integer> learned = new ArrayList<Integer>();

    /**
     * This constructor builds a UEI learned signal from the arguments, which contains timing data.
     *
     * @param signal The signal to be converted to UEI Learned form, as output from the Analyze function.
             The format is one integers per burst, an index into the bursts array
                     (see below).  Number of bursts in array is sum of next three input values.
     * @param sngl_count Number of bursts in signal_out sent once.
     * @param rpt_count Number of bursts in signal_out sent repeatedly.
     * @param extra_count Number of bursts in signal_out sent once following repeat section.
     * @param bursts A list of bursts, two integers per burst, the MARK and SPACE times as positive
							integers.  The values in signal_out index this as an array of bursts, not integers.
     * @param freq Modulation frequency in Hz.
     */
     
    public UeiLearned(int[] signal, int sngl_count, int rpt_count, int extra_count,
            int[][] bursts, int freq) {
        setup(signal, sngl_count, rpt_count, extra_count, bursts, freq);
    }

    /**
     * This version of the constructor takes only the time arguments,
     * and invokes RepeatFinder and Analyzer to determine where start-, repeat-, and end-sequences reside.
     * @param times Timing list of alternating MARK and SPACE times in microseconds.
     * @param freq Modulation frequency in Hz.
     * @param errlimit Error limit in microseconds.
     *
     * @see Analyzer
     * @see RepeatFinder
     */
    public UeiLearned(int[] times, int freq, int errlimit) {
        RepeatFinder repeatFinder = new RepeatFinder(times, errlimit);
        Analyzer analyzer = new Analyzer(times, repeatFinder.getNoIntroBursts(), repeatFinder.getNoRepeatBursts(),
                repeatFinder.getNoEndingBursts(), repeatFinder.getNoRepeats(), freq, errlimit, 0);
        setup(analyzer.getSignalAsBurstIndices(), repeatFinder.getNoIntroBursts(), repeatFinder.getNoRepeatBursts(),
                repeatFinder.getNoEndingBursts(), analyzer.getBursts(), freq);
    }
    
    /**
     * This version of the constructor invokes Analyze to determine bursts etc.
     * 
     * @param times Timing list of alternating MARK and SPACE times in microseconds.
     * @param sngl_count Number of bursts in signal_out sent once.
     * @param rpt_count Number of bursts in signal_out sent repeatedly.
     * @param extra_count Number of bursts in signal_out sent once following repeat section.
     * @param rpts Number of actual repetitions contained in the times array.
     * @param freq Modulation frequency in Hz.
     * @param errlimit Error limit in microseconds.
     * 
     * @see Analyzer
     */
    public UeiLearned(int[] times, int sngl_count, int rpt_count, int extra_count, int rpts, int freq, int errlimit) {
        Analyzer analyzer = new Analyzer(times, sngl_count, rpt_count, extra_count, rpts, freq, errlimit, 0);
        setup(analyzer.getSignalAsBurstIndices(), sngl_count, rpt_count, extra_count, analyzer.getBursts(), freq);
    }

    private UeiLearned() {
    }

    private void setup(int[] signal_out, int sngl_count, int rpt_count, int extra_count,
            int[][] bursts, int freq) {
        if (bursts.length > maxBursts)
            throw new IllegalArgumentException("There are " + bursts.length + " bursts, maximum permitted is " + maxBursts);

        int length = 3 + 4 * bursts.length;
        if (sngl_count > 0)
            length += (sngl_count + 1) / 2 + 1;
        if (rpt_count > 0)
            length += (rpt_count + 1) / 2 + 1;
        if (extra_count > 0)
            length += (extra_count + 1) / 2 + 1;

        if (length + 3 > maxLength)  // Max length including 3-byte header is 0x80
            throw new IllegalArgumentException("Total length is " + length + 3 + " bytes, maximum permitted is " + maxLength);

        learned.add(0);
        learned.add(0);
        learned.add(length);

        int unit = (freq > 0) ? (int) Math.round(frequencyConstant / (double) freq) : 0;
        if (unit > maxUnit)
            throw new IllegalArgumentException("Nonzero frequency is less than 250Hz");

        learned.add(unit >> 8);
        learned.add(unit & 0xFF);
        learned.add(bursts.length);

        for (int i = 0; i < bursts.length; i++) {
            addBurst(bursts[i][0]);
            addBurst(bursts[i][1]);
        }

        if (sngl_count > 0) {
            learned.add(sngl_count);
            for (int i = 0; i < sngl_count;) {
                int x = signal_out[i++] << 4;
                if (i < sngl_count)
                    x |= signal_out[i++];

                learned.add(x);
            }
        }

        if (rpt_count > 0) {
            learned.add(rpt_count | 0x80);
            for (int i = sngl_count; i < sngl_count + rpt_count;) {
                int x = signal_out[i++] << 4;
                if (i < sngl_count + rpt_count)
                    x |= signal_out[i++];

                learned.add(x);
            }
        }

        if (extra_count > 0) {
            learned.add(extra_count);
            for (int i = sngl_count + rpt_count; i < sngl_count + rpt_count + extra_count;) {
                int x = signal_out[i++] << 4;
                if (i < sngl_count + rpt_count + extra_count)
                    x |= signal_out[i++];

                learned.add(x);
            }
        }
    }

    private void addBurst(int duration) {
        int t = Math.min(duration / 2, 0xFFFF);
        learned.add(t >> 8);
        learned.add(t & 0xFF);
    }

    /**
     * Formats the signal in common format, a sequence of twodigit hexadecimal numbers.
     *
     * @return Signal as string.
     */
    @Override
    public String toString() {
        StringBuilder str = new StringBuilder();
        for (int x : learned) {
            if (str.length() > 0)
                str.append(" ");
            str.append(String.format("%02X", x));
        }
        return str.toString();
    }

    /**
     * Returns the signal as an array of shorts.
     *
     * @return array of shorts.
     */
    public short[] toArray() {
        short[] result = new short[learned.size()];
        for (int i = 0; i < learned.size(); i++)
            result[i] = learned.get(i).shortValue();

        return result;
    }
}
