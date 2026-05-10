/**
 * A single segment inside a process.
 */
public class Segment {
    public String name;
    public long size;
    public long base = -1;   // base address once allocated, -1 means unallocated

    public Segment(String name, long size) {
        this.name = name;
        this.size = size;
    }
}
