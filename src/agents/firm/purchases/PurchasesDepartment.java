package agents.firm.purchases;

import agents.EconomicAgent;
import agents.firm.Department;
import agents.firm.Firm;
import agents.firm.purchases.inventoryControl.InventoryControl;
import agents.firm.purchases.inventoryControl.Level;
import agents.firm.purchases.prediction.LookAheadPredictor;
import agents.firm.purchases.prediction.MarketPurchasesPredictor;
import agents.firm.purchases.prediction.PurchasesPredictor;
import agents.firm.purchases.pricing.BidPricingStrategy;
import agents.firm.purchases.pricing.decorators.MaximumBidPriceDecorator;
import agents.firm.sales.exploration.BuyerSearchAlgorithm;
import agents.firm.sales.exploration.SellerSearchAlgorithm;
import com.google.common.base.Preconditions;
import ec.util.MersenneTwisterFast;
import financial.Market;
import financial.MarketEvents;
import financial.utilities.ActionsAllowed;
import financial.utilities.PurchaseResult;
import financial.utilities.Quote;
import goods.Good;
import goods.GoodType;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.Control;
import model.utilities.Deactivatable;
import sim.engine.SimState;
import sim.engine.Steppable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;


/**
 * The  purchases department's job is to keep supplying ONE GOOD-TYPE <p>
 *     Purchase department has 3 main components within it:
 *     <ul>
 <li>Inventory control:
 Inventory control is the routine that tells the department when to place a buy order. How this happens doesn't matter for the purchase department. All it has to do is to answer the order.
 I have to be careful how I call buy: I shouldn't call buy() directly or I end up screwing the expected matched quotes chain of events          </li>
 <li>Pricing:
 This is called everytime the purchases department need to price its goods. The pricing strategy may call updateOfferPrices to force the quotes' prices to call maxPrice.  </li>
 <li>Buy routine:
 This is the part implemented in the purchases department class. It is called by inventory control and either place quote or shop or both or whatever.  </li>
 </ul>
 * </p>
 */
public class PurchasesDepartment implements Deactivatable, Department {


    /**
     * The weekly budget given by the firm to this purchase department to carry out its tasks
     */
    private long budgetGiven;

    /**
     * the amount of weekly budget already spent
     */
    private long budgetSpent;

    /**
     * The firm that owns this department
     */
    final private Firm firm;

    /**
     * What good-type are you looking to buy?
     */
    final private GoodType goodType;

    /**
     * The market you are going to use to buy stuff from
     */
    private Market market;

    /**
     * What price to choose?
     */
    private BidPricingStrategy pricingStrategy;

    /**
     * The strategy we are using to choose how to control the inventory
     */
    private Control control;


    /**
     * what is the department doing RIGHT NOW?
     */
    private PurchasesDepartmentStatus status = PurchasesDepartmentStatus.IDLE;

    /**
     * Since the purchases department is single market-single goodtype we only need to remember AT MOST one quote. This is where we remember it
     */
    private Quote quotePlaced = null;

    /**
     * Sometimes inventory control tells us to buy a good while we are in the middle of buying another.
     * If so set this flag to true so that we can buy such good when we are done with the previous one.
     */
    private boolean queuedBuy = false;

    /**
     * flag that, when set to true the market has "best visible price" activated, forces the purchase department to never
     * overpay
     */
    private boolean looksAhead = false;

    /**
     * algorithm to search the registry for opponents
     */
    private BuyerSearchAlgorithm opponentSearch;

    /**
     * Algorithm to search the registry for suppliers
     */
    private SellerSearchAlgorithm supplierSearch;

    /**
     * What price was paid for the last good bought?
     */
    private long lastClosingPrice = -1;


    /**
     * The predictor for next purchases. It is set in the constructor but that can be changed
     */
    private PurchasesPredictor predictor;

    /**
     * an explicit link to the model, to reschedule yourself
     */
    private final MacroII model;


    protected PurchasesDepartment(long budgetGiven,@Nonnull Firm firm,@Nonnull Market market,
                                  MacroII model) {
        //initialize objects
        this.budgetGiven = budgetGiven;
        this.budgetSpent = 0l;
        this.firm = firm;
        this.market = market;
        this.goodType = market.getGoodType();
        //register as a buyer
        market.registerBuyer(firm);
        if(market.isBestSalePriceVisible())
            predictor = new LookAheadPredictor();
        else
            predictor = new MarketPurchasesPredictor();

        this.model = model;


    }

