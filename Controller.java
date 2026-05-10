import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import java.util.*;

/**
 * FXML Controller — handles every button / input action and refreshes
 * the memory visualisation and tables after each operation.
 */
public class Controller {

    /* ── FXML-injected controls ── */
    @FXML private TextField memSizeField;
    @FXML private TextField holeStartField, holeSizeField;
    @FXML private TextField procNameField;
    @FXML private VBox segmentsBox;
    @FXML private ComboBox<String> algoCombo, deallocCombo, unitCombo;
    @FXML private VBox memoryBox;
    @FXML private TextArea segTablesArea, allocatedArea, freeArea, logArea;
    @FXML private Label memSizeLabel;

    /* ── State ── */
    private final MemoryManager mgr = new MemoryManager();
    private final List<TextField[]> segRows = new ArrayList<>();
    private final Map<String, String> procColors = new LinkedHashMap<>();
    private int colorIdx = 0;

    private static final String[] COLORS = {
        "#7B68EE", "#2ECC71", "#E74C3C", "#F1C40F",
        "#3498DB", "#E67E22", "#9B59B6", "#1ABC9C"
    };

    /* ── Initialise ── */
    @FXML
    public void initialize() {
        algoCombo.getItems().addAll("First Fit", "Best Fit");
        algoCombo.getSelectionModel().selectFirst();
        unitCombo.getItems().addAll("Bytes", "KB", "MB", "GB");
        unitCombo.getSelectionModel().selectFirst();
        updateUnitLabel();
        addSegmentRow();
    }

    /* ── Unit helpers ── */
    private long getUnitMultiplier() {
        String u = unitCombo.getValue();
        if (u == null) return 1;
        switch (u) {
            case "KB": return 1024L;
            case "MB": return 1024L * 1024;
            case "GB": return 1024L * 1024 * 1024;
            default:   return 1L;
        }
    }

    private String getUnitSuffix() {
        String u = unitCombo.getValue();
        return (u == null || "Bytes".equals(u)) ? "B" : u;
    }

    /** Format a byte value into the currently selected unit for display. */
    private String fmt(long bytes) {
        long m = getUnitMultiplier();
        if (m == 1) return String.valueOf(bytes);
        double val = (double) bytes / m;
        if (val == (long) val) return String.valueOf((long) val);
        return String.format("%.2f", val);
    }

    private void updateUnitLabel() {
        memSizeLabel.setText("Total Memory Size (" + getUnitSuffix() + ")");
    }

    @FXML
    private void onUnitChanged() {
        updateUnitLabel();
        refreshAll();
    }

    /* ══════════════ Button handlers ══════════════ */

    @FXML
    private void onSetMemory() {
        try {
            long input = Long.parseLong(memSizeField.getText().trim());
            if (input <= 0) throw new NumberFormatException();
            long bytes = input * getUnitMultiplier();
            mgr.setMemorySize(bytes);
            procColors.clear();
            colorIdx = 0;
            log("Memory set to " + fmt(bytes) + " " + getUnitSuffix() + ".");
            refreshAll();
        } catch (NumberFormatException e) {
            alert("Enter a valid positive integer for memory size.");
        }
    }

    @FXML
    private void onAddHole() {
        try {
            long startIn = Long.parseLong(holeStartField.getText().trim());
            long sizeIn  = Long.parseLong(holeSizeField.getText().trim());
            long m = getUnitMultiplier();
            long start = startIn * m, size = sizeIn * m;
            String err = mgr.addHole(start, size);
            if (err != null) { alert(err); return; }
            String su = getUnitSuffix();
            log("Hole added @" + fmt(start) + " size=" + fmt(size) + " " + su);
            holeStartField.clear();
            holeSizeField.clear();
            refreshAll();
        } catch (NumberFormatException e) {
            alert("Enter valid integers for start and size.");
        }
    }

    @FXML
    private void onAddSegment() { addSegmentRow(); }

    @FXML
    private void onAllocate() {
        String name = procNameField.getText().trim();
        if (name.isEmpty()) { alert("Enter a process name."); return; }

        Process p = new Process(name);
        long m = getUnitMultiplier();
        for (TextField[] row : segRows) {
            String sn = row[0].getText().trim(), ss = row[1].getText().trim();
            if (sn.isEmpty() || ss.isEmpty()) continue;
            try {
                long sz = Long.parseLong(ss) * m;
                if (sz <= 0) throw new NumberFormatException();
                p.addSegment(new Segment(sn, sz));
            } catch (NumberFormatException e) {
                alert("Invalid size for segment '" + sn + "'."); return;
            }
        }
        if (p.segments.isEmpty()) { alert("Add at least one segment."); return; }

        String algo = algoCombo.getSelectionModel().getSelectedIndex() == 0 ? "first-fit" : "best-fit";
        String err = mgr.allocate(p, algo);
        if (err != null) { alert(err); log("FAIL: " + err); return; }

        procColors.putIfAbsent(name, COLORS[colorIdx++ % COLORS.length]);
        log("Allocated " + name + " (" + p.segments.size() + " segs) — " + algo);
        procNameField.clear();
        segmentsBox.getChildren().clear();
        segRows.clear();
        addSegmentRow();
        refreshAll();
    }

    @FXML
    private void onDeallocate() {
        String name = deallocCombo.getValue();
        if (name == null) { alert("Select a process."); return; }
        String err = mgr.deallocate(name);
        if (err != null) { alert(err); return; }
        procColors.remove(name);
        log("Deallocated " + name + " — holes merged.");
        refreshAll();
    }

