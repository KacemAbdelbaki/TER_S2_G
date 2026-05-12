# Pac-Man Game — Presentation Speech (~23 min)

> Speaker notes follow the slide order. Bracketed cues like *[next slide]* mark when to advance.

---

## Slide 1 — Title

Good morning everyone. We are Group G from the Master 1 Artificial Intelligence program, and today we are going to walk you through our TER project: a full Pac-Man game we built from scratch. I'm joined by Abdelbaki Kacem, Abou Orm Daniel, and Kabban Joe.

The idea behind this project was simple on the surface: re-create the classic Pac-Man experience. But to do it properly we had to combine a lot of things — maze generation algorithms, a backend service, a database, a desktop game client, and several flavors of artificial intelligence for the ghosts and even for Pac-Man himself. So this is really an end-to-end project, from the algorithmic core all the way to a packaged desktop application that anyone can download and play.

*[next slide]*

## Slide 2 — Outline

Here is how we are going to organize the next twenty minutes or so.

We start with **maze implementation**, where we look at the four algorithms we use to generate the labyrinth. Then we go into the **project implementation**, with the backend API and the frontend client. After that we open up the **MongoDB** layer, which is how we persist mazes, ratings, and replays. Then comes the most interesting part for us, the **ghosts and the AI** — both how the ghosts hunt Pac-Man and how a bot can play Pac-Man on its own. We finish with the overall **system architecture** and a short list of **features** before a quick demo.

*[next slide]*

---

# Part 1 — Maze Generation

## Slide 3 — Kruskal's Algorithm

We support four maze generation algorithms. They all produce a *perfect maze*, meaning every cell is reachable from every other cell, but each one gives a very different look and feel.

The first one is **Kruskal's**. The idea is borrowed from graph theory: every wall in the grid is treated as an edge, and every cell starts in its own little group thanks to a Union-Find data structure. We shuffle all the walls randomly, then we go through them one by one. For each wall, we look at the two cells it separates. If they belong to different groups, that wall is removed and the two groups merge. If they are already in the same group, we leave the wall alone — otherwise we would create a loop.

When all cells end up merged into a single group, the maze is done. Kruskal's gives us a very **balanced, uniform** maze, with medium-length corridors and no real bias toward any particular shape.

*[next slide]*

## Slide 4 — Prim's Algorithm

Prim's takes a different approach. Instead of looking at edges globally, it grows the maze outward from a single starting cell.

We begin by marking one cell as "in the maze," and we put all of its neighbors into a list called the **frontier**. Then we randomly pick one cell from that frontier, knock down the wall between it and the maze, and the new cell becomes part of the maze. Its own neighbors then join the frontier.

We repeat that process until there is nothing left to add. Because we are always growing outward, Prim's produces a **blob-like, branchy** maze with short corridors and lots of intersections. It feels very different from Kruskal's even though the end result is still a perfect maze.

*[next slide]*

## Slide 5 — Tetris (Active Cells)

The third one we called **Tetris**. It's a variant we wrote ourselves, inspired by the "active cells" technique.

Here we keep a list of *active* cells — cells that are still allowed to grow. We start with one. At each step we pick one active cell and try to expand from it to a free neighbor. To make the maze more interesting, we give an **85% bias toward cells with at least two free neighbors**, so the maze keeps branching aggressively. After expansion, both the parent and the child stay in the active list, which creates many parallel "fronts" growing at the same time.

The result is **very dense, short, busy corridors** — perfect when you want a maze that feels crowded and full of choices.

*[next slide]*

## Slide 6 — DFS (Depth-First Search)

And finally, the one most people think of first: **Depth-First Search with backtracking**.

We start with all walls up and a random starting cell. We pick a random unvisited neighbor, knock down the wall between them, and move into it. We keep going deeper as long as we can. When we hit a dead end — no unvisited neighbors left — we backtrack along our path until we find a cell that still has options, and we continue from there.

The result is the **long, winding corridor** look you probably recognize from classic maze games. Few branches, deep paths, and usually one dominant solution route.

And on this slide you can also see the **post-processing** we do on every maze, regardless of the algorithm: we add some cycles to remove the "single solution path" feeling, we mirror the maze to get the symmetric Pac-Man look, we insert teleport tunnels, and we carve out the ghost room in the center.

*[next slide]*

## Slide 7 — Algorithm Comparison

