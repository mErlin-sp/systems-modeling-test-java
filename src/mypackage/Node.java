package mypackage;

import java.util.*;
import java.util.stream.Collectors;

interface PriorityFunction {
    int server_priority_function(Server srv, Individual ind);
}

interface BaulkingFunction {
    double baulking_function(double i);
}

public class Node extends NodeTop {
    Simulation simulation;
    private PriorityFunction server_priority_function;
    private Object service_discipline;
    private String next_event_type;
    private Schedule schedule;
    int c;
    boolean slotted;
    double next_event_date;
    private double next_shift_change;
     int node_capacity;
    private Map<String, List<Double>> transition_row;
    private Map<String, Map<String, Double>> class_change;
    private List<List<Individual>> individuals;
     int number_of_individuals;
    private int number_in_service;
    int id_number;
     Map<String, BaulkingFunction> baulking_functions;
    private List<Double> overtime;
    private List<List<Integer>> blocked_queue;
    private int len_blocked_queue;
    private List<Server> servers;
    private int highest_id;
    //    private DeadlockDetector deadlock_detector;
    private double priority_preempt = -1;
    private List<?> interrupted_individuals;
    private int number_interrupted_individuals;
    private List<Double> all_servers_total;
    private List<Double> all_servers_busy;
    private boolean reneging;
    private boolean dynamic_classes;
    private double next_class_change_date;
    private Individual next_class_change_ind;
    private List<Individual> next_individual;
    private double server_utilisation;
    private Map<String, Map<List<Individual>, Double>> possible_next_events;

    public Node(int id_, Simulation simulation, Class<? extends NodeTop> nodeType) {
        super(nodeType);
        this.simulation = simulation;
        ServiceCentre node = this.simulation.network.serviceCentres.get(id_ - 1);

        this.server_priority_function = node.serverPriorityFunction;
        this.service_discipline = node.serviceDiscipline;
        this.next_event_type = null;
//        if (node.numberOfServers instanceof Schedule) {
//            this.schedule = (Schedule) node.numberOfServers;
//            this.schedule.initialise();
//            this.c = this.schedule.c;
//            if (this.schedule.schedule_type.equals("slotted")) {
//                this.slotted = true;
//                this.next_event_date = this.schedule.next_slot_date;
//                this.next_event_type = "slotted_service";
//            } else {
//                this.slotted = false;
//                this.next_event_date = this.schedule.next_shift_change_date;
//                this.next_shift_change = this.schedule.next_shift_change_date;
//                this.next_event_type = "shift_change";
//            }
//        } else {
        this.c = node.numberOfServers;
        this.schedule = null;
        this.slotted = false;
        this.next_event_date = Double.POSITIVE_INFINITY;
        this.next_shift_change = Double.POSITIVE_INFINITY;
//        }

        this.node_capacity = node.queueingCapacity + this.c;
        if (!this.simulation.network.processBased) {
            this.transition_row = new HashMap<>();
            for (String clss : this.simulation.network.customerClassNames) {
                List<Double> routing = this.simulation.network.customerClasses.get(clss).routing.get(id_ - 1).stream().flatMap(List::stream).collect(Collectors.toList());
                routing.add(1.0 - routing.stream().mapToDouble(Double::doubleValue).sum());
                this.transition_row.put(clss, routing);
            }
        }
//        this.class_change = node.classChangeMatrix;
        this.individuals = new ArrayList<>();
        for (int i = 0; i < simulation.network.numberOfPriorityClasses; i++) {
            this.individuals.add(new ArrayList<>());
        }
        this.number_of_individuals = 0;
        this.number_in_service = 0;
        this.id_number = id_;
        this.baulking_functions = new HashMap<>();
        for (String clss : simulation.network.customerClassNames) {
            this.baulking_functions.put(clss, simulation.network.customerClasses.get(clss).baulkingFunctions.get(id_ - 1));
        }
        this.overtime = new ArrayList<>();
        this.blocked_queue = new ArrayList<>();
        this.len_blocked_queue = 0;
        if (!Double.isInfinite(this.c)) {
            this.servers = this.create_starting_servers();
        }
        this.highest_id = this.c;
//        this.deadlock_detector = simulation.deadlock_detector;
//        this.deadlock_detector.initialise_at_node(this);
        this.priority_preempt = node.priorityPreempt;
        this.interrupted_individuals = new ArrayList<>();
        this.number_interrupted_individuals = 0;
        this.all_servers_total = new ArrayList<>();
        this.all_servers_busy = new ArrayList<>();
        this.reneging = node.reneging;
        this.dynamic_classes = node.classChangeTime;
        this.next_class_change_date = Double.POSITIVE_INFINITY;
        this.next_individual = null;
    }

