package com.hifiremote.jp1.io;

public class BLEDisconnecter implements Runnable
{
  private Thread disconnectThread = null;
  private JP2BT btio = null;
  private boolean terminate = false;
  
  public BLEDisconnecter( JP2BT btio )
  {
    this.btio = btio;
    disconnectThread = new Thread( this );
    disconnectThread.start();
  }
  
  public void stop() 
  {
    terminate = true;
  }

  @Override
  public void run()
  {
    while ( !terminate )
    {
      if ( !btio.isDisconnecting() )
      {
        try
        {
          Thread.sleep( 500 );
        }
        catch ( InterruptedException e ) { }
        continue;
      }
      terminate = true;
      btio.getOwner().disconnectBLE();
    }
  }
}
