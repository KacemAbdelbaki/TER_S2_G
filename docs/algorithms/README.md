# Maze Generation Algorithms

The backend implements three maze generation algorithms. Each one produces a
spanning tree of the grid (every cell reachable from every other cell), but
with a different visual character.

## Comparison

| Algorithm | Style                   | Dead Ends | Branching | Bias                                  |
| --------- | ----------------------- | --------- | --------- | ------------------------------------- |
| DFS       | Long, winding corridors | Few       | Low       | River like, single long solution path |
| Prim      | Radiates from a seed    | Moderate  | Higher    | Shorter paths from the start          |
| Kruskal   | Balanced, uniform       | Moderate  | Uniform   | None, every spanning tree equally likely |

After the raw algorithm runs, every maze goes through the same post processing
pipeline defined in [common.py](../../RA/labyrinth_gen/common.py):

1. `add_cycles(grid, 0.12)`, randomly removes about 12 percent of remaining
   walls to introduce multiple paths.
2. Strip the last column, then mirror the left half to the right
   (Pac Man symmetric layout).
3. Insert a connecting middle column.
4. Place two pairs of teleport points on the edges.
5. Carve a ghost room in the center.
6. Serialize to JSON via `maze_to_json(grid, steps)`.

## Per algorithm details

* [DFS](DFS.md), depth first search with recursive backtracking.
* [Prim](Prim.md), frontier growth from a seed cell.
* [Kruskal](Kruskal.md), Union Find on shuffled walls.

## Source files

| Algorithm | Implementation                                       |
| --------- | ---------------------------------------------------- |
| DFS       | [RA/labyrinth_gen/DFS.py](../../RA/labyrinth_gen/DFS.py)         |
| Prim      | [RA/labyrinth_gen/prim.py](../../RA/labyrinth_gen/prim.py)       |
| Kruskal   | [RA/labyrinth_gen/kruskal.py](../../RA/labyrinth_gen/kruskal.py) |

## Step recording

All three algorithms accept `record_steps=True`. When set, every wall removal
is appended to a `steps` list as
`{"r1", "c1", "w1", "r2", "c2", "w2"}`. The desktop frontend uses this list
to animate generation step by step.
