package mypackage;

import java.util.ArrayList;
import java.util.List;

public class ExitNode extends Node {
    /**
     * Class for the exit node on our network.
     */

    public List<Individual> all_individuals;
    public int number_of_individuals;
    public int number_of_completed_individuals;
    public int id_number;
    public double next_event_date;
    public double node_capacity;

    public ExitNode(Simulation simulation, Class<? extends NodeTop> nodeType) {
        /**
         * Initialise the exit node.
         */
        super(-1, simulation, nodeType);
        this.all_individuals = new ArrayList<>();
        this.number_of_individuals = 0;
        this.number_of_completed_individuals = 0;
        this.id_number = -1;
        this.next_event_date = Double.POSITIVE_INFINITY;
        this.node_capacity = Double.POSITIVE_INFINITY;
    }

    @Override
    public String toString() {
        /**
         * Representation of the exit node.
         */
        return "Exit Node";
    }

    @Override
    public void accept(Individual next_individual, boolean completed) {
        /**
         * Adds individual to the list of completed individuals.
         */
        this.all_individuals.add(next_individual);
        this.number_of_individuals += 1;
        if (completed) {
            this.number_of_completed_individuals += 1;
        }
    }

    @Override
    public void updateNextEventDate() {
        /**
         * Finds the time of the next event at this node
         * Just passes as next_event_date always set to
         * the max_simulation_time
         */
    }
}
