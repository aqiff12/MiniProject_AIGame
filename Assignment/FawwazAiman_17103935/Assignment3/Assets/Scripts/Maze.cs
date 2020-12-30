using System;
using System.Collections;
using System.Collections.Generic;
using UnityEngine;

public class Maze : MonoBehaviour
{
    public int width;   // Along x axis
    public int height;  // Along z axis
    public bool pathVisible;
    [Range(0.1f, 0.7f)]
    public float exitCellDistance = 0.4f;

    private Cell[,] cells;
    private int[,] cellDistances;
    private List<Cell> activeCells = new List<Cell>();
    private Cell entryCell;
    private Cell exitCell;
    private int currentLength = 0;

    private bool checkRow = false;
    private bool checkCorner = false;

    public Cell CellPrefab;
    public Transform wallPrefab;
    public Material entryCellMaterial;
    public Material exitCellMaterial;
    public Material defaultCellMaterial;
    public float delay;

    public void Awake()
    {
        cells = new Cell[width, height];
        cellDistances = new int[width, height];
    }

    public IEnumerator Generate()
    { 
        // Initialize the first cell as entry cell
        Cell currentCell = PlaceInitialEntryCell();
        currentLength++;
        cellDistances[currentCell.Location.x, currentCell.Location.z] = currentLength;
        activeCells.Add(currentCell);
        yield return new WaitForSeconds(delay);


        // Start connecting
        while (activeCells.Count > 0)
        {
            //Debug.Log(activeCells.Count);
            int index = activeCells.Count - 1;
            currentCell = activeCells[index];
            currentLength = cellDistances[currentCell.Location.x, currentCell.Location.z];
            currentCell.name += "_length=" + currentLength;

            Cell nextCell = MakeConnection(currentCell);
            if (nextCell != null)
            {
                currentLength++;
                cellDistances[nextCell.Location.x, nextCell.Location.z] = currentLength;
                activeCells.Add(nextCell);
                yield return new WaitForSeconds(delay);
            }
            else
            {
                // Set exit cell if it is an edge cell and last cell in the active cells list and not entry cell
                if (IsEdgeCell(currentCell) && exitCell == null && currentCell != entryCell && exitCellDistanceCriteriaMet(currentCell))
                {
                    SetExitCell(currentCell);
                }

                activeCells.Remove(currentCell);
            }
        }

        CreateCellWalls();

        print("Generate complete");
    }

    private bool exitCellDistanceCriteriaMet(Cell cell)
    {
        float ratio = cellDistances[cell.Location.x, cell.Location.z] / (float)(width * height);
        bool met = ratio >= exitCellDistance;
        print(cell.name + ", ratio: " + ratio + ", met:" + met);
        return met;
    }

    public void CreateCellWalls()
    {
        
        for (int i = 0; i < width; i ++)
        {
            for (int j = 0; j < height; j++)
            {
                //Debug.Log(cells[i, j]); 
                cells[i, j].CreateWalls();
            }
        }
    }

    public Cell PlaceInitialEntryCell()
    {
        // Generate four possible start cells along the edges of the maze into a list  
        List<CellLocation> possibleStartCells = new List<CellLocation>()
        {
            new CellLocation(0, 0),
        };

        // Randomly get a possible start cell after shufflering the list elements
        possibleStartCells.Shuffle();
        CellLocation location = possibleStartCells[0];
        Cell cell = PlaceCell(location);
        cell.Material = entryCellMaterial;
        entryCell = cell;
        return cell;
    }

    public Cell PlaceRandomEntryCell()
    {
        // Generate four possible start cells along the edges of the maze into a list  
        List<CellLocation> possibleStartCells = new List<CellLocation>()
        {
            new CellLocation(UnityEngine.Random.Range(0, width), 0),
            new CellLocation(UnityEngine.Random.Range(0, width), height - 1),
            new CellLocation(0, UnityEngine.Random.Range(0, height)),
            new CellLocation(width - 1, UnityEngine.Random.Range(0, height)),
        };

        // Randomly get a possible start cell after shufflering the list elements
        possibleStartCells.Shuffle();
        CellLocation location = possibleStartCells[0];
        Cell cell = PlaceCell(location);
        cell.Material = entryCellMaterial;
        entryCell = cell;
        return cell;      
    }

    public CellDirection GetDirectionThatLeadstoOutOfBound(Cell edgeCell)
    {
        if (!edgeCell)
        {
            throw new Exception("cell with location x:" + edgeCell.Location.x + ", z:" + edgeCell.Location.z + " is not a cell along the maze edges");
        }

        List<CellDirection> directions = CellDirections.GetCellDirections;
        for (int i = 0; i < CellDirections.count; i++)
        {
            CellLocation relativeLocation = edgeCell.Location + directions[i].ToRelativeCellLocation();
            if (relativeLocation.x < 0 || relativeLocation.x == width || relativeLocation.z < 0 || relativeLocation.z == height)
            {
                return directions[i];
            }
        }

        throw new Exception("cell with location x:" + edgeCell.Location.x + ", z:" + edgeCell.Location.z + " is not a cell along the maze edges");
    }

