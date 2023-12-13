package mypackage;

public class ExactArrivalNode extends ArrivalNode {
    public ExactArrivalNode(Simulation simulation) {
        super(simulation, ExactArrivalNode.class);
    }

    /**
     * Inherits from the ArrivalNode class, implements a more precise version of addition to fix discrepancies with floating point numbers.
     */

    @Override
    public double increment_time(double original, double increment) {
        /**
         * Increments the original time by the increment
         */
        return original + increment;
    }

    @Override
    public Double inter_arrival(Integer nd, String clss) {
        /**
         * Samples the inter-arrival time for next class and node.
         */
        return this.simulation.inter_arrival_times.get(nd).get(clss)._sample(this.simulation.current_time, null);
    }
}