    /**
     * The  purchases department's job is to keep ONE PLANT supplied so that
     * @param budgetGiven  budget given to the department by the purchases department
     * @param firm the firm owning the department
     * @param market the market to buy from
     */
    protected PurchasesDepartment(long budgetGiven,@Nonnull Firm firm,@Nonnull Market market) {
        this(budgetGiven,firm,market,firm.getModel());


    }

    /**
     * This factory method returns an INCOMPLETE and NOTFUNCTIONING purchase department objects as it lacks all the strategies it needs to act.
     * This is useful if the caller wants to assign a specific rule from its side; otherwise stick with the other factories
     * @param budgetGiven the amount of money given to the purchase department!
     * @param firm the firm owning the department
     * @param market the market the department belongs to
     * @param  model the reference to the model object
     *
     */
    public static PurchasesDepartment getEmptyPurchasesDepartment(long budgetGiven,@Nonnull Firm firm,@Nonnull Market market,
                                                                  MacroII model){
        //create the simple purchases department
        //return it
        return new PurchasesDepartment(budgetGiven,firm,market,model);

    }

    /**
     * This factory method returns an INCOMPLETE and NOTFUNCTIONING purchase department objects as it lacks all the strategies it needs to act.
     * This is useful if the caller wants to assign a specific rule from its side; otherwise stick with the other factories
     * @param budgetGiven the amount of money given to the purchase department!
     * @param firm the firm owning the department
     * @param market the market the department belongs to
     */
    public static PurchasesDepartment getEmptyPurchasesDepartment(long budgetGiven,@Nonnull Firm firm,@Nonnull Market market){
        //create the simple purchases department
        //return it
        return getEmptyPurchasesDepartment(budgetGiven, firm, market,firm.getModel());

    }

    /**
     *  This is the simplest factory method for the purchases department. It randomly chooses its
     *  search algorithms, inventory control and pricing strategy.
     *  The reason I am going through all this is to avoid having a reference to purchases department leak out from constructor
     * @param budgetGiven  budget given to the department by the purchases department
     * @param firm the firm owning the department
     * @param market the market to buy from
     */
    public static PurchasesDepartment getPurchasesDepartment(long budgetGiven,@Nonnull Firm firm,@Nonnull Market market){
        //create the simple purchases department
        PurchasesDepartment instance = new PurchasesDepartment(budgetGiven,firm,market); //call the constructor
        //create random inventory control and assign it
        InventoryControl inventoryControl = PurchasesRuleFactory.randomInventoryControl(instance);
        instance.setControl(inventoryControl);
        // create a random bid pricing strategy and assign it
        BidPricingStrategy pricingStrategy = PurchasesRuleFactory.randomBidPricingStrategy(instance);
        instance.setPricingStrategy(pricingStrategy);
        //create a random buyer search algorithm and assign it
        BuyerSearchAlgorithm buyerSearchAlgorithm = BuyerSearchAlgorithm.Factory.randomBuyerSearchAlgorithm(market,firm);
        instance.setOpponentSearch(buyerSearchAlgorithm);
        //create a random seller search algorithm and assign it
        SellerSearchAlgorithm sellerSearchAlgorithm = SellerSearchAlgorithm.Factory.randomSellerSearchAlgorithm(market,firm);
        instance.setSupplierSearch(sellerSearchAlgorithm);

        //finally: return it!
        return instance;
    }