    public List<Server> create_starting_servers() {
        List<Server> servers = new ArrayList<>();
        for (int i = 0; i < this.c; i++) {
            servers.add(new Server(this, i + 1, 0.0));
        }
        return servers;
    }

    public double getNow() {
        /**
         * Gets the current time.
         */
        return this.simulation.current_time;
    }

    public List<Individual> getAllIndividuals() {
        if (this.simulation.network.numberOfPriorityClasses == 1) {
            return this.individuals.getFirst();
        }
        return flatten_list(this.individuals);
    }

    public <T> List<T> flatten_list(List<List<T>> list_of_lists) {
        List<T> flat = new ArrayList<>();
        for (List<T> a_list : list_of_lists) {
            flat.addAll(a_list);
        }
        return flat;
    }

    @Override
    public String toString() {
        /**
         * Representation of a node.
         */
        return "Node " + this.id_number;
    }

    public void accept(Individual next_individual) {
        /**
         * Accepts a new customer to the queue:
         * - remove previous exit date and blockage status
         * - see if possible to begin service
         * - record all other information at arrival point
         * - update state tracker
         */
        next_individual.node = this.id_number;
        next_individual.exit_date = -1;
        next_individual.is_blocked = false;
        next_individual.original_class = next_individual.customer_class;
        next_individual.queue_size_at_arrival = this.number_of_individuals;
        this.individuals.get(next_individual.priority_class).add((Individual) next_individual);
        this.number_of_individuals += 1;
        this.begin_service_if_possible_accept(next_individual);
//        this.simulation.statetracker.change_state_accept(this, (Individual) next_individual);
    }

    public void wrapUpServers(double currentTime) {
        /*
         * Updates the servers' total_time and busy_time
         * as the end of the simulation run.
         */
        if (!Double.isInfinite(this.c)) {
            for (Server srvr : this.servers) {
                srvr.total_time = this.increment_time(currentTime, -srvr.start_date);
                if (srvr.busy) {
                    srvr.busy_time += this.increment_time(currentTime, -srvr.cust.service_start_date);
                }
            }
        }
    }

//    public void addNewServers(int num_servers) {
//        /**
//         * Add appropriate amount of servers for the given shift.
//         */
//        for (int i = 0; i < num_servers; i++) {
//            this.highest_id += 1;
//            this.servers.add(new ServerType(this, this.highest_id, this.now));
//        }
//    }

    public void attachServer(Server server, Individual individual) {
        /**
         * Attaches a server to an individual, and vice versa.
         */
        server.cust = individual;
        server.busy = true;
        individual.server = server;
//        this.simulation.deadlock_detector.action_at_attach_server(this, server, individual);
    }

    public void begin_service_if_possible_accept(Individual next_individual) {
        /**
         * Begins the service of the next individual (at acceptance point):
         * - Sets the arrival date as the current time
         * - If there is a free server or there are infinite servers:
         * - Attach the server to the individual (only when servers are not infinite)
         * - Get service start time, service time, service end time
         * - Update the server's end date (only when servers are not infinite)
         */
        next_individual.arrival_date = this.getNow();
        if (this.reneging) {
            next_individual.reneging_date = this.getRenegingDate(next_individual);
        }
        this.decide_class_change(next_individual);

        Server free_server = this.find_free_server(next_individual);
        if (free_server == null && !Double.isInfinite(this.c) && this.c > 0) {
            this.decide_preempt(next_individual);
        }
        if (free_server != null || Double.isInfinite(this.c)) {
            if (!Double.isInfinite(this.c)) {
                this.attachServer(free_server, next_individual);
            }
            next_individual.service_start_date = this.getNow();
            next_individual.service_time = this.get_service_time(next_individual);
            next_individual.service_end_date = this.getNow() + next_individual.service_time;
            this.number_in_service += 1;
            this.resetClassChange(next_individual);
            if (!Double.isInfinite(this.c)) {
                free_server.next_end_service_date = next_individual.service_end_date;
            }
        }
    }

