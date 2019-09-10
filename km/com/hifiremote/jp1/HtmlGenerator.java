package com.hifiremote.jp1;

import java.awt.Color;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.event.ChangeEvent;

import com.hifiremote.jp1.RemoteConfiguration.KeySpec;
import com.hifiremote.jp1.RemoteMaster.SummarySection;

public class HtmlGenerator
{
  /*
  public static void main(String[] args) 
  {
    System.out.println( getHtmlString( "<html>Size, Test    &amp<br>color</html>" ));
  }
  */
  
  public HtmlGenerator( RemoteConfiguration remoteConfig )
  {
    this.remoteConfig = remoteConfig;
  }
    
  public static String getHtmlString( String name )
  {
    String s = name.trim();
    if ( s.isEmpty() )
      return "";
    if ( s.startsWith( "<html>" ) )
    {
      // Existing HTML string, make sure line breaks and special characters are compliant
      s = s.replaceAll( "\\</?html\\>", "" );           // remove html start and end tags
      s = s.replace( "<br>", "<br/>");                  // make sure breaks are properly end tags
      Pattern p = Pattern.compile( "\\&.*?(\\<|;|\\s)" );
      Matcher m = null;
      int n = 0;
      while ( ( m = p.matcher( s ) ).find( n ) )        // make sure special characters have a semicolon terminator
      {
        n = m.end();
        if ( s.charAt( n - 1 ) == ';')
          continue;
        s = s.substring( 0, n - 1 ) + ";" + s.substring( n - 1 );
        n++;
      }
    }
    else
    {
      // Use HTML special characters where required
      s = s.replace( "&", "&amp;" ).replace( "<", "&lt;" )
          .replace( ">", "&gt;" ).replace( "\"", "&quot;" );
    }
    return s;
  }  
  
  @SuppressWarnings( "unchecked" )
  private String getTableRow( SummarySection ss, int row )
  {
    Remote remote = remoteConfig.getRemote();
    JP1TableModel< ? > model = ss.model;
    String ls = System.lineSeparator();
    StringBuilder sb = new StringBuilder();
    sb.append( prefix  + "<tr>" + ls );
    prefix += pfxIncr;
    for ( int col = 0; col < model.getColumnCount(); col++ )
    {
      int xcol = ss.table != null ? ss.table.convertColumnIndexToModel( col ) : col;
      Object val = model.getValueAt( row, xcol );
      String suffix = "";
      if ( val == null )
      {
        val = "";
      }
      
      if ( model.getColumnClass( xcol ) == Color.class )
      {
        Object item = model.getRow( row );
        if ( item instanceof Highlight )
        {
          int mem = ( ( Highlight )item ).getMemoryUsage();
          val = Math.abs( mem );
          if ( model instanceof SettingsTableModel )
          {
            suffix = mem == -1 ? " bit" : mem < 0 ? " bits" : mem == 1 ? " byte" : "bytes";
          }
        }
        else
          continue;
      }
      else if ( model.getColumnRenderer( xcol ) instanceof KeyCodeRenderer )
      {
        val = remote.getButtonName( ( int )val );
      }
      
      if ( model instanceof DeviceUpgradeTableModel )
      {
        DeviceUpgradeTableModel dutm = ( DeviceUpgradeTableModel )model;
        DeviceUpgrade du = dutm.getRow( row );
        switch ( ( dutm.getEffectiveColumn( xcol ) ) )
        {
          case 5:
            val = du.getSetupCode() < 0 ? "" : du.getStarredID();
            break;
          case 6:
            Protocol protocol = du.getSetupCode() < 0 ? null : ( Protocol )val;
            val = protocol == null ? "" : protocol.getVariantDisplayName( remote.getProcessor() );
            break;
          case 10:
            val = du.getProtocolMemoryUsage();
            break;
        } 
      }
      else if ( model instanceof ActivityFunctionTableModel )
      {
        ActivityFunctionTableModel aftm = ( ActivityFunctionTableModel )model;
        if ( aftm.getColumnClass( col )  == List.class )
        {
          if ( val instanceof List< ? > )
          {
            val = Macro.getValueString( ( List< KeySpec > )val );
          }
          else if ( val instanceof Hex )
          {
            val = Macro.getValueString( ( Hex )val, remoteConfig );
          }
          else
          {
            val = "";
          }
        }
      }
      String valStr = getHtmlString( val.toString() );
      sb.append( prefix + "<td>" );
      sb.append( valStr + suffix );
      sb.append( "</td>" + ls );
    }
    prefix = prefix.substring( pfxIncr.length() );
    sb.append( prefix + "</tr>" + ls );
    return sb.toString();
  }
  
