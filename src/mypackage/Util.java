package mypackage;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;

public class Util {

    public static <T> T randomChoice(List<T> array, List<Double> probs) {
        /**
         * This function takes in an array of values to make a choice from,
         * and an pdf corresponding to those values. It returns a random choice
         * from that array, using the probs as weights.
         */
        // If no pdf provided, assume uniform dist:
        if (probs == null) {
            int index = (int) (Math.random() * array.size());
            return array.get(index);
        }

        // A common case, guaranteed to reach the Exit node;
        // No need to sample for this:
        if (new HashSet<>(probs.subList(0, probs.size() - 1)).equals(new HashSet<>(Collections.singletonList(0.0))) && probs.getLast() == 1.0) {
            return array.getLast();
        }

        // Sample a random value from using pdf
        double rdmNum = Math.random();
        int i = 0;
        double p = probs.getFirst();
        while (rdmNum > p) {
            i++;
            p += probs.get(i);
        }
        return array.get(i);
    }

}
