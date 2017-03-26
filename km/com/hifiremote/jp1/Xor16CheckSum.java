package com.hifiremote.jp1;

public class Xor16CheckSum extends CheckSum
{

  public Xor16CheckSum( int addr, AddressRange range, boolean comp )
  {
    super( addr, range, comp );
  }

  public String toString()
  {
    return "*" + super.toString();
  }

  @Override
  public short calculateCheckSum( short[] data, int start, int end )
  {
    // For remotes with segments, UpdateImage() updates the checksum range end to
    // point to the last byte written.  The length certainly needs to be rounded up
    // to a multiple of 2, but remotes such as the URC6820Z Zapper+ appear to need
    // rounding to a multiple of 4.  See this post for a discussion:
    // http://www.hifi-remote.com/forums/viewtopic.php?p=127545#127545
    
    // Here 4 is set as the default, which can be over-ridden by an explicit setting
    // in the RDF by following the normal checksum syntax, without any space, by /2
    // to set to a multiple of 2.  Values /1 and /4 are also recognised but unlikely
    // to be needed.

    // getRoundTo() value 0 denotes default rounding, values > 0 are explicitly set.
    int rounding = addressRange.getRoundTo() - 1;
    end |= rounding < 0 ? 3 : rounding;
    short sum = 0;
    for ( int i = start; i <= end - 1; i += 2 )
    {
      sum ^= ( ( data[ i ] << 8 ) |  data[ i + 1 ] ) & 0xFFFF;
    }
    return sum;
  }
  
  @Override
  public void setCheckSum( short[] data )
  {
    short sum = calculateCheckSum( data, addressRange.getStart(), addressRange.getEnd() );
    if ( complement )
    {
      sum = ( short )( ~sum & 0xFFFF );
    }
    data[ checkSumAddress ] = ( short )( ( sum >> 8 ) & 0xFF );
    data[ checkSumAddress + 1 ] = ( short )( ~sum & 0xFF );
  }
}

