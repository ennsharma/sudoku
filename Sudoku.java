/* This file contains a Sudoku solving algorithm. Simply compile and run the program, and instructions
 * to enter input and formatting instructions will be provided.
 * Author: Nikhil Sharma
 */

/* Imported classes for file reading/writing */
import java.io.BufferedReader;
import java.io.InputStreamReader;

/* Imported data structures */
import java.util.HashMap;
import java.util.HashSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.PriorityQueue;
import java.util.Comparator;
 
/* Imported exceptions for error handling */
import java.io.IOException;
import java.lang.ArrayIndexOutOfBoundsException;

/* Other imports */
import java.lang.Math;
import edu.princeton.cs.introcs.StdDraw;
import java.awt.event.KeyEvent;

public class Sudoku {
    /* Class to compare two Sudoku objects by checking which has more remaining
     * empty boxes.
     */
    public class SudokuComparator implements Comparator {
        /* Compare method that compares two Sudoku objects */
        public int compare(Object h1, Object h2) {
            int s1 = ((Sudoku) h1).possibilities.size();
            int s2 = ((Sudoku) h2).possibilities.size();
            return (s1 < s2 ? -1 : (s1 == s2 ? 0 : 1));
        }
    }

    int D;
    int N;
    int[][] grid;
    HashMap<int[], HashSet<Integer>> possibilities = new HashMap<int[], HashSet<Integer>>();

    /* Sudoku object constructor.
     * grid is a 2D array containing elements of the Sudoku puzzle.
     */
    public Sudoku(int[][] grid) {
        this.grid = grid;
        this.N = grid.length;
        D = (int) Math.sqrt(N);
        setPossibilities();   
    }
    