    /**
     * This factor for purchases department is used when we want the department to follow an integrated rule: inventory control and pricing rule are the same object. <br>
     * Leaving any of the type arguments null will make the constructor generate a rule at random
     * @param budgetGiven the budget given to the department by the firm
     * @param firm the firm owning the department
     * @param market the market the department dabbles in
     * @param integratedControl the type of rule that'll be both BidPricing and InventoryControl
     * @param buyerSearchAlgorithmType the algorithm the buyer follows to search for competitors
     * @param sellerSearchAlgorithmType the algorithm the buyer follows to search for suppliers
     * @return a new instance of PurchasesDepartment
     */
    public static PurchasesDepartment getPurchasesDepartmentIntegrated(long budgetGiven, @Nonnull Firm firm, @Nonnull Market market, @Nullable Class<? extends  BidPricingStrategy> integratedControl,
                                                                       @Nullable Class<? extends BuyerSearchAlgorithm> buyerSearchAlgorithmType, @Nullable Class<? extends SellerSearchAlgorithm> sellerSearchAlgorithmType )
    {

        //create the simple purchases department
        PurchasesDepartment instance = new PurchasesDepartment(budgetGiven,firm,market); //call the constructor

        //create inventory control and assign it
        BidPricingStrategy bidPricingStrategy;
        if(integratedControl == null) //if null randomize
            bidPricingStrategy = PurchasesRuleFactory.randomIntegratedRule(instance);
        else //otherwise instantiate the specified one
            bidPricingStrategy= PurchasesRuleFactory.newIntegratedRule(integratedControl,instance);
        instance.setPricingStrategy(bidPricingStrategy);
        assert bidPricingStrategy instanceof InventoryControl; //if you are really integrated that's true
        instance.setControl((InventoryControl) bidPricingStrategy);



        //create a buyer search algorithm and assign it
        BuyerSearchAlgorithm buyerSearchAlgorithm;
        if(buyerSearchAlgorithmType == null)
            buyerSearchAlgorithm = BuyerSearchAlgorithm.Factory.randomBuyerSearchAlgorithm(market,firm);
        else
            buyerSearchAlgorithm = BuyerSearchAlgorithm.Factory.newBuyerSearchAlgorithm(buyerSearchAlgorithmType,market,firm);
        instance.setOpponentSearch(buyerSearchAlgorithm);


        //create a random seller search algorithm and assign it
        SellerSearchAlgorithm sellerSearchAlgorithm;
        if(sellerSearchAlgorithmType == null)
            sellerSearchAlgorithm = SellerSearchAlgorithm.Factory.randomSellerSearchAlgorithm(market,firm);
        else
            sellerSearchAlgorithm = SellerSearchAlgorithm.Factory.newSellerSearchAlgorithm(sellerSearchAlgorithmType,market,firm);
        instance.setSupplierSearch(sellerSearchAlgorithm);

        //finally: return it!
        return instance;

    }


    /**
     *  This is the simplest factory method for the purchases department. It randomly chooses its
     *  search algorithms, inventory control and pricing strategy.
     *  The reason I am going through all this is to avoid having a reference to purchases department leak out from constructor
     * @param budgetGiven  budget given to the department by the purchases department
     * @param firm the firm owning the department
     * @param market the market to buy from
     * @param inventoryControlType the type of inventory control desired. If null it is randomized
     * @param bidPricingStrategyType the pricing strategy desired. If null it is randomized
     * @param buyerSearchAlgorithmType the buyer search algorithm. If null it is randomized
     * @param  sellerSearchAlgorithmType the seller search algorithm desired. If null it is randomized
     */
    public static PurchasesDepartment getPurchasesDepartment(long budgetGiven, @Nonnull Firm firm, @Nonnull Market market,
                                                             @Nullable Class<? extends InventoryControl> inventoryControlType, @Nullable Class<? extends BidPricingStrategy> bidPricingStrategyType,
                                                             @Nullable Class<? extends BuyerSearchAlgorithm> buyerSearchAlgorithmType, @Nullable Class<? extends SellerSearchAlgorithm> sellerSearchAlgorithmType){
        //create the simple purchases department
        PurchasesDepartment instance = new PurchasesDepartment(budgetGiven,firm,market); //call the constructor

        //create inventory control and assign it
        InventoryControl inventoryControl;
        if(inventoryControlType == null) //if null randomize
            inventoryControl = PurchasesRuleFactory.randomInventoryControl(instance);
        else //otherwise instantiate the specified one
            inventoryControl= PurchasesRuleFactory.newInventoryControl(inventoryControlType,instance);
        instance.setControl(inventoryControl);

        //create BidPricingStrategy and assign it
        BidPricingStrategy bidPricingStrategy;
        if(bidPricingStrategyType == null) //if null randomize
            bidPricingStrategy = PurchasesRuleFactory.randomBidPricingStrategy(instance);
        else //otherwise instantiate the specified one
            bidPricingStrategy= PurchasesRuleFactory.newBidPricingStrategy(bidPricingStrategyType,instance);
        instance.setPricingStrategy(bidPricingStrategy);

        //create a buyer search algorithm and assign it
        BuyerSearchAlgorithm buyerSearchAlgorithm;
        if(buyerSearchAlgorithmType == null)
            buyerSearchAlgorithm = BuyerSearchAlgorithm.Factory.randomBuyerSearchAlgorithm(market,firm);
        else
            buyerSearchAlgorithm = BuyerSearchAlgorithm.Factory.newBuyerSearchAlgorithm(buyerSearchAlgorithmType,market,firm);
        instance.setOpponentSearch(buyerSearchAlgorithm);


        //create a random seller search algorithm and assign it
        SellerSearchAlgorithm sellerSearchAlgorithm;
        if(sellerSearchAlgorithmType == null)
            sellerSearchAlgorithm = SellerSearchAlgorithm.Factory.randomSellerSearchAlgorithm(market,firm);
        else
            sellerSearchAlgorithm = SellerSearchAlgorithm.Factory.newSellerSearchAlgorithm(sellerSearchAlgorithmType,market,firm);
        instance.setSupplierSearch(sellerSearchAlgorithm);

        //finally: return it!
        return instance;
    }

