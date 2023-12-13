package mypackage;

public class Server {
    private Node node;
    int id_number;
    Individual cust;
    boolean busy = false;
    boolean offduty = false;
    private boolean all_time = false;
    double start_date;
    double busy_time;
    double total_time;
    double shift_end;
    double next_end_service_date = Double.POSITIVE_INFINITY;

    public Server(Node node, int id_number, double start_date) {
        this.node = node;
        this.id_number = id_number;
        this.start_date = start_date;
    }

    public double getUtilisation() {
        return this.busy_time / this.total_time;
    }

    @Override
    public String toString() {
        return String.format("Server %s at Node %s", this.id_number, this.node.id_number);
    }
}