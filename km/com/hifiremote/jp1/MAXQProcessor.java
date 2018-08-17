package com.hifiremote.jp1;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import com.hifiremote.jp1.AssemblerOpCode.AddressMode;
import com.hifiremote.jp1.assembler.MAXQ610data;

public class MAXQProcessor extends LittleEndianProcessor
{
  public MAXQProcessor( String name )
  {
    super( name );
    // True RAM Address not yet known, but the 2k of data memory in word mode is
    // addressed from $0000 to $03FF.  For now, try $0100
    setRAMAddress( 0x0100 );
    setPageSize( 0x200 );
  }
  
  @Override
  public String getEquivalentName()
  {
    return "MAXQ610";
  }
  
  @Override
  public AddressMode disasmModify( AddressMode mode, Object[] obj )
  {
    int sIndex = 0;
    int sMod = 0;
    int dIndex = 0;
    int dMod = 0;
    mode.nibbleArgs = 0;  // Ignore nibble args when locating args to replace by labels etc 
    switch ( mode.modifier )
    {
      case 1:
        LinkedHashMap< String, AddressMode > modes = getAddressModes();
        int op1 = ( Integer )obj[ 0 ];
        if ( op1 < 0x70 )
        {
          mode = modes.get( "Fun1" );
        }
        else if ( op1 < 0x80 )
        {
          mode = modes.get( "Fun1B" );
        }
        break;
      case 2:
        sIndex = ( Integer )obj[ 0 ];
        sMod = ( Integer )obj[ 1 ];
        obj[ 0 ] = ( pfx.state == 2 ? ( pfx.val & 0xFF ) << 8 : 0 ) | ( sIndex << 4 ) | sMod;
        if ( pfx.state == 2)
        {
          mode = new AddressMode( mode );
          mode.format = mode.format.replace( "02X", "04X" );
          mode.relMap = 0;
          mode.absMap = 1;
          if ( !addressList.contains( pfx.addr ) )
          {
            addressList.add( pfx.addr );
          }
        }
        break;
      case 3:
        sIndex = applySrcPrefix( ( Integer )obj[ 0 ] );
        sMod = ( Integer )obj[ 1 ];
        dIndex = ( ( Integer )obj[ 2 ] ) & 7;
        obj[ 0 ] = getSource( sIndex, sMod );
        obj[ 2 ] = dIndex;
        break;
      case 4:
        dIndex = applyDstPrefix( 2, ( ( Integer )obj[ 0 ] ) & 7 );
        dMod = ( Integer )obj[ 1 ];
        obj[ 0 ] = getDest( dIndex, dMod );
        break;
      case 5:
        sIndex = ( Integer )obj[ 0 ];
        dIndex = applyDstPrefix( 2, ( ( Integer )obj[ 1 ] ) & 7 );
        dMod = ( Integer )obj[ 2 ];
        obj[ 0 ] = sIndex & 7;
        obj[ 1 ] = getDest( dIndex, dMod );
        break;
      case 6:
        sIndex = ( Integer )obj[ 0 ];
        sMod = ( Integer )obj[ 1 ];
        dIndex = applyDstPrefix( 2, ( ( Integer )obj[ 2 ] ) & 7 );
        dMod = ( Integer )obj[ 3 ];
        obj[ 0 ] = ( pfx.state == 2 ? ( pfx.val & 0xFF ) << 8 : 0 ) | ( sIndex << 4 ) | sMod;
        obj[ 2 ] = getDest( dIndex, dMod );
        if ( pfx.state == 2)
        {
          mode = new AddressMode( mode );
          mode.format = mode.format.replace( "02X", "04X" );
        }
        break;
      case 7:
        sIndex = applySrcPrefix( ( Integer )obj[ 0 ] );
        sMod = ( Integer )obj[ 1 ];
        dIndex = applyDstPrefix( 2, ( ( Integer )obj[ 2 ] ) & 7 );
        dMod = ( Integer )obj[ 3 ];
        obj[ 0 ] = getSource( sIndex, sMod );
        obj[ 2 ] = getDest( dIndex, dMod );
        break;
    }
    return mode;
  }
  