    /**
     *  This is the simplest factory method for the purchases department. It randomly chooses its
     *  search algorithms, inventory control and pricing strategy.
     *  The reason I am going through all this is to avoid having a reference to purchases department leak out from constructor
     * @param budgetGiven  budget given to the department by the purchases department
     * @param firm the firm owning the department
     * @param market the market to buy from
     * @param inventoryControlType the name of inventory control desired.
     * @param bidPricingStrategyType the name of pricing strategy desired.
     * @param buyerSearchAlgorithmType the name of buyer search algorithm.
     * @param  sellerSearchAlgorithmType the name seller search algorithm desired.
     */
    public static PurchasesDepartment getPurchasesDepartment(long budgetGiven,@Nonnull Firm firm,@Nonnull Market market,
                                                             @Nonnull String inventoryControlType, @Nonnull String bidPricingStrategyType,
                                                             @Nonnull String buyerSearchAlgorithmType,@Nonnull String sellerSearchAlgorithmType){
        //create the simple purchases department
        PurchasesDepartment instance = new PurchasesDepartment(budgetGiven,firm,market); //call the constructor
        //create random inventory control and assign it
        InventoryControl inventoryControl = PurchasesRuleFactory.newInventoryControl(inventoryControlType,instance);
        instance.setControl(inventoryControl);
        // create a random bid pricing strategy and assign it
        BidPricingStrategy pricingStrategy =PurchasesRuleFactory.newBidPricingStrategy(bidPricingStrategyType,instance);
        instance.setPricingStrategy(pricingStrategy);
        //create a random buyer search algorithm and assign it
        BuyerSearchAlgorithm buyerSearchAlgorithm = BuyerSearchAlgorithm.Factory.newBuyerSearchAlgorithm(buyerSearchAlgorithmType, market, firm);
        instance.setOpponentSearch(buyerSearchAlgorithm);
        //create a random seller search algorithm and assign it
        SellerSearchAlgorithm sellerSearchAlgorithm = SellerSearchAlgorithm.Factory.newSellerSearchAlgorithm(sellerSearchAlgorithmType,market,firm);
        instance.setSupplierSearch(sellerSearchAlgorithm);

        //finally: return it!
        return instance;
    }

    /**
     * This method just calls the pricing strategy but if the market best ask is visible and lower than the pricing says it is then it just defaults to it. <br>
     * Also whatever is the result, it never returns more than the available budget
     * @param type the type of good we are going to buy
     * @param market the market we are buying it from
     * @return the max price we are willing to pay!
     */
    public long maxPrice(GoodType type, Market market){

        try {if(!market.isBestSalePriceVisible() || market.getBestSellPrice() < 0)    //if it's not visible or whatever, return the strategy or the budget
            return Math.min(pricingStrategy.maxPrice(type), getAvailableBudget());
        else{
            long strategyPrice = pricingStrategy.maxPrice(type);
            long bestReadyPrice = looksAhead? market.getBestSellPrice(): Long.MAX_VALUE;
            long budget = getAvailableBudget();
            return Math.min(Math.min(strategyPrice,bestReadyPrice),budget); //basically return the smallest of the 3: the best price visible, the budget and the strategic price
        }
        } catch (IllegalAccessException e) {
            throw new RuntimeException("I was promised that sale price was visible! But it wasn't!");
        }


    }


