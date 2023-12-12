package mypackage;

import java.util.List;

public interface QueueInterface {
    <T> T get(List<T> individuals);
}
