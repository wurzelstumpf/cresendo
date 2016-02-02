//**********************************************************************
//
// cresendo - EventEngine
//
//***********************************************************************

import com.tivoli.tec.event_delivery.TECEvent;

interface IEventHandler
{
  public boolean processEvent(TECEvent e);
}
