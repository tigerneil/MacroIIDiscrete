/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package model.utilities.stats;

import agents.EconomicAgent;
import agents.people.Person;
import agents.firm.Firm;
import au.com.bytecode.opencsv.CSVWriter;
import financial.market.Market;
import financial.market.OrderBookMarket;
import goods.UndifferentiatedGoodType;
import javafx.collections.FXCollections;
import javafx.collections.ObservableSet;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.stats.collectors.ProducersStatCollector;
import org.junit.Test;

import java.util.LinkedHashSet;

import static org.mockito.Mockito.*;

/**
 * <h4>Description</h4>
 * <p/>
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-07-04
 * @see
 */
public class ProducersStatCollectorTest
{

    @Test
    public void collectStats()
    {
        //create the model and market
        MacroII macroII = mock(MacroII.class);
        Market market = mock(OrderBookMarket.class);
        when(macroII.getMarket(UndifferentiatedGoodType.GENERIC)).thenReturn(market);
        when(market.getGoodType()).thenReturn(UndifferentiatedGoodType.GENERIC);

        //now create 3 sellers, 2 firms and 1 person.
        ObservableSet<EconomicAgent> sellers = FXCollections.observableSet(new LinkedHashSet<>()); //make it linked so it keeps the order
        Firm firm1 = mock(Firm.class); sellers.add(firm1); when(firm1.getName()).thenReturn("uno");
        Firm firm2 = mock(Firm.class); sellers.add(firm2); when(firm2.getName()).thenReturn("due");
        Person p = mock(Person.class); sellers.add(p);
        when(market.getSellers()).thenReturn(sellers);

        //create the stat collector
        CSVWriter prices = mock(CSVWriter.class);
        CSVWriter quantities = mock(CSVWriter.class);
        ProducersStatCollector collector = new ProducersStatCollector(macroII, UndifferentiatedGoodType.GENERIC,
                prices,quantities);
        collector.start();
        //it should have scheduled itself already
        verify(macroII).scheduleSoon(ActionOrder.CLEANUP_DATA_GATHERING,collector);

        //DAY 1: (write header and first observation)
        when(firm1.hypotheticalSellPrice(UndifferentiatedGoodType.GENERIC)).thenReturn(10);
        when(firm2.hypotheticalSellPrice(UndifferentiatedGoodType.GENERIC)).thenReturn(20);
        collector.step(macroII);
        verify(prices).writeNext(new String[]{"uno","due"});   //write header
        verify(prices).writeNext(new String[]{"10","20"}); //write the prices (notice that the person has been ignored!)

        //DAY 2: change the firm list, shouldn't matter
        sellers.add(mock(Firm.class));
        when(firm1.hypotheticalSellPrice(UndifferentiatedGoodType.GENERIC)).thenReturn(30);
        when(firm2.hypotheticalSellPrice(UndifferentiatedGoodType.GENERIC)).thenReturn(20);
        collector.step(macroII);
        verify(prices).writeNext(new String[]{"30","20"}); //write the prices (notice that the person has been ignored!)





    }

}