    /**
     * if approached by a peddler, how much is the purchase department willing to pay? It just calls maxPrice() if it's willing to buy
     * @return maxPrice or -1 if it doesn't want to buy
     */
    public long maximumOffer(Good g){
        if(!canBuy())
            return -1l;
        else
            return maxPrice(g.getType(),getMarket());

    }

    /**
     * React to a bid filled quote
     * @param g good bought
     * @param price price of the good.
     */
    public void reactToFilledQuote(Good g, long price, EconomicAgent seller){

        budgetSpent += price; //budget recording
        lastClosingPrice = price;

        //two things: either I was waiting for it, or it was immediate
        if(status == PurchasesDepartmentStatus.WAITING){ //If I was waiting for it
            assert quotePlaced != null;
            assert getFirm().has(g) || g.getType().isLabor(); //it should be ours now! (unless we are buying labor in which case it makes no sense to "have"

            quotePlaced = null; //remove the memory!
            status = PurchasesDepartmentStatus.IDLE;

            if(queuedBuy){
                queuedBuy = false; //take down the flag
                buy(goodType,market);

            }
        }
        else{
            assert status == PurchasesDepartmentStatus.PLACING_QUOTE : "current status: " + status + ", quote placed: " + quotePlaced;
            assert quotePlaced == null;
            assert getFirm().has(g)  || g.getType().isLabor(); //it should be ours now!
            //we will return to the buy method so we don't need to deal with anything inside here.
        }

        getFirm().logEvent(this, MarketEvents.BOUGHT, getFirm().getModel().getCurrentSimulationTimeInMillis(), "price: " + price);

    }

    /**
     * You are calling this because you can't, or won't, use the order book. Basically it manually searches around
     */
    public void shop(){


        model.scheduleSoon(ActionOrder.TRADE, new Steppable() {
            @Override
            public void step(SimState state) {

                assert control.canBuy(); //make sure it's okay
                status = PurchasesDepartmentStatus.SHOPPING; //put yourself in shopping mood
                long maxPrice = maxPrice(goodType,market); //maximum we are willing to pay

                EconomicAgent seller = supplierSearch.getBestInSampleSeller(); //get the best seller available
                //if we couldn't find any
                if(seller == null){
                    supplierSearch.reactToFailure(seller, PurchaseResult.NO_MATCH_AVAILABLE);
                    shop(); //call shop again soon!

                }
                else
                {
                    Quote sellerQuote = seller.askedForASaleQuote(getFirm(), goodType); //ask again for a price offer
                    assert sellerQuote.getPriceQuoted() >= 0 ; //can't be negative!!!
                    if(maxPrice >= sellerQuote.getPriceQuoted()) //if the match is good:
                    {
                        long finalPrice = market.price(sellerQuote.getPriceQuoted(),maxPrice);

                        //build a fake buyer quote for stat collection
                        Quote buyerQuote = Quote.newBuyerQuote(getFirm(),maxPrice,goodType);
                        buyerQuote.setOriginator(PurchasesDepartment.this);


                        //TRADE
                        PurchaseResult result = seller.shopHere(buyerQuote,sellerQuote);
                        assert result.equals(PurchaseResult.SUCCESS) : "haven't coded what happens otherwise";

                        //record info
                        budgetSpent += result.getPriceTrade(); lastClosingPrice = result.getPriceTrade(); //spent!
                        getFirm().logEvent(this, MarketEvents.BOUGHT, getFirm().getModel().getCurrentSimulationTimeInMillis(), "price: " + finalPrice);
                        status = PurchasesDepartmentStatus.IDLE; //you are done!
                        supplierSearch.reactToSuccess(seller,PurchaseResult.SUCCESS);


                        if(queuedBuy) //if we need to buy another good, do it!
                        {
                            queuedBuy = false; //reset the queue

                            buy(goodType,market);
                        }
                    }
                    else{   //we didn't make it!
                        supplierSearch.reactToFailure(seller,PurchaseResult.PRICE_REJECTED);
                        shop(); //call shop again soon!

                    }

                }

            }
        });



    }