    /* Initializes HashMap of possible values for each unknown square */
    public void setPossibilities() {
        possibilities = new HashMap<int[], HashSet<Integer>>();

        // initialize rows, columns, and boxes
        HashMap<Integer, ArrayList<Integer>> columns = new HashMap<Integer, ArrayList<Integer>>();
        HashMap<Integer, ArrayList<Integer>> rows = new HashMap<Integer, ArrayList<Integer>>();
        HashMap<Integer, ArrayList<Integer>> boxes = new HashMap<Integer, ArrayList<Integer>>();
        for (int i = 0; i < N; i++) boxes.put(i, new ArrayList<Integer>());
        for (int i = 0; i < N; i++) {
            ArrayList<Integer> column = new ArrayList<Integer>();
            ArrayList<Integer> row = new ArrayList<Integer>();
            for (int j = 0; j < N; j++) {
                row.add(grid[i][j]);
                column.add(grid[j][i]);
                boxes.get(D*(i/D) + j/D).add(grid[i][j]);
            }
            rows.put(i, row);
            columns.put(i, column);
        }

        // set possible values for each unfilled box
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                if (grid[i][j] == 0) {
                    HashSet<Integer> possible_values = new HashSet<Integer>();
                    for (int p = 1; p <= N; p++) possible_values.add(p);
                    for (int x : rows.get(i)) possible_values.remove(x);
                    for (int x : columns.get(j)) possible_values.remove(x);
                    for (int x : boxes.get(D*(i/D) + j/D)) possible_values.remove(x);
                    possibilities.put(new int[]{i, j}, possible_values);
                }
            }
        }
    }

    /* Updates the map of possibilities for all squares reflecting a change at square (i, j) */
    public void updatePossibilities(int i, int j) {
        for (int[] k : possibilities.keySet()) {
            if (k[0] == i) possibilities.get(k).remove(grid[i][j]);
            if (k[1] == j) possibilities.get(k).remove(grid[i][j]);
            if ((D*(i/D) + j/D) == (D*(k[0]/D) + k[1]/D)) possibilities.get(k).remove(grid[i][j]);
        }
    }

    /* Determines if a possible value for a square with coordinates i, j is not a possible value
     * for other squares in the same row, column, or box */
    public boolean checkPossibility(int i, int j, int possibility) {
        boolean[] isPossible = new boolean[]{true, true, true};
        for (int[] k : possibilities.keySet()) {
            if ((k[0] == i && k[1] != j) && (possibilities.get(k).contains(possibility))) isPossible[0] = false;
            if ((k[0] != i && k[1] == j) && (possibilities.get(k).contains(possibility))) isPossible[1] = false;
            if (((D*(k[0]/D) + k[1]/D) == (D*(i/D) + j/D)) && (possibilities.get(k).contains(possibility))) isPossible[2] = false;
        }

        return !Arrays.equals(isPossible, new boolean[]{false, false, false});
    }

    /* Fills in all purely deterministic squares with the appropriate numbers. */
    public void basicSolve() {
        boolean changeThisIteration = true;
        while (changeThisIteration) {
            changeThisIteration = false;
            ArrayList<int[]> toBeRemoved = new ArrayList<int[]>();

            // one-possibility method
            for (int[] k : possibilities.keySet()) {
                if (possibilities.get(k).size() == 1) {
                    changeThisIteration = true;
                    grid[k[0]][k[1]] = possibilities.get(k).iterator().next();
                    toBeRemoved.add(k);
                    updatePossibilities(k[0], k[1]);
                }
            }

            // remove keys corresponding to squares solved in this iteration
            for (int[] k : toBeRemoved) possibilities.remove(k);
            toBeRemoved = new ArrayList<int[]>();

            // cross-elimination method
            for (int[] k : possibilities.keySet()) {
                for (int possibility : possibilities.get(k)) {
                    if (checkPossibility(k[0], k[1], possibility)) {
                        grid[k[0]][k[1]] = possibility;
                        toBeRemoved.add(k);
                        changeThisIteration = true;
                    }
                }
            }

            // remove keys corresponding to squares solved in this iteration
            for (int[] key : toBeRemoved) {
                updatePossibilities(key[0], key[1]);
                possibilities.remove(key);
            }
        }
    }

    /* Attempts to solve using basic solve, but if unfilled squares still exist, uses guesswork to arrive
     * at a correct solution */
    public void solve() {
        PriorityQueue<Sudoku> pq = new PriorityQueue<Sudoku>(11, new SudokuComparator());
        pq.add(new Sudoku(this.grid));

        while (pq.size() > 0) {
            Sudoku curr = pq.poll();
            curr.basicSolve();

            if (curr.possibilities.size() == 0) {
                this.possibilities = curr.possibilities;
                this.grid = curr.grid;
                break;
            }

            int[] k = curr.possibilities.keySet().iterator().next();
            for (int x : curr.possibilities.get(k)) {
                curr.grid[k[0]][k[1]] = x;
                pq.add(new Sudoku(copyGrid(curr.grid)));
            }
        }
    }

    /* Makes and returns a copy of the input grid */
    public int[][] copyGrid(int[][] grid) {
        int[][] copy = new int[N][N];
        for (int i = 0; i < N; i++) {
            for (int j = 0; j < N; j++) {
                copy[i][j] = grid[i][j];
            }
        }

        return copy;
    }

    /* Uses values in grid to generate a Sudoku grid visual representation */
    public void display(boolean showNumbers) {
        for (int i = 0; i <= N; i++) {
            if (i%D == 0) {
                StdDraw.filledRectangle(i, -N/2.0, 0.05, N/2.0);
                StdDraw.filledRectangle(N/2.0, -i, N/2.0, 0.05);
            } else {
                StdDraw.line(i, 0, i, -N);
                StdDraw.line(0, -i, N, -i);
            }
        }

        if (showNumbers) {
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < N; j++) {
                    if (!(grid[i][j] == 0)) {
                        StdDraw.text(j + 0.5, i + 0.5 - N, Integer.toString(grid[i][j]));
                    }
                }
            }
        }
    }

    public static void main(String[] args) {
        try {
            StdDraw.setXscale(0, 10);
            StdDraw.setYscale(0, 1);
            StdDraw.textLeft(0.5, 0.5, "Enter the box size, N: _____");
            StdDraw.textLeft(0.5, 0.42, "(e.g. a standard Sudoku has N = 3)");
            int N = 0;
            boolean dimensionEntered = false;
            while (!StdDraw.isKeyPressed(KeyEvent.VK_ENTER)) {
                if (StdDraw.hasNextKeyTyped()) {
                    N = Character.getNumericValue(StdDraw.nextKeyTyped());
                    StdDraw.text(4.33, 0.5, Integer.toString(N));
                    N = (int) Math.pow(N, 2);
                    dimensionEntered = true;
                }
            }

            StdDraw.show(200);
            
            Sudoku sudoku = new Sudoku(new int[N][N]);
            int i = -1; 
            int j = -1;
            StdDraw.setXscale(-0.05, N + 0.05);
            StdDraw.setYscale(1.25, -N - 0.05);
            StdDraw.setPenColor(StdDraw.WHITE);
            StdDraw.filledSquare(N/2.0, -N/2.0, N/2.0);
            StdDraw.setPenColor();
            StdDraw.text(N/2.0, 0.5, "Enter any known numbers in the boxes above.");
            StdDraw.text(N/2.0, 1.0, "Then, hit enter to display the puzzle's solution.");
            sudoku.display(false);
            StdDraw.show();
            while (!StdDraw.isKeyPressed(KeyEvent.VK_ENTER)) {
                if (StdDraw.mousePressed()) {
                    i = ((int) StdDraw.mouseX());
                    j = ((int) StdDraw.mouseY());
                }

                if (i >= 0 && i < N && j <=0 && j > -N && StdDraw.hasNextKeyTyped()) {
                    int value = Character.getNumericValue(StdDraw.nextKeyTyped());
                    value = (value > 0 && value <= N ? value : 0);
                    if (sudoku.grid[N + j - 1][i] != 0) {
                        StdDraw.setPenColor(StdDraw.WHITE);
                        StdDraw.filledSquare(i + 0.5, j - 0.5, 0.5);
                        StdDraw.setPenColor();
                    }
                    sudoku.grid[N + j - 1][i] = value;
                    sudoku.display(true);
                    i = -1;
                    j = -1;
                    StdDraw.show();
                }
            }

            sudoku.setPossibilities();
            sudoku.solve();
            StdDraw.clear();
            sudoku.display(true);
            StdDraw.text(N/2.0, 0.75, "SOLUTION");
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            System.out.println("Invalid number of arguments.");
        } catch (Exception e) {
            e.printStackTrace();
            System.out.println("Incorrect argument types.");
        }
    }
}
