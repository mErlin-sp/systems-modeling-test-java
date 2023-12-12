package mypackage;

import java.util.Map;
import java.util.List;
import java.util.stream.Collectors;

class ServiceCentre {
    int numberOfServers;
    int queueingCapacity;
    Map<String, Double> classChangeMatrix;
    boolean priorityPreempt;
    int psThreshold;
    Object serverPriorityFunction;
    Object serviceDiscipline;
    boolean classChangeTime;
    boolean reneging;

    public ServiceCentre(
            int numberOfServers,
            int queueingCapacity,
            Map<String, Double> classChangeMatrix,
            boolean priorityPreempt,
            int psThreshold,
            Object serverPriorityFunction,
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

class CustomerClass {
    Map<String, Object> arrivalDistributions;
    Map<String, Object> serviceDistributions;
    Map<String, Object> batchingDistributions;
    Object routing;
    int priorityClass;
    Map<String, Object> baulkingFunctions;
    Map<String, Object> renegingTimeDistributions;
    Map<String, Object> renegingDestinations;
    Map<String, Object> classChangeTimeDistributions;

    public CustomerClass(
            Map<String, Object> arrivalDistributions,
            Map<String, Object> serviceDistributions,
            Map<String, Object> routing,
            int priorityClass,
            Map<String, Object> baulkingFunctions,
            Map<String, Object> batchingDistributions,
            Map<String, Object> renegingTimeDistributions,
            Map<String, Object> renegingDestinations,
            Map<String, Object> classChangeTimeDistributions
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

class Network {
    List<ServiceCentre> serviceCentres;
    Map<String, CustomerClass> customerClasses;
    int numberOfNodes;
    int numberOfClasses;
    List<String> customerClassNames;
    int numberOfPriorityClasses;
    Map<String, Integer> priorityClassMapping;

    boolean processBased;

    public Network(List<ServiceCentre> serviceCentres, Map<String, CustomerClass> customerClasses) {
        this.serviceCentres = serviceCentres;
        this.customerClasses = customerClasses;
        this.numberOfNodes = serviceCentres.size();
        this.numberOfClasses = customerClasses.size();
        this.customerClassNames = customerClasses.keySet().stream().sorted().toList();
        this.numberOfPriorityClasses = customerClasses.values().stream().map(clss -> clss.priorityClass).collect(Collectors.toSet()).size();
        this.priorityClassMapping = customerClasses.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue().priorityClass));

        for (int ndId = 0; ndId < this.numberOfNodes; ndId++) {
            int finalNdId = ndId;
            boolean hasReneging = customerClasses.values().stream()
                    .anyMatch(clss -> clss.renegingTimeDistributions.get(String.valueOf(finalNdId)) != null);
            this.serviceCentres.get(ndId).reneging = hasReneging;
        }

        boolean hasClassChangeTime = customerClasses.values().stream()
                .flatMap(clss -> clss.classChangeTimeDistributions.values().stream())
                .anyMatch(dist -> dist != null);

        for (ServiceCentre node : this.serviceCentres) {
            node.classChangeTime = hasClassChangeTime;
        }
    }
}
