package com.hifiremote.jp1;

public class HexInteger
  extends Number
  implements Comparable< HexInteger >
{
  public HexInteger( int i )
  {
    value = new Integer( i );
  }

  public HexInteger( String text )
  {
    value = Integer.valueOf( text, 16 );
  }

  public String toString()
  {
    return Integer.toString( value.intValue(), 16 );
  }

  public byte   byteValue(){ return value.byteValue(); }
  public double doubleValue(){ return value.doubleValue(); }
  public float floatValue(){ return value.floatValue(); }
  public int intValue(){ return value.intValue(); }
  public long longValue(){ return value.longValue(); }
  public short shortValue(){ return value.shortValue(); }

  public int compareTo( HexInteger o )
  {
    return value.compareTo( o.value );
  }

  private Integer value = null;
}
