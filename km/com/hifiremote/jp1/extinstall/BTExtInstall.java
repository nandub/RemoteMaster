package com.hifiremote.jp1.extinstall;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.hifiremote.jp1.Hex;
import com.hifiremote.jp1.ProgressUpdater;
import com.hifiremote.jp1.RDFReader;
import com.hifiremote.jp1.io.JP2BT;

public class BTExtInstall
{
  public BTExtInstall( File file, JP2BT btio, ProgressUpdater progressUpdater )
  {
    this.file = file;
    this.btio = btio;
    this.progressUpdater = progressUpdater;
  }
  
  public void install()
  {
    List< Hex > records = new ArrayList< Hex >();
    String title = "Bluetooth Extender Install";
    String message = null;
    try
    {
      RDFReader rdr = new RDFReader( file );
      StringBuilder sb = null;
      Hex record = null;
      String line = rdr.readLine().trim();
      if ( !line.toUpperCase().startsWith( "BLUETOOTH EXTENDER" ) )
      {
        message = "The file " + file.getName() + " is not a Bluetooth Extender file.\n"
            + "Installation aborted.";       
      }
      else if ( !line.contains( btio.getRemoteSignature() ) )
      {
        message = "The extender is not for this remote.\n"
            + "Installation aborted.";  
      }
      if ( message != null )
      {
        JOptionPane.showMessageDialog( null, message, title, JOptionPane.ERROR_MESSAGE );
        return;
      }
      
      progressUpdater.updateProgress( 0 );
      line = rdr.readLine();
      while ( line != null )
      {
        line = line.trim();
        if ( line.length() == 0 )
        {
          if ( sb != null )
          {
            record = new Hex( sb.toString() );
            records.add( record );
          }
          sb = null;
          line = rdr.readLine();
          continue;
        }
        if ( sb == null )
          sb = new StringBuilder();
        sb.append( line + " " );
        line = rdr.readLine();
      }
      if ( sb != null )
      {
        record = new Hex( sb.toString() );
        records.add( record );
      }
    }
    catch ( IOException e )
    {
      e.printStackTrace();
    }
    message = "Installation succeeded.";
    progressUpdater.updateProgress( 10 );
    int count = records.size();
    int n = 0;
    for ( Hex record : records )
    {
      n++;
      int error = btio.sendRecord( record );
      if ( error != 0 )
      {
        message = "Installation failed with error code " + error;
        break;
      }
      progressUpdater.updateProgress( 10 + ( 90 * n ) / count  ); 
    }
    JOptionPane.showMessageDialog( null, message, title, JOptionPane.PLAIN_MESSAGE );
  }
  
  private File file = null;
  private JP2BT btio = null;
  private ProgressUpdater progressUpdater = null;
}