This table sums it all up. DFS uses a **recursive stack** and gives long winding corridors with low branching. Kruskal works on **random edges** and gives medium, uniform corridors. Prim grows from a **frontier** and gives short, branchy ones. Tetris uses **active cells** and gives the densest, busiest maps.

What's important is that all four share the **same post-processing pipeline**, which is implemented in a parent class. Whatever algorithm you choose, you still get the ghost room, the mirroring for symmetry, the middle connector column, the teleport pairs, and a sealed border. Only step 3, the actual carving, differs. That gave us a clean inheritance structure in the code.

*[next slide]*

---

# Part 2 — Project Implementation

## Slide 8 — Back End

Let's now move to the **backend**. The backend is a Flask service written in Python, and we expose it through **Swagger** so the API is self-documented and testable from the browser.

It does three things: it generates mazes on demand, it stores them in MongoDB, and it handles ratings and replays.

*[next slide]*

## Slide 9 — Maze generation and retrieval

These are the main maze endpoints.

`GET /labyrinth` is the one the game calls most often. You give it the algorithm name, the half-width and half-height, and it generates a new maze, stores it in MongoDB, and returns the JSON. The reason we ask for *half* width and height is that the maze gets mirrored, so the final size is roughly twice that.

`GET /labyrinth/{maze_id}` lets you fetch a specific maze again later — useful when you want to replay an old game on the exact same map. And `GET /mazes` lists the metadata of every saved maze so you can browse them.

*[next slide]*

## Slide 10 — Maze rating

We also added a small **community rating system**. After playing a maze, you can submit a score from 0 to 5 through `POST /labyrinth/rate`. The backend keeps the running sum, the count, and the average for each maze, so over time you can see which generated maps people actually enjoyed.

It's a tiny feature but it's the kind of thing that makes the system feel alive rather than just a one-shot generator.

*[next slide]*

## Slide 11 — Game replay management

Now the replay system. We wanted every game played to be **recordable and watchable later**, like a film of the match.

`POST /replay` saves a full replay: player type, the maze ID, the final score, dots eaten, whether the player won, duration, and most importantly the frame-by-frame state and the inputs.

`GET /replay/{replay_id}` fetches one back, and `GET /replays` lists them all. From the desktop client you can open the replay browser, pick any saved game, and watch it run.

*[next slide]*

---

# Part 3 — MongoDB

## Slide 12 — MongoDB Data Model

We chose **MongoDB Atlas** as our database because the data we save is naturally document-shaped — a maze is a nested grid of cells, a replay is a long array of frames, none of that fits neatly into SQL tables.

We have two collections.

The first one is **`mazes`**. Each document stores the algorithm used, the dimensions, the full maze structure with all the walls, and the community rating data — average, count, sum — plus a creation timestamp.

The second collection is **`replays`**. Each replay document stores who played — a human or a bot, and if it's a bot, which strategy — the final score, the dots eaten, the win or loss status, the duration in seconds, and then two arrays: the **frames** array, which captures the game state over time, and the **inputs** array, which captures every direction change the player made. You can see on the left an actual document we extracted from the database for illustration.

This dual storage — frames *and* inputs — is what lets us reconstruct a game faithfully when we replay it.

*[next slide]*

---

# Part 4 — Front-End

## Slide 13 — Front-End

Time to talk about the front-end. The frontend is a **desktop game** built in Java with **LibGDX**, which is a 2D game framework on top of OpenGL.

We chose LibGDX rather than Swing or JavaFX because it gives us a real GPU-accelerated rendering pipeline, a proper game loop, viewport handling, and on top of that the Scene2D toolkit for the menus. The result is smooth animations and a UI that scales nicely.

*[next slide]*

## Slide 14 — Menu Screen

When you launch the game, this is what you see.

You can pick the **generation algorithm** — DFS, Kruskal, Prim, or Tetris. You can adjust the **height and width** with sliders. If you turn on **animated generation**, the game will play back the maze being built step by step before you start playing.

In the AI section, you choose the **ghost AI** — Classic or A-Star, we will talk about that in detail later — and you can switch on **AI Autoplay**, which lets a bot play Pac-Man instead of you, perfect for a demo.

Then under Gameplay you can toggle whether ghosts are enabled at all, and whether you want a Race versus Bot mode.

