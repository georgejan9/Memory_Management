import java.util.ArrayList;
import java.util.List;

/**
 * A process that owns one or more segments.
 */
public class Process {
    public String name;
    public List<Segment> segments = new ArrayList<>();

    public Process(String name) {
        this.name = name;
    }

    public void addSegment(Segment s) {
        segments.add(s);
    }
}
