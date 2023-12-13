package mypackage;

import java.util.HashMap;
import java.util.Map;

public class ArrivalNode extends Node {
    /**
     * Class for the arrival node of the network.
     * <p>
     * See Also
     * --------
     * ciw.Simulation : Main simulation class.
     * <p>
     * Notes
     * -----
     * This class handles arrivals, baulking, and rejections of individuals.
     */

    final Simulation simulation;
    private int numberOfIndividuals = 0;
    private final Map<String, Integer> numberOfIndividualsPerClass;
    private int numberOfAcceptedIndividuals = 0;
    private final Map<String, Integer> numberOfAcceptedIndividualsPerClass;
    private final Map<Integer, Map<String, Double>> eventDatesDict;
    private Integer next_node;
    private String next_class;

    public ArrivalNode(Simulation simulation, Class<? extends NodeTop> nodeType) {
        /**
         * Initialise the arrival node.
         *
         * Parameters
         * ----------
         * simulation : object
         * The simulation to which this arrival node belongs.
         */
        super(-1, simulation, nodeType);
        this.simulation = simulation;
        numberOfIndividualsPerClass = new HashMap<>();
        for (String clss : this.simulation.network.customerClassNames) {
            numberOfIndividualsPerClass.put(clss, 0);
        }
        numberOfAcceptedIndividualsPerClass = new HashMap<>();
        for (String clss : this.simulation.network.customerClassNames) {
            numberOfAcceptedIndividualsPerClass.put(clss, 0);
        }
        eventDatesDict = new HashMap<>();
        for (int nd = 0; nd < this.simulation.network.numberOfNodes; nd++) {
            Map<String, Double> innerMap = new HashMap<>();
            for (String clss : this.simulation.network.customerClassNames) {
                innerMap.put(clss, -1.0);
            }
            eventDatesDict.put(nd + 1, innerMap);
        }
    }

    @Override
    public void initialise() {
        initialise_event_dates_dict();
        find_next_event_date();
    }

    @Override
    public String toString() {
        return "Arrival Node";
    }

    public void decideBaulk(Node nextNode, Individual nextIndividual) {
        /*
         * Either makes an individual baulk, or sends the individual
         * to the next node.
         */
        if (nextNode.baulking_functions.get(this.next_class) == null) {
            this.send_individual(nextNode, nextIndividual);
        } else {
            double rndNum = Math.random();
            if (rndNum < nextNode.baulking_functions.get(this.next_class).baulking_function(nextNode.number_of_individuals)) {
                this.recordBaulk(nextNode, nextIndividual);
                this.simulation.nodes.getLast().accept(nextIndividual,false);
            } else {
                this.send_individual(nextNode, nextIndividual);
            }
        }
    }

    public void recordBaulk(Node nextNode, Individual individual) {
        /*
         * Adds an individual to the baulked dictionary.
         */
        nextNode.writeBaulkingOrRejectionRecord(individual, "baulk");
    }

    public void recordRejection(Node nextNode, Individual individual) {
        /*
         * Adds an individual to the rejection dictionary.
         */
        nextNode.writeBaulkingOrRejectionRecord(individual, "rejection");
    }


    public void find_next_event_date() {
        /**
         * Finds the time of the next arrival.
         */
        Integer minnd = null;
        String minclss = null;
        Double mindate = Double.POSITIVE_INFINITY;
        for (Integer nd : this.eventDatesDict.keySet()) {
            for (String clss : this.eventDatesDict.get(nd).keySet()) {
                if (this.eventDatesDict.get(nd).get(clss) < mindate) {
                    minnd = nd;
                    minclss = clss;
                    mindate = this.eventDatesDict.get(nd).get(clss);
                }
            }
        }
        this.next_node = minnd;
        this.next_class = minclss;
        this.next_event_date = mindate;
    }

