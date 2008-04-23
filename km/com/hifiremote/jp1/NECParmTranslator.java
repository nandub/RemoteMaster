package com.hifiremote.jp1;

public class NECParmTranslator
  extends Translate
{
  public NECParmTranslator( String[] textParms )
  {
    super( textParms );
    initialDefaultParm = Integer.parseInt( textParms[ 0 ], 16 );
  }

  public void in( Value[] parms, Hex hexData, DeviceParameter[] devParms, int onlyIndex )
  {
    short[] hex = hexData.getData();
    Integer deviceNumber = ( Integer )parms[ 0 ].getUserValue();
    Integer subDevice = ( Integer )parms[ 1 ].getUserValue();
    Integer parm = ( Integer )parms[ 2 ].getUserValue();

    if ( parm != null )
      hex[ 0 ] = parm.shortValue();
    else
    {
      if ( subDevice == null )
        hex[ 0 ] = ( short )initialDefaultParm;
      else
        hex[ 0 ] = ( short )( initialDefaultParm + 0x20 );
    }

    if ( deviceNumber == null )
      deviceNumber = new Integer( 0 );
    hex[ 1 ] = ( short )reverse( complement( deviceNumber.intValue()));

    if ( subDevice == null )
    {
      if (( hex[ 0 ] & 0x20 ) == 0 )
        hex[ 2 ] = hex[ 1 ];
      else
        hex[ 2 ] = ( short )complement( hex[ 1 ]);
    }
    else
      hex[ 2 ] = ( short )reverse( complement( subDevice.intValue()));
  }

  public void out( Hex hexData, Value[] parms, DeviceParameter[] devParms )
  {
    short[] hex = hexData.getData();
    
    Integer deviceNumber = new Integer( byte2int( reverse( complement( hex[ 1 ]))));

    Integer subDevice = new Integer( byte2int( reverse( complement( hex[ 2 ] ))));

    Integer parm = new Integer( hex[ 0 ]);
   
    if (( parm.intValue() & 0x20 ) == 0 )
    {
      if ( subDevice.equals( deviceNumber ))
        subDevice = null;
      if ( parm.intValue() == initialDefaultParm )
        parm = null;
    }  
    else
    {
      if ( subDevice.equals( byte2int( complement( deviceNumber.intValue()))))
        subDevice = null;
      if ( parm.intValue() == ( initialDefaultParm | 0x20 ))
        parm = null;
    }
    
    if ( deviceNumber.intValue() == 0 )
      deviceNumber = null;
    
    parms[ 0 ] = new Value( deviceNumber, null );
    parms[ 1 ] = new Value( subDevice, null );
    parms[ 2 ] = new Value( parm, null );
  }

  private int initialDefaultParm;
}
