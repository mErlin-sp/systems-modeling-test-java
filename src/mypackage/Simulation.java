package mypackage;

import java.lang.reflect.InvocationTargetException;
import java.util.*;

public class Simulation {
    double current_time = 0.0;
    Network network;
    private Class<?> node_class;
    private Class<?> arrival_node_class;
    private Class<?> individual_class;
    private Class<?> server_class;
    private boolean exact = false;
    private String name = "Simulation";
    //    private DeadlockDetector deadlock_detector = new NoDetection();
    private List<Class<? extends Node>> NodeTypes;
    private Class<? extends Node> ArrivalNodeType;
    private int number_of_priority_classes;
    List<Node> transitive_nodes;
    public List<Node> nodes;
    private List<Node> active_nodes;
    Map<Integer, Map<String, Distribution>> inter_arrival_times;
    Map<Integer, Map<String, Distribution>> service_times;
    Map<Integer, Map<String, Distribution>> batch_sizes;
    //    private StateTracker statetracker;
    private Map<String, Double> times_dictionary;
    private Map<String, Double> times_to_deadlock;
    private boolean unchecked_blockage = false;
    private List<Map<String, String>> all_records;

    public Simulation(Network network) throws NoSuchMethodException, InvocationTargetException, InstantiationException, IllegalAccessException {
        this.network = network;

        this.set_classes(Node.class, ArrivalNode.class, Individual.class, Server.class);
        if (exact) {
            this.NodeTypes = new ArrayList<>(Collections.nCopies(network.numberOfNodes, ExactNode.class));
            this.ArrivalNodeType = ExactArrivalNode.class;
        }
        this.name = name;
//        this.deadlock_detector = deadlock_detector;
        this.inter_arrival_times = this.findArrivalDists();
        this.service_times = this.findServiceDists();
        this.batch_sizes = this.findBatchingDists();
        this.show_simulation_to_distributions();
        this.number_of_priority_classes = this.network.numberOfPriorityClasses;
        this.transitive_nodes = new ArrayList<>();
        for (int i = 0; i < this.NodeTypes.size(); i++) {
            this.transitive_nodes.add(this.NodeTypes.get(i).getDeclaredConstructor(int.class, Simulation.class, Class.class).newInstance(i + 1, this, this.NodeTypes.get(i)));
        }
        this.nodes = new ArrayList<>();
        this.nodes.add(this.ArrivalNodeType.getDeclaredConstructor(Simulation.class, Class.class).newInstance(this, ArrivalNodeType));
        this.nodes.addAll(this.transitive_nodes);
        this.nodes.add(new ExitNode());

        this.active_nodes = this.nodes.subList(0, this.nodes.size() - 1);
        this.nodes.getFirst().initialise();
//        if (tracker == null) {
//            this.statetracker = new StateTracker();
//        } else {
//            this.statetracker = tracker;
//        }
//        this.statetracker.initialise(this);
//        this.times_dictionary = new HashMap<>();
//        this.times_dictionary.put(this.statetracker.hash_state(), 0.0);
//        this.times_to_deadlock = new HashMap<>();
    }

    public void simulateUntilMaxTime(double maxSimulationTime, boolean progressBar) {
        Node nextActiveNode = findNextActiveNode();
        current_time = nextActiveNode.next_event_date;

//        if (progressBar) {
//            ProgressBar pb = new ProgressBar();
//            pb.setTotal(maxSimulationTime);
//        }

        while (current_time < maxSimulationTime) {
            nextActiveNode = eventAndReturnNextnode(nextActiveNode);
//            statetracker.timestamp();

//            if (progressBar) {
//                double remainingTime = maxSimulationTime - pb.n;
//                double timeIncrement = nextActiveNode.next_event_date - current_time;
//                pb.update(Math.min(timeIncrement, remainingTime));
//            }

            current_time = nextActiveNode.next_event_date;
        }

        wrapUpServers(maxSimulationTime);
//        if (progressBar) {
//            double remainingTime = Math.max(maxSimulationTime - pb.n, 0);
//            pb.update(remainingTime);
//            pb.close();
//        }
    }

    public void wrapUpServers(double currentTime) {
        /*
         * Updates the servers' total_time and busy_time as
         * the end of the simulation run. Finds the overall
         * server utilisation for each node.
         */
        for (Node nd : this.transitive_nodes) {
            nd.wrapUpServers(currentTime);
            nd.findServerUtilisation();
        }
    }


