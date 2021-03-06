/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package agents.firm.sales;

import agents.EconomicAgent;
import agents.firm.Department;
import agents.firm.Firm;
import agents.firm.production.Plant;
import agents.firm.sales.exploration.BuyerSearchAlgorithm;
import agents.firm.sales.exploration.SellerSearchAlgorithm;
import agents.firm.sales.prediction.ErrorCorrectingSalesPredictor;
import agents.firm.sales.prediction.RegressionSalePredictor;
import agents.firm.sales.prediction.SalesPredictor;
import agents.firm.sales.pricing.AskPricingStrategy;
import agents.firm.sales.pricing.decorators.AskReservationPriceDecorator;
import agents.firm.utilities.LastClosingPriceEcho;
import agents.firm.utilities.PriceAverager;
import com.google.common.base.Preconditions;
import ec.util.MersenneTwisterFast;
import financial.market.Market;
import financial.utilities.ActionsAllowed;
import financial.utilities.PurchaseResult;
import financial.utilities.Quote;
import goods.Good;
import goods.GoodType;
import goods.UndifferentiatedGoodType;
import javafx.beans.value.ObservableDoubleValue;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.logs.*;
import model.utilities.stats.collectors.SalesData;
import model.utilities.stats.collectors.enums.SalesDataType;
import sim.engine.SimState;
import sim.engine.Steppable;

import java.util.*;

/**
 * <h4>Description</h4>
 * <p/> This is the class representing the sales department
 * <p/> Chiefly it takes care of three tasks:
 * <ul>
 *     <li> Responds to a customer approaching the firm willing to buy a good</li>
 *     <li> Be tasked by the firm to sell something</li>
 *     <li> Be asked to search the market for the best price to sell to</li>
 * </ul>
 * <p/>
 * The subclasses are different in the way they respond to HQ when asked what is next price going to be.
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-04-07
 * @see
 */
public abstract class  SalesDepartment  implements Department<SalesDataType>, LogNode {



    protected final SaleQuotesManager quotesManager;


    /**
     * The firm where the sales department beints
     */
    private final Firm firm;
    private final MacroII model;


    /**
     * a list of all listeners to nofify
     */
    private LinkedList<SalesDepartmentListener> salesDepartmentListeners;
    /**
     * The market the sales department deals in
     */
    protected final Market market;
    /**
     * The procedure used by the sales department to search the market.
     */
    protected BuyerSearchAlgorithm buyerSearchAlgorithm;
    protected SellerSearchAlgorithm sellerSearchAlgorithm;

    public static final Class<? extends  SalesPredictor> defaultPredictorStrategy =
            ErrorCorrectingSalesPredictor.class;

    /**
     * This is the strategy to predict future sale prices when the order book is not visible.
     */
    protected SalesPredictor predictorStrategy;
    /**
     * This is the strategy used by the sales department to choose its price
     */
    protected AskPricingStrategy askPricingStrategy;
    /**
     * goods sold today. Reset every day at PREPARE_TO_TRADE step
     */
    private int todayOutflow;
    /**
     * goods that were given to us to sell today
     */
    private int todayInflow;


    /**
     * How many days will inventories last at the current flow? It is 0 if there is no inventory, MAX_VALUE if there are more inflows than outflows!
     */
    private float daysOfInventory = 0;
    /**
     * this flag is set to true whenever the first sellThis is called. It is never set to false
     */
    private boolean started = false;
    /**
     * This is the price of the last good the sales department managed to sell
     */
    private int lastClosingPrice = -1;

    /**
     * average last week price weighted by outflow
     */
    private PriceAverager priceAverager = new LastClosingPriceEcho();



    /**
     * This is the cost of the last good the sales department managed to sell
     */
    private int lastClosingCost = -1;

    private int lastAskedPrice = -1;
    /**
     * When this is true, the sales department peddles its goods around when it can't make a quote.
     * If this is false and the sales department can't quote, it just passively wait for buyers
     */
    protected boolean canPeddle = false;

    private boolean aboutToUpdateQuotes = false;

    private SalesData data;
    private List<Good> goodsToRequote;

    public SalesDepartment(SellerSearchAlgorithm sellerSearchAlgorithm, Market market,  MacroII model, Firm firm, BuyerSearchAlgorithm buyerSearchAlgorithm) {
        data = new SalesData();
        this.logNode = new LogNodeSimple();
        this.sellerSearchAlgorithm = sellerSearchAlgorithm;
        this.market = market;

        final GoodType typeSold = market.getGoodType();
        if(typeSold.isDifferentiated()) //create the proper sale quote manager!
            quotesManager = new DifferentiatedSaleQuotesManager();
        else
            quotesManager = new UndifferentiatedSaleQuotesManager((UndifferentiatedGoodType)typeSold);


        this.model = model;
        this.firm = firm;
        predictorStrategy = RegressionSalePredictor.Factory.newSalesPredictor(defaultPredictorStrategy,this);
        logNode.listenTo(predictorStrategy);
        this.buyerSearchAlgorithm = buyerSearchAlgorithm;
        salesDepartmentListeners = new LinkedList<>();
        market.registerSeller(firm); //register!

    }

