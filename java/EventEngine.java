//**********************************************************************
//
// cresendo - EventEngine
//
//***********************************************************************

import java.util.Vector;
import java.util.Enumeration;
import com.ibm.logging.Logger;
import com.ibm.logging.LogRecord;
import com.ibm.logging.MessageLogger;
import com.ibm.logging.TraceLogger;
import com.tivoli.tec.event_delivery.IEventProcessing;
import com.tivoli.tec.event_delivery.TECEvent;

public class EventEngine implements IEventProcessing
{
  private Vector<IEventHandler> handler = null;
  private MessageLogger msg = null;
  private TraceLogger trc = null;

  public EventEngine(Vector<IEventHandler> eventHandler, MessageLogger m, TraceLogger t)
  {
    handler = eventHandler;
    msg = m;
    trc = t;
  }

  public boolean onMessage(String events)
  {
    String trcTemp = null;

    TECEvent[] tea = TECEvent.convert(events, msg, trc);

    // For each TECEvent object
    // 
    for (int i = 0; i < tea.length; i++)
    {
      Enumeration ev = handler.elements();

      StatusLogger.receivedCount++;   // Increment the count of received events

      // For each event handler
      //
      while (ev.hasMoreElements())   
      {
        IEventHandler eh = (IEventHandler) ev.nextElement();

        // If the event handler does not return true
        //
        if (! eh.processEvent(tea[i])) 
        {
          break;   // Then don't pass the TECEvent on to the next handler
        }
      }
    }
    return true;  // Let's always be happy
  }
}
