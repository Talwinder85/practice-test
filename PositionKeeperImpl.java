package au.com.ing.challenge;

import au.com.ing.core.*;

import java.util.HashMap;
import java.util.Map;

public class PositionKeeperImpl implements PositionKeeper {
   private Map<Long,TradeEvent> resultEvents = new HashMap<>();
    @Override
    public void processEvent(Event event) {
        //Your implementation
        if (event instanceof TradeEvent) {
            TradeEvent tradeEvent = (TradeEvent) event;
            if(TradeEventType.NEW.equals(tradeEvent.getTradeEventType())) {
                resultEvents.putIfAbsent(tradeEvent.getTradeId(), tradeEvent.computeTrade());
            }

            if(TradeEventType.AMEND.equals(tradeEvent.getTradeEventType())) {
                TradeEvent e = resultEvents.get(tradeEvent.getTradeId());
                if(tradeEvent.getVersion() > e.getVersion() && !e.isTradeCancelled()) {
                    resultEvents.remove(tradeEvent.getTradeId());
                    resultEvents.put(tradeEvent.getTradeId(), tradeEvent.computeTrade());
                }
            }
        }
        if(event instanceof FxRate) {
            FxRate f = (FxRate)event;

            resultEvents.values().stream()
                                 .filter(e -> e.getCurrencyPair().equals(f.getCurrencyPair()))
                                 .peek(e -> e.setFxRate(f.getRate()))
                                 .forEach(TradeEvent::computeTrade);

        }
        if(event instanceof CancelTradeEvent) {
            CancelTradeEvent c = (CancelTradeEvent)event;
            TradeEvent e = resultEvents.get(c.getTradeId());
            e.setTradeCancelled(true);

            resultEvents.remove(c.getTradeId());
            resultEvents.put(c.getTradeId(), e);
        }
    }


    @Override
    public String printPositions() {
        StringBuilder builder = new StringBuilder();
        resultEvents.values().stream()
                .filter(e -> !e.isTradeCancelled())
                .map(TradeEvent::toString)
                .forEach(e -> builder.append(e).append("\n"));

        return builder.toString();
    }
}