    /**
     * This is the method used by whoever controls the purchase department to order it to go ahead and buy
     */
    public void buy(){

        if(status == PurchasesDepartmentStatus.IDLE)
        { //if we were waiting for the order to be given, just go ahead and buy
            assert !queuedBuy : quotePlaced + " ---- " + maxPrice(getGoodType(),getMarket()) + "  ///---\\\\" ; //we can't be here if there is a buy order in queue!
            buy(goodType,market);
        }
        else
        { //otherwise 2 possibilities: we are in the process of trading or we still haven't reacted to a filled quote. So wait
//            assert !queuedBuy;
            queuedBuy = true;

        }

        //log it
        getFirm().logEvent(this, MarketEvents.TASKED_TO_BUY, getFirm().getModel().getCurrentSimulationTimeInMillis());



    }


    private void buy(@Nonnull GoodType type,@Nonnull Market market)
    {

        assert !queuedBuy; //as we call this there can't be any additional order to buy in queue. But this flag might be turned on later as we trade.
        assert quotePlaced == null; //there shouldn't be any quote already or this is weird.


        if(market.getBuyerRole() == ActionsAllowed.QUOTE)
        {
            placeQuote(type, market);




        }
        else{
            status = PurchasesDepartmentStatus.SHOPPING; //set your status to shopping!
            shop(); //shop around if you can't use the order book
        }


    }

    /**
     * Place quote through the phase scheduler at the nearest/current TRADE phase
     * @param type the type of good we are selling
     * @param market the market to trade into
     */
    private void placeQuote(final GoodType type, final Market market) {
        model.scheduleSoon(ActionOrder.TRADE, new Steppable() {
            @Override
            public void step(SimState state) {
                if(status != PurchasesDepartmentStatus.IDLE) //if something came up while we were waiting, don't buy
                {
                    queuedBuy = true;
                    return;
                }

                status = PurchasesDepartmentStatus.PLACING_QUOTE; //we are going to be placing a quote!


                Quote q = market.submitBuyQuote(firm,maxPrice(type,market),PurchasesDepartment.this); //submit the quote!

                if(q.getPriceQuoted() == -1) //if the quote is null
                {
                    assert q.getAgent() == null; //make sure we got back the right quote
                    //if we are here, it means that trade has been called, which has called react to filled quote and notified the inventory which probably called the inventory control.
                    //by now all that is past, we have a new good in inventory and we might have been ordered to buy again. If so, do it now.
                    if(queuedBuy)
                    { //if inventory control asked us to buy another good in the mean-time
                        queuedBuy = false;     //empty the queue
                        status = PurchasesDepartmentStatus.IDLE; //temporarily set yourself as idle
                        buy(goodType,market); //call another buy.
                    }
                    else{
                        status = PurchasesDepartmentStatus.IDLE; //if there is nothing to buy, stay idle
                    }

                }
                else
                {
                    assert q.getAgent() == firm; //make sure we got back the right quote
                    assert q.getPriceQuoted() >= 0; //make sure it's positive!
                    //we'll have to store it and wait
                    quotePlaced = q;
                    status = PurchasesDepartmentStatus.WAITING; //set your status to waiting

                }

            }
        });

    }


    public GoodType getGoodType() {
        return goodType;
    }

    public Firm getFirm() {
        return firm;
    }

    public void setControl(@Nonnull Control control) {
        if(this.control != null) //if there was one before, turn it off
            this.control.turnOff();
        this.control = control;
    }

    public void setPricingStrategy(@Nonnull BidPricingStrategy pricingStrategy) {
        if(this.pricingStrategy != null)   //if you had one before, turn it off
            this.pricingStrategy.turnOff();
        this.pricingStrategy = pricingStrategy;
    }

    /**
     * Generate a new pricing strategy of this specific type!
     * @param pricingStrategyType  the class of which the new pricing strategy will be
     */
    public void setPricingStrategy(@Nonnull Class<? extends BidPricingStrategy> pricingStrategyType) {
        if(this.pricingStrategy != null)   //if you had one before, turn it off
            this.pricingStrategy.turnOff();
        this.pricingStrategy = PurchasesRuleFactory.newBidPricingStrategy(pricingStrategyType,this);
    }



