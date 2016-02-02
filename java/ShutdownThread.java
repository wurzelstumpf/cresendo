//**********************************************************************
//
// cresendo - Log when the jvm is shutdown
//
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