    /**
     * A buyer asks the sales department for the price they are willing to sell one of their good.
     * @param buyer the economic agent that asks you that
     * @return a price quoted or -1 if there are no quotes
     */
    public Quote askedForASalePrice(EconomicAgent buyer){
        int priceQuoted = Integer.MAX_VALUE;
        if(!hasAnythingToSell()){ //if you have nothing to sell
            //was the seller rich enough to buy something if we had any?
            if(lastClosingCost >=0) //if we ever sold something before
                if(buyer.maximumOffer(hypotheticalGood(lastClosingCost)) >= lastClosingPrice)
                    //then tell the listeners this is a stockout
                    for(SalesDepartmentListener listener : salesDepartmentListeners)
                        listener.stockOutEvent(getFirm(),this,buyer);

            return Quote.emptySellQuote(null);
        }


        if(market.getSellerRole() == ActionsAllowed.QUOTE) //if you are in an environment for quoting
        {
            //find your own lowest quote

            return getCheapestQuote(); //return lowest quoted price
        }
        else
        {
            Good goodQuoted = null;
            //you don't have a list of quotes; go through each toSell and price them
            for(Good g : listOfGoodsToSell()){
                int price = price(g); //the price of this good
                assert price >=0; //can't be -1!
                if(price < priceQuoted) //if that's the lowest, quote that
                {
                    priceQuoted = price;
                    goodQuoted = g;
                }
            }

            assert goodQuoted != null; //can't be null because the list toSell is not empty!
            assert priceQuoted < Integer.MAX_VALUE;
            //return it!
            Quote q = Quote.newSellerQuote(getFirm(),priceQuoted,goodQuoted);
            q.setOriginator(this);
            return q;

        }
    }


    /**
     * This method may be called by the firm to ask the sales department to predict what the sell price for a new good may be (usually to guide production). <br>
     * It works in 2 steps: <ul>
     *     <li> If quotes are visible in the market, just return the best quote</li>
     *     <li> Otherwise use the department pricePredictor</li>
     * </ul>
     *
     *
     * @param expectedProductionCost the HQ estimate of costs in producing whatever it wants to sell. It isn't necessarily used.
     * @param increaseStep by how much does production increase?
     * @return the best offer available or -1 if there are no quotes
     */
    public float predictSalePriceAfterIncreasingProduction(int expectedProductionCost, int increaseStep)
    {
        Preconditions.checkArgument(increaseStep >= 0);



        return predictorStrategy.predictSalePriceAfterIncreasingProduction(this, expectedProductionCost, increaseStep);
    }

    /**
     * This is called by the firm when it wants to predict the price they can sell to if they increase production
     *
     * @param expectedProductionCost the HQ estimate of costs in producing whatever it wants to sell. It isn't necessarily used.
     * @param decreaseStep by how much daily production will decrease
     * @return the best offer available/predicted or -1 if there are no quotes/good predictions
     */
    public float predictSalePriceAfterDecreasingProduction(int expectedProductionCost, int decreaseStep) {
        Preconditions.checkArgument(decreaseStep >= 0);

        return predictorStrategy.predictSalePriceAfterDecreasingProduction(this, expectedProductionCost, decreaseStep);
    }

    /**
     * The sales department is asked at what should be the sale price for a specific good; this, I guess, is the fundamental
     * part of the sales department
     * <p>
     *     This method doesn't check if the department actually owns this good.
     * </p>
     *
     * @param g the good to price
     * @return price of the good
     */
    public int price(Good g){
        return askPricingStrategy.price(g);
    }

    /**
     * This is the method called by the firm when it tasks the department to sell a specific good!  <p>
     * The way I see it, most likely you just want to implement shouldIPeddle, peddle and price. Those are the really big ones. <br>
     * This method calls them, especially price(); placing a quote is straightforward and so implemented here.
     * @param g the good the department needs to sell!
     */
    public void sellThis(final Good g)
    {

        //preconditions
        assert firm.has(g); //we should be selling something we have!
        assert !market.getGoodType().isDifferentiated() ||!listOfGoodsToSell().contains(g); //if differentiated, we this good should already be in our sell list
        assert market.getGoodType().equals(g.getType());
        recordThisGoodAsSellable(g);


        //log it (this also fires listeners)
        logInflow(1);
        newGoodsToSellEvent(g);
    }

    public void sellThese(int amount){
        Preconditions.checkArgument(!market.getGoodType().isDifferentiated());

        recordTheseGoodsAsSellable(amount);
        logInflow(amount);
        newGoodsToSellEvent(amount);


    }

    private void recordTheseGoodsAsSellable(int amount) {
        quotesManager.recordTheseGoodsAsSellable(amount);
    }


    /**
     * The real difference in sales departments is just how they handle new goods to sell!
     * @param g
     */
    protected abstract void newGoodsToSellEvent(Good... g);

    /**
     * The real difference in sales departments is just how they handle new goods to sell!
     * @param g
     */
    protected abstract void newGoodsToSellEvent(int amount);





    /**
     * Does three things: Logs the event that we were tasked to sell a good, tell the listeners about this event and if this was teh very
     * first good we have to sell, schedule daily a beginningOfTheDayStatistics() call
     * @param amount
     */
    private void logInflow(int amount) {
        assert started;

        //tell the listeners about it
        for(SalesDepartmentListener listener : salesDepartmentListeners)
            listener.sellThisEvent(firm,this, amount);




        todayInflow+=amount;
        handleNewEvent(new LogEvent(this, LogLevel.TRACE,"Tasked to sell"));







    }


