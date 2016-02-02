//**********************************************************************
// cresendo - EventEngine
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