    @FXML
    private void onReset() {
        mgr.setMemorySize(0);
        procColors.clear();
        colorIdx = 0;
        segmentsBox.getChildren().clear();
        segRows.clear();
        addSegmentRow();
        logArea.clear();
        log("Reset.");
        refreshAll();
    }

    /* ══════════════ Refresh everything ══════════════ */

    private void refreshAll() {
        drawMemory();
        refreshDeallocCombo();
        refreshSegTables();
        refreshAllocTable();
        refreshFreeTable();
    }

    /* ── Memory visualisation (coloured bars in VBox) ── */
    private void drawMemory() {
        memoryBox.getChildren().clear();
        if (mgr.getTotalSize() == 0) {
            Label l = new Label("Set memory size to begin.");
            l.setStyle("-fx-text-fill: #999;");
            memoryBox.getChildren().add(l);
            return;
        }
        double totalH = 520;
        for (MemoryManager.MemoryBlock b : mgr.getBlocks()) {
            double h = Math.max((double) b.size / mgr.getTotalSize() * totalH, 22);
            StackPane pane = new StackPane();
            pane.setPrefHeight(h);
            pane.setMinHeight(22);
            pane.setMaxWidth(Double.MAX_VALUE);

            String color, text, su = getUnitSuffix();
            switch (b.type) {
                case HOLE:
                    color = "#E0E0E0"; text = "Hole (" + fmt(b.size) + " " + su + ")  @" + fmt(b.start); break;
                case SEGMENT:
                    color = procColors.getOrDefault(b.processName, "#7B68EE");
                    text = b.processName + " : " + b.segmentName + "  (" + fmt(b.size) + " " + su + ")"; break;
                default:
                    color = "#CCCCCC"; text = "System (" + fmt(b.size) + " " + su + ")  @" + fmt(b.start); break;
            }
            pane.setStyle("-fx-background-color:" + color + ";"
                    + "-fx-border-color:#AAA; -fx-border-width:0 0 1 0;");
            Label lbl = new Label(text);
            lbl.setStyle("-fx-font-size:11; -fx-text-fill:" +
                    (b.type == MemoryManager.BlockType.SEGMENT ? "white" : "#444") + ";");
            pane.getChildren().add(lbl);
            pane.setAlignment(Pos.CENTER);
            HBox.setHgrow(pane, Priority.ALWAYS);
            memoryBox.getChildren().add(pane);
        }
    }

    /* ── Tables ── */
    private void refreshDeallocCombo() {
        deallocCombo.getItems().clear();
        deallocCombo.getItems().addAll(mgr.getProcesses().keySet());
    }

    private void refreshSegTables() {
        StringBuilder sb = new StringBuilder();
        String su = getUnitSuffix();
        for (Process p : mgr.getProcesses().values()) {
            sb.append("── ").append(p.name).append(" ──\n");
            sb.append(String.format("%-4s %-12s %-10s %-10s\n", "#", "Name", "Base("+su+")", "Limit("+su+")"));
            for (int i = 0; i < p.segments.size(); i++) {
                Segment s = p.segments.get(i);
                sb.append(String.format("%-4d %-12s %-10s %-10s\n",
                        i, s.name, s.base == -1 ? "—" : fmt(s.base), fmt(s.size)));
            }
            sb.append("\n");
        }
        segTablesArea.setText(sb.length() == 0 ? "No processes allocated." : sb.toString());
    }

    private void refreshAllocTable() {
        StringBuilder sb = new StringBuilder();
        String su = getUnitSuffix();
        sb.append(String.format("%-8s %-12s %-10s %-10s\n", "Process", "Segment", "Start("+su+")", "Size("+su+")"));
        boolean any = false;
        for (MemoryManager.MemoryBlock b : mgr.getBlocks())
            if (b.type == MemoryManager.BlockType.SEGMENT) {
                sb.append(String.format("%-8s %-12s %-10s %-10s\n",
                        b.processName, b.segmentName, fmt(b.start), fmt(b.size)));
                any = true;
            }
        allocatedArea.setText(any ? sb.toString() : "Nothing allocated.");
    }

    private void refreshFreeTable() {
        StringBuilder sb = new StringBuilder();
        String su = getUnitSuffix();
        sb.append(String.format("%-4s %-10s %-10s\n", "#", "Start("+su+")", "Size("+su+")"));
        int n = 0;
        for (MemoryManager.MemoryBlock b : mgr.getBlocks())
            if (b.type == MemoryManager.BlockType.HOLE) {
                n++;
                sb.append(String.format("%-4d %-10s %-10s\n", n, fmt(b.start), fmt(b.size)));
            }
        freeArea.setText(n == 0 ? "No free holes." : sb.toString());
    }

    /* ── Helpers ── */
    private void addSegmentRow() {
        HBox row = new HBox(6);
        TextField nameF = new TextField(); nameF.setPromptText("Seg name");  nameF.setPrefWidth(110);
        TextField sizeF = new TextField(); sizeF.setPromptText("Size");      sizeF.setPrefWidth(70);
        Button rm = new Button("✕");
        rm.getStyleClass().add("btn-small");
        rm.setOnAction(e -> { if (segRows.size() > 1) { segRows.remove(new TextField[]{nameF, sizeF}); segmentsBox.getChildren().remove(row); }});
        row.getChildren().addAll(nameF, sizeF, rm);
        segRows.add(new TextField[]{nameF, sizeF});
        segmentsBox.getChildren().add(row);
    }

    private void log(String msg) {
        logArea.appendText("[" + java.time.LocalTime.now().withNano(0) + "] " + msg + "\n");
    }

    private void alert(String msg) {
        Alert a = new Alert(Alert.AlertType.WARNING, msg, ButtonType.OK);
        a.setHeaderText(null);
        a.showAndWait();
    }
}
