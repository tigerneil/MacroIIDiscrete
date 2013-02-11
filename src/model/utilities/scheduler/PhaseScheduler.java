package model.utilities.scheduler;

import com.google.common.base.Preconditions;
import com.sun.javafx.beans.annotations.NonNull;
import ec.util.MersenneTwisterFast;
import model.MacroII;
import model.utilities.ActionOrder;
import sim.engine.SimState;
import sim.engine.Steppable;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.LinkedList;

/**
 * <h4>Description</h4>
 * <p/> I try to maintain an iron grip over the schedule calls. I prefer my objects registering actions here and then just stepping this class instead
 * <p/>
 * <p/>
 * <h4>Notes</h4>
 * Created with IntelliJ
 * <p/>
 * <p/>
 * <h4>References</h4>
 *
 * @author carrknight
 * @version 2012-11-20
 * @see
 */
public class PhaseScheduler implements Steppable {

    /**
     * where we store every possible action!
     */
    private EnumMap<ActionOrder,RandomQueue<Steppable>> steppablesByPhase;

    private MersenneTwisterFast randomizer;




    private final int simulationDays;

    private ActionOrder currentPhase = ActionOrder.DAWN;

    final private ArrayList<Steppable> tomorrowSamePhase;

    final private ArrayList<FutureAction> futureActions;


    public PhaseScheduler(int simulationDays,MersenneTwisterFast randomizer) {
        this.simulationDays = simulationDays;
        this.randomizer = randomizer;


        //initialize the enums
        steppablesByPhase = new EnumMap<>(ActionOrder.class);
        for(ActionOrder order :ActionOrder.values())
        {
            steppablesByPhase.put(order,new RandomQueue<Steppable>(randomizer));
        }

        //initialize tomorrow schedule
        tomorrowSamePhase = new ArrayList<>();

        futureActions = new ArrayList<>();

    }


    /**
     * Go through a "day" in the model. It self schedules to do it again tomorrow
     */
    @Override
    public void step(SimState simState) {
        System.out.println("Day" + simState.schedule.getTime() );
        assert simState instanceof MacroII;
        MersenneTwisterFast random = simState.random; //grab the random from the simstate
        assert tomorrowSamePhase.isEmpty() : "things shouldn't be scheduled here";


        //for each phase
        for(ActionOrder phase : ActionOrder.values())
        {
            currentPhase = phase; //currentPhase!

            RandomQueue<Steppable> steppables = steppablesByPhase.get(phase);
            assert steppables != null;

            //while there are actions to take this phase, take them
            while(!steppables.isEmpty())
            {
                Steppable steppable = steppables.poll();
                assert steppable != null;
                //act nau!!!
                steppable.step(simState);
            }
            //schedule stuff to happen tomorrow
            steppables.addAll(tomorrowSamePhase);
            tomorrowSamePhase.clear(); //here we kept all steppables that are called to act the same phase tomorrow!


            //go to the next phase!

        }

        //prepare for tomorrow
        prepareForTomorrow();

        //see you tomorrow
        if(simState.schedule.getTime() <= simulationDays)
            simState.schedule.scheduleOnceIn(1,this);



    }

    private void prepareForTomorrow() {
        assert tomorrowSamePhase.isEmpty() : "things shouldn't be scheduled here";
        //set phase to dawn
        currentPhase = ActionOrder.DAWN;
        //check for delayed actions
        LinkedList<FutureAction> toRemove = new LinkedList<>();
        for(FutureAction futureAction : futureActions)
        {
            boolean ready = futureAction.spendOneDay();
            if(ready)
            {
                scheduleSoon(futureAction.getPhase(),futureAction.getAction());
                toRemove.add(futureAction);
            }
        }
        futureActions.removeAll(toRemove);
    }

    /**
     * schedule the event to happen when the next specific phase comes up!
     * @param phase which phase the action should be performed?
     * @param action the action taken!
     */
    public void scheduleSoon(@NonNull ActionOrder phase,@NonNull Steppable action){

        //put it among the steppables of that phase
        steppablesByPhase.get(phase).add(action);

    }

    /**
     * force the schedule to record this action to happen tomorrow.
     * This is allowed only if you are at a phase (say PRODUCTION) and you want the action to occur tomorrow at the same phase (PRODUCTION)
     * @param phase which phase the action should be performed?
     * @param action the action taken!
     */
    public void scheduleTomorrow(ActionOrder phase,Steppable action){

        Preconditions.checkArgument(phase.equals(currentPhase));
        //put it among the steppables of that phase
        tomorrowSamePhase.add(action);

    }

    /**
     *
     * @param phase The action order at which this action should be scheduled
     * @param action the action to schedule
     * @param daysAway how many days from now should it be scheduled
     */
    public void scheduleAnotherDay(@Nonnull ActionOrder phase,@Nonnull Steppable action,
                                   int daysAway)
    {
        Preconditions.checkArgument(daysAway > 0, "Days away must be positive");
        futureActions.add(new FutureAction(phase,action,daysAway));

    }



    public ActionOrder getCurrentPhase() {
        return currentPhase;
    }


    public void clear()
    {
        steppablesByPhase.clear();
        steppablesByPhase = new EnumMap<>(ActionOrder.class);
        for(ActionOrder order :ActionOrder.values())
        {
            steppablesByPhase.put(order,new RandomQueue<Steppable>(randomizer));
        }

        tomorrowSamePhase.clear();
        futureActions.clear();
        currentPhase = ActionOrder.DAWN;
    }

    /**
     * A simple struct storing the phase, the kind of action and how many days from now it is supposed to be
     */
    class FutureAction{

        final private ActionOrder phase;

        final private Steppable action;

        private int daysAway;

        public FutureAction(ActionOrder phase, Steppable action, int daysAway) {
            Preconditions.checkArgument(daysAway > 0); //delay has to be positive
            this.phase = phase;
            this.action = action;
            this.daysAway = daysAway;
        }


        /**
         * Decrease days away by 1
         * @return true if days away are 0
         */
        public boolean spendOneDay()
        {
            daysAway--;
            Preconditions.checkState(daysAway >=0);
            return daysAway == 0;

        }

        public ActionOrder getPhase() {
            return phase;
        }

        public Steppable getAction() {
            return action;
        }
    }



}