    public Server find_free_server(Individual ind) {
        /**
         * Finds a free server.
         */
        if (Double.isInfinite(this.c)) {
            return null;
        }

        List<Server> all_servers;
        if (this.server_priority_function == null) {
            all_servers = this.servers;
        } else {
            all_servers = new ArrayList<>(this.servers);
            all_servers.sort((srv1, srv2) -> this.server_priority_function.server_priority_function(srv1, ind) - this.server_priority_function.server_priority_function(srv2, ind));
        }

        for (Server svr : all_servers) {
            if (!svr.busy) {
                return svr;
            }
        }
        return null;
    }

    public void updateNextEventDate() {
        possible_next_events = new HashMap<>();
        updateNextEndServiceWithoutServer();
        updateNextEndServiceWithServer();
        updateNextRenegeTime();
        updateNextClassChangeWhileWaiting();
        updateNextShiftChangeOrSlotTime();
        if (reneging || dynamic_classes || schedule != null) {
            Map<Map<List<Individual>, Double>, String> nextEvent = decideNextEvent();
            next_event_type = nextEvent.values().stream().toList().getFirst();
            next_event_date = nextEvent.keySet().stream().toList().getFirst().values().stream().toList().getFirst();
            next_individual = nextEvent.keySet().stream().toList().getFirst().keySet().stream().toList().getFirst();;
        } else {
            Map<List<Individual>, Double> nextEndService = possible_next_events.get("end_service");
            next_event_date = nextEndService.values().stream().toList().getFirst();
            next_individual = nextEndService.keySet().stream().toList().getFirst();
            next_event_type = "end_service";
        }
    }

    public void updateNextEndServiceWithoutServer() {
        /*
         * Updates the next end of a slotted service in the `possible_next_events` dictionary.
         */
        if (this.slotted || Double.isInfinite(this.c)) {
            double nextEndServiceDate = Double.POSITIVE_INFINITY;
            for (Individual ind : this.getAllIndividuals()) {
                if (!ind.is_blocked && ind.service_end_date >= this.getNow()) {
                    if (ind.service_end_date < nextEndServiceDate)
                        this.possible_next_events.put("end_service", new HashMap<>() {{
                            put(List.of(ind), ind.service_end_date);
                        }});

                    nextEndServiceDate = ind.service_end_date;
                } else if ((ind.service_end_date == nextEndServiceDate) && (!Double.isInfinite(nextEndServiceDate))) {
                    this.possible_next_events.get("end_service").keySet().stream().toList().getFirst().add(ind);
                }
            }
        }
    }


    public void updateNextEndServiceWithServer() {
        /*
         * Updates the next end service with a server in the `possible_next_events` dictionary.
         */
        if (!this.slotted && !Double.isInfinite(this.c)) {
            double nextEndServiceDate = Double.POSITIVE_INFINITY;
            for (Server s : this.servers) {
                if (s.next_end_service_date < nextEndServiceDate) {
//                this.possible_next_events.put("end_service", (new ArrayList<Customer>(Arrays.asList(s.cust)), s.nextEndServiceDate))
                    ;
                    this.possible_next_events.put("end_service", new HashMap<>() {{
                        put(List.of(s.cust), s.next_end_service_date);
                    }});
                    nextEndServiceDate = s.next_end_service_date;
                } else if ((s.next_end_service_date == nextEndServiceDate) && (!Double.isInfinite(nextEndServiceDate))) {
                    this.possible_next_events.get("end_service").keySet().stream().toList().getFirst().add(s.cust);
                }
            }
        }
    }

