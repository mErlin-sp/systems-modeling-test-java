package mypackage;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Individual {
    public double class_change_date;
    public double time_left;
    public double original_service_time;
    public double original_service_start_date;
    public int route;
    double arrival_date, reneging_date;
    double service_start_date;
    double service_time;
    double service_end_date;
    double exit_date;
    int id_number;
    List<Map<String, String>> data_records = new ArrayList<>();
    String customer_class;
    String previous_class;
    String next_class;
    int priority_class;
    int prev_priority_class;
    String original_class;
    boolean is_blocked = false;
    Server server;
    int queue_size_at_arrival;
    int queue_size_at_departure;
    int destination;
    boolean interrupted = false;
    int node;
     Simulation simulation;

    public Individual(int id_number, String customer_class, int priority_class, Simulation simulation) {
        this.id_number = id_number;
        this.customer_class = customer_class;
        this.previous_class = customer_class;
        this.priority_class = priority_class;
        this.prev_priority_class = priority_class;
        this.original_class = customer_class;
        this.simulation = simulation;
    }

    @Override
    public String toString() {
        return "Individual " + this.id_number;
    }
}