    public void setOpponentSearch(@Nonnull BuyerSearchAlgorithm opponentSearch) {
        if(this.opponentSearch != null)
            opponentSearch.turnOff();
        this.opponentSearch = opponentSearch;
    }

    public void setSupplierSearch(@Nonnull SellerSearchAlgorithm supplierSearch) {
        if(this.supplierSearch != null)
            supplierSearch.turnOff();
        this.supplierSearch = supplierSearch;
    }


    /**
     * Tells the purchase department to never buy above a specific value. Under the hood we are decorating the pricing strategy.
     * Once set, don't change!
     * @param reservationPrice the new maximum
     */
    public void setReservationPrice(long reservationPrice)
    {
        Preconditions.checkState(pricingStrategy != null, "Can't add a reservation wage until a pricing strategy is in place!");
        pricingStrategy = new MaximumBidPriceDecorator(pricingStrategy,reservationPrice);
    }

    /**
     * Whenever we can ONLY shop and we fail to do so, we call this method to know how much to wait before trying again
     * @return the wait time, or -1 if we are NOT going to try again!
     */
    public final double tryAgainNextTime(){
        return getFirm().getModel().getPeddlingSpeed();

    }

    /**
     * At weekend remove budget spent from total budget; purely accounting trick
     */
    public void weekEnd() {
        assert budgetGiven >= budgetSpent;
        budgetGiven -= budgetSpent;
        budgetSpent = 0;
    }

    /**
     * Add some money to the purchaser's budget
     * @param additionalBudget additional money to spend by the purchases department
     */
    public void addToBudget(long additionalBudget){
        budgetGiven +=additionalBudget;

    }

    /**
     * This is called by a pricing strategy that decided to change prices. It only affects the quote if there is any
     */
    public void updateOfferPrices(){
        if(quotePlaced != null) //if you already have a quote
        {
            //call straight the buy algorithm
            final boolean queued =  cancelQuote();
            //TODO is ASAP a good thing?
            //schedule ASAP
            model.scheduleASAP(new Steppable() {
                @Override
                public void step(SimState state) {

                    buy(); //try again!
                    if(!queuedBuy && queued){
                        //there was something in queue we should buy
                        if( status != PurchasesDepartmentStatus.IDLE)
                            queuedBuy = true;     //empty the queue (unlesss you are waiting in which case you already have it full)

                    }
                }
            });



        }






    }

    /**
     * removes the current quote in the market, returns the old "queuedbuy" boolean
     */
    public boolean cancelQuote(){
        assert status == PurchasesDepartmentStatus.WAITING;
        market.removeBuyQuote(quotePlaced); //remove the buy order
        quotePlaced = null; //remove it from your memory
        status = PurchasesDepartmentStatus.IDLE; //you aren't waiting for the old buy order anymore
        boolean oldQueuedBuy = queuedBuy;
        queuedBuy = false;
        return oldQueuedBuy;
    }

    /**
     * Returns true if the department has already placed a quote
     */
    public boolean hasQuoted(){
        return quotePlaced != null;

    }

    /**
     * just return the amount of budget allocated that is still up for grabs
     * @return the budget left
     */
    public long getAvailableBudget(){
        long budget = budgetGiven - budgetSpent;
        assert budget>=0;
        return budget;

    }


    /**
     * This method returns the inventory control rating on the level of inventories. <br>
     * @return the rating on the inventory conditions or null if the department is not active.
     */
    @Nullable
    public Level rateCurrentLevel() {
        assert control != null;
        return control.rateCurrentLevel();
    }

    public Market getMarket() {
        return market;
    }

    /**
     * Should we accept offer from peddlers?
     * @return true if the purchase department is looking to buy
     */
    public boolean canBuy() {
        return control.canBuy();
    }


    @Override
    public String toString() {
        return  market.getGoodType().name() + "-Purchases" +
                "opponentSearch=" + opponentSearch +
                ", supplierSearch=" + supplierSearch +
                ", control=" + control +
                ", pricingStrategy=" + pricingStrategy +
                ", quotePlaced=" + quotePlaced +
                '}';
    }