    public void start(MacroII model)
    {
        started = true;
        data.start(model,this);
        model.scheduleSoon(ActionOrder.DAWN,new Steppable() {
            @Override
            public void step(SimState state) {
                beginningOfTheDayStatistics();

            }
        });
        model.scheduleSoon(ActionOrder.ADJUST_PRICES,new Steppable() {
            @Override
            public void step(SimState state) {
                if(!isActive())
                    return;

                priceAverager.endOfTheDay(SalesDepartment.this);
                model.scheduleTomorrow(ActionOrder.ADJUST_PRICES,this);

            }
        });


    }

    /**
     * this method resets the daily counters of inflow and outflow and if the firm is active, reschedule itself assuming
     * this was called when the phase was DAWN .
     * It also computes the "days of inventory"
     */
    private void beginningOfTheDayStatistics() {
        if(!firm.isActive())
            return;



        float netflow = todayOutflow - todayInflow;
        if(numberOfGoodsToSell() == 0)   //no inventory, days of inventory is 0
            daysOfInventory = 0;
        else if(netflow > 0) //positive netflow (we sell more than we produce, that's great)
            daysOfInventory = ((float) numberOfGoodsToSell()) / netflow;
        else //negative or 0 netflow, days of inventory is infinite
            daysOfInventory = Float.MAX_VALUE;





        //reset
        todayInflow = 0;
        todayOutflow = 0;
        lastAskedPrice= -1;
        //if you still have a quote active, use it as your "last asked price"
        if(numberOfQuotesPlaced() > 0)
            lastAskedPrice = getAnyPriceQuoted();

        model.scheduleTomorrow(ActionOrder.DAWN,new Steppable() {
            @Override
            public void step(SimState state) {

                beginningOfTheDayStatistics();
            }
        });


    }



    /**
     * Schedule yourself to peddle when you can
     * @param g the good to sell
     */
    protected void peddle(final Good g) {
        model.scheduleSoon(ActionOrder.TRADE,new Steppable() {
            @Override
            public void step(SimState state) {
                //if we are here it means that the market didn't allow us to quote; this means that we should peddle
                boolean success = peddleNow(g);
                if(success){               //did we manage to sell?
                    assert !firm.has(g);

                    //done!

                }
                else{
                    //we didn't manage to sell!
                    assert firm.has(g);
                    //shall we try again?
                    double tryAgainIn = tryAgainNextTime(g);
                    if(tryAgainIn > 0)   //if we do want to try again
                        firm.getModel().scheduleSoon(ActionOrder.TRADE, new Steppable() {
                            @Override
                            public void step(SimState simState) {     //schedule to peddle again!
                                peddleNow(g);
                            }
                        });
                }
            }
        });

    }

    /**
     * Place an ask in the order book
     * @param g the good to quote
     */
    protected void prepareToPlaceAQuote(final Good g)
    {
        Preconditions.checkState(getFirm().has(g));


        model.scheduleSoon(ActionOrder.TRADE, state -> placeAQuoteNow(g));



    }

    protected void placeAQuoteNow(Good g) {
        Preconditions.checkState(getFirm().has(g));

        int price = price(g);
        lastAskedPrice = price;
        handleNewEvent(new LogEvent(this, LogLevel.TRACE,"submitted a sell quote at price:{}",price));
        Quote q = market.submitSellQuote(firm,price,g, this); //put a quote into the market
        if(q.getPriceQuoted() != -1) //if the quote is not null
        {
            //if the quote is not null, we quoted but not sold
            assert q.getAgent() == firm; //make sure we got back the right quote
            recordQuoteAssociatedWithThisGood(g, q);


            if(shouldIPeddle(q))    //do you want to try and peddle too?
                peddleNow(q.getGood()); //then peddle!

        }
        else{
            //if we are here, the quote returned was null which means that we already sold the good!
            assert !firm.has(g); //shouldn't be ours anymore!
            assert q.getAgent() == null; //should be null

        }
    }



