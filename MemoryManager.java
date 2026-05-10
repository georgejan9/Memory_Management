import java.util.*;

/**
 * Core memory-management logic.
 * Maintains a list of contiguous blocks covering [0, totalSize).
 * Supports First-Fit / Best-Fit allocation and deallocation with hole merging.
 */
public class MemoryManager {

    public enum BlockType { SYSTEM, HOLE, SEGMENT }

    public static class MemoryBlock {
        public long start, size;
        public BlockType type;
        public String processName, segmentName;

        public MemoryBlock(long start, long size, BlockType type) {
            this.start = start;
            this.size = size;
            this.type = type;
        }

        public long end() { return start + size; }

        public MemoryBlock copy() {
            MemoryBlock c = new MemoryBlock(start, size, type);
            c.processName = processName;
            c.segmentName = segmentName;
            return c;
        }
    }

    private long totalSize;
    private List<MemoryBlock> blocks = new ArrayList<>();
    private Map<String, Process> processes = new LinkedHashMap<>();

    public long getTotalSize()                 { return totalSize; }
    public List<MemoryBlock> getBlocks()       { return blocks; }
    public Map<String, Process> getProcesses() { return processes; }

    /* ── Step 1: initialise memory ── */
    public void setMemorySize(long size) {
        totalSize = size;
        blocks.clear();
        processes.clear();
        blocks.add(new MemoryBlock(0, size, BlockType.SYSTEM));
    }

    /* ── Step 2: carve a hole out of SYSTEM space ── */
    public String addHole(long start, long size) {
        if (start < 0 || size <= 0 || start + size > totalSize)
            return "Hole out of bounds.";

        for (MemoryBlock b : blocks)
            if (b.type != BlockType.SYSTEM) {
                long os = Math.max(start, b.start), oe = Math.min(start + size, b.end());
                if (os < oe) return "Overlaps existing " + b.type + " at " + b.start + ".";
            }

        List<MemoryBlock> next = new ArrayList<>();
        long hEnd = start + size;
        for (MemoryBlock b : blocks) {
            if (b.type != BlockType.SYSTEM || start >= b.end() || hEnd <= b.start) {
                next.add(b);
                continue;
            }
            if (start > b.start)
                next.add(new MemoryBlock(b.start, start - b.start, BlockType.SYSTEM));
            next.add(new MemoryBlock(Math.max(start, b.start),
                    Math.min(hEnd, b.end()) - Math.max(start, b.start), BlockType.HOLE));
            if (hEnd < b.end())
                next.add(new MemoryBlock(hEnd, b.end() - hEnd, BlockType.SYSTEM));
        }
        blocks = next;
        mergeHoles();
        return null;
    }

    /* ── Step 3: allocate a process ── */
    public String allocate(Process p, String algo) {
        if (processes.containsKey(p.name))
            return "Process '" + p.name + "' already allocated.";

        List<MemoryBlock> backup = new ArrayList<>();
        for (MemoryBlock b : blocks) backup.add(b.copy());

        for (Segment seg : p.segments) {
            int idx = findHole(seg.size, algo);
            if (idx == -1) {
                blocks = backup;
                for (Segment s : p.segments) s.base = -1;
                return "Segment '" + seg.name + "' (size " + seg.size
                        + ") of " + p.name + " does not fit. Process NOT allocated.";
            }
            placeSegment(idx, p.name, seg);
        }
        processes.put(p.name, p);
        return null;
    }

    private int findHole(long size, String algo) {
        if ("first-fit".equals(algo)) {
            for (int i = 0; i < blocks.size(); i++)
                if (blocks.get(i).type == BlockType.HOLE && blocks.get(i).size >= size) return i;
        } else {
            int best = -1; long bestSz = Long.MAX_VALUE;
            for (int i = 0; i < blocks.size(); i++) {
                MemoryBlock b = blocks.get(i);
                if (b.type == BlockType.HOLE && b.size >= size && b.size < bestSz) {
                    best = i; bestSz = b.size;
                }
            }
            return best;
        }
        return -1;
    }

    private void placeSegment(int idx, String proc, Segment seg) {
        MemoryBlock hole = blocks.remove(idx);
        seg.base = hole.start;
        MemoryBlock sb = new MemoryBlock(hole.start, seg.size, BlockType.SEGMENT);
        sb.processName = proc;
        sb.segmentName = seg.name;
        blocks.add(idx, sb);
        long rem = hole.size - seg.size;
        if (rem > 0)
            blocks.add(idx + 1, new MemoryBlock(hole.start + seg.size, rem, BlockType.HOLE));
    }

    /* ── Step 4: deallocate a process ── */
    public String deallocate(String name) {
        Process p = processes.get(name);
        if (p == null) return "Process not found.";
        for (MemoryBlock b : blocks)
            if (b.type == BlockType.SEGMENT && name.equals(b.processName)) {
                b.type = BlockType.HOLE;
                b.processName = null;
                b.segmentName = null;
            }
        for (Segment s : p.segments) s.base = -1;
        mergeHoles();
        processes.remove(name);
        return null;
    }

    private void mergeHoles() {
        List<MemoryBlock> merged = new ArrayList<>();
        merged.add(blocks.get(0));
        for (int i = 1; i < blocks.size(); i++) {
            MemoryBlock prev = merged.get(merged.size() - 1), cur = blocks.get(i);
            if (prev.type == BlockType.HOLE && cur.type == BlockType.HOLE)
                prev.size += cur.size;
            else
                merged.add(cur);
        }
        blocks = merged;
    }
}
