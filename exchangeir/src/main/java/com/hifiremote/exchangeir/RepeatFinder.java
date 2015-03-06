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

/**
 * This class is a Java translation of the <code>FindRepeat</code> function of Graham Dixon's ("mathdon") C++ library ExchangeIR.
 *
 * This function/class takes a signal as a timing list of alternating MARK and SPACE times in microseconds
 * and analyzes it for a repeat section, returning the burst counts of the single, repeat and extra
 * sections and the number of copies of the repeat section in the signal.  The length (in bursts)
 * of the timing list is:
 * <pre>
 *		sngl_count + rpts*rpt_count + extra_count
 * </pre>
 *
 * The <code>errlimit</code> parameter is used in determining whether two times should be considered as nominally
 * equal.  Two times are nominally equal if they are either within 2.5% of one another or if their
 * difference in microseconds does not exceed <code>errlimit</code>.  The errlimit criterion represents a
 * variation inherent in the signal capture mechanism, the percentage criterion represents natural
 * variations in the signal.
 *
 * @see Analyzer
*/
public class RepeatFinder extends Exchange {
    private int sngl_count;
    private int rpt_count;
    private int extra_count;
    private int rpts;
    
    private final static int N = 6;
    
    public int getNoIntroBursts() {
        return sngl_count;
    }

    public int getNoRepeatBursts() {
        return rpt_count;
    }

    public int getNoEndingBursts() {
        return extra_count;
    }

    public int getNoRepeats() {
        return rpts;
    }

    //public RepeatFinder(int[] times) {
    //    this(times, defaultErrLimit);
    //}

    @Override
    public String toString() {
        return "{into = " + sngl_count
                + "; repeatLength = " + rpt_count
                + "; repeats = " + rpts
                + "; extra_count = " + extra_count
                + "}";
    }