    /**
     * This is called by a buyer that is shopping at this department. This means that it is not going through quotes and I assume the buyer had a quote from this department to make a choice
     * @param buyerQuote the quote of the buyer, it must have at least a goodtype
     * @param sellerQuote our quote, we must have given it previously
     */
    public PurchaseResult shopHere(Quote buyerQuote, Quote sellerQuote)
    {
        assert buyerQuote.getType().equals(market.getGoodType()); //it's of the right type, correct?
        Preconditions.checkArgument(sellerQuote.getOriginator().equals(this), "We didn't make this quote");


        /***************************************************************************
         * Choose the good to trade
         ***************************************************************************/
        Good g;
        //did we quote a specific good?
        if(sellerQuote.getGood() != null)
        {
            //then trade it!
            g = sellerQuote.getGood();
            assert listOfGoodsToSell().contains(g); assert getFirm().has(g); //make sure we own it and we can sell it

        }
        else
        {
            assert false : "This should never happen as int as I don't introduce displayPrice";
            //if we didn't quote a specific good

            if(!hasAnythingToSell()) //if we have none, tell them it's a stockout
            {
                assert false : "This should never happen as int as I don't introduce displayPrice";

                //tell all your listeners
                for(SalesDepartmentListener listener : salesDepartmentListeners)
                    listener.stockOutEvent(getFirm(),this,buyerQuote.getAgent());
                //return a stockout
                return PurchaseResult.STOCKOUT;

            }
            else //otherwise it's the cheapest possible
                g = Collections.min(listOfGoodsToSell(),new Comparator<Good>() {
                    @Override
                    public int compare(Good o1, Good o2) {
                        return Integer.compare(o1.getLastValidPrice(),o2.getLastValidPrice());

                    }
                });
        }
        //we are here, good is not null
        assert  g != null;

        /*********************************************************************
         * TRADE
         *********************************************************************/
        assert sellerQuote.getPriceQuoted() >= 0 ; //can't be negative!!!
        assert buyerQuote.getPriceQuoted() >= sellerQuote.getPriceQuoted(); //the price offers must cross

        int finalPrice = market.price(sellerQuote.getPriceQuoted(),buyerQuote.getPriceQuoted());

        //exchange hostages
        market.trade(buyerQuote.getAgent(),getFirm(),g,finalPrice,buyerQuote,sellerQuote);
        handleNewEvent(new LogEvent(this, LogLevel.TRACE,"Sold good at price:{} through buyFromHere()",finalPrice));
        /********************************************************************
         * Record information
         *******************************************************************/
        assert isThisGoodBeingSold(g);
        Quote oldQuote = stopSellingThisGoodAndReturnItsAssociatedQuote(g);
        if(oldQuote != null)
        {
            removeQuoteFromMarket(oldQuote);
        }
        assert !isThisGoodBeingSold(g) || !g.getType().isDifferentiated();
        logOutflow(g, finalPrice);
        PurchaseResult toReturn =  PurchaseResult.SUCCESS;
        toReturn.setPriceTrade(finalPrice);
        return toReturn;


    }



    /**
     * This is called by the owner to tell the department to stop selling this specific good because it
     * was consumed/destroyed.
     * In the code all that happens is that reactToFilledQuote is called with a negative price as second argument
     * @param g the good to stop selling
     * @return true if, when this method was called, the sales department removed an order from the market. false otherwise
     */
    public boolean stopSellingThisGoodBecauseItWasConsumed(Good g)
    {


        //remove it from the masterlist
        Quote q = removeFromToSellMasterlist(g);
        boolean toReturn = false;
        //if needed, remove the quote
        if( q != null)
        {
            //remove the good quoted in the market (and also remove it from memory)
            removeQuoteFromMarket(q);
            toReturn = true;
        }
        else if( aboutToUpdateQuotes){
            goodsToRequote.remove(g);
        }


        //make sure there is no trace in the tosell and  wait list
        assert !g.getType().isDifferentiated() ||!isThisGoodBeingSold(g);



        return toReturn;




    }

    /**
     * call this AFTER you remove the good from the goodQuoted map to remove from the market as well
     */
    private void removeQuoteFromMarket(Quote q) {
        market.removeSellQuote(q);
        assert numberOfQuotesPlaced() >= 0;
    }

    /**
     * This is called automatically whenever a quote we made was filled
     * @param filledQuote
     * @param g the good sold
     * @param price the price for which it sold, the price must be positive or 0. If it's negative it is assumed that the good was consumed rather than sold.
     */
    public void reactToFilledQuote(Quote filledQuote, Good g, int price, EconomicAgent buyer){

        Preconditions.checkArgument(price>=0);

        //remove it from the masterlist
        thisQuoteHasBeenFilledSoRemoveItWithItsAssociatedGood(filledQuote);

        //if the price is 0 or positive it means it was a sale, so you should register/log it as such
        logOutflow(g, price);

        //make sure there is no trace in the tosell and  wait list
        assert !g.getType().isDifferentiated() ||!isThisGoodBeingSold(g);


    }




    protected Quote removeFromToSellMasterlist(Good g) {
        assert !g.getType().isDifferentiated() || !firm.has(g); //if it's differentiated then we surely don't have it
        Preconditions.checkState(isThisGoodBeingSold(g),"Removed a good I didn't have!");
        Quote quoteRemoved = stopSellingThisGoodAndReturnItsAssociatedQuote(g);//remove it from the list of tosell
        assert !g.getType().isDifferentiated() || !isThisGoodBeingSold(g);
        return quoteRemoved;
    }

    /**
     * After having removed the good from the sales department toSell, this records it as a sale and tell the listeners
     */
    private void logOutflow(Good g, int price)
    {
        assert !g.getType().isDifferentiated() ||!firm.has(g); //you can't have it if you sold it!
        assert !g.getType().isDifferentiated() ||!isThisGoodBeingSold(g); //you should have removed it!!!

        //finally, register the sale!!
        //       newResult.setPriceSold(price); //record the price of sale
        lastClosingPrice = price;

        //tell the listeners!
        fireGoodSoldEvent(g,price);

        //log it
        handleNewEvent(new LogEvent(this, LogLevel.TRACE, "sold at price:{}", price));


        todayOutflow++;




    }