    public void updateNextRenegeTime() {
        /*
         * Updates the next renege time in the `possible_next_events` dictionary.
         */
        if (!Double.isInfinite(this.c) && this.reneging) {
            double nextRenegeDate = Double.POSITIVE_INFINITY;
            for (Individual ind : this.getAllIndividuals()) {
                if ((ind.reneging_date < nextRenegeDate) && ind.server == null) {
                    this.possible_next_events.put("renege", new HashMap<>() {{
                        put(List.of(ind), ind.reneging_date);
                    }});
                    nextRenegeDate = ind.reneging_date;
                } else if ((ind.reneging_date == nextRenegeDate) && (ind.server == null) && (!Double.isInfinite(nextRenegeDate))) {
                    this.possible_next_events.get("renege").keySet().stream().toList().getFirst().add(ind);

                }
            }
        }
    }

    public void updateNextClassChangeWhileWaiting() {
        /*
         * Updates the next time to change a customer's class while waiting in the `possible_next_events` dictionary.
         */
        if (this.dynamic_classes && !Double.isInfinite(this.c)) {
            this.possible_next_events.put("class_change", new HashMap<>() {{
                put(List.of(next_class_change_ind), next_class_change_date);
            }});
        }
    }

    public void updateNextShiftChangeOrSlotTime() {
        /*
         * Updates the `possible_next_events` dictionary with the time of the next shift change or the next slotted service time.
         */
        if (this.schedule != null) {
            if (this.schedule.scheduleType.equals("schedule")) {
                this.possible_next_events.put("shift_change", new HashMap<>() {{
                    put(null, next_shift_change);
                }});
            }
            if (this.schedule.scheduleType.equals("slotted")) {
                this.possible_next_events.put("slotted_service", new HashMap<>() {{
                    put(null, schedule.next_slot_date);
                }});
            }
        }
    }

    public void decide_class_change(Object next_individual) {
        /**
         * Decides on the next_individual's next class and class change date
         */
        if (this.dynamic_classes) {
            double next_time = Double.POSITIVE_INFINITY;
            String next_class = ((Individual) next_individual).customer_class;
            for (Map.Entry<String, Distribution> entry : this.simulation.network.customerClasses.get(((Individual) next_individual).customer_class).classChangeTimeDistributions.entrySet()) {
                if (entry.getValue() != null) {
                    double t = 0;
                    if (t < next_time) {
                        next_time = t;
                        next_class = entry.getKey();
                    }
                }
            }
            ((Individual) next_individual).next_class = next_class;
            ((Individual) next_individual).class_change_date = this.increment_time(this.getNow(), next_time);
            this.find_next_class_change();
        }
    }

    public Map<Map<List<Individual>, Double>, String> decideNextEvent() {
        /*
         * Decides the next event. Chooses the next service end,
         * renege, shift change, or class change. In the case of
         * a tie, prioritise as follows:
         *     1) slotted service
         *     2) shift change
         *     3) end service
         *     4) class change
         *     5) renege
         */
        double nextDate = Double.POSITIVE_INFINITY;
        Map<List<Individual>, Double> nextEvent = new HashMap<>() {{
            put(null, Double.POSITIVE_INFINITY);
        }};
//        Pair<Object, Double> nextEvent = new Pair<>(null, Double.POSITIVE_INFINITY);
        String nextEventType = null;
        for (String eventType : Arrays.asList("slotted_service", "shift_change", "end_service", "class_change", "renege")) {

            Map<List<Individual>, Double> possibleNextEvent = this.possible_next_events.getOrDefault(eventType, new HashMap<>() {{
                put(null, Double.POSITIVE_INFINITY);
            }});
            if (possibleNextEvent.values().stream().toList().getFirst() < nextDate) {
                nextEvent = possibleNextEvent;
                nextEventType = eventType;
                nextDate = nextEvent.values().stream().toList().getFirst();
            }
        }
        String finalNextEventType = nextEventType;
        Map<List<Individual>, Double> finalNextEvent = nextEvent;
        return new HashMap<>() {{
            put(finalNextEvent, finalNextEventType);
        }};
    }

