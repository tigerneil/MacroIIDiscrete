/*
 * Copyright (c) 2014 by Ernesto Carrella
 * Licensed under MIT license. Basically do what you want with it but cite me and don't sue me. Which is just politeness, really.
 * See the file "LICENSE" for more information
 */

package model.utilities.stats.collectors;

import agents.firm.Firm;
import agents.firm.production.Plant;
import com.google.common.base.Preconditions;
import model.MacroII;
import model.utilities.ActionOrder;
import model.utilities.stats.collectors.enums.PlantDataType;
import sim.engine.SimState;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * <h4>Description</h4>
 * <p/> An extension to the DataStorage class to store plant specific data
 * <p/> It is only slightly different from the other DataStorage because it takes some information straight from Firm rather than the plant.
 * Also, while the profits are computed weekly they are queried and stored daily.
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2013-08-22
 * @see
 */
public class PlantData extends DataStorage<PlantDataType> {

    /**
     * the firm owning the plant we are documenting
     */
    private Firm plantOwner = null;

    /**
     * this says when was the last meaningful change of workforce (sometimes we hire and fire a guy the same day, but this wouldn't show up in the data here)
     */
    private int lastDayAMeaningfulChangeInWorkforceOccurred = -1;

    /**
     * shock days.
     */
    private LinkedList<Integer>  daysWhenAMeaningfulChangeInWorkforceOccurred;


    /**
     * the firm owning the plant we are documenting
     */
    private Plant plant = null;


    public PlantData() {
        super(PlantDataType.class);
        daysWhenAMeaningfulChangeInWorkforceOccurred = new LinkedList<>();
    }

    /**
     * called when the data gathering is supposed to start. It schedules itself to start at next CLEANUP phase
     */
    public void start( MacroII state, Plant plant,  Firm plantOwner) {
        if(!isActive())
            return;

        Preconditions.checkState(this.plantOwner == null, " can't start the gatherer twice!");

        //schedule yourself
        this.plantOwner = plantOwner;
        this.plant = plant;
        //we are going to set the starting day at -1 and then change it at our first step()
        setStartingDay(-1);

        state.scheduleSoon(ActionOrder.CLEANUP_DATA_GATHERING,this);
    }

    /**
     * called when the data gathering is supposed to start. It schedules itself to start at next CLEANUP phase. It grabs the Firm
     * reference from getOwner() of the plant
     */
    public void start( MacroII state, Plant plant) {
        this.start(state,plant,plant.getOwner());

    }



    @Override
    public void step(SimState state) {
        if(!isActive())
            return;

        //make sure it's the right time
        assert state instanceof MacroII;
        MacroII model = (MacroII) state;
        assert model.getCurrentPhase().equals(ActionOrder.CLEANUP_DATA_GATHERING);
        assert  (this.plantOwner)!=null;


        if(getStartingDay()==-1)
            setCorrectStartingDate(model);

        assert getStartingDay() >=0;


        //memorize
        data.get(PlantDataType.PROFITS_THAT_WEEK).add(Double.valueOf(plantOwner.getPlantProfits(plant)));
        data.get(PlantDataType.REVENUES_THAT_WEEK).add(Double.valueOf(plantOwner.getPlantRevenues(plant)));
        data.get(PlantDataType.COSTS_THAT_WEEK).add(Double.valueOf(plantOwner.getPlantCosts(plant)));

        int numberOfWorkers = plant.getNumberOfWorkers();
        //before adding it, check if it's different!
        if(data.get(PlantDataType.TOTAL_WORKERS).size()>0
                && ((int)Math.round(data.get(PlantDataType.TOTAL_WORKERS).getLastObservation())) != plant.getNumberOfWorkers())
        {
            lastDayAMeaningfulChangeInWorkforceOccurred = (int)model.getMainScheduleTime();
            daysWhenAMeaningfulChangeInWorkforceOccurred.add(lastDayAMeaningfulChangeInWorkforceOccurred);
        }
        data.get(PlantDataType.WORKER_TARGET).add((double) plant.getWorkerTarget());
        data.get(PlantDataType.TOTAL_WORKERS).add((double) numberOfWorkers);
        data.get(PlantDataType.WAGES_PAID_THAT_WEEK).add((double) plant.getWagesPaid());


        //reschedule
        model.scheduleTomorrow(ActionOrder.CLEANUP_DATA_GATHERING, this);



    }


    @Override
    public void turnOff() {
        super.turnOff();
        plant = null;
    }

    public int getLastDayAMeaningfulChangeInWorkforceOccurred() {
        return lastDayAMeaningfulChangeInWorkforceOccurred;
    }


    public List<Integer> getShockDays(){
        return Collections.unmodifiableList(daysWhenAMeaningfulChangeInWorkforceOccurred);
    }

    public int howManyShockDays(){
        return daysWhenAMeaningfulChangeInWorkforceOccurred.size();
    }



}
