package mypackage;

public class NodeTop {
    public double next_event_date;

    public Class<? extends NodeTop> nodeType;

    public NodeTop(Class<? extends NodeTop> nodeType) {
        this.nodeType = nodeType;
    }


    public void initialise() {

    }

}
