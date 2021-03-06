/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package model.scenario.oil;

import agents.firm.GeographicalFirm;
import financial.market.GeographicalMarket;
import financial.market.Market;
import financial.market.OrderBookMarket;
import financial.utilities.BuyerSetPricePolicy;
import goods.UndifferentiatedGoodType;
import javafx.beans.property.SimpleIntegerProperty;
import model.MacroII;
import model.scenario.MonopolistScenario;

/**
 * A single labor market, created at initialization.
 * Created by carrknight on 4/21/14.
 */
public class SingleLaborMarketStrategy implements LaborMarketOilScenarioStrategy {

    private final OrderBookMarket laborMarket;

    private final SimpleIntegerProperty laborSupplySlope;

    private final SimpleIntegerProperty laborSupplyIntercept;

    private final SimpleIntegerProperty totalNumberOfWorkers;




    public SingleLaborMarketStrategy() {
        this(1,20,50);
    }


    public SingleLaborMarketStrategy(int laborSupplySlope, int laborSupplyIntercept, int totalNumberOfWorkers)
    {
        this.laborMarket = new OrderBookMarket(UndifferentiatedGoodType.LABOR);
        laborMarket.setPricePolicy(new BuyerSetPricePolicy());

        this.laborSupplySlope = new SimpleIntegerProperty(laborSupplySlope);
        this.laborSupplyIntercept = new SimpleIntegerProperty(laborSupplyIntercept);
        this.totalNumberOfWorkers = new SimpleIntegerProperty(totalNumberOfWorkers);


    }

    @Override
    public void initializeLaborMarkets(OilDistributorScenario scenario, GeographicalMarket oilMarket, MacroII model) {
        //register the market
        model.getGoodTypeMasterList().addNewSector(UndifferentiatedGoodType.LABOR);
        scenario.getMarkets().put(UndifferentiatedGoodType.LABOR,laborMarket);

        //fill labor market
        MonopolistScenario.fillLaborSupply(laborSupplyIntercept.get(),laborSupplySlope.get(),true,
                totalNumberOfWorkers.get(),laborMarket,model);

        //get ready to update the market if any observable changes
        new LaborMarketForOilUpdater(laborSupplySlope,
                laborSupplyIntercept,totalNumberOfWorkers,model,laborMarket);
        //the updater lives on its own, listens and deactivates itself. We really don't even need a reference to it

    }

    @Override
    public Market assignLaborMarketToFirm(GeographicalFirm oilStation, OilDistributorScenario scenario, MacroII model) {
        return laborMarket;
    }






}