The **server address** is configurable too. By default it points to localhost, but you can also point it to our hosted Render instance and play against the cloud backend without running anything locally. Then you hit Generate to play, or Browse Replays to watch saved games.

*[next slide]*

## Slide 15 — Browse Replays menu

This is the replay browser. Every game is saved in the database, and from here you can list them all, see who played, the maze size, the algorithm, the score, the result — won or lost — and the timestamp. Click Watch and the game replays exactly as it happened, frame by frame.

*[next slide]*

## Slide 16 — Demo Replay

Same screen scrolled down. We already have thirty-three saved replays. Some are human runs, others are bot runs, scores vary from very low to over two thousand. This is a small but real dataset that we can use to analyze how well our different AI strategies perform.

*[next slide]*

---

# Part 5 — Ghosts and AI

## Slide 17 — Ghost AI: Modes of Operation

Now we get to the most interesting part: the AI. Let's start with the ghosts.

In our game, just like in the original Pac-Man, every ghost cycles through **four modes**.

**Scatter** is the calm mode. The ghosts back off and head toward their own assigned corner of the maze. It gives you a brief breathing moment.

**Chase** is the offensive mode. Each ghost actively tries to catch Pac-Man, and this is where each one shows its personality.

**Frightened** is triggered when Pac-Man eats a power pellet. The ghosts turn blue, slow down, move semi-randomly, and now Pac-Man can eat *them* for bonus points.

**Eaten** is what happens after Pac-Man does eat a ghost. The ghost becomes a pair of eyes and goes back to the ghost room to respawn, ignoring walls on the way.

These four modes alternate during the game and create the classic Pac-Man pacing.

*[next slide]*

## Slide 18 — Scatter mode

Here you can see Scatter in action. Each ghost is heading to its own corner — red is top-right, pink is top-left, orange is bottom-left, cyan is bottom-right. Pac-Man is safe in the middle for a few seconds. You can see the targeting lines if you look closely.

*[next slide]*

## Slide 19 — Chase mode

This is Chase. The colored lines you see are the paths the ghosts are computing in real time toward Pac-Man. Notice that they all converge on him from different sides — that's the whole point: with four different targeting strategies, the ghosts surround Pac-Man rather than just stacking on top of each other.

*[next slide]*

## Slide 20 — Frightened mode

When Pac-Man eats a power pellet, every ghost turns blue and switches to Frightened mode. They wander, they try to escape, and Pac-Man becomes the hunter. You can see all four ghosts here in their blue state.

*[next slide]*

## Slide 21 — Eaten mode

And here, a couple of ghosts have already been eaten. They're now just floating eyes making their way back to the central ghost room, where they will respawn and re-enter the chase. This is the recovery phase.

*[next slide]*

## Slide 22 — The Four Apparitions

Each of the four ghosts has its own **chase strategy**, and that's what makes Pac-Man genuinely challenging instead of feeling like four clones of the same enemy.

**Blinky**, the red one, is the **aggressor**. He targets Pac-Man's current tile directly. So he's always coming straight at you.

**Pinky**, the pink one, is the **ambusher**. She doesn't aim where Pac-Man is — she aims **four tiles ahead** in the direction Pac-Man is currently moving. The idea is to cut him off.

**Inky**, the cyan one, is the **unpredictable** one. His target is computed using both Blinky's position and Pac-Man's position, which creates a strange flanking behavior that's hard to anticipate.

**Clyde**, the orange one, is the **timid** one. When he's far from Pac-Man, he chases like Blinky. But the moment he gets close, he turns around and scatters back to his corner. He's the comic relief of the group.

Together, these four behaviors are what give the original game its strategic depth, and we reproduced all of them in our implementation.

*[next slide]*

## Slide 23 — Blinky

Quick visual on Blinky. Always aiming straight at Pac-Man's tile. This is the core of the threat.

*[next slide]*

## Slide 24 — Pinky

Pinky, the ambusher, projecting four tiles ahead of Pac-Man's direction. If you stop or turn, you can mess up her prediction.

*[next slide]*

## Slide 25 — Inky

Inky uses Blinky as a reference point. His target is computed from a vector that depends on both Blinky and Pac-Man, which makes his path unpredictable.

*[next slide]*

## Slide 26 — Clyde

And Clyde, the timid one. As long as you stay away, he hunts. Get close, and he runs back to his corner.

*[next slide]*

## Slide 27 — Ghost AI: Difficulty Levels