    @Override
    public void haveEvent() {
        /*
         * Finds a batch size. Creates that many Individuals and send
         * them to the relevent node. Then updates the eventDatesDict.
         */
        int batch = this.batch_size(this.next_node, this.next_class);
        for (int i = 0; i < batch; i++) {
            this.numberOfIndividuals += 1;
            this.numberOfIndividualsPerClass.put(this.next_class, this.numberOfIndividualsPerClass.get(this.next_class) + 1);
            int priorityClass = this.simulation.network.priorityClassMapping.get(this.next_class);
            Individual nextIndividual = new Individual(
                    this.numberOfIndividuals,
                    this.next_class,
                    priorityClass,
                    this.simulation
            );
//            if (this.simulation.network.processBased) {
//                nextIndividual.route = this.simulation.network.customerClasses.get(nextIndividual.customer_class).routing.get(this.next_node - 1).route(nextIndividual);
//            }
            Node nextNode = this.simulation.transitive_nodes.get(this.next_node - 1);
            this.release_individual(nextNode, nextIndividual);
        }

        this.eventDatesDict.get(this.next_node).put(
                this.next_class,
                this.increment_time(
                        this.eventDatesDict.get(this.next_node).get(this.next_class),
                        this.inter_arrival(this.next_node, this.next_class)
                )
        );
        this.find_next_event_date();
    }

    @Override
    public double increment_time(double original, double increment) {
        /**
         * Increments the original time by the increment.
         */
        return original + increment;
    }

    public void initialise_event_dates_dict() {
        /**
         * Initialises the next event dates dictionary
         * with random times for each node and class.
         */
        for (Integer nd : this.eventDatesDict.keySet()) {
            for (String clss : this.eventDatesDict.get(nd).keySet()) {
                if (this.simulation.inter_arrival_times.get(nd).get(clss) != null) {
                    this.eventDatesDict.get(nd).put(clss, this.inter_arrival(nd, clss));
                } else {
                    this.eventDatesDict.get(nd).put(clss, Double.POSITIVE_INFINITY);
                }
            }
        }
    }

    public Double inter_arrival(Integer nd, String clss) {
        /**
         * Samples the inter-arrival time for next class and node.
         */
        return this.simulation.inter_arrival_times.get(nd).get(clss)._sample(this.simulation.current_time, null);
    }

    public int batch_size(Integer nd, String clss) {
        /**
         * Samples the batch size for next class and node.
         * Raises error if a positive integer is not sampled.
         */
        int batch = (int) this.simulation.batch_sizes.get(nd).get(clss)._sample(this.simulation.current_time, null);
        if (batch >= 0) {
            return batch;
        }
        throw new IllegalArgumentException("Batch sizes must be positive integers.");
    }

    public void record_baulk(Node nextNode, Individual individual) {
        /**
         * Adds an individual to the baulked dictionary.
         */
        nextNode.writeBaulkingOrRejectionRecord(individual, "baulk");
    }

    public void record_rejection(Node nextNode, Individual individual) {
        /**
         * Adds an individual to the rejection dictionary.
         */
        nextNode.writeBaulkingOrRejectionRecord(individual, "rejection");
    }

    public void release_individual(Node nextNode, Individual nextIndividual) {
        /**
         * Either rejects the next_individual die to lack of capacity,
         * or sends that individual to baulk or not.
         */
        if (nextNode.number_of_individuals >= nextNode.node_capacity) {
            this.record_rejection(nextNode, nextIndividual);
            this.simulation.nodes.getLast().accept(nextIndividual,false);
        } else {
            this.decideBaulk(nextNode, nextIndividual);
        }
    }

    public void send_individual(Node nextNode, Individual nextIndividual) {
        /**
         * Sends the next_individual to the next_node.
         */
        this.numberOfAcceptedIndividuals += 1;
        this.numberOfAcceptedIndividualsPerClass.put(nextIndividual.customer_class, this.numberOfAcceptedIndividualsPerClass.get(nextIndividual.customer_class) + 1);
        nextNode.accept(nextIndividual,false);
    }

    public void update_next_event_date() {
        /**
         * Passes, as updating next event happens at time of event.
         */
        return;
    }
}