    public Cell MakeConnection(Cell currentCell)
    {
        List<int> randomizedCellDirections = CellDirections.GetRandomizedCellDirections; // e.g [2, 1, 0, 3]

        for (int i = 0; i < randomizedCellDirections.Count; i++)
        {
            // The random direction from the current cell
            CellDirection direction = (CellDirection)randomizedCellDirections[i];
            //Debug.Log("MAIN: " + direction);
            //Debug.Log("Check: " + checkRow);
            CellLocation nextLocation = currentCell.Location;
            //Debug.Log("loc = "+nextLocation.x + " " +nextLocation.z);
            // The neighbor cell location from the direction
            if (!checkRow)
            {
                nextLocation = currentCell.Location + direction.ToRelativeCellLocation();
            }
            else if (checkRow)
            {
                nextLocation = currentCell.Location + direction.GetOpposite().ToRelativeCellLocation();
            }
            //Debug.Log(AtEnd(nextLocation, currentCell.Location));    
            //Debug.Log("loc = " + nextLocation.x + " " + nextLocation.z);
            if (AtEnd(nextLocation, currentCell.Location))
            {
                checkRow = !checkRow;
            }

            //Debug.Log("Check2: "+i+" "+ checkCorner);
            //Debug.Log(CanPlaceCell(nextLocation));
            if (CanPlaceCell(nextLocation) && !checkRow && !checkCorner )
            {
                //Debug.Log("Check3: " + checkRow);
                CellDirection fromDirection = direction.GetOpposite();
                Cell nextCell = PlaceCell(nextLocation, fromDirection);
                //Debug.Log("it1");
                //Debug.Log(direction);
                currentCell.AddConnection(direction); // Direction that connects it to the newly generated cell
                currentCell.name += "_" + direction;
                if (i == 1)
                {
                    checkCorner = true;
                }
                return nextCell;
            }

            else if (CanPlaceCell(nextLocation) && checkRow && !checkCorner)
            {

                CellDirection fromDirection = direction.GetOpposite();
                Cell nextCell = PlaceCell(nextLocation, fromDirection);
                //Debug.Log("it2");
                //Debug.Log("O: "+direction);
                currentCell.AddConnection(direction); // Direction that connects it to the newly generated cell
                currentCell.name += "_" + direction;
                if (i == 1)
                {
                    checkCorner = true;
                }
                return nextCell;
            }
            else if(CanPlaceCell(nextLocation) && checkCorner && i==0)
            {
                CellDirection fromDirection = direction.GetWest();
                Cell nextCell = PlaceCell(nextLocation, fromDirection);
                //Debug.Log("it3");
                //Debug.Log("O: " + direction.GetOpposite());
                currentCell.AddConnection(direction.GetOpposite()); // Direction that connects it to the newly generated cell
                currentCell.name += "_" + direction;
                checkCorner = false;
                return nextCell;
            }

        }

        return null;
    }

    private bool IsEdgeCell(Cell cell)
    {
        return (cell.Location.x == 0 || cell.Location.x == width - 1 || cell.Location.z == 0 || cell.Location.z == height - 1); 
    }

    public bool AtEnd(CellLocation previousLocation, CellLocation currentLocation)
    {
        //Debug.Log(previousLocation.z + " " + currentLocation.z);
        return
            previousLocation.z > currentLocation.z ||
            previousLocation.z < currentLocation.z;
    }

    public bool CanPlaceCell(CellLocation location)
    {
        //Debug.Log("Can:" + location.x + " " + location.z);
        return 
            location.x >= 0 && 
            location.x < width &&
            location.z >= 0 && 
            location.z < height &&
            cells[location.x, location.z] == null;
    }

    public Cell PlaceCell(CellLocation location)
    {
        Cell cell = Instantiate(CellPrefab);
        cell.transform.parent = transform;
        cell.Location = location;
        cells[location.x, location.z] = cell;
        
        cell.pathVisible = pathVisible;
        return cell;
    }

    public Cell PlaceCell(CellLocation location, CellDirection fromDirection)
    {
        Cell cell = PlaceCell(location);
        //Debug.Log(location.x + "-" + location.z);
        //Debug.Log("FROM: "+fromDirection);
        cell.AddConnection(fromDirection);
        return cell;
    }

    public Cell PlaceCellOpposite(CellLocation location, CellDirection fromDirection)
    {
        Cell cell = PlaceCell(location);
        cell.AddConnection(fromDirection);
        return cell;
    }

    private void SetExitCellOppositeOfEntryCell()
    {
        int x, z;

        //entry cell located along south or north edges
        if (entryCell.Location.x == 0 || entryCell.Location.x == width - 1)
        {
            x = width - 1 - entryCell.Location.x; // returns 0  or width - 1
            z = UnityEngine.Random.Range(0, height);
        }
        // entry cell located along east or west edges
        else
        {
            z = height - 1 - entryCell.Location.z; // returns 0  or width - 1
            x = UnityEngine.Random.Range(0, width);
        }

        SetExitCell(cells[x, z]);
    }

    private void SetExitCell(Cell cell)
    {
        cell.Material = exitCellMaterial;
        CellDirection exitCellDirection = GetDirectionThatLeadstoOutOfBound(cell);
        cell.AddConnection(exitCellDirection);
        exitCell = cell;
    }
}