  public boolean makeHtml( List< SummarySection > ssList )
  {
    if ( ssList == null )
      return false;
    
    FileWriter fw = null;
    try
    {
      fw = new FileWriter( RemoteMaster.getSummaryFile() );
    }
    catch ( IOException e )
    {
      e.printStackTrace();
      return false;
    }
    PrintWriter pw = new PrintWriter( fw );
    pw.println( "<html>" );
    prefix = pfxIncr;
    pw.println( prefix + "<head>" );
    prefix += pfxIncr;
    pw.println( prefix + "<meta http-equiv=\"Content-Type\" content=\"text/html; charset=UTF-8\">" );
    pw.println( prefix + "<style>" );
    pw.println( prefix + "  table, th, td {" );
    pw.println( prefix  + "    border: 1px solid black;" );
    pw.println( prefix  + "    border-collapse: collapse;" );
    pw.println( prefix  + "  }" );   
    pw.println( prefix  + "</style>" );
    prefix = prefix.substring( pfxIncr.length() );
    pw.println( prefix  + "</head>" );
    pw.println( prefix  + "<body>" );
    prefix += pfxIncr;
    
    int currentActivityIndex = -1;
    ActivityPanel ap = remoteConfig.getOwner().getActivityPanel();
    FavoritesPanel fp = remoteConfig.getOwner().getFavoritesPanel();
    Remote remote = remoteConfig.getRemote();
    
    pw.println( prefix + "<h2>" + remote.getName() + "</h2>" );
    for ( SummarySection ss : ssList )
    {
      JP1TableModel< ? > model = ss.model;
      if ( ss.activity != null )
      {   
        currentActivityIndex = ap.getTabbedPane().getSelectedIndex();
        Button btn = ss.activity.getButton();
        if ( model instanceof ActivityFunctionTableModel )
        {
          ActivityFunctionTableModel aftm = ( ActivityFunctionTableModel )model;
          aftm.set( btn, remoteConfig );
        }
        else if ( model instanceof ActivityGroupTableModel )
        {
          ActivityGroupTableModel agtm = ( ActivityGroupTableModel )model;
          agtm.set( btn, remoteConfig, null );
        }
      }
      
      pw.println( prefix  + "<h3>" + ( ss.title == null ? "" : ss.title ) + "</h3>" );
      if ( ss.subtitle != null && !ss.subtitle.trim().isEmpty() )
        pw.println( prefix + "<h4>" + ss.subtitle + "</h4>" );

      if ( ss.model instanceof FavScanTableModel )
      {
        DeviceButton favDb = remoteConfig.getFavKeyDevButton();
        if ( remote.hasFavorites() )
        {
          Button favFinalKey = remoteConfig.getFavFinalKey();
          pw.println( prefix + "<p>" );
          pw.println( prefix + "  Channel change device: " + getHtmlString( favDb.toString() ) + "<br/>" );
          String pause = String.format( "%2.1f sec", remoteConfig.getFavPause()/10.0 );
          pw.println( prefix + "  Interdigit pause: " + pause + "<br/>" );
          pw.println( prefix + "  Digits: " + ( favDb == null || favDb == DeviceButton.noButton ? 0 : favDb.getFavoriteWidth() ) + "<br/>" );
          pw.println( prefix + "  Send final key? " + ( favFinalKey == null ? "no" 
              : "yes<br/>Key: " + favFinalKey ) );
          pw.println( prefix + "</p>" );
        }
        else
        {
          pw.println( prefix + "<p>  Scan device: " + getHtmlString( favDb.toString() ) + "</p>");
        }
      }

      pw.println( prefix  + "<table>" );
      prefix += pfxIncr;
      pw.print( getTableHead( ss ) );
      if ( model != fp.getActivityGroupModel() || fp.getFavModel().getRowCount() > 0 )
      {
        for ( int row = 0; row < model.getRowCount(); row++ )
        {
          int modelRow = ss.sorter != null ? ss.sorter.modelIndex( row ) : row;
          pw.print( getTableRow( ss, modelRow ) );
        }
      }
      prefix = prefix.substring( pfxIncr.length() );
      pw.println( prefix + "</table>" );
    }
    if ( currentActivityIndex >= 0 )
    {
      ap.getTabbedPane().setSelectedIndex( currentActivityIndex );
      ChangeEvent e = new ChangeEvent( ap.getTabbedPane() );
      ap.stateChanged( e );
    }
 
    prefix = prefix.substring( pfxIncr.length() );
    pw.println( prefix + "</body>" );
    prefix = prefix.substring( pfxIncr.length() );
    pw.println( prefix + "</html>" ); 
    pw.close();
    try
    {
      fw.close();
    }
    catch ( IOException e )
    {
      e.printStackTrace();
      return false;
    }
    return true;
  }
  
  private String getTableHead( SummarySection ss )
  {
    JP1TableModel< ? > model = ss.model;
    String ls = System.lineSeparator();
    StringBuilder sb = new StringBuilder();
    sb.append( prefix  + "<tr>" + ls );
    prefix += pfxIncr;
    for ( int col = 0; col < model.getColumnCount(); col++ )
    {
      int xcol = ss.table != null ? ss.table.convertColumnIndexToModel( col ) : col;
      String colHead = getHtmlString( model.getColumnName( xcol ) );
      sb.append( prefix + "<th>" + colHead + "</th>" + ls );
    }
    prefix = prefix.substring( pfxIncr.length() );
    sb.append( prefix + "</tr>" + ls );
    return sb.toString();
  }
  
  String prefix = "";
  String pfxIncr = "  ";
  RemoteConfiguration remoteConfig = null;
}
 