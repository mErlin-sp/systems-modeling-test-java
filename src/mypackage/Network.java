package mypackage;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

class ServiceCentre {
    int numberOfServers;
    int queueingCapacity;
    List<Double> classChangeMatrix;
    double priorityPreempt;
    int psThreshold;
    PriorityFunction serverPriorityFunction;
    Object serviceDiscipline;
    boolean classChangeTime;
    boolean reneging;

    public ServiceCentre(
            int numberOfServers,
            int queueingCapacity,
            List<Double> classChangeMatrix,
            double priorityPreempt,
            int psThreshold,
            PriorityFunction serverPriorityFunction,
            Object serviceDiscipline
    ) {
        this.numberOfServers = numberOfServers;
        this.queueingCapacity = queueingCapacity;
        this.classChangeMatrix = classChangeMatrix;
        this.priorityPreempt = priorityPreempt;
        this.psThreshold = psThreshold;
        this.serverPriorityFunction = serverPriorityFunction;
        this.serviceDiscipline = serviceDiscipline;
        this.classChangeTime = false;
    }
}

interface RoutingFunction {
    Integer route(Individual ind);
}

class CustomerClass {
    List<Distribution> arrivalDistributions;
    List<Distribution> serviceDistributions;
    List<Distribution> batchingDistributions;
    List<List<Double>> routing;
    int priorityClass;
    List<BaulkingFunction> baulkingFunctions;
    List<Distribution> renegingTimeDistributions;
    List<Integer> renegingDestinations;
    Map<String, Distribution> classChangeTimeDistributions;

    public CustomerClass(
            List<Distribution> arrivalDistributions,
            List<Distribution> serviceDistributions,
            List<List<Double>>  routing,
            int priorityClass,
            List<BaulkingFunction> baulkingFunctions,
            List<Distribution> batchingDistributions,
            List<Distribution> renegingTimeDistributions,
            List<Integer> renegingDestinations,
            Map<String, Distribution> classChangeTimeDistributions
    ) {
        this.arrivalDistributions = arrivalDistributions;
        this.serviceDistributions = serviceDistributions;
        this.batchingDistributions = batchingDistributions;
        this.routing = routing;
        this.priorityClass = priorityClass;
        this.baulkingFunctions = baulkingFunctions;
        this.renegingTimeDistributions = renegingTimeDistributions;
        this.renegingDestinations = renegingDestinations;
        this.classChangeTimeDistributions = classChangeTimeDistributions;
    }
}

public class Network {
    List<ServiceCentre> serviceCentres;
    Map<String, CustomerClass> customerClasses;
     public int numberOfNodes;
    int numberOfClasses;
    List<String> customerClassNames;
    int numberOfPriorityClasses;
    Map<String,Integer> priorityClassMapping;

    boolean processBased;

    public Network(List<ServiceCentre> serviceCentres, Map<String, CustomerClass> customerClasses) {
//        this.serviceCentres = serviceCentres;
//        this.customerClasses = customerClasses;
//        this.numberOfNodes = serviceCentres.size();
//        this.numberOfClasses = customerClasses.size();
//        this.customerClassNames = customerClasses.keySet().stream().sorted().toList();
//        this.numberOfPriorityClasses = customerClasses.values().stream().map(clss -> clss.priorityClass).collect(Collectors.toSet()).size();
//        this.priorityClassMapping = customerClasses.entrySet().stream()
//                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().priorityClass));
//
//        for (int ndId = 0; ndId < this.numberOfNodes; ndId++) {
//            int finalNdId = ndId;
//            boolean hasReneging = customerClasses.values().stream()
//                    .anyMatch(clss -> clss.renegingTimeDistributions.get(finalNdId) != null);
//            this.serviceCentres.get(ndId).reneging = hasReneging;
//        }
//
//        boolean hasClassChangeTime = customerClasses.values().stream()
//                .flatMap(clss -> clss.classChangeTimeDistributions.stream())
//                .anyMatch(dist -> dist != null);
//
//        for (ServiceCentre node : this.serviceCentres) {
//            node.classChangeTime = hasClassChangeTime;
//        }

        this.serviceCentres = serviceCentres;
        this.customerClasses = customerClasses;
        this.numberOfNodes = serviceCentres.size();
        this.numberOfClasses = customerClasses.size();
        this.customerClassNames = customerClasses.keySet().stream().sorted().toList();
        this.numberOfPriorityClasses = customerClasses.values().stream().map(clss -> clss.priorityClass).collect(Collectors.toSet()).size();

        this.priorityClassMapping = new HashMap<>();
        for (String clss : customerClasses.keySet()) {
            priorityClassMapping.put(clss, customerClasses.get(clss).priorityClass);
        }

        for (int nd_id = 0; nd_id < serviceCentres.size(); nd_id++) {
            boolean allRenegingTimeDistributionsAreNone = true;
            for (CustomerClass clss : customerClasses.values()) {
                if (clss.renegingTimeDistributions.get(nd_id) != null) {
                    allRenegingTimeDistributionsAreNone = false;
                    break;
                }
            }
            serviceCentres.get(nd_id).reneging = !allRenegingTimeDistributionsAreNone;
        }

        for (CustomerClass clss : customerClasses.values()) {
            if (clss.classChangeTimeDistributions != null) {
                for (Object dist : clss.classChangeTimeDistributions.values()) {
                    if (dist != null) {
                        for (ServiceCentre node : serviceCentres) {
                            node.classChangeTime = true;
                        }
                        break;
                    }
                }
            }
        }

//        for (int ndId = 0; ndId < numberOfNodes; ndId++) {
//            if (customerClasses.values().stream().allMatch(clss -> clss.getRenegingTimeDistributions().get(ndId) == null)) {
//                serviceCentres.get(ndId).setReneging(false);
//            } else {
//                serviceCentres.get(ndId).setReneging(true);
//            }
//        }
//        if (customerClasses.values().stream().flatMap(clss -> clss.getClassChangeTimeDistributions().values().stream()).anyMatch(Objects::nonNull)) {
//            serviceCentres.forEach(node -> node.setClassChangeTime(true));
//        }
    }
}


