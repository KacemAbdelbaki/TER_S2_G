# Prim's Algorithm Maze Generator

## Overview

This algorithm generates a labyrinth using **Prim's Algorithm**, growing the maze outward from a starting point.

## How It Works

1. **Start** at an initial cell, mark it as visited
2. **Add** all unvisited neighbors of the starting cell to the **frontier**
3. **Randomly pick** a cell from the frontier
4. **Connect** it to a visited neighbor by removing the wall between them
5. **Mark** the new cell as visited and add its unvisited neighbors to the frontier
6. **Repeat** until the frontier is empty (all cells visited)

## Algorithm Pseudocode

```
Prim(grid, start):
    mark start as visited
    frontier = neighbors of start

    while frontier not empty:
        cell = random cell from frontier
        if cell not visited:
            mark cell as visited
            remove wall between cell and its visited neighbor
            add cell's unvisited neighbors to frontier
```

## Characteristics

| Property           | Description                           |
| ------------------ | ------------------------------------- |
| **Corridor Style** | Radiating outward from center         |
| **Dead Ends**      | Moderate                              |
| **Branching**      | Higher than DFS                       |
| **Bias**           | Tends toward shorter paths from start |

## Key Functions

- `prim_algorithm(grid, start_r, start_c)` — Main maze generation
- `remove_wall_between(c1, c2)` — Removes walls between adjacent cells
- `add_cycles(grid, chance)` — Optionally adds loops for multiple paths
- `mirror_map(grid)` — Creates a symmetric maze by mirroring
- `ghost_room_init(grid)` — Initializes a special ghost room

## Usage

```python
grid = initMap(height, width)
prim_algorithm(grid, start_row, start_col)
```

## Bonus: Adding Cycles

The `add_cycles(grid, chance=0.1)` function can knock down additional walls to create alternative routes, making the maze less of a perfect tree:

```python
add_cycles(grid, chance=0.15)  # 15% chance per interior cell
```

## Why Prim's?

- **Growth Pattern**: Expands like a growing organism from the start
- **Flexibility**: Easy to bias toward certain areas
- **Visual Appeal**: Creates mazes that feel "centered" around the origin
