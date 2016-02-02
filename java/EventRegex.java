//**********************************************************************
//
// cresendo - EventRegex
//
//***********************************************************************

import java.util.Enumeration;
import java.util.Arrays;
import com.ibm.logging.Logger;
import com.ibm.logging.LogRecord;
import com.tivoli.tec.event_delivery.TECEvent;

public class EventRegex implements IEventHandler
{
  private String regex = null;        // Regular expression to match parts of the event string
  private String replace = null;      // String to replace parts of matched string
  private String[] attribute = null;  // List of attribute names to consider for regex
  private boolean otherAttrs = false; // Flag to indicate whether to include or exclude attrs from regex
  private LogRecord msg = null;       // Message record
  private LogRecord trc = null;       // Trace record

  private void initAttributes()
  {
    for (int i = 0; i < attribute.length ; i++)
    {
      attribute[i].trim();   // Trim leading and trailing whitespace
    }
  }

  private void initMsgAndTrcRecord()
  {
    msg = new LogRecord(LogRecord.TYPE_WARN, "EventRegex", "processEvent", "", "", "", "", "");
    trc = new LogRecord(LogRecord.TYPE_ENTRY, "EventRegex", "processEvent", "", "", "", "", "");
  }
  
  public EventRegex(String match, String with)
  {
    regex = match;
    replace = with;
    initMsgAndTrcRecord();
  }

  public EventRegex(String match, String with, String attrs)
  {
    regex = match;
    replace = with;
    attribute = attrs.split("[,]");   // Split on commas
    initAttributes();
    initMsgAndTrcRecord();
  }

  public EventRegex(String match, String with, String attrs, boolean other)
  {
    regex = match;
    replace = with;
    attribute = attrs.split("[,]");   // Split on commas
    initAttributes();
    otherAttrs = other;
    initMsgAndTrcRecord();
  }

  public boolean processEvent(TECEvent e)
  {
    // Clear previous text in messaage and trace records
    //
    msg.setText("");
    trc.setText("");

    if (Cresendo.trcLogger.isLogging)
    {
      trc.setText("Received event -----> \n  " + e.toString() +
                  "\n        Matching with regex: '" + regex + "'\n" +
                  "\n      Replacement string is: '" + replace + "'\n"
                   );

      String tmpText = trc.getText();

      if (attribute != null && attribute.length > 0)
      {

        trc.setText(tmpText + "\n  Designated attributes are: '" + Arrays.toString(attribute) + "'\n");

        tmpText = trc.getText();

        if (otherAttrs)
        {
          trc.setText(tmpText + "\n  Designated attributes are excluded from regex processing\n");
        }
        else
        {
          trc.setText(tmpText + "\n  Regex processing is limited to the designated attributes\n");
        }
      }
      else
      {
        trc.setText(tmpText + "\n  All attributes will be processed by regex replacement\n");
      }
    }

    Enumeration enu = e.slots();

    element: while (enu.hasMoreElements())   
    {
      String attr = (String) enu.nextElement();

      // Limit regex to attribute names contained in the attribute array 
      //
      if (attribute != null && attribute.length > 0)
      {
        // For each of the attributes
        //
        for (int j = 0; j < attribute.length; j++)
        {
          if (otherAttrs)  // Flag indicates whether attributes are included or excluded from regex
          {
            if (attr.matches(attribute[j]))
            {
              continue element;  // Don't regex the attribute
            }
          }
          else
          {
            if (! attr.matches(attribute[j]))
            {
              continue element;  // Don't regex the attribute
            }
          }
        }
      }

      String value = e.getSlot(attr);

      e.setSlot(attr, value.replaceAll(regex, replace));  // Perform the regex replace
    }

    // Write trace file
    //
    if (Cresendo.trcLogger.isLogging)
    {
      String tmpText = trc.getText();
      trc.setText(tmpText + "\n  Transformed event to -----> \n  " + e.toString());
      Cresendo.trcLogger.log(trc);
    }

    return true;    // Allow event handlers to continue processing events
  }
}