    /**
     * this deregister the purchase department and looks in its components to see if any of them can be turned off as well.
     * It also deregisters the department from the firm.
     */
    public void turnOff(){
        //deregister the firm
        market.deregisterBuyer(firm);
        pricingStrategy.turnOff(); //turn off prices
        control.turnOff(); //turn off control
        supplierSearch.turnOff(); //turn off seller search
        opponentSearch.turnOff(); //turn off buyer search
        predictor.turnOff(); //turn off the predictor



    }


    /**
     * pass the randomizer from the firm object
     * @return
     */
    public MersenneTwisterFast getRandom() {
        return firm.getRandom();
    }

    /**
     * look into the seller registry and return what the search algorithm deems the best
     * @return the best seller available or null if you can't find any
     */
    @Nullable
    public EconomicAgent getBestSupplierFound() {
        return supplierSearch.getBestInSampleSeller();
    }

    /**
     * look into the buyer registry and return what the search algorithm deems the best
     * @return the best buyer available or null if there were none
     */
    public EconomicAgent getBestOpponentFound() {
        return opponentSearch.getBestInSampleBuyer();
    }


    /**
     * Tell the supplier search algorithm that the last match was a bad one
     * @param seller match made
     * @param reason purchase result of the transaction
     */
    public void supplierSearchFailure(EconomicAgent seller, PurchaseResult reason) {
        supplierSearch.reactToFailure(seller, reason);
    }

    /**
     * Tell the search algorithm that the last match was a good one
     * @param seller match made
     * @param reason purchase result of the transaction
     */
    public void supplierSearchSuccess(EconomicAgent seller, PurchaseResult reason) {
        supplierSearch.reactToSuccess(seller, reason);
    }

    /**
     * Tell the search algorithm that the last match was a good one
     * @param buyer match made
     * @param reason purchase result of the transaction
     */
    public void opponentSearchFailure(EconomicAgent buyer, PurchaseResult reason) {
        opponentSearch.reactToSuccess(buyer, reason);
    }

    /**
     * Tell the search algorithm that the last match was a bad one
     * @param buyer match made
     * @param reason purchase result of the transaction
     */
    public void opponentSearchSuccess(EconomicAgent buyer, PurchaseResult reason) {
        opponentSearch.reactToFailure(buyer, reason);
    }

    /**
     * look into the buyer registry and return what the search algorithm deems the best
     * @return the best buyer available or null if there were none
     */
    public EconomicAgent getBestOpponent() {
        return opponentSearch.getBestInSampleBuyer();
    }

    /**
     * Start the inventory control and make the purchaseDepartment, if needed, buy stuff
     */
    public void start(){
        control.start();
    }

    /**
     * Returns a link to the pricing strategy. Only useful for subclasses
     * @return reference to the pricing strategy.
     */
    protected BidPricingStrategy getPricingStrategy() {
        return pricingStrategy;
    }


    /**
     * used by a subclass to tell you they are spending money!!
     */
    protected void spendFromBudget(long amountSpent){
        budgetSpent += amountSpent;
    }


    /**
     * let the subclass see and interact with control
     * @return
     */
    protected Control getControl() {
        return control;
    }

    /**
     * The last price the purchases department got for its goods
     * @return
     */
    public long getLastClosingPrice() {
        return lastClosingPrice;
    }

    /**
     * changes the way the purchases department makes prediction about future prices
     * @param predictor the predictor
     */
    public void setPredictor(PurchasesPredictor predictor) {
        this.predictor = predictor;
    }

    /**
     * Predicts the future price of the next good to buy
     * @return the predicted price or -1 if there are no predictions.
     */
    public long predictPurchasePrice() {
        return predictor.predictPurchasePrice(this);
    }


    /**
     * Gets flag that, when set to true and the market has "best visible price" activated, forces the purchase department to never
     * overpay.
     *
     * @return Value of flag that, when set to true the market has "best visible price" activated, forces the purchase department to never
     *         overpay.
     */
    public boolean isLooksAhead() {
        return looksAhead;
    }

    /**
     * Sets new flag that, when set to true and the market has "best visible price" activated, forces the purchase department to never
     * overpay.
     *
     * @param looksAhead New value of flag that, when set to true the market has "best visible price" activated, forces the purchase department to never
     *                   overpay.
     */
    public void setLooksAhead(boolean looksAhead) {
        this.looksAhead = looksAhead;
    }
}