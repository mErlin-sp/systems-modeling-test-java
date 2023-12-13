package mypackage;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class Schedule {
    /**
     * A Schedule class, for storing information about server schedules and generating the next shifts.
     */
    String preemption;
    String scheduleType;
    private List<Integer> scheduleDates;
    private List<Integer> scheduleServers;
    int cycleLength;
    private int nextC;
    int c;
    double nextShiftChangeDate;
    public double next_slot_date;
    public boolean capacitated;
    public int slotSize;

    Iterator<Map.Entry<Integer, Integer>> scheduleGenerator;

    public Schedule(List<Map.Entry<Integer, Integer>> schedule, String preemption) throws Exception {
        /**
         * Initialises the instance of the Schedule object
         */
        if (!List.of(false, "resume", "restart", "resample").contains(preemption)) {
            throw new Exception("Pre-emption options should be either 'resume', 'restart', 'resample', or False.");
        }
        this.scheduleType = "schedule";
        this.scheduleDates = new ArrayList<>();
        this.scheduleServers = new ArrayList<>();
        for (Map.Entry<Integer, Integer> shift : schedule) {
            this.scheduleDates.add(shift.getValue());
            this.scheduleServers.add(shift.getKey());
        }
        this.preemption = preemption;
        this.cycleLength = this.scheduleDates.get(this.scheduleDates.size() - 1);
    }

    public void initialise() {
        /**
         * Initialises the generator object at the beginning of a simulation
         */
        this.nextC = this.scheduleServers.get(0);
        this.scheduleGenerator = this.getScheduleGenerator(this.scheduleDates, this.scheduleServers);
        this.getNextShift();
    }

    Iterator<Map.Entry<Integer, Integer>> getScheduleGenerator(List<Integer> boundaries, List<Integer> values) {
        /**
         * A generator that yields the next time and number of servers according to a given schedule.
         */
        int numBoundaries = boundaries.size();
        int index = 0;
        int date = 0;
        List<Map.Entry<Integer, Integer>> result = new ArrayList<>();
        while (true) {
            date = boundaries.get(index % numBoundaries) + ((index) / numBoundaries * this.cycleLength);
            index += 1;
            result.add(Map.entry(date, values.get(index % numBoundaries)));
            if (result.size() == 2) {
                break;
            }
        }
        return result.iterator();
    }

    void getNextShift() {
        /**
         * Updates the next shifts from the generator
         */
        this.c = this.nextC;
        Map.Entry<Integer, Integer> entry = this.scheduleGenerator.next();
        this.nextShiftChangeDate = entry.getKey();
        this.nextC = entry.getValue();
    }

}

