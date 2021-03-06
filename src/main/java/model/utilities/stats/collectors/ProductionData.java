/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package model.utilities.stats.collectors;

import agents.firm.production.Plant;
import com.google.common.base.Preconditions;
import goods.GoodType;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.stats.collectors.enums.DataStorageSkeleton;
import sim.engine.SimState;

import java.util.HashMap;
import java.util.Set;


/**
 * <h4>Description</h4>
 * <p/> This is also a plant data, but it deals exclusively with how much was produced for each goodtype,
 * hence its separatedness
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-11-02
 * @see
 */
public class ProductionData extends DataStorageSkeleton<GoodType>
{

    /**
     * when it is set to off, it stops rescheduling itself!
     */
    protected boolean active = true;

    /**
     * the firm owning the plant we are documenting
     */
    protected Plant plant = null;


    public ProductionData() {
        data = new HashMap<>();

    }


    /**
     * called when the data gathering is supposed to start. It schedules itself to start at next CLEANUP phase. It grabs the Firm
     * reference from getOwner() of the plant
     */
    public void start( MacroII state, Plant plant) {
        //fill it as much as possible
        final Set<GoodType> listOfAllSectors = state.getGoodTypeMasterList().getListOfAllSectors();
        for(GoodType type : listOfAllSectors)
        {
            data.put(type,new DailyObservations());
        }


        this.plant = plant;
        //we are going to set the starting day at -1 and then change it at our first step()
        setStartingDay(-1);

        state.scheduleSoon(ActionOrder.CLEANUP_DATA_GATHERING,this);
    }



    @Override
    public void turnOff() {
        active = false;
    }

    @Override
    public void step(SimState state) {
        Preconditions.checkState(plant != null);
        if(!active)
            return;

        //make sure it's the right time
        assert state instanceof MacroII;
        MacroII model = (MacroII) state;
        assert model.getCurrentPhase().equals(ActionOrder.CLEANUP_DATA_GATHERING);
        //set starting day if needed
        if(getStartingDay()==-1)
            setCorrectStartingDate(model);
        assert getStartingDay() >=0;


        //memorize
        //grab the production vector
        final Set<GoodType> listOfAllSectors = model.getGoodTypeMasterList().getListOfAllSectors();
        for(GoodType type : listOfAllSectors)
        {
            if(data.get(type)== null) //this can happen if a new good sector has been created
                fillNewSectorObservationsWith0(model, type);


            data.get(type).add((double) plant.getProducedToday(type));
        }
        Preconditions.checkState(super.doubleCheckNumberOfObservationsAreTheSameForAllObservations(super.numberOfObservations()));
        //reschedule
        model.scheduleTomorrow(ActionOrder.CLEANUP_DATA_GATHERING, this);


    }

    protected void fillNewSectorObservationsWith0(MacroII model, GoodType type) {
        final DailyObservations newObservations = new DailyObservations();
        data.put(type, newObservations);
        int currentDay = (int)model.getMainScheduleTime();
        for(int i=getStartingDay(); i<currentDay; i++)
            newObservations.add(0d);
    }

    protected void setCorrectStartingDate(MacroII model) {
        setStartingDay((int) Math.round(model.getMainScheduleTime()));

        for(DailyObservations obs : data.values())
            obs.setStartingDay(getStartingDay());
    }


}
