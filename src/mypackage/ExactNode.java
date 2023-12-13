package mypackage;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class ExactNode extends Node {
    public ExactNode(int id_, Simulation simulation) {
        super(id_, simulation,ExactNode.class);
    }

    /**
     * Inherits from the Node class, implements a more precise version of addition to fix discrepancies with floating point numbers.
     */

    @Override
    public double getNow() {
        /**
         * Gets the current time
         */
        return this.simulation.current_time;
    }

    @Override
    public List<Server> create_starting_servers() {
        /**
         * Initialise the servers
         */
        List<Server> servers = new ArrayList<>();
        for (int i = 0; i < this.c; i++) {
            servers.add(new Server(this, i + 1, 0));
        }
        return servers;
    }

    @Override
    public double increment_time(double original, double increment) {
        /**
         * Increments the original time by the increment
         */
        return original + increment;
    }

    @Override
    public double get_service_time(Individual ind) {
        /**
         * Returns a service time for the given customer class
         */
        return this.simulation.service_times.get(this.id_number).get(ind.customer_class)._sample(this.simulation.current_time, ind);
    }
}

