package mypackage;

import java.util.List;
import java.util.Random;

public class QueueDiscipline {
    /**
     * A queue discipline that determines the order in which individuals are served.
     */

    public static <T> T FIFO(List<T> individuals) {
        /**
         * FIFO: First in first out / First come first served
         * Returns the individual at the head of the queue
         */
        return individuals.getFirst();
    }

    public static <T> T SIRO(List<T> individuals) {
        /**
         * SIRO: Service in random order
         * Returns a random individual from the queue
         */
        return individuals.get(new Random().nextInt(individuals.size()));
    }

    public static <T> T LIFO(List<T> individuals) {
        /**
         * LIFO: Last in first out / Last come first served
         * Returns the individual who joined the queue most recently
         */
        return individuals.getLast();
    }

}
