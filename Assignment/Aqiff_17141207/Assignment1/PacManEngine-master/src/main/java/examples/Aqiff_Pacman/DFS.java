package examples.Aqiff_Pacman;

import pacman.controllers.PacmanController;
import pacman.game.Constants.DM;
import pacman.game.Constants.GHOST;
import pacman.game.Constants.MOVE;
import pacman.game.Game;

import java.util.ArrayList;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DFS extends PacmanController{
    private Game game;
    MOVE pacmanLastMove ;

    //we initialize route in an array list
    private List<Route>paths = new ArrayList<>();
    private int currentPacmanIndexNode;
    int pathLength = 100;

    //we will initialize visionArea class which will be as a parent
    public class visionArea {
        public int start;
        public int end;
        public int pillsCount = 0;
        public int powerPillsCount = 0;
        public int lengthSoFar;
        public MOVE direction;
        public visionArea parent;
        public List<GHOST> ghosts = new ArrayList<>();
        public boolean safe = true;
    }

    //get movement of the pacman
    public MOVE getMove(Game game, long timeDue){
        this.game = game;
        currentPacmanIndexNode = game.getPacmanCurrentNodeIndex();
        pacmanLastMove = game.getPacmanLastMoveMade();

        // Get possible paths
        paths = getPaths(pathLength);

        // Sort the path with highest value DESC
        Collections.sort(paths, new comparePath());
        Route bestpath = paths.get(0);
        MOVE bestpathMove = game.getMoveToMakeToReachDirectNeighbour(currentPacmanIndexNode, bestpath.start);

        // No pills around while at junction but has safe paths
        if (bestpath.value == 0 && game.isJunction(currentPacmanIndexNode))
        {
            // Get only safe paths from paths
            List<MOVE> safeMoves = new ArrayList<>();
            for (Route path: paths)
            {
                if(path.safe)
                {
                    //we will add it to our save move
                    MOVE safeMove = game.getMoveToMakeToReachDirectNeighbour(currentPacmanIndexNode, path.start);
                    safeMoves.add(safeMove);
                }
            }
        }

        // Not safe path
        else if (bestpath.value < 0)
        {
            bestpathMove = pacmanLastMove;
        }

        // if the current best move is not better than previous move, then we maintain previous move, this is to avoid pacman flickering movement
        else if (bestpathMove != pacmanLastMove)
        {
            for (Route path: paths)
            {
                MOVE move = game.getMoveToMakeToReachDirectNeighbour(currentPacmanIndexNode, path.start);

                if (move == pacmanLastMove && path.value == bestpath.value)
                {
                    bestpathMove = move;
                    break;
                }
            }
        }
        return bestpathMove;
    }

    public class Route {
        public int start;
        public int end;
        public List<GHOST> ghosts = new ArrayList<GHOST>();
        public int powerPillsCount = 0;
        public int pillsCount = 0;
        public List<visionArea> segments = new ArrayList<visionArea>();
        public int length;
        public String description = "";
        public boolean safe = true;
        public int value = 0;

        // Important: It must be in sequence
        Route(List<visionArea> segments)
        {
            this.segments = segments;
        }

        public void process()
        {
            // TODO: calculate path value
            int segmentsCount = segments.size();

            //if child is more than 1
            if(segmentsCount > 0)
            {
                //initialize the firstsegment (top node) and lastsegment (last node)
                visionArea firstSegment = segments.get(0);
                visionArea lastSegment = segments.get(segmentsCount - 1);
                start = firstSegment.start;
                end = lastSegment.end;
                length = lastSegment.lengthSoFar;
                pillsCount = lastSegment.pillsCount;
                value = pillsCount;
                powerPillsCount = lastSegment.powerPillsCount;
                int unsafeSegmentsCount = 0;

                //for each node, we will check the condition of it
                for (visionArea area : segments)
                {
                    //if the ghost at the is not empty
                    if (!area.ghosts.isEmpty())
                    {
                        //we will add at the area with ghost
                        ghosts.addAll(area.ghosts);
                        for (GHOST ghost: ghosts)

                            //this is the code that i change where i add a condition where ghost is not edible

                            if (game.isGhostEdible(ghost)) {
                                int distance = game.getShortestPathDistance(currentPacmanIndexNode, game.getGhostCurrentNodeIndex(ghost));
                                if (distance < 10)
                                    value += 1;//15;
                                else
                                    value += 1;//10;
                            }
                            //if ghost is not edible, we will make the pacman move away from target, which is the ghost
                            else{

                                game.getNextMoveAwayFromTarget(currentPacmanIndexNode,game.getGhostCurrentNodeIndex(ghost),DM.PATH);
                            }
                    }

                    //check whether parent is not null and unsafe
                    if (area.parent != null && !area.parent.safe)
                        //then the child node inherited the parent .safe
                        area.safe = area.parent.safe;

                    //if the area unsafe, we will lower down the value and increase unsafe count
                    if (!area.safe)
                    {
                        unsafeSegmentsCount++;
                        value -= 10;
                    }
                    value += area.powerPillsCount * 5;
                    description += area.direction.toString() + " ";
                }

                //then from the unsafe count, if its greater  than 0, it is considered as unsafe
                if (unsafeSegmentsCount > 0)
                    safe = false;
            }
        }
    }


    public class comparePath implements Comparator<Route> {
        @Override
        public int compare(Route path1, Route path2)
        {
            return path2.value - path1.value;
        }
    }

    public List<Route> getPaths(int maxPathLength)
    {
        MOVE[] startingPossibleMoves = game.getPossibleMoves(currentPacmanIndexNode);
        List<Route> paths = new ArrayList<>();
        int minGhostDistance=94;

        // Start searching from the possible moves at the current pacman location
        for (MOVE startingPossibleMove : startingPossibleMoves)
        {
            //we initialize the pending area that will hold the area
            List<visionArea> pendingArea = new ArrayList<visionArea>();

            // Step into next node
            int currentNode = game.getNeighbour(currentPacmanIndexNode, startingPossibleMove);

            // Create new segment starting from the node next to pacman
            visionArea currentArea = new visionArea();
            currentArea.start = currentNode;
            currentArea.parent = null;
            currentArea.direction = startingPossibleMove;
            currentArea.lengthSoFar++;

            // Get all ghosts node index in a list
            ArrayList<Integer> ghostNodeIndices = new ArrayList<>();
            GHOST[] ghosts= GHOST.values();

            //then i convert the ghost list into arrayghosts, so it is easier to perform game.getClosestNodeFromIndex,
            for (GHOST ghost: ghosts)
                ghostNodeIndices.add(game.getGhostCurrentNodeIndex(ghost));
            int[] ghostsArray = new int[ghostNodeIndices.size()];
            for (int i = 0; i < ghostsArray.length; i++) {
                ghostsArray[i] = ghostNodeIndices.get(i);
            }

            // Loop each step
            do
            {
                // Check pills and power pills
                int pillIndex = game.getPillIndex(currentNode);
                try
                {
                    if (pillIndex != -1 && game.isPillStillAvailable(pillIndex))
                    {
                        currentArea.pillsCount++;
                    }
                }
                catch (Exception e)
                {
                    throw e;
                }

                // Segment contains ghost(s), not safe if ghost direction is opposite of segment direction and is not edible
                if (ghostNodeIndices.contains(currentNode))
                    for (GHOST ghost: ghosts)
                    {
                        if(game.getGhostCurrentNodeIndex(ghost) == currentNode)
                        {
                            currentArea.ghosts.add(ghost);

                            //if the ghost is not edible
                            if (!game.isGhostEdible(ghost)
                                    && game.getGhostLastMoveMade(ghost) == currentArea.direction.opposite()
                                    && game.getEuclideanDistance(currentPacmanIndexNode, currentNode) <= minGhostDistance )
                            {
                                //we will change the currentarea.safe as false which is unsafe
                                currentArea.safe = false;
                                //if it has a parent and not null, we will change it's parents safe too
                                if (currentArea.parent != null)
                                    currentArea.parent.safe = false;
                            }

                            //if the ghost is edible
                            else if (game.isGhostEdible(ghost)
                                    && game.getGhostLastMoveMade(ghost) != currentArea.direction.opposite()
                                    && game.getEuclideanDistance(currentPacmanIndexNode, currentNode) >= minGhostDistance )
                            {
                                //we will change the currentArea.safe = true
                                currentArea.safe = true;
                                minGhostDistance = 40;
                                if (currentArea.parent != null)
                                    currentArea.parent.safe = true;
                                game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(),game.getClosestNodeIndexFromNodeIndex(currentPacmanIndexNode,ghostsArray, DM.PATH),DM.PATH);
                            }
                        }
                    }

                // Check if length is max
                if (currentArea.lengthSoFar >= maxPathLength){
                    currentArea.end = currentNode;

                    // Create a new path and insert segments that make up the path
                    ArrayList<visionArea> pathSegments = new ArrayList<>();
                    do {
                        pathSegments.add(currentArea);
                        currentArea = currentArea.parent;
                    }
                    while(currentArea != null);

                    Collections.reverse(pathSegments);
                    Route path = new Route(pathSegments);
                    paths.add(path);

                    // Pop out the latest pending segment and set it as current segment
                    if (!pendingArea.isEmpty())
                    {
                        currentArea = pendingArea.remove(pendingArea.size()-1);
                        currentNode = currentArea.start;
                        currentArea.lengthSoFar++;
                        continue;
                    }
                    else
                        break;
                }

                //the possible moves of the pacman based on its current node and current area direction
                MOVE[] possibleMoves = game.getPossibleMoves(currentNode, currentArea.direction);

                // If neighbor is a junction or a corner, end the current segment and create a new segment
                if (possibleMoves.length > 1 || (possibleMoves.length == 1 && possibleMoves[0] != currentArea.direction))
                {
                    currentArea.end = currentNode;
                    visionArea parentSegment = currentArea;

                    for (int i = 0; i < possibleMoves.length; i++)
                    {
                        MOVE possibleMove = possibleMoves[i];
                        int neighborNode = game.getNeighbour(currentNode, possibleMove);

                        // Create new segment for each neighbor node
                        visionArea area = new visionArea();
                        area.start = neighborNode;
                        area.direction = possibleMove;
                        area.parent = parentSegment;
                        area.pillsCount = parentSegment.pillsCount;
                        area.powerPillsCount = parentSegment.powerPillsCount;
                        area.lengthSoFar = currentArea.lengthSoFar;
                        area.safe = parentSegment.safe;

                        if (i == 0)
                            currentArea = area;
                        else
                            pendingArea.add(area);
                    }
                }

                // Step into next node
                currentNode = game.getNeighbour(currentNode, currentArea.direction);
                currentArea.lengthSoFar++;
            }
            while(!pendingArea.isEmpty() || currentArea.lengthSoFar <= maxPathLength);

        }

        // Required to calculate the required data in each path
        for (Route path : paths)
            path.process();

        return paths;
    }
}
