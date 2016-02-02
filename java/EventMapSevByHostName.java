//**********************************************************************
//
// cresendo - EventMapSevByHostName
//
//***********************************************************************

import java.io.File;
import com.tivoli.tec.event_delivery.TECEvent;
import com.ibm.logging.Logger;
import com.ibm.logging.LogRecord;

public class EventMapSevByHostName implements IEventHandler
{
  // Note that the itm6 informational severity appears
  // to be mapped to harmless by itm6 when a corresponding
  // event is sent to TEC
  //
  private String [] tecSev = {
                               "fatal", 
                               "critical",
                               "minor",
                               "warning",
                               "harmless",
                               "unknown"
                             };

  private File mapDir = null;               // Map directory
  private LogRecord msg = null;             // Message record
  private LogRecord trc = null;             // Trace record

  private void initMsgAndTrcRecord()
  {
    msg = new LogRecord(LogRecord.TYPE_WARN, "EventMapSevByHostName", "processEvent", "", "", "", "", "");
    trc = new LogRecord(LogRecord.TYPE_ENTRY, "EventMapSevByHostName", "processEvent", "", "", "", "", "");
  }

  private void stdErr(String m)
  {
    String message = "EventMapSevByHostName: " + m;
    System.err.println("Cresendo: " + message);
    System.exit(1);
  }

  public EventMapSevByHostName(String d) throws Exception
  {
    File baseDir = new File(d);

    initMsgAndTrcRecord();

    // Test whether the base directory exists
    //
    if (! baseDir.exists())
    {
      stdErr("Base directory does not exist: '" + baseDir.getPath() + "'");
    }

    // Test whether the base directory is actually a directory
    //
    if (! baseDir.isDirectory())
    {
      stdErr("Base directory does not appear to be a directory: '" + baseDir.getPath() + "'");
    }

    // Test that base directory is writeable
    //
    if (! baseDir.canWrite())
    {
      stdErr("Unable to write to base directory: '" + baseDir.getPath() + "'");
    }

    // Create map directory which will contain a subdirectory
    // for each tec severity
    //
    try
    {
      mapDir = new File(baseDir, "MapSevByHostName");

      if (! mapDir.exists())      // If the directory does not exist
      {
        if (! mapDir.mkdir())     // Then create the directory
        {
          stdErr("Unable to create directory: '" + mapDir.getPath() + "'");
        }
      }
    }
    catch (Exception e)
    {
      stdErr("Exception: " + e.getMessage());
    }


    // Create the severity directories as subdirectories of the map directory
    //
    for (int i = 0; i < tecSev.length ; i++)
    {
      try
      {
        File sevDir = new File(mapDir, tecSev[i]);

        if (! sevDir.exists())      // If the directory does not exist
        {
          if (! sevDir.mkdir())     // Then create the directory
          {
            stdErr("Unable to create directory: '" + sevDir.getPath() + "'");
          }
        }
      }
      catch (Exception e)
      {
        stdErr("Exception: " + e.getMessage());
      }
    }

  }

  public boolean processEvent(TECEvent ev)
  {
    // Clear previous text in messaage and trace records
    //
    msg.setText("");
    trc.setText("");

    if (Cresendo.trcLogger.isLogging)
    {
      trc.setText("Received event -----> \n  " + ev.toString() );
    }

    // Retrieve the value of the hostname attribute from the event
    //
    String fqhost = ev.getSlot("hostname");

    // Check hostname attribute is not null or empty
    //
    if (fqhost == null || fqhost.length() == 0)
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  BAILING as value of hostname attribute is null or string length is zero \n");
        Cresendo.trcLogger.log(trc);
      }
      return true;    // Allow event handlers to continue processing event
    }

    // Strip any quotes
    //
    fqhost = fqhost.replaceAll("[\'\"]", "");
    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Removed any single or double quotes from value of hostname attribute\n");
      Cresendo.trcLogger.log(trc);
    }

    // Check hostname attribute is not null or empty
    //
    if (fqhost == null || fqhost.length() == 0 || fqhost.matches("^\\s*$"))
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  BAILING as value of hostname attribute is empty after removal of quotes \n");
        Cresendo.trcLogger.log(trc);
      }
      return true;    // Allow event handlers to continue processing event
    }
    
    // Strip off domain name (ie everthing after and including first dot character)
    //
    String host = null;
    int idot = fqhost.indexOf('.');
    if (idot >= 0)                         // A dot was found in the host name
    {
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  Dot character found in hostname  -----> '" + fqhost  + "'\n");
      }

      host = fqhost.substring(0, idot);    // Strip domain name 
      if (Cresendo.trcLogger.isLogging)
      {
        String tmpText = trc.getText();
        trc.setText(tmpText + "\n  Stripped domain name from hostname  -----> '" + host + "'\n");
      }
    }
    else
    {
      host = fqhost;
    }

    host = host.toLowerCase();   // Convert host name to lower case

    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Converted hostname to lower case -----> '" + host + "'\n");
    }

    // Check to see if event severity should be remapped based on the host name.
    // This starts from highest to lowest severity and stops searching as soon
    // as the first matching host name is found.
    //
    for (int i = 0; i < tecSev.length ; i++)
    {
      try
      {
        File sevDir = new File(mapDir, tecSev[i]);
        File hostMap = new File(sevDir, host);

        if (Cresendo.trcLogger.isLogging)
        {
          String tmpText = trc.getText();
          trc.setText(tmpText + "\n  Testing if map file exists '" + hostMap.getPath() + "'\n" );  
        }

        if (hostMap.exists())      // If the host map file exists
        {
          StatusLogger.mapSevByHostName++;

          if (Cresendo.trcLogger.isLogging)
          {
            String tmpText = trc.getText();
            trc.setText(tmpText + "\n  Found host map file '" + hostMap.getPath() + "'\n" +
                                  "\n  Setting event severity to '" + tecSev[i].toUpperCase() + "'\n");
          }

          if (! ev.setSlot("severity", tecSev[i].toUpperCase()))     // Then set the severity of the event
          {
            if (Cresendo.trcLogger.isLogging)
            {
              String tmpText = trc.getText();
              trc.setText(tmpText + "\n  Unable to map severity of event to '" +
                          tecSev[i].toUpperCase() + "' -----> " + ev.toString());
            }
          }

          break;  // Break as soon as a host map file is found
        }
      }
      catch (Exception e)
      {
        if (Cresendo.trcLogger.isLogging)
        {
          String tmpText = trc.getText();
          trc.setText(tmpText + "\n  Exception -----> \n  " + e.getMessage());
        }
      }
    }

    // Write trace file
    //
    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Passing event to next handler -----> \n  " + ev.toString());
      Cresendo.trcLogger.log(trc);
    }

    return true;    // Allow event handlers to continue processing event
  }
}