    /**
     * The market asks you to quote, and you did. Do you want to try and peddle as well?
     * @param q the quote made in the market
     * @return true if you want to peddle too
     */
    boolean shouldIPeddle(Quote q){
        return false; //TODO fix this
    }

    /**
     * This is called by sellThis if the market doesn't allow the seller to quote or the seller wants to peddle anyway. <br>
     *     Basically it's just a search. <p>
     *         It's this method responsibility to record the salesResults
     * @param g the good to sell
     * @return true if the peddling was successful
     */
    public boolean peddleNow(Good g)
    {
        assert firm.has(g); //should be owned by us, now
        assert isThisGoodBeingSold(g); //should be owned by us, now



        //look for a buyer
        EconomicAgent buyer = buyerSearchAlgorithm.getBestInSampleBuyer(); //call the search algorithm for that
        int priceAsked;
        if(getQuoteAssociatedWithThisGood(g) == null){   //if it's not quoted, compute its price
            priceAsked = price(g);
        }
        else{
            priceAsked =  getQuoteAssociatedWithThisGood(g).getPriceQuoted();  //otherwise your asking price will be whatever was in the quote
        }

        int priceBuyer = buyer.maximumOffer(g);
        if(priceAsked <= priceBuyer )
        {
            //price is somewhere in the middle
            int finalPrice = market.getPricePolicy().price(priceAsked ,priceBuyer);
            assert finalPrice>=priceAsked;
            assert finalPrice<=priceBuyer;

            //create fake quote objects, these might be useful for record keeping and such
            Quote buyerQuote = Quote.newBuyerQuote(buyer,priceBuyer,market.getGoodType());
            Quote sellerQuote = Quote.newSellerQuote(getFirm(),priceAsked,g);
            sellerQuote.setOriginator(this);

            PurchaseResult result = market.trade(buyer,this.firm,g,finalPrice,buyerQuote,sellerQuote); //TRADE!
            if(result == PurchaseResult.SUCCESS)
            {
                //we made it!
                assert !firm.has(g);  //we sold it, right?
                Quote q = stopSellingThisGoodAndReturnItsAssociatedQuote(g); //if you had a quote, remove it
                assert !isThisGoodBeingSold(g);

                //remove it from the market too, if needed
                if(q!=null)
                    removeQuoteFromMarket(q);
                lastClosingPrice = finalPrice;

                buyerSearchAlgorithm.reactToSuccess(buyer,result); //tell the search algorithm

                //tell the listeners!
                fireGoodSoldEvent(g,finalPrice);
                logOutflow(g,finalPrice);

                return true;

            }
            else
            {
                //we failed!
                assert firm.has(g);  //we still have it, uh?

                buyerSearchAlgorithm.reactToFailure(buyer,result);
                return false;

            }


        }
        //we failed
        assert firm.has(g);  //we still have it, uh?
        //they rejected our price!
        buyerSearchAlgorithm.reactToFailure(buyer, PurchaseResult.PRICE_REJECTED);
        return false;



    }



    /**
     * Whenever we can ONLY peddle and we fail to do so, we call this method to know how much to wait before trying again
     * @param g the good to sell
     * @return the wait time, or -1 if we are NOT going to try again!
     */
    final double tryAgainNextTime(Good g){
        return getFirm().getModel().getPeddlingSpeed();

    }

    /**
     * Weekend is a magical time when we record all quoted but unsold goods as unsold and then compute statistics and move on.
     */
    public void weekEnd(double time){


        if(MacroII.SAFE_MODE) //if safe mode is on
        {
            additionalDiagnostics();
        }


        askPricingStrategy.weekEnd();



    }

    private void additionalDiagnostics() {
        for(Good g : listOfGoodsToSell())
        {
            assert firm.has(g); //unsold should still be in inventory
        }
    }

    /**
     * If this is called, we add a reservation price decorators to the ask pricing object.
     * This is a ugly hack utility method until I finally give up and do injection dependency properly <br>
     * It can't be removed.
     */
    public void addReservationPrice(int reservationPrice){
        Preconditions.checkState(askPricingStrategy != null, "Can't add reservation prices until ");
        askPricingStrategy = new AskReservationPriceDecorator(askPricingStrategy,reservationPrice);
    }

    /**
     * @return the value for the field firm.
     */
    public Firm getFirm() {
        return firm;
    }

    /**
     * @return the value for the field market.
     */
    public Market getMarket() {
        return market;
    }

    /**
     * @return the value for the field buyerSearchAlgorithm.
     */
    public BuyerSearchAlgorithm getBuyerSearchAlgorithm() {
        return buyerSearchAlgorithm;
    }

    /**
     * @return the value for the field sellerSearchAlgorithm.
     */
    SellerSearchAlgorithm getSellerSearchAlgorithm() {
        return sellerSearchAlgorithm;
    }

    /**
     * Setter for field askPricingStrategy.
     *
     * @param askPricingStrategy the value to set for the field.
     */
    public void setAskPricingStrategy(AskPricingStrategy askPricingStrategy) {

        if(this.askPricingStrategy != null) {
            this.askPricingStrategy.turnOff();
            this.stopListeningTo(askPricingStrategy);
        }
        this.askPricingStrategy = askPricingStrategy;
        this.listenTo(askPricingStrategy);

    }