    public void decide_preempt(Individual individual) {
        /**
         * Decides if priority preemption is needed, finds the individual to preempt, and preempt them.
         */
        if (this.priority_preempt != -1) {
            int least_priority = Collections.max(this.servers, Comparator.comparing(s -> s.cust.priority_class)).cust.priority_class;
            if (((Individual) individual).priority_class < least_priority) {
                List<Individual> least_prioritised_individuals = new ArrayList<>();
                for (Server s : this.servers) {
                    if (s.cust.priority_class == least_priority) {
                        least_prioritised_individuals.add(s.cust);
                    }
                }
                Individual individual_to_preempt = Collections.max(least_prioritised_individuals, Comparator.comparing(cust -> ((Individual) cust).service_start_date));
                this.preempt(individual_to_preempt, individual);
            }
        }
    }


    public void preempt(Individual individual_to_preempt, Individual next_individual) {
        /**
         * Removes individual_to_preempt from service and replaces them with next_individual
         */
        Server server = individual_to_preempt.server;
        individual_to_preempt.original_service_time = individual_to_preempt.service_time;
        this.write_interruption_record(individual_to_preempt);
        individual_to_preempt.time_left = individual_to_preempt.service_end_date - this.getNow();
        individual_to_preempt.service_time = this.priority_preempt;
        this.detatch_server(server, individual_to_preempt);
        this.decide_class_change(individual_to_preempt);
        this.attachServer(server, next_individual);
        next_individual.service_start_date = this.getNow();
        next_individual.service_time = this.get_service_time(next_individual);
        next_individual.service_end_date = this.getNow() + next_individual.service_time;
        this.resetClassChange(next_individual);
        server.next_end_service_date = next_individual.service_end_date;
    }

    public void detatch_server(Server server, Individual individual) {
        /**
         * Detaches a server from an individual, and vice versa.
         */
//        this.simulation.deadlock_detector.action_at_detatch_server(server);
        server.cust = null;
        server.busy = false;
        individual.server = null;
        server.busy_time = this.increment_time(server.busy_time, individual.exit_date - individual.service_start_date);
        server.total_time = this.getNow() - server.start_date;
        if (server.offduty) {
            this.kill_server(server);
        }
    }

    public void kill_server(Server srvr) {
        /**
         * Kills a server when they go off duty.
         */
        srvr.total_time = this.increment_time(this.next_event_date, -srvr.start_date);
        this.overtime.add(this.increment_time(this.next_event_date, -srvr.shift_end));
        this.all_servers_busy.add(srvr.busy_time);
        this.all_servers_total.add(srvr.total_time);
        this.servers.remove(srvr);
    }

    public double get_service_time(Individual ind) {
        /**
         * Returns a service time for the given customer class.
         */
        return this.simulation.service_times.get(this.id_number).get(ind.customer_class).sample(this.getNow(), null);
    }

    public void writeIndividualRecord(Individual individual) {
        int serverId;
        if (Double.isInfinite(c) || slotted) {
            serverId = 0;
        } else {
            serverId = individual.server.id_number;
        }

        Map<String, String> record = new HashMap<>();
        record.put("id_number", String.valueOf(individual.id_number));
        record.put("server_id", String.valueOf(serverId));
        record.put("arrival_date", String.valueOf(individual.arrival_date));
        record.put("type", "service");

//        DataRecord record = new DataRecord(
//                individual.id_number,
//                individual.previous_class,
//                individual.original_class,
//                id_number,
//                individual.arrival_date,
//                individual.service_start_date - individual.arrival_date,
//                individual.service_start_date,
//                individual.service_end_date - individual.service_start_date,
//                individual.service_end_date,
//                individual.exit_date - individual.service_end_date,
//                individual.exit_date,
//                individual.destination,
//                individual.queue_size_at_arrival,
//                individual.queue_size_at_departure,
//                serverId,
//                "service"
//        );
        individual.data_records.add(record);
    }