So that covers *what* the ghosts try to do. The next question is *how* they actually navigate the maze to get there.

We implemented **two difficulty levels** for the ghosts: **Classic**, which uses a greedy algorithm, and **A-Star**, which uses proper pathfinding. They produce visibly different behavior, especially on larger mazes, and you can switch between them from the menu.

*[next slide]*

## Slide 28 to 31 — Greedy

Let's see the **Greedy** mode first.

A greedy ghost only looks at its four immediate neighbors and picks the one that **minimizes the squared Euclidean distance** to Pac-Man's target tile. It doesn't think about walls beyond the very next move.

Look at the example. The ghost has to decide which direction to take. From its current position, going up leads to a tile that's about square root of twenty-five away from Pac-Man — that's five. Going left would be square root of forty-five — that's about six point seven.

*[advance]*

So the greedy ghost picks the move with the smallest local distance, which is up. The good thing is that it's fast — constant time per decision. The bad thing is that it can get fooled by walls. If the shortest *Euclidean* direction leads into a dead end, the ghost will happily walk into it and waste time turning around.

*[advance through Greedy slides 29, 30, 31]*

That's the Classic ghost behavior — fast, simple, sometimes silly. It matches the feel of the original arcade Pac-Man pretty well, because the original was greedy too.

*[next slide]*

## Slide 32 to 35 — A-Star

The A-Star mode is more sophisticated.

Instead of just looking at neighbors, an A-Star ghost computes the **actual shortest path** through the maze graph using the A-Star algorithm. It uses Manhattan distance as the heuristic, and a priority queue to explore nodes in the right order.

The decision is no longer based on raw distance but on the **true path length** through walls. So in this example, the ghost can see that going one way has a real path cost of nine, while going the other way has a cost of seven — even though the Euclidean distance looked similar.

*[advance through A-Star slides]*

The result is a ghost that doesn't get fooled by dead ends and that takes much smarter routes. It's significantly harder to escape from. On the downside, it's more expensive to compute, especially on large mazes — but in practice it runs comfortably within a frame.

*[next slide]*

## Slide 36 — Comparison Table

Here is the summary side by side.

Greedy uses **Greedy Best-First Search** with the squared Euclidean distance and only looks at immediate neighbors. It picks moves using a fixed directional priority — Up, Left, Down, Right — when distances are tied, which mirrors the original arcade behavior. It's fast but suboptimal.

A-Star uses **proper A-Star Search** with Manhattan distance as a heuristic, explores the complete maze graph, and uses a priority queue. It finds the **most optimal path** every time, at the cost of more computation.

*[next slide]*

## Slide 37 — Ghost Hunting Efficiency

We also ran some measurements. This chart shows the **average Euclidean distance from the ghosts to Pac-Man over time**, for both algorithms, averaged across multiple runs.

You can see that the A-Star ghosts — in orange — manage to keep a smaller distance to Pac-Man on average during most of the game. They navigate walls more efficiently, so even though Pac-Man moves at the same speed, the ghosts close the gap faster and stay closer.

The Classic Greedy ones — in blue — are more erratic. They sometimes get stuck against walls, distance spikes back up, and the ghosts have to recover. So this is a quantitative confirmation that A-Star really is the harder difficulty.

*[next slide]*

---

# Part 6 — Pac-Man AI Bot

## Slide 38 — Player AI Bot Strategies

We also wanted Pac-Man himself to be playable by an AI. So we built a **bot agent** with two strategies you can switch between.

The **Greedy bot** uses **Breadth-First Search** to find the closest dot and walks straight to it. It only cares about eating dots quickly. It completely ignores ghosts.

The **Ghost Avoidance bot** uses a **weighted Dijkstra** algorithm. The cost of each cell is increased the closer it is to a ghost, so the bot is pulled toward dots but pushed away from ghosts at the same time. It trades raw speed for survival.

*[next slide]*

## Slide 39 to 42 — Greedy BFS Bot

Let's walk through the Greedy bot first.

Pac-Man is here, there are two dots in the maze, and we have walls. The bot starts a **BFS** from Pac-Man's current position, exploring outward layer by layer until it finds the first dot.

*[advance]*

It puts Pac-Man's position in the queue, then expands to all reachable neighbors. Walls are not traversable, so they're skipped. The frontier grows uniformly.

*[advance]*

