package pacman.controllers.examples;


import pacman.controllers.PacmanController;
import pacman.game.Game;
import pacman.game.internal.Ghost;

import java.util.ArrayList;

import static pacman.game.Constants.*;

/*
 * Pac-Man controller as part of the starter package - simply upload this file as a zip called
 * MyPacMan.zip and you will be entered into the rankings - as simple as that! Feel free to modify
 * it or to start from scratch, using the classes supplied with the original software. Best of luck!
 *
 * This controller utilises 3 tactics, in order of importance:
 * 1. Get away from any non-edible ghost that is in close proximity
 * 2. Go after the nearest edible ghost
 * 3. Go to the nearest pill/power pill
 */
public class newStarterPacMan extends PacmanController {
    private static final int MIN_DISTANCE = 30;    //if a ghost is this close, run away

    @Override
    public MOVE getMove(Game game, long timeDue) {
        int current = game.getPacmanCurrentNodeIndex();
        int[] pills = game.getPillIndices();
        int[] powerPills = game.getPowerPillIndices();

        //Strategy 1: if any non-edible ghost is too close (less than MIN_DISTANCE), run away
        for (GHOST ghost : GHOST.values()) {
            if (game.getGhostEdibleTime(ghost) == 0 && game.getGhostLairTime(ghost) == 0) {
                //if position hantu is less than minimum distance (dah dekat)
                if (game.getShortestPathDistance(current, game.getGhostCurrentNodeIndex(ghost)) < MIN_DISTANCE) {
                    ArrayList<Integer> targets = new ArrayList<Integer>();
                    for (int i = 0; i < powerPills.length; i++)            //check with power pills are available
                    {
                        if (game.isPowerPillStillAvailable(i) ) {
                            targets.add(powerPills[i]);
                        }
                    }

                    int[] targetsArray = new int[targets.size()];        //convert from ArrayList to array
                    for (int i = 0; i < targetsArray.length; i++) {
                        targetsArray[i] = targets.get(i);
                        }

                    game.getNextMoveAwayFromTarget(current,game.getGhostCurrentNodeIndex(ghost),DM.PATH);
                    return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), game.getClosestNodeIndexFromNodeIndex(game.getPacmanCurrentNodeIndex(), targetsArray, DM.PATH), DM.PATH);
                }

                //if hantu punya is more than minimum distance
                else{
                    ArrayList<Integer> targets = new ArrayList<Integer>();
                    for (int i = 0; i < pills.length; i++)                    //check which pills are available
                    {
                        if (game.isPillStillAvailable(i)) {
                            targets.add(pills[i]);
                        }
                    }
                    int[] targetsArray = new int[targets.size()];
                    for (int i = 0; i < targetsArray.length; i++) {
                        targetsArray[i] = targets.get(i);
                    }
                    game.getNextMoveAwayFromTarget(current,game.getGhostCurrentNodeIndex(ghost),DM.PATH);
                    return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), game.getClosestNodeIndexFromNodeIndex(game.getPacmanCurrentNodeIndex(), targetsArray, DM.PATH), DM.PATH);
                }
            }

            else{
                if (game.getShortestPathDistance(current, game.getGhostCurrentNodeIndex(ghost)) < MIN_DISTANCE) {
                    return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(ghost), DM.PATH);
                }
                else{
                    ArrayList<Integer> targets = new ArrayList<Integer>();
                    for (int i = 0; i < pills.length; i++)                    //check which pills are available
                    {
                        if (game.isPillStillAvailable(i)) {
                            targets.add(pills[i]);
                        }
                    }
                    int[] targetsArray = new int[targets.size()];
                    for (int i = 0; i < targetsArray.length; i++) {
                        targetsArray[i] = targets.get(i);
                    }
                    return game.getNextMoveTowardsTarget(current,game.getGhostCurrentNodeIndex(ghost),game.getPacmanLastMoveMade(),DM.PATH);
                }
            }
        }

        //Strategy 2: find the nearest edible ghost and go after them
        int minDistance = Integer.MAX_VALUE;
        GHOST minGhost = null;

        for (GHOST ghost : GHOST.values()) {
            if (game.getGhostEdibleTime(ghost) > 0) {
                int distance = game.getShortestPathDistance(current, game.getGhostCurrentNodeIndex(ghost));

                if (distance < minDistance) {
                    minDistance = distance;
                    minGhost = ghost;
                }
            }
        }

        if (minGhost != null)    //we found an edible ghost
        {
            return game.getNextMoveTowardsTarget(game.getPacmanCurrentNodeIndex(), game.getGhostCurrentNodeIndex(minGhost), DM.PATH);
        }

        //Strategy 3: go after the pills and power pills
        //return the next direction once the closest target has been identified
        ArrayList<Integer> targets = new ArrayList<Integer>();
        for (int i = 0; i < pills.length; i++)                    //check which pills are available
        {
            if (game.isPillStillAvailable(i)) {
                targets.add(pills[i]);
            }
        }
        int[] targetsArray = new int[targets.size()];
        for (int i = 0; i < targetsArray.length; i++) {
            targetsArray[i] = targets.get(i);
        }

        if (powerPills.length==1){
            game.getNextMoveAwayFromTarget(current,game.getClosestNodeIndexFromNodeIndex(game.getGhostCurrentNodeIndex(minGhost),powerPills,DM.PATH),DM.PATH);
            return game.getNextMoveTowardsTarget(current, powerPills[0], DM.PATH);
        }

        return game.getNextMoveTowardsTarget(current, game.getClosestNodeIndexFromNodeIndex(current, targetsArray, DM.PATH), DM.PATH);
    }
}
























