//**********************************************************************
// cresendo - EventSend
//
//  Copyright (C) 2008,2009 Mark Matthews
//  Distributed under the terms of the GNU General Public License
//
//  This file is part of Cresendo.
//  Cresendo is free software: you can redistribute it and/or modify
//  it under the terms of the GNU General Public License as published by
//  the Free Software Foundation, either version 3 of the License, or
//  (at your option) any later version.
//  
//  Cresendo is distributed in the hope that it will be useful,
//  but WITHOUT ANY WARRANTY; without even the implied warranty of
//  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//  GNU General Public License for more details.
//  
//  You should have received a copy of the GNU General Public License
//  along with Cresendo.  If not, see <http://www.gnu.org/licenses/>.
//***********************************************************************

import java.io.File;
import java.io.FileReader;
import com.tivoli.tec.event_delivery.TECEvent;
import com.tivoli.tec.event_delivery.TECAgent;
import com.tivoli.tec.event_delivery.EDException;
import com.ibm.logging.Logger;
import com.ibm.logging.LogRecord;

public class EventSend implements IEventHandler
{
  private File cf = null;             // File object holding configuration file
  private TECAgent sendTo = null;     // Sender TECAgent object
  private LogRecord msg = null;       // Message record
  private LogRecord trc = null;       // Trace record

  private void initMsgAndTrcRecord()
  {
    msg = new LogRecord(LogRecord.TYPE_WARN, "EventSend", "processEvent", "", "", "", "", "");
    trc = new LogRecord(LogRecord.TYPE_ENTRY, "EventSend", "processEvent", "", "", "", "", "");
  }
  
  public EventSend(String c) throws Exception
  {
    // Assume config file is in the config directory which is
    // passed as an argument on the commmand line
    //
    cf = new File(Cresendo.configDir + Cresendo.dirSep + c);

    // Check config file exists and is readable
    //
    if (!cf.canRead())
    {
      // Note that throwing exceptions when instaniating with java.lang.reflect isn't
      // a sure-fire way of getting the message to the calling class.  So we pump out
      // an error message on System.err as well
      //
      String error = "Error: Unable to access config file: '" + cf.getPath() + "'";
      System.err.println(error);
      throw new Exception(error);
    }

    FileReader cfReader = new FileReader(cf);

    try
    {
      sendTo = new TECAgent(cfReader ,TECAgent.SENDER_MODE, false);
    }
    catch (EDException e)
    {
      // Note that throwing exceptions when instaniating with java.lang.reflect isn't
      // a sure-fire way of getting the message to the calling class.  So we pump out
      // an error message on System.err as well
      //
      String error = "Error: Unable to instantiate TECAgent Sender using config: '" + cf.getPath() + "'\n" + e.toString();
      System.err.println(error);
      throw new Exception(error);
    }

    initMsgAndTrcRecord();
  }

  public boolean processEvent(TECEvent ev)
  {
    // Clear previous text in messaage and trace records
    //
    msg.setText("");
    trc.setText("");

    if (Cresendo.trcLogger.isLogging)
    {
      trc.setText("Received event -----> \n  " + ev.toString() +
                  "\n  Using config file: '" + cf.getPath() + "'\n"
                   );
    }

    // Note that sendEvent returns:
    //
    //   -1 (SEND_FAILURE), if an error occurs
    //    0 (SEND_FILTERED), if the event was filtered
    //   >0 (number of bytes sent), if success
    //
    int rc = sendTo.sendEvent(TECEvent.normalizeEvEnd(ev.toString(true)));

    switch (rc)
    {
    case -1: StatusLogger.sentFailed++;  break ;
    case  0: StatusLogger.sentFiltered++; break ;
    default: StatusLogger.sentSuccess++; break ;
    }

    // Write trace file
    //
    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Sent event -----> \n  " + ev.toString());
      Cresendo.trcLogger.log(trc);
    }

    return true;    // Allow event handlers to continue processing events
  }
}