    /**
     * Looks for the price of the best competitor this department could find!
     * @return the price found or -1 if there is none
     */
    public int getLowestOpponentPrice()  {



        try
        {
            if(getMarket().isBestSalePriceVisible())         //can we just see it from the order book?
            {
                EconomicAgent bestSeller = getMarket().getBestSeller();

                if(bestSeller == this.getFirm()) //if our firm is the best, just search at random
                {
                    EconomicAgent opponent = getSellerSearchAlgorithm().getBestInSampleSeller();
                    if(opponent != null)
                        return opponent.askedForASaleQuote(getFirm(), getMarket().getGoodType()).getPriceQuoted();
                    else
                        return -1;
                }
                else //if we aren't the best, return the market best
                    return getMarket().getBestSellPrice(); //if so, return it
            }
            else{
                //otherwise search for it
                EconomicAgent opponent = getSellerSearchAlgorithm().getBestInSampleSeller();
                if(opponent == null)
                    return -1;
                return opponent.askedForASaleQuote(getFirm(), getMarket().getGoodType()).getPriceQuoted();
            }


        }catch (IllegalAccessException e){assert false; System.exit(-1); return -1;}            //this won't happen4
    }







    /**
     * last closing price
     * @return
     */
    public int getLastClosingPrice() {
        return lastClosingPrice;
    }

    public SalesPredictor getPredictorStrategy() {
        return predictorStrategy;
    }

    public void setPredictorStrategy(SalesPredictor predictorStrategy) {
        //turn off the previous one if needed
        if(this.predictorStrategy != null) {
            this.predictorStrategy.turnOff();
            logNode.stopListeningTo(this.predictorStrategy);
        }

        this.predictorStrategy = predictorStrategy;
        logNode.listenTo(this.predictorStrategy);
    }







    /**
     *
     *
     * This function is called by strategies that "adjust" when they changed their opinion of what the price should be.
     * The seller remove all its quotes and place them again on the market
     */
    public void updateQuotes()
    {
        aboutToUpdateQuotes=true;

        //get all the quotes to remove
        final Collection<Quote> allQuotes = new LinkedList<>(getAllQuotes());
        if(allQuotes.isEmpty()) {
            assert numberOfQuotesPlaced() == 0;
            return;

        }
        assert numberOfQuotesPlaced() > 0;
        goodsToRequote = new LinkedList<>();

        //forget the old quotes
        for(Quote q: allQuotes)
        {
            if(q != null)
            {
                forgetTheQuoteAssociatedWithThisGood(q.getGood());
                removeQuoteFromMarket(q);
            }
            goodsToRequote.add(q.getGood());
        }



        assert numberOfQuotesPlaced() == 0 : "quotesCurrentlyPlaced: " + numberOfQuotesPlaced();

        //when you can, requote
        if(goodsToRequote.size() > 0)
            model.scheduleSoon(ActionOrder.TRADE, state -> {
                aboutToUpdateQuotes=false;
                //go through all the old quotes
                if(market.getGoodType().isDifferentiated())
                {
                    for (Good g : goodsToRequote) {
                        if (firm.has(g)) //it might have been consumed it in the process for whatever reason
                            //resell it tomorrow
                            newGoodsToSellEvent(g);//sell it again
                    }
                }
                else
                {
                    final int howManyToRequote = Math.min(goodsToRequote.size(), quotesManager.numberOfGoodsToSell());
                    assert howManyToRequote>=0;
                    if(howManyToRequote > 0) //don't bother running the rest if you have nothing to update really (because you consumed it all)
                        newGoodsToSellEvent(howManyToRequote);
                }


            });








    }



    public int getLastClosingCost() {
        return lastClosingCost;
    }

    /**
     * This method turns off all sub-components and clears all data structures
     */
    public void turnOff(){
        data.turnOff();
        askPricingStrategy.turnOff(); askPricingStrategy = null;
        buyerSearchAlgorithm.turnOff(); buyerSearchAlgorithm = null;
        sellerSearchAlgorithm.turnOff(); sellerSearchAlgorithm = null;
        if(predictorStrategy!= null)
            predictorStrategy.turnOff(); predictorStrategy = null;
        removeAllQuotes();
        assert salesDepartmentListeners.isEmpty(); //hopefully it is clear by now!
        market.deregisterSeller(getFirm());
        salesDepartmentListeners.clear();
        salesDepartmentListeners = null;
        quotesManager.turnOff();

        logNode.turnOff();

    }



    /**
     * Get the randomizer of the owner
     */
    public MersenneTwisterFast getRandom() {
        return model.getRandom();
    }