    public void write_interruption_record(Individual individual) {
        /**
         * Write a data record for an individual when being interrupted.
         */
        Object server_id;
        if (this.slotted) {
            server_id = false;
        } else {
            server_id = individual.server.id_number;
        }

        Map<String, String> record = new HashMap<>();
        record.put("id_number", String.valueOf(individual.id_number));
        record.put("server_id", String.valueOf(server_id));
        record.put("arrival_date", String.valueOf(individual.arrival_date));
        record.put("type", "interrupted service");

//        DataRecord record = new DataRecord(
//                ((Individual) individual).id_number,
//                ((Individual) individual).previous_class,
//                ((Individual) individual).original_class,
//                this.id_number,
//                ((Individual) individual).arrival_date,
//                ((Individual) individual).service_start_date - ((Individual) individual).arrival_date,
//                ((Individual) individual).service_start_date,
//                ((Individual) individual).original_service_time,
//                Double.NaN,
//                Double.NaN,
//                this.getNow(),
//                Double.NaN,
//                ((Individual) individual).queue_size_at_arrival,
//                ((Individual) individual).queue_size_at_departure,
//                server_id,
//                "interrupted service"
//        );

        individual.data_records.add(record);

    }

    public void write_reneging_record(Individual individual) {


        Map<String, String> record = new HashMap<>();
        record.put("id_number", String.valueOf(individual.id_number));
        record.put("arrival_date", String.valueOf(individual.arrival_date));
        record.put("type", "renege");

        individual.data_records.add(record);

    }

    public void writeBaulkingOrRejectionRecord(Individual individual, String recordType) {
        /*
         * Write a data record for an individual baulks.
         */

        Map<String, String> record = new HashMap<>();
        record.put("id_number", String.valueOf(individual.id_number));
        record.put("arrival_date", String.valueOf(individual.arrival_date));
        record.put("type", recordType);

        individual.data_records.add(record);
//        DataRecord record = new DataRecord(
//                individual.idNumber,
//                individual.previousClass,
//                individual.originalClass,
//                this.idNumber,
//                this.now,
//                Double.NaN,
//                Double.NaN,
//                Double.NaN,
//                Double.NaN,
//                Double.NaN,
//                this.now,
//                Double.NaN,
//                this.number_of_individuals,
//                Double.NaN,
//                Double.NaN,
//                recordType
//        );
//        individual.dataRecords.add(record);
    }

    public double increment_time(double original, double increment) {
        /**
         * Increments the original time by the increment.
         */
        return original + increment;
    }

    public void find_next_class_change() {
        /**
         * Updates the next_class_change_date and next_class_change_ind
         */
        this.next_class_change_date = Double.POSITIVE_INFINITY;
        this.next_class_change_ind = null;
        for (Individual ind : this.getAllIndividuals()) {
            if ((ind.class_change_date < this.next_class_change_date) && (ind.server == null)) {
                this.next_class_change_ind = ind;
                this.next_class_change_date = ind.class_change_date;
            }
        }
    }

    public void resetClassChange(Individual individual) {
        if (dynamic_classes) {
            individual.class_change_date = Double.POSITIVE_INFINITY;
            if (individual == next_class_change_ind) {
                findNextClassChange();
            }
        }
    }

    public void release(Individual nextIndividual, Node nextNode) {
        individuals.get(nextIndividual.prev_priority_class).remove(nextIndividual);
        number_of_individuals -= 1;
        number_in_service -= 1;
        nextIndividual.queue_size_at_departure = number_of_individuals;
        nextIndividual.exit_date = getNow();
        writeIndividualRecord(nextIndividual);
        Server newlyFreeServer = null;
        if (!Double.isInfinite(c) && !slotted) {
            newlyFreeServer = nextIndividual.server;
            detatch_server(newlyFreeServer, nextIndividual);
        }
//        resetIndividualAttributes(nextIndividual);
//        simulation.statetracker.changeStateRelease(this, nextNode, nextIndividual, nextIndividual.is_blocked);
//        beginServiceIfPossibleRelease(nextIndividual, newlyFreeServer);
        nextNode.accept(nextIndividual);
        releaseBlockedIndividual();
    }

