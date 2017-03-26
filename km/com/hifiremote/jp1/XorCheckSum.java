package com.hifiremote.jp1;

// TODO: Auto-generated Javadoc
/**
 * The Class XorCheckSum.
 */
public class XorCheckSum extends CheckSum
{

  /**
   * Instantiates a new xor check sum.
   * 
   * @param addr
   *          the addr
   * @param range
   *          the range
   */
  public XorCheckSum( int addr, AddressRange range, boolean comp )
  {
    super( addr, range, comp );
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.CheckSum#toString()
   */
  public String toString()
  {
    return "^" + super.toString();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.hifiremote.jp1.CheckSum#calculateCheckSum(short[], int, int)
   */
  public short calculateCheckSum( short[] data, int start, int end )
  {
    // getRoundTo() value 0 denotes default rounding, values > 0 are explicitly set.
    int rounding = addressRange.getRoundTo() - 1;
    end |= rounding < 0 ? 0 : rounding;
    short sum = 0;
    for ( int i = start; i <= end; i++ )
    {
      sum ^= data[ i ] & 0xFF;
    }
    return sum;
  }
}
