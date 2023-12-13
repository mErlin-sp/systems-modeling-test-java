package mypackage;


import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class NetworkCreator {

    public static Network createNetwork(Map<String, Object> params) {
        if (!params.containsKey("arrival_distributions") ||
                !params.containsKey("number_of_servers") ||
                !params.containsKey("service_distributions")) {
            throw new IllegalArgumentException("arrival_distributions, service_distributions, and number_of_servers are required arguments.");
        }

        Map<String, Object> networkParams = new HashMap<>();
        networkParams.put("arrival_distributions", params.get("arrival_distributions"));
        networkParams.put("number_of_servers", params.get("number_of_servers"));
        networkParams.put("service_distributions", params.get("service_distributions"));

        if (params.get("baulking_functions") != null) {
            networkParams.put("baulking_functions", params.get("baulking_functions"));
        }
        if (params.containsKey("class_change_matrices")) {
            networkParams.put("class_change_matrices", params.get("class_change_matrices"));
        }
        if (params.containsKey("class_change_time_distributions")) {
            networkParams.put("class_change_time_distributions", params.get("class_change_time_distributions"));
        }
        if (params.containsKey("priority_classes")) {
            networkParams.put("priority_classes", params.get("priority_classes"));
        }
        if (params.containsKey("queue_capacities")) {
            networkParams.put("queue_capacities", params.get("queue_capacities"));
        }
        if (params.containsKey("routing")) {
            networkParams.put("routing", params.get("routing"));
        }
        if (params.containsKey("batching_distributions")) {
            networkParams.put("batching_distributions", params.get("batching_distributions"));
        }
        if (params.containsKey("server_priority_functions")) {
            networkParams.put("server_priority_functions", params.get("server_priority_functions"));
        }
        if (params.containsKey("reneging_time_distributions")) {
            networkParams.put("reneging_time_distributions", params.get("reneging_time_distributions"));
        }
        if (params.containsKey("reneging_destinations")) {
            networkParams.put("reneging_destinations", params.get("reneging_destinations"));
        }
        if (params.containsKey("service_disciplines")) {
            networkParams.put("service_disciplines", params.get("service_disciplines"));
        }

        return createNetworkFromDictionary(networkParams);
    }

    public static Network createNetworkFromDictionary(Map<String, Object> paramsInput) {

        Map<String, Object> params = fillOutDictionary(paramsInput);

        int numberOfClasses = (int) params.get("number_of_classes");
        int numberOfNodes = (int) params.get("number_of_nodes");

        List<Double> preemptPriorities = new ArrayList<>();
        if (params.containsKey("priority_classes") && params.get("priority_classes") instanceof Map<?, ?>) {
            preemptPriorities = Collections.nCopies(numberOfNodes, 0.0);
//            preemptPriorities = Collections.singletonList(Stream.generate(() -> false)
//                    .limit(numberOfNodes)
//                    .toList());
        }
        if (params.containsKey("priority_classes") && params.get("priority_classes") instanceof List<?>) {
            preemptPriorities = ((List<List<Double>>) params.get("priority_classes")).get(1);
            params.put("priority_classes", Collections.singletonMap("clss", ((List<String>) params.get("customer_class_names")).stream().map(clss -> (List<?>) ((Map<?, ?>) ((List<?>) params.get("priority_classes")).getFirst()).get(clss))));

            List<List<Boolean>> priorityClasses = new ArrayList<>();
            for (int i = 0; i < ((List<?>) params.get("customer_class_names")).size(); i++) {
                List<Boolean> innerList = new ArrayList<>();
                for (int j = 0; j < ((List<?>) params.get("number_of_servers")).size(); j++) {
                    innerList.add(((List<List<Boolean>>) params.get("priority_classes")).get(0).get(((List<Integer>) params.get("customer_class_names")).get(i)));
                }
                priorityClasses.add(innerList);
            }
            params.put("priority_classes", priorityClasses);
        }

        params.putIfAbsent("class_change_matrices", Collections.nCopies(numberOfNodes, new ArrayList<>()));
        List<List<Double>> classChangeMatrices = (List<List<Double>>) params.get("class_change_matrices");

        Map<String, Map<String, ?>> classChangeTimeDistributions = new HashMap<>();
        for (String clss1 : (List<String>) params.get("customer_class_names")) {
            Map<String, Object> innerMap = new HashMap<>();
            for (String clss2 : (List<String>) params.get("customer_class_names")) {
                try {
                    classChangeTimeDistributions.put(clss1, Collections.singletonMap(clss2, ((Map<?, ?>) ((Map<?, ?>) params.get("class_change_time_distributions")).get(clss1)).get(clss2)));
                } catch (Exception e) {
                }
            }
        }

        List<ServiceCentre> nodes = new ArrayList<>();
        Map<String, CustomerClass> classes = new HashMap<>();

        for (int nd = 0; nd < numberOfNodes; nd++) {
            nodes.add(new ServiceCentre((int) ((List<?>) params.get("number_of_servers")).get(nd), (int) ((List<?>) params.get("queue_capacities")).get(nd),
                    classChangeMatrices.get(nd),
                    preemptPriorities.get(nd),
                    (int) ((List<?>) params.get("ps_thresholds")).get(nd),
                    ((List<PriorityFunction>)params.get("server_priority_functions")).get(nd),
                    ((List<?>) params.get("service_disciplines")).get(nd)));
        }


        for (String clssName : (List<String>) params.get("customer_class_names")) {
//            if (((Map<String, ?>) params.get("routing")).values().stream().allMatch(f -> f instanceof QueueInterface)) {
//                classes.put(clssName, new CustomerClass(
//                        (List<Distribution>) (((Map<String, List<?>>) params.get("arrival_distributions")).get(clssName)),
//                        (List<Distribution>) (((Map<String, List<?>>) params.get("service_distributions")).get(clssName)),
//                        ((Map<Integer, RoutingFunction>) params.get("routing")),
//                        (int) ((Map<?, ?>) params.get("priority_classes")).get(clssName),
//                        (List<BaulkingFunction>) ((Map<?, ?>) params.get("baulking_functions")).get(clssName),
//                        (List<Distribution>) ((Map<?, ?>) params.get("batching_distributions")).get(clssName),
//                        (List<Distribution>) ((Map<?, ?>) params.get("reneging_time_distributions")).get(clssName),
//                        (List<Integer>) ((Map<?, ?>) params.get("reneging_destinations")).get(clssName),
//                        (Map<String, Distribution>) classChangeTimeDistributions.get(clssName)
//                ));
//            } else {
                classes.put(clssName, new CustomerClass(
                        (List<Distribution>) (((Map<String, List<?>>) params.get("arrival_distributions")).get(clssName)),
                        (List<Distribution>) (((Map<String, List<?>>) params.get("service_distributions")).get(clssName)),
                        ((List<List<Double>>)params.get("routing")),
                        (int) ((Map<?, ?>) params.get("priority_classes")).get(clssName),
                        (List<BaulkingFunction>) ((Map<?, ?>) params.get("baulking_functions")).get(clssName),
                        (List<Distribution>) ((Map<?, ?>) params.get("batching_distributions")).get(clssName),
                        (List<Distribution>) ((Map<?, ?>) params.get("reneging_time_distributions")).get(clssName),
                        (List<Integer>) ((Map<?, ?>) params.get("reneging_destinations")).get(clssName),
                        (Map<String, Distribution>)classChangeTimeDistributions.get(clssName)
                ));
//            }
        }

        Network n = new Network(nodes, classes);
        n.processBased = false;
        return n;
    }

    public static Map<String, Object> fillOutDictionary(Map<String, Object> params) {
        if (params.get("arrival_distributions") instanceof List) {
            List<?> arrDists = (List<?>) params.get("arrival_distributions");
            params.put("arrival_distributions", Collections.singletonMap("Customer", arrDists));
        }

        if (params.get("service_distributions") instanceof List<?> srvDists) {
            params.put("service_distributions", Collections.singletonMap("Customer", srvDists));
        }

//        if (params.containsKey("routing") && params.get("routing") instanceof List<?> rtngMat) {
//            params.put("routing", Collections.singletonMap("Customer", rtngMat));
//        }

//        if (params.containsKey("routing")) {
//            if (((List<List<Double>>) params.get("routing")).stream().allMatch(f -> f instanceof List<?>)) {
//                List<List<Double>> rtngMat = (List<List<Double>>) params.get("routing");
//                params.put("routing", Collections.singletonMap("Customer", rtngMat));
//            }
//        }

        if (params.containsKey("baulking_functions") && params.get("baulking_functions") instanceof List) {
            List<?> blkFncs = (List<?>) params.get("baulking_functions");
            params.put("baulking_functions", Collections.singletonMap("Customer", blkFncs));
        }

        if (params.containsKey("batching_distributions") && params.get("batching_distributions") instanceof List) {
            List<?> btchDists = (List<?>) params.get("batching_distributions");
            params.put("batching_distributions", Collections.singletonMap("Customer", btchDists));
        }

        if (params.containsKey("reneging_time_distributions") && params.get("reneging_time_distributions") instanceof List) {
            List<?> renegingDists = (List<?>) params.get("reneging_time_distributions");
            params.put("reneging_time_distributions", Collections.singletonMap("Customer", renegingDists));
        }

        if (params.containsKey("reneging_destinations") && params.get("reneging_destinations") instanceof List) {
            List<?> renegingDests = (List<?>) params.get("reneging_destinations");
            params.put("reneging_destinations", Collections.singletonMap("Customer", renegingDests));
        }

        List<String> classNames = new ArrayList<>(((Map<String, ?>) params.get("arrival_distributions")).keySet());
        Collections.sort(classNames);
        params.put("customer_class_names", classNames);

//        List<?> classNames = new ArrayList<>(Objects.requireNonNull(
//                ((Map<?, ?>) params.get("arrival_distributions")).keySet()));
//        params.put("customer_class_names", classNames);

        Map<String, Object> defaultDict = new HashMap<>();
        defaultDict.put("name", "mypackage.Simulation");
        defaultDict.put("routing", classNames.stream().collect(Collectors.toMap(Function.identity(), c -> Collections.singletonList(Collections.singletonList(0.0)))));
        defaultDict.put("number_of_nodes", ((List<?>) params.get("number_of_servers")).size());
        defaultDict.put("number_of_classes", classNames.size());
        defaultDict.put("queue_capacities", Collections.nCopies(((List<?>) params.get("number_of_servers")).size(), Double.POSITIVE_INFINITY));
        defaultDict.put("priority_classes", classNames.stream().collect(Collectors.toMap(Function.identity(), c -> 0)));
        defaultDict.put("baulking_functions", classNames.stream().collect(Collectors.toMap(Function.identity(), c -> Collections.nCopies(((List<?>) params.get("number_of_servers")).size(), null))));
        defaultDict.put("batching_distributions", classNames.stream().collect(Collectors.toMap(Function.identity(), c -> Collections.nCopies(((List<?>) params.get("number_of_servers")).size(), new Deterministic(1)))));
        defaultDict.put("ps_thresholds", Collections.nCopies(((List<?>) params.get("number_of_servers")).size(), 1));
        defaultDict.put("server_priority_functions", Collections.nCopies(((List<?>) params.get("number_of_servers")).size(), null));
        defaultDict.put("reneging_time_distributions", classNames.stream().collect(Collectors.toMap(Function.identity(), c -> Collections.nCopies(((List<?>) params.get("number_of_servers")).size(), null))));
        defaultDict.put("reneging_destinations", classNames.stream().collect(Collectors.toMap(Function.identity(), c -> Collections.nCopies(((List<?>) params.get("number_of_servers")).size(), -1))));

        QueueInterface qi = QueueDiscipline::FIFO;
        defaultDict.put("service_disciplines", Collections.nCopies(((List<?>) params.get("number_of_servers")).size(), qi));

        for (String a : defaultDict.keySet()) {
            params.putIfAbsent(a, defaultDict.get(a));
        }

        return params;
    }

}