    public void releaseBlockedIndividual() {
        if (len_blocked_queue > 0 && number_of_individuals < node_capacity) {
            Node nodeToReceiveFrom = simulation.nodes.get(blocked_queue.getFirst().getFirst());
            int individualToReceiveIndex = nodeToReceiveFrom.getAllIndividuals().stream().map(i -> i.id_number).toList().indexOf(blocked_queue.getFirst().get(1));
            Individual individualToReceive = nodeToReceiveFrom.getAllIndividuals().get(individualToReceiveIndex);
            blocked_queue.removeFirst();
            len_blocked_queue -= 1;
            if (individualToReceive.interrupted) {
                individualToReceive.interrupted = false;
                individualToReceive.service_start_date = individualToReceive.original_service_start_date;
                individualToReceive.service_end_date = individualToReceive.service_start_date + individualToReceive.original_service_time;
                nodeToReceiveFrom.interrupted_individuals.remove(individualToReceive);
                nodeToReceiveFrom.number_interrupted_individuals -= 1;
            }
            nodeToReceiveFrom.release(individualToReceive, this);
        }
    }


    public void renege() {
        Individual renegingIndividual = decideBetweenSimultaneousIndividuals();
        renegingIndividual.reneging_date = Double.POSITIVE_INFINITY;
        int nextNodeNumber = simulation.network.customerClasses.get(renegingIndividual.customer_class).renegingDestinations.get(id_number - 1);
        Node nextNode = simulation.nodes.get(nextNodeNumber);
        individuals.get(renegingIndividual.prev_priority_class).remove(renegingIndividual);
        number_of_individuals -= 1;
        renegingIndividual.queue_size_at_departure = number_of_individuals;
        renegingIndividual.exit_date = getNow();
        write_reneging_record(renegingIndividual);
//        resetIndividualAttributes(renegingIndividual);
//        simulation.statetracker.changeStateRenege(this, nextNode, renegingIndividual, false);
        nextNode.accept(renegingIndividual);
        releaseBlockedIndividual();
    }

    public double getRenegingDate(Individual ind) {
        Distribution dist = simulation.network.customerClasses.get(ind.customer_class).renegingTimeDistributions.get(id_number - 1);
        if (dist == null) {
            return Double.POSITIVE_INFINITY;
        }
        return getNow() + dist.sample(getNow(), ind);
    }


    public void haveEvent() {
        switch (next_event_type) {
//            case "end_service" -> finishService();
//            case "shift_change" -> changeShift();
            case "renege" -> renege();
            case "class_change" -> changeCustomerClassWhileWaiting();
//            case "slotted_service" -> slottedService();
        }
    }

    public void finishService() throws Exception {
        Individual nextIndividual = decideBetweenSimultaneousIndividuals();
        changeCustomerClass(nextIndividual);
        Node nextNode = nextNode(nextIndividual);
        nextIndividual.destination = nextNode.id_number;
        if (!(Double.isInfinite(c)) && c > 0) {
            nextIndividual.server.next_end_service_date = Double.POSITIVE_INFINITY;
        }
        if (nextNode.number_of_individuals < nextNode.node_capacity) {
            release(nextIndividual, nextNode);
        } else {
//            blockIndividual(nextIndividual, nextNode);
        }
    }

    public Node nextNode(Individual ind) throws Exception {
        if (!simulation.network.processBased) {
            String customerClass = ind.customer_class;
            return Util.randomChoice(simulation.nodes.subList(1, simulation.nodes.size()), transition_row.get(customerClass));
        } else {
            if (ind.route.isEmpty() || !ind.route.getFirst().equals(id_number)) {
                throw new Exception("Individual process route sent to wrong node");
            }
            ind.route.removeFirst();
            int nextNodeNumber;
            if (ind.route.isEmpty()) {
                nextNodeNumber = -1;
            } else {
                nextNodeNumber = ind.route.getFirst();
            }
            return simulation.nodes.get(nextNodeNumber);
        }
    }

    public Individual decideBetweenSimultaneousIndividuals() {
        Individual nextIndividual;
        if (next_individual.size() > 1) {
            nextIndividual = next_individual.get((int) (Math.random() * next_individual.size()));
        } else {
            nextIndividual = next_individual.getFirst();
        }
        return nextIndividual;
    }

