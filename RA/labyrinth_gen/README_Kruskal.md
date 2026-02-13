# Kruskal's Algorithm Maze Generator

## Overview

This algorithm generates a labyrinth using **Kruskal's Algorithm** with a Union-Find (Disjoint Set) data structure.

## How It Works

1. **Initialize** each cell as its own isolated set
2. **List all walls** between adjacent cells
3. **Shuffle** the wall list randomly
4. **Iterate** through each wall:
   - If the two cells belong to **different sets**, remove the wall and **union** the sets
   - If they're already connected, skip (to avoid cycles)
5. **Complete** when all cells belong to a single set (fully connected)

## Algorithm Pseudocode

```
Kruskal(grid):
    create DisjointSet for all cells
    walls = list of all internal walls
    shuffle(walls)

    for each wall (cell1, cell2):
        if find(cell1) != find(cell2):
            remove wall between cell1 and cell2
            union(cell1, cell2)
```

## Data Structure: Disjoint Set (Union-Find)

```python
class DisjointSet:
    def find(i)   # Returns root of set containing i (with path compression)
    def union(i, j)  # Merges sets containing i and j
```

## Characteristics

| Property           | Description                            |
| ------------------ | -------------------------------------- |
| **Corridor Style** | Balanced, no directional bias          |
| **Dead Ends**      | Moderate                               |
| **Branching**      | Uniform distribution                   |
| **Bias**           | None — produces uniform spanning trees |

## Key Functions

- `kruskal_algorithm(grid)` — Main maze generation using Kruskal's
- `DisjointSet` — Union-Find data structure for tracking connected components
- `mirror_map(grid)` — Creates a symmetric maze by mirroring

## Usage

```python
grid = [[Cell(r, c) for c in range(width)] for r in range(height)]
kruskal_algorithm(grid)
```

## Why Kruskal's?

- **Randomness**: Shuffling walls ensures unpredictable maze layouts
- **Efficiency**: Union-Find with path compression is nearly O(1) per operation
- **Uniformity**: Every possible spanning tree is equally likely
