# Memory Allocation Simulator

A JavaFX-based visualization tool for simulating contiguous memory allocation in an Operating System. This project demonstrates how system memory is partitioned, allocated to processes, and managed using various allocation algorithms.

## 🚀 Features

* **Visual Memory Map:** Dynamic, color-coded visualization of System memory, Free Holes, and Allocated Process Segments.
* **Allocation Algorithms:**
  * **First-Fit:** Allocates the first available hole that is large enough.
  * **Best-Fit:** Allocates the smallest available hole that is large enough to minimize wasted space.
* **Segmentation Support:** Processes can be defined with multiple independent segments (e.g., Code, Data, Stack).
* **Hole Management:** 
  * Ability to carve out specific custom holes in the initial system memory.
  * Automatic merging (coalescing) of adjacent free memory holes upon process deallocation.
* **Real-time Tables:** Live-updating tables displaying:
  * Segment Base/Limit registers for each process.
  * Currently allocated memory blocks.
  * Available free holes list.
* **Flexible Units:** Support for Bytes, KB, MB, and GB formatting.

## 🛠️ Technology Stack

* **Language:** Java
* **UI Framework:** JavaFX (FXML + CSS)
* **Architecture:** MVC (Model-View-Controller)


## ⚙️ How to Run

### Option 1: Using the Pre-compiled Executable (Windows)
If you have downloaded the release `.zip` containing the `exe` folder:
1. Extract the folder.
2. Double click `MemoryAllocator.exe`.
*(Note: Requires the bundled `javafx-sdk` and `out` directories to remain in the same folder as the executable).*

### Option 2: Running from Source
To compile and run the source code yourself, ensure you have the **Java Development Kit (JDK)** installed.

1. Clone this repository.
2. Download the [JavaFX SDK](https://gluonhq.com/products/javafx/) for your operating system and place it in the project directory (name the folder `javafx-sdk-23.0.1` or adjust the batch files).
3. Open a terminal in the project root.
4. Run the compilation script:
   ```bash
   compile.bat
   ```
5. Run the application script:
   ```bash
   run.bat
   ```

## 🏗️ Architecture

This project follows an Object-Oriented design:
* `MemoryManager`: The core logic engine that tracks memory blocks, processes, and algorithms.
* `Controller`: The UI bridge that hooks JavaFX components to the underlying logic.
* `Process` & `Segment`: Data structures representing applications requesting memory.

See the [UML Class Diagram](Memory_Simulator_Deliverable/report/memory_simulator_uml.puml) for more details.

## 🎓 Academic Context
Developed as an Operating Systems course project for **Ain Shams University (ASU)**.

---
*Created by [George Jan]*