    public void findNextClassChange() {
        next_class_change_date = Double.POSITIVE_INFINITY;
        next_class_change_ind = null;
        for (Individual ind : getAllIndividuals()) {
            if (ind.class_change_date < next_class_change_date && ind.server == null) {
                next_class_change_ind = ind;
                next_class_change_date = ind.class_change_date;
            }
        }
    }

    public void findServerUtilisation() {
        if (Double.isInfinite(c) || c == 0) {
            this.server_utilisation = -1;
        } else {
            for (Server server : servers) {
                all_servers_total.add(server.total_time);
                all_servers_busy.add(server.busy_time);
            }
            server_utilisation = all_servers_busy.stream().reduce(Double::sum).get() / all_servers_total.stream().reduce(Double::sum).get();
        }
    }

    public void changeCustomerClass(Individual individual) {
        if (this.class_change != null) {
            individual.previous_class = individual.customer_class;
            individual.customer_class = Util.randomChoice(simulation.network.customerClassNames, class_change.get(individual.previous_class).values().stream().toList());
            individual.prev_priority_class = individual.priority_class;
            individual.priority_class = simulation.network.priorityClassMapping.get(individual.customer_class);
        }
    }

    public void changeCustomerClassWhileWaiting() {
        Individual changingIndividual = this.next_individual.getFirst();
        changingIndividual.customer_class = changingIndividual.next_class;
        changingIndividual.priority_class = simulation.network.priorityClassMapping.get(changingIndividual.next_class);
        if (changingIndividual.priority_class != changingIndividual.prev_priority_class) {
            changePriorityQueue(changingIndividual);
            decidePreempt(changingIndividual);
        }
//        simulation.statetracker.changeStateClasschange(this, changingIndividual);
        changingIndividual.previous_class = changingIndividual.next_class;
        changingIndividual.prev_priority_class = changingIndividual.priority_class;
        decideClassChange(changingIndividual);
    }

    public void changePriorityQueue(Individual individual) {
        individuals.get(individual.prev_priority_class).remove(individual);
        individuals.get(individual.priority_class).add(individual);
    }

//    public void changeShift() {
//        schedule.getNextShift();
//        next_shift_change = schedule.nextShiftChangeDate;
//        c = schedule.c;
//        takeServersOffDuty(schedule.preemption);
//        addNewServers(schedule.c);
//        beginServiceIfPossibleChangeShift();
//    }
//
//    public int findNumberOfSlottedServices() {
//        if (schedule.capacitated) {
//            return Math.min(Math.max(schedule.slot_size - number_in_service, 0), getAllIndividuals().size());
//        }
//        return Math.min(schedule.slot_size, getAllIndividuals().size());
//    }

    public void decideClassChange(Individual nextIndividual) {
        if (dynamic_classes) {
            double nextTime = Double.POSITIVE_INFINITY;
            String nextClass = nextIndividual.customer_class;
            for (Map.Entry<String, Distribution> entry : simulation.network.customerClasses.get(nextIndividual.customer_class).classChangeTimeDistributions.entrySet()) {
                if (entry.getValue() != null) {
                    double t = entry.getValue().sample(0, null);
                    if (t < nextTime) {
                        nextTime = t;
                        nextClass = entry.getKey();
                    }
                }
            }
            nextIndividual.next_class = nextClass;
            nextIndividual.class_change_date = increment_time(getNow(), nextTime);
            findNextClassChange();
        }
    }

    public void decidePreempt(Individual individual) {
        if (priority_preempt != -1) {
            int leastPriority = Collections.max(servers, Comparator.comparing(s -> s.cust.priority_class)).cust.priority_class;
            if (individual.priority_class < leastPriority) {
                ArrayList<Individual> leastPrioritisedIndividuals = new ArrayList<Individual>();
                for (Server s : servers) {
                    if (s.cust.priority_class == leastPriority) {
                        leastPrioritisedIndividuals.add(s.cust);
                    }
                }
                Individual individualToPreempt = Collections.max(leastPrioritisedIndividuals, Comparator.comparing(cust -> cust.service_start_date));
                preempt(individualToPreempt, individual);
            }
        }
    }


}