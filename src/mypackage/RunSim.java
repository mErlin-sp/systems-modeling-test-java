package mypackage;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RunSim {
    public static void main(String[] args) throws Exception {
        Map<String, Object> params = new HashMap<>();

        params.put("arrival_distributions", new ArrayList<>() {{
            add(new Exponential(3.0));
            add(null);
            add(null);
        }});

        params.put("service_distributions", new ArrayList<>() {{
            add(new Exponential(5.0));
            add(new Exponential(10.0));
            add(new Exponential(15.0));
        }});

        params.put("routing", new ArrayList<>() {{
            add(new ArrayList<>() {{
                add(0.0);
                add(0.75);
                add(0.25);
            }});
            add(new ArrayList<>() {{
                add(0.0);
                add(0.0);
                add(0.0);
            }});
            add(new ArrayList<>() {{
                add(0.0);
                add(0.0);
                add(0.0);
            }});
        }});

        params.put("queue_capacities", new ArrayList<>() {{
            add(0);
            add(0);
            add(2);
        }});

        params.put("number_of_servers", new ArrayList<>() {{
            add(2);
            add(1);
            add(1);
        }});


        Network network = NetworkCreator.createNetwork(params);

        Simulation simulation = new Simulation(network);
        simulation.simulateUntilMaxTime(1000, true);

        // Access the collected data
        List<Map<String, String>> records = simulation.getAllRecords();

        double[] queueLengthsNode1 = new double[records.size()];
        double[] queueLengthsNode2 = new double[records.size()];
        double[] queueLengthsNode3 = new double[records.size()];

        double[] serviceTimes = new double[records.size()];

        int failures = 0;

        // Calculate the mean queue length
        for (int i = 0; i < records.size(); i++) {
            Map<String, String> record = records.get(i);

            if (record.get("type").equals("rejection")) {
                failures += 1;
            } else if (record.get("type").equals("service")) {
                serviceTimes[i] = Double.parseDouble(record.get("service_time"));
            }

            int node = Integer.parseInt(record.get("node_id"));
            double queueSizeAtArrival = Double.parseDouble(record.get("queue_size_at_arrival"));

            if (node == 1) {
                queueLengthsNode1[i] = queueSizeAtArrival;
            } else if (node == 2) {
                queueLengthsNode2[i] = queueSizeAtArrival;
            } else if (node == 3) {
                queueLengthsNode3[i] = queueSizeAtArrival;
            }
        }

        double meanQueueLengthNode1 = calculateMean(queueLengthsNode1);
        double meanQueueLengthNode2 = calculateMean(queueLengthsNode2);
        double meanQueueLengthNode3 = calculateMean(queueLengthsNode3);

        double meanServiceTime = calculateMean(serviceTimes);

        System.out.println("Mean Queue Length on Node 1: " + meanQueueLengthNode1);
        System.out.println("Mean Queue Length on Node 2: " + meanQueueLengthNode2);
        System.out.println("Mean Queue Length on Node 3: " + meanQueueLengthNode3);

        System.out.println("Mean Service Time: " + meanServiceTime);

        System.out.println("Records processed: " + records.size());
        System.out.println("Failures count: " + failures);
    }

    private static double calculateMean(double[] values) {
        double sum = 0;
        int count = 0;

        for (double value : values) {
            if (value != 0) {
                sum += value;
                count++;
            }
        }

        return count > 0 ? sum / count : 0;
    }
}