    /**
     * Constructor
     * @param times Integer timing list of alternating MARK and SPACE times in microseconds
     * @param errlimit Two times are nominally equal if they are either within 2.5% of one another or if their
 * difference in microseconds does not exceed <code>errlimit</code>.
     */
    public RepeatFinder(int[] times, int errlimit) {
        // Note that off-times are negative

        super(errlimit);

        int i, j = -1, rpt_start = -1;
        sngl_count = times.length/2;
	rpts = 0;
        rpt_count = 0;
        extra_count = 0;
        int iEnd = sngl_count + rpts*rpt_count + extra_count;
	int maxOff = -1, maxOn = -1, maxBurst = -1, minBurst = 99999, minOn = 99999, maxOff2 = -1;
	int ndxOff = 0, ndxOn = 0, offset = 0;
	int maxOffPos[] = new int[N];
        int maxOnPos[] = new int[N];

	// We need to allow timing errors that are due to the way IRScope works, as well as
	// those in the original signal.
	//this.errlimit = errlimit;
	// Find extreme burst lengths excluding possible lead-outs.
	// Test first burst against second as in some protocols it is a lead-in identical to lead-out burst.
        for (i = 0; i < iEnd - 1; i++) {
            if (i > 0 || times[0] + Math.abs(times[1]) < Math.abs(times[3])) {
                maxBurst = Math.max(maxBurst, times[2 * i] + Math.abs(times[2 * i + 1]));
                minBurst = Math.min(minBurst, times[2 * i] + Math.abs(times[2 * i + 1]));

                if (i<15)
                    outputDebugString("i=%d minBurst=%d maxBurst=%d abs(times[2*i+3]=%d",
					i, minBurst, maxBurst, Math.abs(times[2*i+3]));


                if (Math.abs(times[2 * i + 3]) > 2 * maxBurst - minBurst
                        && (i == iEnd - 2 || Math.abs(times[2 * i + 3]) > 1.2 * (times[2 * i + 4] + Math.abs(times[2 * i + 5]))))
                    i++; // possible lead-out so skip
            }
        }

	// Find extreme off and on times that are large enough to divide frames.
        for (i = 1; i < iEnd - 1; i++) {
            if (Math.abs(times[2 * i + 1]) > 2 * maxBurst - minBurst)
                maxOff = Math.max(maxOff, Math.abs(times[2 * i + 1]));
        }
        for (i = 1; i < iEnd - 1; i++) {
            if (Math.abs(times[2 * i + 1]) > 2 * maxBurst - minBurst && Math.abs(times[2 * i + 1]) <= 0.9 * maxOff)
                maxOff2 = Math.max(maxOff2, Math.abs(times[2 * i + 1]));
        }
        for (i = 1; i < iEnd - 1; i++) {
            maxOn = Math.max(maxOn, times[2 * i]);
            minOn = Math.min(minOn, times[2 * i]);
        }

        outputDebugString("maxOff=%d maxOff2=%d maxOn=%d minOn=%d maxBurst=%d\n",
                            maxOff, maxOff2, maxOn, minOn, maxBurst);

	for (i = 0; i < N; i++) {
            maxOnPos[i] = -999;
            maxOffPos[i] = -999;
        }
	maxOffPos[ndxOff++] = -1;
	for (i=1; i < iEnd-1; i++) {
            // Allow 10% leeway in locating extreme values, skip an extreme in first burst.
            if (maxOn > 0 && times[2*i] > 0.9*maxOn && ndxOn < N)
                maxOnPos[ndxOn++] = 2*i;
            if (maxOff > 0 && Math.abs(times[2*i+1]) > 0.9*maxOff && ndxOff < N)
                maxOffPos[ndxOff++] = 2*i+1;
	}

	boolean match = false;
	boolean withLeadout = false;
	for (i = 0; !match && i < N - 1 && maxOffPos[i] > -3; i++) {
            for (j = i + 1; !match && j < N && maxOffPos[j] > -3; j++) {
                for (offset = 0;
                        !match && offset < (i == 0 ? 3 : 1)
                        && 2 * maxOffPos[j] - maxOffPos[i] - offset <= 2 * iEnd - 1;
                        offset += 2) // The 'offset' loop is to catch protocols with a lead-in on only first frame
                {
                    // Don't test the last off-time if matching value is final lead-out.
                    // (Now changed to not testing any lead-out as already tested in locating
                    // extreme values and lead-outs can have greater error than data durations.)

                    outputDebugString("Match test at i=%d j=%d offset=%d maxOffPos[i]=%d maxOffPos[j]=%d",
					i,j,offset,maxOffPos[i],maxOffPos[j]);

	                   match = equalTimes(times, maxOffPos[i] + offset + 1, times, maxOffPos[j] + 1,
                            maxOffPos[j] - maxOffPos[i] - offset
                            // -(2*maxOffPos[j]-maxOffPos[i]-offset == 2*iEnd-1));
                            - 1);
                }
            }
        }

        outputDebugString("max on 1st pos %d", maxOnPos[1]);
        if (!match && maxOnPos[1] >= 0)
            outputDebugString("max on times %d %d", times[maxOnPos[0]], Math.abs(times[maxOnPos[0] + 1]));

        if (match) {
            // i, j, offset were incremented after match found, offset by 2
            rpt_start = (maxOffPos[i - 1] + offset - 1) / 2;
            rpt_count = (maxOffPos[j - 1] - maxOffPos[i - 1] - offset + 2) / 2;
            withLeadout = true;
            // Make exception if rpt_count<4 (lead-in + lead_out with possibly one data bit, as NEC1x)
            // and rpt_start<8 and maxOff not particularly big, as all protocols that use such ditto
            // repeats have substantial single sections. It is probably an artificial repeat seen
            // within the data of a non-repeating signal.
            if (rpt_start < 8 && rpt_count < 3 && maxOff < 10 * minBurst) {
                match = false;
                rpt_start = sngl_count;
                rpt_count = 0;
            }
        }
	else if (maxOnPos[0]>=0 // && times[maxOnPos[0]]+Math.abs(times[maxOnPos[0]+1]) > 5000
		&& maxOn > 5*minOn
		&& times[maxOnPos[0]] > 0.9*Math.abs(times[maxOnPos[0]+1])) // (maxOn > 10000)
	       {
            // Frame division by MARK rather than SPACE, unusual but happens in RCA, X10, Lutron, Somfy.
            // May be in a lead-in or a lead-out.  First treat it as a lead-in, which it is except in X10.
            for (i = 0; !match && i < N - 1 && maxOnPos[i] >= 0; i++) {
                for (j = i + 1; !match && j < N && maxOnPos[j] >= 0; j++) {
                    match = equalTimes(times, maxOnPos[i], times, maxOnPos[j],
                            maxOnPos[j] - maxOnPos[i]);
                }
            }
            if (match) {
                // i, j were incremented after match found
                rpt_start = (maxOnPos[i - 1]) / 2;
                rpt_count = (maxOnPos[j - 1] - maxOnPos[i - 1]) / 2;
                outputDebugString("ON break: rpt_start=%d rpt_count=%d", rpt_start, rpt_count);
            }
	}

	// Check if the repeats could have started one repeat earlier (arises with ditto frames)
	// Again, don't check the lead-out as already tested with greater error bounds.
	if ( match && rpt_start >= rpt_count
				&& equalTimes(times, 2*(rpt_start-rpt_count), times, 2*rpt_start, 2*rpt_count-(withLeadout?1:0)) )
	{
            rpt_start -= rpt_count;
	}
	// Check if the repeat could have started at a maxOff2 frame division, such as in Denon/Sharp.
        if (match && maxOff2 > 0) {
            for (i = Math.max(0, rpt_count - rpt_start - 1); i < rpt_count - 1; i++) {
                int offtime = Math.abs(times[2 * (rpt_start + i) + 1]);
                if (offtime > 0.9 * maxOff2 && offtime < 1.1 * maxOff2
                        && equalTimes(times, 2 * (rpt_start - rpt_count + i + 1),
                        times, 2 * (rpt_start + i + 1), 2 * (rpt_count - i - 1) - (withLeadout ? 1 : 0)))
                    break;
            }
            if (i < rpt_count - 1)
                rpt_start -= rpt_count - i - 1;
        }

	if (match) {
            // Count the number of repeats
            if (rpt_count > 0) // Just to catch errors
            {
                for (i = 0; rpt_start + (i + 2) * rpt_count <= iEnd
                        && equalTimes(times, 2 * (rpt_start + i * rpt_count),
                        times, 2 * (rpt_start + (i + 1) * rpt_count),
                        // Don't check the final lead-out
                        // 2*rpt_count-(rpt_start+(i+2)*rpt_count == iEnd)); i++);
                        // Changed to:  Don't check lead-outs
                        2 * rpt_count - ((withLeadout || rpt_start + (i + 2) * rpt_count == iEnd) ? 1 : 0)); i++);
                rpts = i + 1;
                extra_count = iEnd - rpt_start - rpts * rpt_count;
                sngl_count = rpt_start;

                outputDebugString("Burst counts: single = %d, repeat = %d, extra = %d.\nNo. of repeats = %d\n",
				  sngl_count, rpt_count, extra_count, rpts);
            }

            // If repeats delimited by MARK rather than SPACE, see if it is really a lead-out by
            // testing whether the repeat section ends in a data burst, which suggests that the
            // repeat pattern is off by one position.  Change the counts to reflect this unless it
            // would create a singleton single section instead of a singleton extra section.
            if (!withLeadout && rpts > 0) {
                //int signal_out[] = new int[2 * (sngl_count + rpt_count + extra_count + 1)];
                //IrSignal irSignal = new IrSignal(times, );
                //int rpt_offset = Analyze(times, sngl_count, rpt_count, extra_count, rpts, signal_out, NULL, 0, errlimit, -2, NULL);
                Analyzer analyzer = new Analyzer(times, sngl_count, rpt_count, extra_count, rpts, /*
                         * signal_out, null, 0, errlimit,
                         */ -2, errlimit, 10);
                int rpt_offset = analyzer.getNumberDistinctBursts();
                int[] signal_out = analyzer.getCleansedSignal();
                if (rpt_offset > 0 && (extra_count < 2 || signal_out[2 * (sngl_count + rpt_count)] == signal_out[2 * sngl_count])) {
                    if (extra_count < 2) {
                        signal_out[2 * (sngl_count + rpt_count)] = signal_out[2 * sngl_count];
                    }
                    sngl_count += rpt_offset;
                    extra_count = Math.max(extra_count - rpt_offset, 0);
                    if (sngl_count >= rpt_count
                            && equalTimes(times, 2 * (sngl_count - rpt_count), times, 2 * sngl_count, 2 * rpt_count)) {
                        sngl_count -= rpt_count;
                    }
                }
            }
        }
    }

    /**
     * Static function mimicking the API of the original library. Returns the output values in the Integer parameters.
     * @param times
     * @param sngl_count
     * @param rpt_count
     * @param extra_count
     * @param rpts
     * @param errlimit
     */
    // FindBugs considers this as very silly code. So do I... ;-)
    //public static void findRepeat(int[] times, Integer sngl_count, Integer rpt_count,
    //        Integer extra_count, Integer rpts, int errlimit) {
    //    RepeatFinder repeatFinder = new RepeatFinder(times, errlimit);
    //    sngl_count = repeatFinder.sngl_count;
    //    rpt_count = repeatFinder.rpt_count;
    //    extra_count = repeatFinder.rpt_count;
    //    rpts = repeatFinder.rpts;
    //}
}