    public Node eventAndReturnNextnode(Node nextActiveNode) {
        nextActiveNode.haveEvent();
        for (Node node : transitive_nodes) {
            node.updateNextEventDate();
        }
        return findNextActiveNode();
    }

    public Node findNextActiveNode() {
        double mindate = Double.POSITIVE_INFINITY;
        ArrayList<Node> nextActiveNodes = new ArrayList<>();
        for (Node nd : active_nodes) {
            if (nd.next_event_date < mindate) {
                mindate = nd.next_event_date;
                nextActiveNodes = new ArrayList<>();
                nextActiveNodes.add(nd);
            } else if (nd.next_event_date == mindate) {
                nextActiveNodes.add(nd);
            }
        }
        if (nextActiveNodes.size() > 1) {
            return Util.randomChoice(nextActiveNodes, null);
        }
        return nextActiveNodes.getFirst();
    }

    public List<Individual> getAllIndividuals() {
        List<Individual> individuals = new ArrayList<>();
        for (Node node : nodes.subList(1, nodes.size())) {
            for (Individual individual : node.getAllIndividuals()) {
                if (individual.data_records.size() > 0) {
                    individuals.add(individual);
                }
            }
        }
        return individuals;
    }

    public List<Map<String, String>> getAllRecords(ArrayList<String> only) {
        List<Map<String, String>> records = new ArrayList<>();
        for (Individual individual : getAllIndividuals()) {
            for (Map<String, String> record : individual.data_records) {
                if (only.contains(record.get("type"))) {
                    records.add(record);
                }
            }
        }
        this.all_records = records;
        return records;
    }

    public Map<Integer, Map<String, Distribution>> findArrivalDists() {
        Map<Integer, Map<String, Distribution>> result = new HashMap<>();
        for (int node = 0; node < this.network.numberOfNodes; node++) {
            Map<String, Distribution> innerMap = new HashMap<>();
            for (String clss : this.network.customerClassNames) {
                innerMap.put(clss, this.network.customerClasses.get(clss).arrivalDistributions.get(node));
            }
            result.put(node + 1, innerMap);
        }
        return result;
    }

    public Map<Integer, Map<String, Distribution>> findServiceDists() {
        Map<Integer, Map<String, Distribution>> result = new HashMap<>();
        for (int node = 0; node < this.network.numberOfNodes; node++) {
            Map<String, Distribution> innerMap = new HashMap<>();
            for (String clss : this.network.customerClassNames) {
                innerMap.put(clss, this.network.customerClasses.get(clss).serviceDistributions.get(node));
            }
            result.put(node + 1, innerMap);
        }
        return result;
    }

    public Map<Integer, Map<String, Distribution>> findBatchingDists() {
        Map<Integer, Map<String, Distribution>> result = new HashMap<>();
        for (int node = 0; node < this.network.numberOfNodes; node++) {
            Map<String, Distribution> innerMap = new HashMap<>();
            for (String clss : this.network.customerClassNames) {
                innerMap.put(clss, this.network.customerClasses.get(clss).batchingDistributions.get(node));
            }
            result.put(node + 1, innerMap);
        }
        return result;
    }

    public void show_simulation_to_distributions() {
        /**
         * Adds the simulation object as an attribute of the distribution objects
         */
        for (String clss : this.network.customerClassNames) {
            for (int nd = 0; nd < this.network.numberOfNodes; nd++) {
                if (this.inter_arrival_times.get(nd + 1).get(clss) != null) {
                    this.inter_arrival_times.get(nd + 1).get(clss).simulation = this;
                    this.service_times.get(nd + 1).get(clss).simulation = this;
                    this.batch_sizes.get(nd + 1).get(clss).simulation = this;
                }
            }
        }
    }

    public void set_classes(Class<? extends Node> node_class, Class<? extends Node> arrival_node_class, Class<?> individual_class, Class<?> server_class) {
        this.ArrivalNodeType = Objects.requireNonNullElse(arrival_node_class, ArrivalNode.class);

        if (node_class != null) {
            this.NodeTypes = new ArrayList<>((Collections.nCopies(network.numberOfNodes, node_class)));
        } else {
            this.NodeTypes = Collections.nCopies(this.network.numberOfNodes, Node.class);
        }

        this.individual_class = Objects.requireNonNullElse(individual_class, Individual.class);

        this.server_class = Objects.requireNonNullElse(server_class, Server.class);
    }
}