  private int applyDstPrefix( int state, int dIndex )
  {
    if ( pfx.state == state )
    {
      dIndex += ( pfx.ndx >> 1 ) * 8;
      if ( !addressList.contains( pfx.addr ) )
      {
        addressList.add( pfx.addr );
      }
    }
    return dIndex;
  }
  
  private int applySrcPrefix( int sIndex )
  {
    if ( pfx.state == 2 )
    {
      sIndex += ( pfx.ndx & 1 ) * 16;
    }
    return sIndex;
  }
  
  @Override
  public AssemblerOpCode getOpCode( Hex hex )
  {
    if ( !getName().contains( "Native" ) )
    {
      return super.getOpCode( hex );
    }

    if ( hex == null || hex.length() == 0 ) return null;
    AssemblerOpCode opCode = new AssemblerOpCode();
    LinkedHashMap< String, AddressMode > modes = getAddressModes();
    opCode.setHex( hex.subHex( 0, 2 ) );
    opCode.setLength( 1 );
    short[] data = opCode.getHex().getData();
    int flag = data[ 1 ] >> 7;
    int dIndex = applyDstPrefix( 1, ( ( data[ 1 ] >> 4 ) & 7 ) );
    int dMod = data[ 1 ] & 0x0F;
    int sIndex = data[ 0 ] >> 4;
    int sMod = data[ 0 ] & 0x0F;

    // sMod == 10 && getSource( sIndex, sMod ).length() > 1 implies sIndex < 2,
    // with src = Acc or A[AP].  dMod == 10 && dIndex == 0 gives MOVE Acc, src
    // while dMod == 10, dIndex > 0 gives op src where op is one of the ALU
    // mnemonics.  These instructions are all trivial ones that are overridden
    // by bitwise instructions.
    if ( flag == 0 || getSource( sIndex, sMod ).length() > 1 
        && ( dMod != 10 || sMod != 10 ) )
    {
      if ( dMod == 13 && dIndex == 0 )
      {
        // Equiv to and overrides MOVE @++SP, src
        opCode.setName( "PUSH" );
        opCode.setMode( modes.get( flag == 0 ? "Imm" : "Src" ) ); 
      }
      else if ( getDest( dIndex, dMod ).length() > 1 )
      {
        // The condition excludes register moves to the PFX register
        if ( flag == 1 && sMod == 13 && ( sIndex & 7 ) == 0 )
        {
          // Equiv to and overrides MOVE dddd, @SP-- / @SPI--
          opCode.setName( sIndex == 0 ? "POP" : "POPI" );
          opCode.setMode( modes.get( "Dst" ) );
        }
        else
        {
          opCode.setName( "MOVE" );
          opCode.setMode( modes.get( flag == 0 ? "DstImm" : "DstSrc" ) );
          if ( dMod == 11 && flag == 0 )
          {
            pfx.set( JP2Analyzer.currentAddr, dIndex, ( sIndex << 4 ) | sMod );
          }
          else if ( flag == 0 && dMod == 15 && ( dIndex & 3 ) == 3 )
          {
            opCode.setMode( modes.get( "DstImmZ" ) );
          }
        }
      }
      // Available dIndex values given as comments.
      // None available for dMod = 0,..,5 and 9,11,14,15
      // dMod = 6 has 0,..,6 available
      else if ( dMod == 7 )  // 0,..,7
      {
        opCode.setName( "MOVE" );
        opCode.setMode( modes.get( flag == 0 ? "CImmb" : "CSrcb" ) );
      }
      else if ( dMod == 8 && dIndex == 7 )  // 6,7
      {
        opCode.setName( "CMP" );
        opCode.setMode( modes.get( flag == 0 ? "Imm" : "Src" ) );
      }
      else if ( dMod == 10 )  // 1,..,7
      {
        opCode.setName( MAXQ610data.aluOps[ dIndex ] );
        opCode.setMode( modes.get( flag == 0 ? "Imm" : "Src"  ) );
      }
      else if ( dMod == 12 && ( flag == 0 || ( dIndex & 3 ) != 3 ) )  // 0,..,7
      {
        // Condition codes E, NE only available when flag == 0 (immediate mode)
        if ( flag == 1 && sMod == 13 && ( sIndex & 7 ) == 0 )
        {
          opCode.setName( sIndex == 0 ? "RET" : "RETI" );
          opCode.setMode( modes.get( dIndex == 0 ? "Nil" : "Cond" ) );
        }
        else
        {
          opCode.setName( "JUMP" );
          opCode.setMode( modes.get( dIndex == 0 ? flag == 0 ? "Rel" : "Src" :
            flag == 0 ? "CondRel" : "CondSrc" ) );
        }
      }
      else if ( dMod == 13 )  // 3,4,5
      {
        opCode.setName( MAXQ610data.miscOpsP[ dIndex ] );
        opCode.setMode( new AddressMode( flag == 0 ? MAXQ610data.miscModesPImm[ dIndex ] 
            : MAXQ610data.miscModesP[ dIndex ] ) );
      } 
    }
    else if ( dMod == 10 && sMod == 10 )  // flag == 1
    {
      if ( dIndex == 0 )
      {
        opCode.setName( MAXQ610data.accOps[ sIndex ] );
        opCode.setMode( modes.get( "Nil" ) );
      }
      else if ( dIndex == 5 && sIndex < 4 )
      {
        opCode.setName( MAXQ610data.miscOpsC[ sIndex ] );
        opCode.setMode( new AddressMode( MAXQ610data.miscModesC[ sIndex ] ) );
      }
      else
      {
        opCode.setName( MAXQ610data.bitOps[ dIndex ] );
        opCode.setMode( modes.get( MAXQ610data.bitModes[ dIndex ] ) );
      }
    }
    // Now flag == 1 && getSource() returns "*" && ( dMod != 10 || sMod != 10 )
    else if ( sMod == 7 )
    {
      opCode.setName( "MOVE" );
      opCode.setMode( modes.get( sIndex < 8 ? "DstbImm0" : "DstbImm1" ) );
    }
    else
    {
      opCode.setName( "*" );
    }
    
    if ( opCode.getName().equals( "*" ) )
    {
      opCode.setMode( modes.get( "Nil" ) );
    }
    pfx.check();
    return opCode;
  }

