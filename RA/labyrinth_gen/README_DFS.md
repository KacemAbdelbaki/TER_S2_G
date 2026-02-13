# DFS (Depth-First Search) Maze Generator

## Overview

This algorithm generates a labyrinth using **Depth-First Search** with recursive backtracking.

## How It Works

1. **Start** at an initial cell and mark it as visited
2. **Explore** by randomly selecting an unvisited neighboring cell
3. **Remove the wall** between the current cell and the chosen neighbor
4. **Recurse** into the neighbor cell
5. **Backtrack** when no unvisited neighbors remain, returning to previous cells until a new path is found
6. **Complete** when all cells have been visited

## Algorithm Pseudocode

```
DFS(cell):
    mark cell as visited
    for each neighbor in random order:
        if neighbor not visited:
            remove wall between cell and neighbor
            DFS(neighbor)
```

## Characteristics

| Property           | Description              |
| ------------------ | ------------------------ |
| **Corridor Style** | Long, winding passages   |
| **Dead Ends**      | Few                      |
| **Branching**      | Low branching factor     |
| **Bias**           | Creates river-like paths |

## Key Functions

- `DFS(grid, r, c)` — Main recursive maze generation
- `initMap(height, width)` — Initializes the grid with cells
- `mirror_map(grid)` — Creates a symmetric maze by mirroring
- `ghost_room_init(grid)` — Sets up a special ghost room area
- `print_map(grid)` — ASCII visualization of the maze

## Usage

```python
grid = initMap(height, width)
DFS(grid, start_row, start_col)
print_map(grid)
```

## Output Example

The maze is printed with:

- `+---+` for walls
- `   ` for open passages
- `¥` for ghost cells
- `@` for teleport cells
