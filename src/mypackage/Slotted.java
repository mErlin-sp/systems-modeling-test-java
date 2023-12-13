package mypackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Slotted extends Schedule {
    /**
     * A Slotted Schedule class, for storing information about server schedules and generating the next shifts.
     */
    private boolean capacitated;
    private List<Integer> slots;
    private List<Integer> slotSizes;
    private Integer slotSize;

    public Slotted(List<Map.Entry<Integer, Integer>> schedule, List<Integer> slots, String preemption, boolean capacitated) throws Exception {
        super(schedule, preemption);
        if (!capacitated && !preemption.equals(false)) {
            throw new Exception("Pre-emption options not available for non-capacitated slots.");
        }
        this.scheduleType = "slotted";
        this.slots = slots;
        this.slotSizes = new ArrayList<>();
        this.capacitated = capacitated;
        for (int i = 0; i < slots.size(); i++) {
            if (i == slots.size() - 1) {
                this.slotSizes.add(schedule.get(schedule.size() - 1).getKey());
            } else {
                this.slotSizes.add(slots.get(i + 1));
            }
        }
        this.cycleLength = this.slots.get(this.slots.size() - 1);
    }

    public void initialise() {
        /**
         * Initialises the generator object at the beginning of a simulation
         */
        this.scheduleGenerator = this.getScheduleGenerator(this.slots, this.slotSizes);
        this.getNextSlot();
    }

    public void getNextSlot() {
        /**
         * Updates the next slot time and size from the generator
         */
        Map.Entry<Integer, Integer> entry = this.scheduleGenerator.next();
        this.nextShiftChangeDate = entry.getKey();
        this.slotSize = entry.getValue();
    }
}