  @Override
  public String getConditionCode( int n )
  {
    if ( !getName().contains( "Native" ) )
    {
      return super.getConditionCode( n );
    }
    return MAXQ610data.conditionCodes[ n & 7 ];
  }
  
  private String getSource( int sIndex, int sMod )
  {
    if ( sMod < 6 )
    {
      return "MN[" + sIndex + "]";
    }
    else if ( sMod >= 8 && sIndex < MAXQ610data.sources[ sMod - 8 ].length )
    {
      return MAXQ610data.sources[ sMod - 8 ][ sIndex ];
    }
    return "*";
  }
  
  private String getDest( int dIndex, int dMod )
  {
    if ( dMod < 6 )
    {
      return "MN[" + dIndex + "]";
    }
    else if ( dMod == 6 && dIndex == 7 )
    {
      return "NUL";
    }
    else if ( dMod >= 8 && dIndex < MAXQ610data.dests[ dMod - 8 ].length )
    {
      return MAXQ610data.dests[ dMod - 8 ][ dIndex ];
    }
    else return "*";
  }
  
  @Override
  public int[] getCarrierData( Hex hex )
  {
    int[] result = new int[ 2 ];
    short[] data = hex.getData();
    int on = data[ 0 ];
    int off = data[ 1 ];
    if ( on == 0 && off == 0 )
    {
      result = new int[]{ 12, 0 };
    }
    else
    {
      int carrier = on + off + getCarrierTotalOffset();
      int duty = ( int )( ( ( on + getCarrierOnOffset() ) * 1000.0 ) / carrier + 0.5 );
      result = new int[]{ carrier, duty };
    }
    return result;
  }
  
  private static class PFX
  {
    public void set( int addr, int ndx, int val )
    {
      this.addr = addr;
      this.ndx = ndx;
      this.val = val;
      state = 0;
    }
    
    public void check()
    {
      if ( state < 3 )
      {
        state++;
      }
      else
      {
        ndx = 0;
        val = 0;
      }
    }
    
    public int addr = 0;
    public int val = 0;
    public int ndx = 0;
    private int state = 3;  // 0 = available to set, 1,2 = set, 3 = inactive
  }
  
  private PFX pfx = new PFX();
//  public List< Integer > usedPfxAddr = new ArrayList< Integer >();
}