The BFS keeps expanding. Because BFS explores in concentric rings, the **first dot it touches is guaranteed to be the closest one** in terms of number of moves.

*[advance]*

Once a dot is found, the bot reconstructs the path back from the dot to Pac-Man and starts moving along it. The bot recomputes this every few ticks, so as soon as the closest dot changes — for example, because a closer one becomes reachable or another one was eaten — it adapts.

This works well in safe situations, but if a ghost is nearby, the Greedy bot will happily walk right into it because it doesn't see ghosts at all.

*[next slide]*

## Slide 43 to 48 — Ghost Avoidance (Weighted Dijkstra)

That's where the Ghost Avoidance bot comes in.

Same maze, but now there's a ghost in the way. The bot computes, for every cell, **how far it is from the nearest ghost** — that's the "d" value you see on the slides. The closer a cell is to a ghost, the more dangerous, the higher its cost.

*[advance]*

The cost of moving through a cell is `1 + d * 3`, where `d` is the ghost-proximity bonus. So a cell right next to a ghost is very expensive, and a cell far from any ghost has roughly unit cost.

*[advance through slides 45 to 47]*

Then we run a **Dijkstra search** from Pac-Man to all reachable dots using these weighted costs. The shortest-cost path tells us not the shortest physical path, but the **safest balance between distance and risk**.

*[advance]*

In this example, the bot does not take the direct path through the corridor next to the ghost — it goes around. Total cost is sixteen, but Pac-Man is alive. That's the whole point of this strategy: a slightly longer life is worth a slightly longer route.

In practice we found that the Ghost Avoidance bot wins many more games and reaches higher scores, while the Greedy bot is faster but dies more often. Both are useful for benchmarking, which is why we kept them both.

*[next slide]*

---

# Part 7 — System Architecture

## Slide 49 — System Architecture

Let's step back and look at the **full system architecture**, because all these pieces talk to each other.

On the left, the **developer** pushes code or tags to **GitHub**. That triggers our **CI/CD pipeline on GitHub Actions**, which has two workflows.

The **backend workflow** runs the test suite first. If tests pass, it builds the Docker image, pushes it to **Docker Hub**, and then triggers a deploy hook on **Render**, which pulls the image and runs our Flask API in the cloud.

The **release workflow** kicks in when we push a Git tag starting with `v`. It uses **jpackage** to build native desktop bundles for Linux, Windows, and macOS in parallel, zips them, and publishes them as a **GitHub Release**. So users can just download and run the game without installing Java or Python.

On the right, the deployed **Flask API on Render** talks to **MongoDB Atlas**, which stores maze ratings and game replays.

Finally, on the bottom right, the **LibGDX Java client** running on the user's machine downloads either from GitHub Releases for the binary, or talks directly to the Flask API over HTTP to fetch labyrinths, post ratings, and save or load replays.

So we have a fully automated pipeline: push code, and within minutes the cloud backend is updated and new native installers are ready. Nothing is manual.

*[next slide]*

---

# Part 8 — Features and Demo

## Slide 50 — Features

Let's recap the feature set.

A **full GUI** desktop client written in Java and LibGDX, with custom Pac-Man rendering — walls, dots, power pellets, ghosts, teleport tunnels.

**MongoDB Atlas persistence** for mazes, ratings, and replays.

**Ghost AI** with four distinct personalities — Blinky, Pinky, Inky, Clyde — and four operating modes — Scatter, Chase, Frightened, Eaten.

Two **difficulty levels** for the ghosts: Classic Greedy and A-Star optimal pathfinding.

A **Player AI Bot** with two strategies, Greedy BFS and weighted Dijkstra Ghost Avoidance.

A **Race mode** so a human can compete head-to-head against a bot.

And a complete **replay system** to record and watch every game back.

*[next slide]*

## Slide 51 — Demo & Conclusion

That brings us to the demo. Rather than just describing the game, we will now show it running live — generating a maze, playing a round, switching ghost AI, and triggering a replay.

Before we move to that, just a quick word on what this project taught us. We touched a really wide range of topics in a single codebase: graph algorithms for maze generation, classical AI pathfinding with A-Star and Dijkstra, real-time game state management, REST API design, containerization, automated CI/CD, and packaged desktop distribution. Combining all of that into a coherent product was the real challenge — and we are quite happy with how it turned out.

Thank you for listening. We are now happy to take your questions, and to play a quick demo. *[switch to live demo]*