    /**
     * Appends the specified element to the end of this list.
     *
     *
     * @param salesDepartmentListener element to be appended to this list
     * @return {@code true} (as specified by {@link java.util.Collection#add})
     */
    public boolean addSalesDepartmentListener(SalesDepartmentListener salesDepartmentListener) {
        return salesDepartmentListeners.add(salesDepartmentListener);
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * {@code i} such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns {@code true} if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return {@code true} if this list contained the specified element
     */
    public boolean removeSalesDepartmentListener(SalesDepartmentListener o) {
        return salesDepartmentListeners.remove(o);
    }

    /**
     * Get an unmodifiable view to the listeners
     * @return
     */
    public List<SalesDepartmentListener> getSalesDepartmentListeners() {
        return Collections.unmodifiableList(salesDepartmentListeners);
    }


    /**
     * How many goods are left to be sold by the department
     * @return goods to sell
     */
    public int getHowManyToSell(){
        return numberOfGoodsToSell();
    }

    /**
     * Notify the listeners and the logger/gui that a good was sold! Also count it among the daily goods sold
     */
    void fireGoodSoldEvent(Good good, int price){



        for(SalesDepartmentListener listener : salesDepartmentListeners)
            listener.goodSoldEvent(this, price);



    }

    /**
     * When this is true, the sales department peddles its goods around when it can't make a quote.
     * If this is false and the sales department can't quote, it just passively wait for buyers
     */
    public boolean isCanPeddle() {
        return canPeddle;
    }

    /**
     * When this is true, the sales department peddles its goods around when it can't make a quote.
     * If this is false and the sales department can't quote, it just passively wait for buyers
     */
    public void setCanPeddle(boolean canPeddle) {
        this.canPeddle = canPeddle;
    }

    /**
     * Gets goods sold today. Reset every day at PREPARE_TO_TRADE step.
     *
     * @return # of goods sold today. Reset every day at DAWN
     */
    public int getTodayOutflow() {
        return todayOutflow;
    }

    /**
     * Gets goods that were given to us to sell today.   Reset every day at DAWN
     *
     * @return # of goods that were given to us to sell today.
     */
    public int getTodayInflow() {
        return todayInflow;
    }

    /**
     * alias for today outflow
     *
     * @return the today outflow
     */
    @Override
    public int getTodayTrades() {
        return getTodayOutflow();
    }

    /**
     * Asks the sale department if its current inventory is where it should be according to the ask pricing strategy
     * @return
     */
    public boolean isInventoryAcceptable() {
        return askPricingStrategy.isInventoryAcceptable(numberOfGoodsToSell());
    }

    /**
     * little flag that is true whenever quotes are about to be updated  (we scheduled updateQuotes())
     * @return
     */
    public boolean isAboutToUpdateQuotes() {
        return aboutToUpdateQuotes;
    }


    public GoodType getGoodType() {
        return market.getGoodType();
    }

    /**
     * Get the owner's plants you are supposed to sell the products of
     * @return
     */
    public List<Plant> getServicedPlants()
    {
        return firm.getListOfPlantsProducingSpecificOutput(market.getGoodType());
    }


    /**
     * asks the seller if it is selling a specific good
     * @param o the good supposed to be sold by this department
     * @return true if the sales department masterlist contains that good
     */
    public boolean isSelling(Good o) {
        return isThisGoodBeingSold(o);
    }

    /**
     * this is a "utility" method that should be used sparingly. What it does is it creates a mock good, passes it to the pricing department
     * and asks for a price. It is no guarantee that the firm actually will charge such price when a real good gets created.
     * @return
     */
    public int hypotheticalSalePrice(){
        return hypotheticalSalePrice(0);
    }




    /**
     * this is a "utility" method that should be used sparingly. What it does is it creates a mock good, passes it to the pricing department
     * and asks for a price. It is no guarantee that the firm actually will charge such price when a real good gets created.
     * @param productionCost the hypothetical cost of production of this good
     * @return
     */
    public int hypotheticalSalePrice(int productionCost){
        Good imaginaryGood = hypotheticalGood(productionCost);
        return price(imaginaryGood);
    }

    protected Good hypotheticalGood(int productionCost) {
        if(getGoodType().isDifferentiated())
            return Good.getInstanceOfDifferentiatedGood(getGoodType(),getFirm(),productionCost);
        else
            return Good.getInstanceOfUndifferentiatedGood(getGoodType());
    }

    public MacroII getModel() {
        return model;
    }

    /**
     * This is somewhat similar to rate current level. It estimates the excess (or shortage)of goods sold. It is basically
     * getCurrentInventory-AcceptableInventory or getCurrentFlow-acceptableFlow if that's what the seller is targeting.
     * @return positive if there is an excess of goods bought, negative if there is a shortage, 0 if you are right on target.
     */
    public float estimateSupplyGap() {
        return askPricingStrategy.estimateSupplyGap();
    }

    /**
     * Count all the workers at plants that produce a specific output
     * @return the total number of workers
     */
    public int getTotalWorkersWhoProduceThisGood() {
        return firm.getNumberOfWorkersWhoProduceThisGood(getGoodType());
    }


    /**
     * returns a copy of all the observed last prices so far!
     */
    public double[] getAllRecordedObservations(SalesDataType type) {
        return data.getAllRecordedObservations(type);
    }

    /**
     * utility method to analyze only specific days
     */
    public double[] getObservationsRecordedTheseDays(SalesDataType type,  int[] days) {
        return data.getObservationsRecordedTheseDays(type, days);
    }

    /**
     * utility method to analyze only specific days
     */
    public double[] getObservationsRecordedTheseDays(SalesDataType type, int beginningDay, int lastDay) {
        return data.getObservationsRecordedTheseDays(type, beginningDay, lastDay);
    }

    /**
     * utility method to analyze only  a specific day
     */
    public double getObservationRecordedThisDay(SalesDataType type, int day) {
        return data.getObservationRecordedThisDay(type, day);
    }

    /**
     * return the latest price observed
     */
    public Double getLatestObservation(SalesDataType type) {
        return data.getLatestObservation(type);
    }

    /**
     * return an observable value that keeps updating
     */
    public ObservableDoubleValue getLatestObservationObservable(SalesDataType type) {
        return data.getLatestObservationObservable(type);
    }

    /**
     * basically this is an average of the latest observations of last closing price. As int as we are selling a lot of goods, it shouldn't matter.
     * The idea is that sometimes price goes very high to increase inventory but then there is only one sale or so, which isn't useful to know what the last price is
     * @return
     */
    public double getAveragedPrice(){

        return priceAverager.getAveragedPrice(this);




    }


    /**
     * last price set for a good. Might not have been sold/bought
     */
    @Override
    public int getLastSetPrice() {
        return lastAskedPrice;
    }

    /**
     * This is a little bit weird to predict, but basically you want to know what will be "tomorrow" price if you don't change production.
     * Most predictors simply return today closing price, because maybe this will be useful in some cases. It's used by Marginal Maximizer Statics
     * @return predicted price
     */
    public float predictSalePriceWhenNotChangingPoduction() {
        return predictorStrategy.predictSalePriceWhenNotChangingProduction(this);
    }

    @Override
    public int getStartingDay() {
        return data.getStartingDay();
    }

    @Override
    public int getLastObservedDay() {
        return data.getLastObservedDay();
    }


    /**
     * how many days worth of observations are here?
     */
    public int numberOfObservations() {
        return data.numberOfObservations();
    }

    /**
     * give this sales department a new memory object
     */
    public void setData(SalesData data) {
        this.data = data;
    }

    public SalesData getData() {
        return data;
    }

    public int getLastAskedPrice() {
        return lastAskedPrice;
    }

    public float getDaysOfInventory() {
        return daysOfInventory;
    }

    @Override
    public boolean hasTradedAtLeastOnce() {
        return lastClosingPrice >=0 ; //lastclosing price is -1 until one trade occurs!
    }

    public void setPriceAverager(PriceAverager averagedPrice) {
        this.priceAverager = averagedPrice;
    }



    public boolean isActive() {
        return firm.isActive();
    }

    public boolean isStarted() {
        return started;
    }



    /***
     *       __
     *      / /  ___   __ _ ___
     *     / /  / _ \ / _` / __|
     *    / /__| (_) | (_| \__ \
     *    \____/\___/ \__, |___/
     *                |___/
     */

    /**
     * simple lognode we delegate all loggings to.
     */
    private final LogNodeSimple logNode;

    @Override
    public boolean addLogEventListener(LogListener toAdd) {
        return logNode.addLogEventListener(toAdd);
    }

    @Override
    public boolean removeLogEventListener(LogListener toRemove) {
        return logNode.removeLogEventListener(toRemove);
    }

    @Override
    public void handleNewEvent(LogEvent logEvent)
    {
        logNode.handleNewEvent(logEvent);
    }

    @Override
    public boolean stopListeningTo(Loggable branch) {
        return logNode.stopListeningTo(branch);
    }

    @Override
    public boolean listenTo(Loggable branch) {
        return logNode.listenTo(branch);
    }


    public boolean hasAnythingToSell() {
        return quotesManager.hasAnythingToSell();
    }

    public int numberOfGoodsToSell() {
        return quotesManager.numberOfGoodsToSell();
    }

    public int numberOfQuotesPlaced() {
        return quotesManager.numberOfQuotesPlaced();
    }

    public int getAnyPriceQuoted() {
        return quotesManager.getAnyPriceQuoted();
    }

    public Collection<Quote> getAllQuotes() {
        return quotesManager.getAllQuotes();
    }

    public void recordQuoteAssociatedWithThisGood(Good g, Quote q) {
        quotesManager.recordQuoteAssociatedWithThisGood(g, q);
    }

    public Good peekFirstGoodAvailable() {
        return quotesManager.peekFirstGoodAvailable();
    }

    public Quote stopSellingThisGoodAndReturnItsAssociatedQuote(Good g) {
        return quotesManager.stopSellingThisGoodAndReturnItsAssociatedQuote(g);
    }


    public void thisQuoteHasBeenFilledSoRemoveItWithItsAssociatedGood(Quote q){
        quotesManager.thisQuoteHasBeenFilledSoRemoveItWithItsAssociatedGood(q);
    }

    public void removeAllQuotes() {
        quotesManager.removeAllQuotes();
    }

    public Collection<Good> listOfGoodsToSell() {
        return quotesManager.listOfGoodsToSell();
    }

    public void forgetTheQuoteAssociatedWithThisGood(Good g) {
        quotesManager.forgetTheQuoteAssociatedWithThisGood(g);
    }

    public boolean isThisGoodBeingSold(Good g) {
        return quotesManager.isThisGoodBeingSold(g);
    }

    public Quote getCheapestQuote() {
        return quotesManager.getCheapestQuote();
    }

    public Quote getQuoteAssociatedWithThisGood(Good g) {
        return quotesManager.getQuoteAssociatedWithThisGood(g);
    }

    public void recordThisGoodAsSellable(Good g) {
        quotesManager.recordThisGoodAsSellable(g);
    }
}

