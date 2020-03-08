package com.hifiremote.jp1.rf;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import javax.crypto.spec.IvParameterSpec;
import javax.swing.JOptionPane;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.rf.Mpdu.MACAddrData;
import com.hifiremote.jp1.rf.Npdu.NSDUDirection;
import com.hifiremote.jp1.rf.Npdu.NSDUType;
import com.hifiremote.jp1.rf.Npdu.NSPrimitive;
import com.hifiremote.jp1.rf.RfRemote;
import com.hifiremote.jp1.rf.RfRemote.Pairing;
import com.hifiremote.jp1.rf.RfTools;

import java.util.Arrays;
import java.util.List;

public class Rf4ceAuthenticator
{
  public static enum Source
  { CONTROLLER, TARGET, NONE };
  
  public Rf4ceAuthenticator( List< RfRemote > rfRemotesList, RfTools owner )
  {
    this.rfRemotesList = rfRemotesList;
    this.owner = owner;
  }

  public void decrypt( NSPrimitive nsPrim ) 
  {     
    Source source = Source.NONE;
    MACAddrData addrData = nsPrim.addrData;
    for ( RfRemote rfr : rfRemotesList )
    {
      source = getSource( rfr, addrData.srcPAN, addrData.srcAddr, addrData.destPAN, addrData.destAddr );
      if ( source != Source.NONE )
      {
        rfRemote = rfr;
        break;
      }
    }
    if ( source == Source.NONE && rfRemote != null )
    {
      source = getSource( rfRemote, addrData.srcPAN, addrData.srcAddr, addrData.destPAN, addrData.destAddr );
    } 
    if ( nsPrim.secured && source == Source.NONE )
    {
      nsPrim.setError( "Decryption failed.  This signal is not from, or for, a registered RF Remote" );
      return;
    }
    if ( nsPrim.type != NSDUType.COMMAND )
    {
      nsPrim.direction = source == Source.CONTROLLER ? NSDUDirection.OUT
          : source == Source.TARGET ? NSDUDirection.IN : null;
    }
    if ( !nsPrim.secured )
    {
      return;
    }
    
//    System.err.println( "Decrypting and authenticating the NSDU" );
//    System.err.println( "RfRemote is " + rfRemote.name );
//    System.err.print( "NS Primitive is " + nsPrim.type );
//    System.err.println( nsPrim.type == NSDUType.COMMAND ? " " + nsPrim.cmd : "" );
    Hex inData = nsPrim.rawNsdu;
    Hex inFtr = nsPrim.authData;
    Hex securityKey = rfRemote.pairings.get( pairNdx ).getSecurityKey();
    Hex remoteIEEEaddr = rfRemote.extAddr;
    Hex destIEEEaddr = rfRemote.pairings.get( pairNdx ).getPeerExtAddr();
    Hex nonceIEEEaddr = source == Source.CONTROLLER ? remoteIEEEaddr : destIEEEaddr;
    Hex authIEEEaddr = source == Source.CONTROLLER ? destIEEEaddr : remoteIEEEaddr;

    byte[] nonce = new byte[ 13 ];
    System.arraycopy( nonceIEEEaddr.toByteArray(), 0, nonce, 0, 8 );
    System.arraycopy( nsPrim.frameCtrHex.toByteArray(), 0, nonce, 8, 4 );
    nonce[ 12 ] = ( byte )5;
    System.err.println( "Nonce: " + ( new Hex( nonce ) ) );

    int frameCtl = nsPrim.frameCtl;
    byte[] addAuthData = new byte[ 13 ];
    addAuthData[ 0 ] = ( byte )frameCtl;
    System.arraycopy( nsPrim.frameCtrHex.toByteArray(), 0, addAuthData, 1, 4 );
    System.arraycopy( authIEEEaddr.toByteArray(), 0, addAuthData, 5, 8 );
    System.err.println( "AddAuthData: " + ( new Hex( addAuthData ) ) );

    byte[] initBytes = new byte[ 16 ];
    Arrays.fill( initBytes, ( byte )0 );
    initBytes[ 0 ] = 1;
    System.arraycopy( nonce, 0, initBytes, 1, 13 );

    IvParameterSpec iv = new IvParameterSpec( initBytes );
    SecretKeySpec skeySpec = new SecretKeySpec( securityKey.toByteArray(), "AES");
    System.err.println( "Key encoded: " + ( new Hex(skeySpec.getEncoded() ) ) );
    try
    {
      Cipher cipher = Cipher.getInstance("AES/OFB/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

//      System.err.println( "Footer in : " + inFtr );
      Hex outFtr = new Hex( cipher.doFinal( inFtr.toByteArray() ) );
//      System.err.println( "Footer out : " + outFtr );

      initBytes[ 15 ] = 1;
      iv = new IvParameterSpec( initBytes );
      skeySpec = new SecretKeySpec( securityKey.toByteArray(), "AES");

      cipher = Cipher.getInstance("AES/OFB/NoPadding");
      cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
//      System.err.println( "Data in: " + inData );
      Hex outData = new Hex( cipher.doFinal( inData.toByteArray() ) );
//      System.err.println( "Data out: " + outData );

      // Recalculate authentication field
      byte[] blockB0 = new byte[ 16 ];
      blockB0[ 0 ] = ( byte )0x49;
      System.arraycopy( nonce, 0, blockB0, 1, 13 );
      blockB0[ 14 ] = 0;
      blockB0[ 15 ] = ( byte )inData.length();
      byte[] blockB1 = new byte[ 16 ];
      Arrays.fill( blockB1, ( byte )0 );
      blockB1[ 0 ] = 0;
      blockB1[ 1 ] = ( byte )13;
      System.arraycopy( addAuthData, 0, blockB1, 2, 13 );
      byte[] blockB2 = new byte[ 16 ];
      Arrays.fill( blockB2, ( byte )0 );       
      System.arraycopy( outData.toByteArray(), 0, blockB2, 0, inData.length() );      
//      System.err.println( "BlockB0: " + ( new Hex( blockB0 ) ) );
//      System.err.println( "BlockB1: " + ( new Hex( blockB1 ) ) );
//      System.err.println( "BlockB2: " + ( new Hex( blockB2 ) ) );

      Arrays.fill( initBytes, ( byte )0 );
      iv = new IvParameterSpec( initBytes );
      cipher = Cipher.getInstance("AES/CBC/NoPadding");
      cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
      cipher.update( blockB0 );
      cipher.update( blockB1 );
      Hex authCode = ( new Hex( cipher.doFinal( blockB2 ) ) ).subHex( 0, 4 );
//      System.err.println( "Auth code: " + authCode );
      boolean valid = outFtr.equals( authCode );
//      System.err.println( "Authentication status: " + valid );
      if ( valid )
      {
        nsPrim.nsdu = outData;
        nsPrim.setCommand();
        nsPrim.valid = true;
      }
      else
      {
        nsPrim.setError( "Authentication failed" );
      }
    } 
    catch (Exception ex) 
    {
      ex.printStackTrace();
    }
    return;
  }

  public void processCommand( NSPrimitive nsPrim )
  {
    if ( nsPrim.type != NSDUType.COMMAND )
    {
      return;
    }
    int appCaps = 0;
    int nodeCaps = 0;
    int devTypeSize = 0;
    int profileSize = 0;
    MACAddrData addrData = nsPrim.addrData;
    Pairing pair = pairNdx >= 0 ? rfRemote.pairings.get( pairNdx ) : null;
    switch ( nsPrim.cmd )
    {
      case DISCOVERY_REQ:
        // The remote is advertising itself, so here we pick up everything it provides.
        // The DISCOVERY_RSP reply is ignored, as there may be several devices replying.
        nodeCaps = nsPrim.nsdu.getData()[ 1 ];
        if ( ( nodeCaps & 1 ) == 0 )
        {
          nsPrim.direction = NSDUDirection.OUT;
        }
        else
        {
          nsPrim.direction = NSDUDirection.IN;
          System.err.println( "Discovery Request received from target, RMIR does not yet handle this correctly" );
        }
        
        appCaps = nsPrim.nsdu.getData()[ 11 ];
        Hex extAddr = addrData.srcAddr.length() == 8 ? addrData.srcAddr : null;
        rfRemote =  null;
        RfRemote fullRemote = null;
        RfRemote provRemote = null;
        for ( RfRemote rfr : rfRemotesList )
        {
          // In the case of provisional registration from an RMIR download, the IEEE address
          // is not available so we test what is available through non-volatile NIB attributes.
          if ( rfr.extAddr != null && rfr.extAddr.equals( extAddr ) )
          {
            fullRemote = rfr;
          }
          if ( rfr.extAddr == null
               && rfr.vendorID.equals( nsPrim.nsdu.subHex( 2, 2 ) )
               && rfr.vendorString.equals( nsPrim.nsdu.subHex( 4, 7 ) )
               && ( ( appCaps & 1 )  == 0 || rfr.userString.equals( nsPrim.nsdu.subHex( 12, 15 ) ) ) )
          {
            provRemote = rfr;
          }
        }
        
        if ( fullRemote != null && provRemote != null )
        {
          if ( lastFull != null && fullRemote == lastFull )
          {
            return;
          }
          lastFull = fullRemote;
          String title = "Registration of RF Remote";
          String message =
              "The Discovery Request in this file appears to be for the completion of the\n"
            + "provisional registration of the RF Remote named " + provRemote.name + ".  However,\n"
            + "it also matches the full registration of that named " + fullRemote.name + ".\n"
            + "As this would create a duplicate, the registration update is aborting.";
          JOptionPane.showMessageDialog( owner, message, title, JOptionPane.ERROR_MESSAGE );
          return;
        }
        
        rfRemote = fullRemote != null ? fullRemote : provRemote;

        if ( rfRemote ==  null )
        {
          rfRemote = new RfRemote( extAddr );
        }
        rfRemote.changed = false;
        
        int pos = 12;
        if ( rfRemote.extAddr == null )
        {
          rfRemote.extAddr = extAddr;
          if ( ( appCaps & 1 ) == 1 )
          {
            pos += 15;
          }
          rfRemote.changed = true;
        }
        else
        {
          rfRemote.nodeCaps = nodeCaps;
          rfRemote.vendorID = nsPrim.nsdu.subHex( 2, 2 );
          rfRemote.vendorString = nsPrim.nsdu.subHex( 4, 7 );
          if ( ( appCaps & 1 ) == 1 )
          {
            rfRemote.userString = nsPrim.nsdu.subHex( pos, 15 );
            pos += 15;
          }
          else
          {
            rfRemote.userString = null;
          }
        }
        devTypeSize = ( appCaps >> 1 ) & 3;
        rfRemote.devTypes = nsPrim.nsdu.subHex( pos, devTypeSize ); 
        pos += devTypeSize;
        profileSize = ( appCaps >> 4 ) & 7;
        rfRemote.profiles = nsPrim.nsdu.subHex( pos, profileSize );
        break;
      case DISCOVERY_RSP:
        nodeCaps = nsPrim.nsdu.getData()[ 2 ];
        if ( ( nodeCaps & 1 ) == 1 )
        {
          nsPrim.direction = NSDUDirection.IN;
        }
        else
        {
          nsPrim.direction = NSDUDirection.OUT;
          System.err.println( "Discovery Response sent by remote, RMIR does not yet handle this correctly" );
        }
        break;
      case PAIR_REQ:
        // Here the remote has selected which respondent it wishes to pair with.  In the 
        // DISCOVERY_RSP the respondent has provided its extended (IEEE) address and its
        // PAN id.  These are used to address the respondent, so we pick these up here.
        // The remote also provides the number of KEYSEED packets it wishes the security
        // key to be sent in, so we pick that up for later use.nodeCaps = nsPrim.nsdu.getData()[ 1 ];
        nodeCaps = nsPrim.nsdu.getData()[ 3 ];
        if ( ( nodeCaps & 1 ) == 0 )
        {
          nsPrim.direction = NSDUDirection.OUT;
        }
        else
        {
          nsPrim.direction = NSDUDirection.IN;
          System.err.println( "Pair Request received from target, RMIR does not yet handle this correctly" );
        }
        
        if ( getSource( rfRemote, addrData.srcPAN, addrData.srcAddr, addrData.destPAN, addrData.destAddr ) != Source.CONTROLLER )         
        { 
          pairNdx = rfRemote.pairings.size();
          pair = new Pairing();
          rfRemote.pairings.add( pair );
        }
        else
        {
          pair = rfRemote.pairings.get( pairNdx );
        }
        pair.setPairRef( 0xFF );  // mark as unpaired, as the pairing may fail
        nwkAddrChanged = pair.getPanID() == null || !pair.getPanID().equals( addrData.destPAN );
        pair.setPanID( addrData.destPAN );
        pair.setPeerExtAddr( addrData.destAddr );
        appCaps = nsPrim.nsdu.getData()[ 13 ];
        pos = 14;
        pos += ( appCaps & 1 ) == 1 ? 15 : 0; 
        pos += ( appCaps >> 1 ) & 3;
        pos += ( appCaps >> 4 ) & 7;
        keyExCount = nsPrim.nsdu.getData()[ pos ];
        keySeeds = new Hex[ keyExCount + 1 ];
        break;
      case PAIR_RSP:
        // The target now issues the remote with a short (network) address and provides its
        // own network address.  It also provides its vendor id and vendor string and optionally
        // also a user string.  We pick all these up here.
        nodeCaps = nsPrim.nsdu.getData()[ 6 ];
        if ( ( nodeCaps & 1 ) == 1 )
        {
          nsPrim.direction = NSDUDirection.IN;
        }
        else
        {
          nsPrim.direction = NSDUDirection.OUT;
          System.err.println( "Pair Response sent by remote, RMIR does not yet handle this correctly" );
        }
        if ( pair.getNwkAddr() == null 
            || !pair.getNwkAddr().equals( nsPrim.nsdu.subHex( 2, 2 ) )  )
        {
          nwkAddrChanged = true;
        }
        pair.setNwkAddr( nsPrim.nsdu.subHex( 2, 2 ) );
        pair.setPeerNwkAddr( nsPrim.nsdu.subHex( 4, 2 ) );
        pair.setPeerNodeCaps( nsPrim.nsdu.getData()[ 6 ] );
        pair.setPeerVendorID( nsPrim.nsdu.subHex( 7, 2 ) );
        pair.peerVendorString = nsPrim.nsdu.subHex( 9, 7 );
        appCaps = nsPrim.nsdu.getData()[ 16 ];
        pos = 17;
        if ( ( appCaps & 1 ) == 1 )
        {
          pair.peerUserString = nsPrim.nsdu.subHex( pos, 15 );
          pos += 15;
        }
        else
        {
          pair.peerUserString = null;
        }
        devTypeSize = ( appCaps >> 1 ) & 3;
        pair.setPeerDevTypes( nsPrim.nsdu.subHex( pos, devTypeSize ) ); 
        pos += devTypeSize;
        profileSize = ( appCaps >> 4 ) & 7;
        pair.peerProfiles = nsPrim.nsdu.subHex( pos, profileSize );
        break;
      case KEYSEED:
      {
        if ( keySeeds == null )
          return;
        int seedNum = nsPrim.nsdu.getData()[ 1 ];
        if ( seedNum < 0 || seedNum >= keySeeds.length )
          return;
        nsPrim.direction = NSDUDirection.IN;
        keySeeds[ seedNum ] = nsPrim.nsdu.subHex( 2, 80 );
        for ( int i = 0; i < keySeeds.length; i++ )
          if ( keySeeds[ i ] == null ) return;
        Hex key = keyGeneration( keySeeds );
        pair.setSecurityKey( key );
        break;
      }
      case PING_REQ:
      {
        // As part of setting the security key, the RF4CE standard dictates that the
        // pingOptions byte is 0 and the random payload is 4 bytes.
        int pingOptions = nsPrim.nsdu.getData()[ 1 ];
        if ( pingOptions != 0 )
          return;
        nsPrim.direction = NSDUDirection.OUT;
        pingData = nsPrim.nsdu.subHex( 1 ); // include options byte
        if ( pingData.length() != 5 )
          pingData = null;
        break;  
      }
      case PING_RSP:
      {
        int pingOptions = nsPrim.nsdu.getData()[ 1 ];
        if ( pingOptions != 0 )
          return;
        nsPrim.direction = NSDUDirection.IN;
        Hex rspData = nsPrim.nsdu.subHex( 1 ); // include options byte
        if ( pingData != null && rspData.equals( pingData ) )
        {
          // Pairing succeeded, so set pair reference
          pair.setPairRef( pairNdx );
          rfRemote.changed = nwkAddrChanged;
        }
        break;
      }
    }
    if ( pair != null && nsPrim.direction == NSDUDirection.IN && nsPrim.channelDesignator > 0 )
    {
      pair.setChannel( 10 + 5*nsPrim.channelDesignator );
    }
  }

  private Source getSource( RfRemote rfRem, Hex srcPAN, Hex srcAddr, Hex destPAN, Hex destAddr )
  {
    if ( srcAddr.length() == 8 && destAddr.equals( new Hex( "FF FF" ) ) && srcAddr.equals( rfRem.extAddr ) )
    {
      // Broadcast signal from remote with no specific pairing
      pairNdx = -1;    
      return Source.CONTROLLER;
    }
 
    if ( srcAddr == null || destAddr == null )
    {
      return Source.NONE;
    }
    
    for ( int i = 0; i < rfRem.pairings.size(); i++ )
    {
      Pairing pair = rfRem.pairings.get( i );
      pairNdx = i;
      Source result = Source.NONE;
      if ( srcAddr.length() == 8 )
      {
        if ( srcAddr.equals( rfRem.extAddr ) )
          result = Source.CONTROLLER;
        else if ( srcAddr.equals( pair.getPeerExtAddr() ) )
          result =  Source.TARGET;
        else
          continue;
      }
      else
      {
        if ( srcPAN.equals( pair.getPanID() ) 
            && srcAddr.equals( pair.getNwkAddr() ) )
          result = Source.CONTROLLER;
        else if ( srcPAN.equals( pair.getPanID() )
            && srcAddr.equals( pair.getPeerNwkAddr() ) )
          result = Source.TARGET;
        else
          continue;
      }
      if ( result == Source.NONE )
        return result;
      if ( destAddr.length() == 8 )
      {
        if ( destAddr.equals( pair.getPeerExtAddr() )
          && result == Source.CONTROLLER )
          return result;
        else if ( destAddr.equals( rfRem.extAddr )
          && result == Source.TARGET )
          return result;
        else
          continue;
      }
      else
      {
        if ( destPAN.equals( pair.getPanID() ) 
            && destAddr.equals( pair.getPeerNwkAddr() ) 
            && result == Source.CONTROLLER )
          return result;
        else if ( destPAN.equals( pair.getPanID() )
            && srcAddr.equals( pair.getNwkAddr() ) 
            && result == Source.TARGET )
          return result;
        else
          continue;
      }
    }
    pairNdx = -1;  // Unset
    return Source.NONE;
  }
  
  public Hex keyGeneration( Hex[] keySeeds )
  {
    Hex finalSeed = new Hex( 80 );
    for ( int i = 0; i < 80; i++ )
    {
      finalSeed.getData()[ i ] = 0;
      for ( int j = 0; j < keySeeds.length; j++ )
      {
        finalSeed.getData()[ i ] ^= keySeeds[ j ].getData()[ i ];
      }
    }
    Hex key = new Hex( 16 );
    for ( int i = 0; i < 16; i++ )
    {
      key.getData()[ i ] = 0;
      for ( int j = 0; j < 5; j++ )
      {
        key.getData()[ i ] ^= finalSeed.getData()[ 16*j + i ];
      }
    }
    return key;
  }

  public RfRemote getRfRemote()
  {
    return rfRemote;
  }

  public int getPairNdx()
  {
    return pairNdx;
  }

  private RfRemote rfRemote = null; 
  private RfRemote lastFull = null;
  private int pairNdx = -1;  // Unset
  private int keyExCount = 0;
  private Hex[] keySeeds = null;
  private boolean nwkAddrChanged = false;
  private Hex pingData = null;
  private List< RfRemote > rfRemotesList = null;
  private RfTools owner = null;

}
