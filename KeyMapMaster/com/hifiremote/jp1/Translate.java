package com.hifiremote.jp1;

public abstract class Translate
{
  public Translate( String[] textParms ){}
  public abstract void in( Value[] parms, byte[] hex, DeviceParameter[] devParms, int onlyIndex );
  public abstract void out( byte[] hex, Value[] parms, DeviceParameter[] devParms );

  public static byte reverse( byte b )
  {
    return (byte)reverse( b, 8 );
  }

  public static int reverse( int v, int bits )
  {
    int rc;
    rc = (((  v >> 1 ) & 0x55555555 ) | ((  v & 0x55555555 ) << 1 ));
    rc = ((( rc >> 2 ) & 0x33333333 ) | (( rc & 0x33333333 ) << 2 ));
    rc = ((( rc >> 4 ) & 0x0F0F0F0F ) | (( rc & 0x0F0F0F0F ) << 4 ));
    rc = ((( rc >> 8 ) & 0x00FF00FF ) | (( rc & 0x00FF00FF ) << 8 ));
    rc = (( rc >>> 16 ) | ( rc <<16 ));
    return rc >>> ( 32 - bits );
  }

  public static int byte2int( byte b )
  {
    return b & 0xFF;
  }

  // insert a field of up to 32 bits crossing up to 9 bytes
  public static void insert( byte[] hex, int msbOffset, int bits, int v)
  {
    int lastOffset = msbOffset + bits - 1;
	int by = lastOffset / 8;                // byte position of lowest bit
    if (by >= hex.length)
    {
	  System.err.println("insert(offset=" + msbOffset + ", bits=" + bits +") exceeds " + hex.length + " byte buffer");
  	  return;
    }
	int bi = 7 - (lastOffset % 8);          // lsb position of lowest bit
	int mask = (2<<(bits-1))-1;             // Works for bits = 1 to 32
    while ( mask != 0 )
	{
	  int mask2 = mask << bi;
	  hex[by] = (byte)( ( hex[by] &~ mask2 ) | ( ( v << bi ) & mask2 ) );
	  mask = mask >>> (8-bi);
	  v = v >> (8-bi);
	  bi = 0;
	  --by;
	}
  }

  // extract a field of up to 32 bits crossing up to 9 bytes
  public static int extract( byte[] hex, int msbOffset, int bits )
  {
    if (msbOffset+bits > 8 * hex.length)
    {
	  System.err.println("extract(offset=" + msbOffset + ", bits=" + bits +") exceeds " + hex.length + " byte buffer");
  	  return 0;
    }
    int v=0;
	int by = msbOffset / 8;
	int bi = msbOffset % 8;
	int mask = ( 0x100 >> bi ) - 1;
	bits += bi;
	while (bits > 8)
	{
	  v = ( v << 8 ) + ( hex[by] & mask );
	  mask = 0xFF;
	  by++;
	  bits -= 8;
	}
    return ( v << bits ) + ( ( hex[by] & mask ) >> ( 8 - bits ) );
  }

}
