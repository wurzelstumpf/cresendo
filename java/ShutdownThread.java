//**********************************************************************
// cresendo - Log when the jvm is shutdown
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

import com.ibm.logging.MessageLogger;
import com.ibm.logging.LogRecord;

class ShutdownThread extends Thread
{
  private MessageLogger logger;
  private LogRecord record;
  private String instanceName;

  ShutdownThread(MessageLogger log, String instance) {
    this.logger = log;
    this.instanceName = instance;
    this.record = new LogRecord(LogRecord.TYPE_INFO, "Cresendo", "Shutdown", "", "", "", "", "");
  }
  
  public void run()
  {
    // Flush out the status log
    //
    if (Cresendo.statLogger.isLogging())
    {
      try
      {
        Cresendo.statLogger.log();
      }
      catch (Exception ex)
      {
        this.record.setText("\n  An error occurred while writing to '" + Cresendo.statusPath + "'" +
                            "\n  '" + ex.toString() + "'"  );
      }
    }

    // Write shutdown message to the log file
    //
    String tmpMsg = this.record.getText();
    tmpMsg = tmpMsg + "Shutdown instance '" + instanceName + "'";
    this.record.setText(tmpMsg);
    this.logger.log(this.record);
  }
}
