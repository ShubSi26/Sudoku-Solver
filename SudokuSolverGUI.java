import javax.swing.*;
import javax.swing.border.Border;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class SudokuSolverGUI extends JFrame {

    private static final int SIZE = 9;
    private JTextField[][] cells = new JTextField[SIZE][SIZE];
    private int[][] board = new int[SIZE][SIZE];
    private HashSet<Integer>[] rows = new HashSet[SIZE];
    private HashSet<Integer>[] cols = new HashSet[SIZE];
    private HashSet<Integer>[] subgrids = new HashSet[SIZE];
    private final Color lightBlue = new Color(173, 216, 230); // Light blue color
    private AtomicBoolean solvingPaused = new AtomicBoolean(false);
    private AtomicBoolean solvingStopped = new AtomicBoolean(false);
    private Thread solvingThread;
    private JSlider speedSlider;
    private int delay;

    public SudokuSolverGUI() {
        setTitle("Sudoku Solver");
        setSize(600, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel gridPanel = new JPanel();
        gridPanel.setLayout(new GridLayout(SIZE, SIZE));
        for (int row = 0; row < SIZE; row++) {
            rows[row] = new HashSet<>();
            cols[row] = new HashSet<>();
            subgrids[row] = new HashSet<>();
            for (int col = 0; col < SIZE; col++) {
                cells[row][col] = new JTextField();
                cells[row][col].setHorizontalAlignment(JTextField.CENTER);
                cells[row][col].setFont(new Font("Arial", Font.BOLD, 20));
                cells[row][col].setBackground(lightBlue); // Set the background color to light blue

                Border border = BorderFactory.createMatteBorder(
                        row % 3 == 0 ? 2 : 1, 
                        col % 3 == 0 ? 2 : 1, 
                        row % 3 == 2 ? 2 : 1, 
                        col % 3 == 2 ? 2 : 1, 
                        Color.BLACK
                );
                cells[row][col].setBorder(border);

                gridPanel.add(cells[row][col]);
            }
        }
        add(gridPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel();
        buttonPanel.setLayout(new GridLayout(1, 5));

        JButton loadButton = new JButton("Load Puzzle");
        loadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                loadPuzzle();
            }
        });
        buttonPanel.add(loadButton);

        JButton solveButton = new JButton("Solve");
        solveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                startSolving();
            }
        });
        buttonPanel.add(solveButton);

        JButton stopButton = new JButton("Stop");
        stopButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                stopSolving();
            }
        });
        buttonPanel.add(stopButton);

        JButton resumeButton = new JButton("Resume");
        resumeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                resumeSolving();
            }
        });
        buttonPanel.add(resumeButton);

        JButton clearButton = new JButton("Clear");
        clearButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                clearBoard();
            }
        });
        buttonPanel.add(clearButton);

        add(buttonPanel, BorderLayout.SOUTH);

        JPanel controlPanel = new JPanel();
        controlPanel.setLayout(new BorderLayout());
        
        JLabel speedLabel = new JLabel("Speed:");
        controlPanel.add(speedLabel, BorderLayout.WEST);
        
        speedSlider = new JSlider(JSlider.HORIZONTAL, 0, 200, 50);
        speedSlider.setMajorTickSpacing(50);
        speedSlider.setMinorTickSpacing(10);
        speedSlider.setPaintTicks(true);
        speedSlider.setPaintLabels(true);
        controlPanel.add(speedSlider, BorderLayout.CENTER);

        add(controlPanel, BorderLayout.NORTH);
        
        delay = speedSlider.getValue();
        speedSlider.addChangeListener(e -> delay = speedSlider.getValue());
    }

    private void startSolving() {
        solvingPaused.set(false);
        solvingStopped.set(false);
        solvingThread = new Thread(new Runnable() {
            @Override
            public void run() {
                solvePuzzle();
            }
        });
        solvingThread.start();
    }

    private void loadPuzzle() {
        int[][] puzzle = {
            {5, 3, 0, 0, 7, 0, 0, 0, 0},
            {6, 0, 0, 1, 9, 5, 0, 0, 0},
            {0, 9, 8, 0, 0, 0, 0, 6, 0},
            {8, 0, 0, 0, 6, 0, 0, 0, 3},
            {4, 0, 0, 8, 0, 3, 0, 0, 1},
            {7, 0, 0, 0, 2, 0, 0, 0, 6},
            {0, 6, 0, 0, 0, 0, 2, 8, 0},
            {0, 0, 0, 4, 1, 9, 0, 0, 5},
            {0, 0, 0, 0, 8, 0, 0, 7, 9}
        };

        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                board[row][col] = puzzle[row][col];
                if (puzzle[row][col] != 0) {
                    cells[row][col].setText(String.valueOf(puzzle[row][col]));
                    cells[row][col].setEditable(false);
                    rows[row].add(puzzle[row][col]);
                    cols[col].add(puzzle[row][col]);
                    subgrids[(row / 3) * 3 + col / 3].add(puzzle[row][col]);
                } else {
                    cells[row][col].setText("");
                    cells[row][col].setEditable(true);
                }
            }
        }
    }

    private void solvePuzzle() {
        if (solve()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(SudokuSolverGUI.this, "Sudoku Solved!", "Success", JOptionPane.INFORMATION_MESSAGE);
                }
            });
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    JOptionPane.showMessageDialog(SudokuSolverGUI.this, "No solution exists for the given Sudoku board.", "Error", JOptionPane.ERROR_MESSAGE);
                }
            });
        }
    }

    private void stopSolving() {
        solvingPaused.set(true);
    }

    private void resumeSolving() {
        solvingPaused.set(false);
        synchronized (solvingThread) {
            solvingThread.notify();
        }
    }

    private void clearBoard() {
        solvingPaused.set(false);
        solvingStopped.set(true);
        for (int row = 0; row < SIZE; row++) {
            rows[row].clear();
            cols[row].clear();
            subgrids[row].clear();
            for (int col = 0; col < SIZE; col++) {
                cells[row][col].setText("");
                cells[row][col].setEditable(true);
                cells[row][col].setBackground(lightBlue); // Reset the background color to light blue
                board[row][col] = 0;
            }
        }
    }

    private boolean isValid(int row, int col, int num) {
        if (rows[row].contains(num) || cols[col].contains(num) || subgrids[(row / 3) * 3 + col / 3].contains(num)) {
            return false;
        }
        return true;
    }

    private boolean solve() {
        int[] empty = findEmptyCell();
        if (empty == null) {
            return true;
        }
        int row = empty[0];
        int col = empty[1];

        for (int num = 1; num <= SIZE; num++) {
            if (solvingStopped.get()) {
                return false; // Stop solving if stopped
            }
            if (solvingPaused.get()) {
                synchronized (solvingThread) {
                    try {
                        solvingThread.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return false;
                    }
                }
            }
            if (isValid(row, col, num)) {
                board[row][col] = num;
                rows[row].add(num);
                cols[col].add(num);
                subgrids[(row / 3) * 3 + col / 3].add(num);
                highlightCell(row, col, Color.GREEN); // Highlight the cell in green during solving
                updateGUI(row, col, num);
                delay(delay); // Delay to visualize steps
                if (solve()) {
                    return true;
                }
                board[row][col] = 0;
                rows[row].remove(num);
                cols[col].remove(num);
                subgrids[(row / 3) * 3 + col / 3].remove(num);
                highlightCell(row, col, Color.RED); // Highlight the cell in red during backtracking
                updateGUI(row, col, 0);
                delay(delay); // Delay to visualize steps
                highlightCell(row, col, lightBlue); // Reset the cell color to light blue after backtracking
            }
        }
        return false;
    }

    private int[] findEmptyCell() {
        for (int row = 0; row < SIZE; row++) {
            for (int col = 0; col < SIZE; col++) {
                if (board[row][col] == 0) {
                    return new int[]{row, col};
                }
            }
        }
        return null;
    }

    private void updateGUI(int row, int col, int num) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                cells[row][col].setText(num == 0 ? "" : String.valueOf(num));
            }
        });
    }

    private void highlightCell(int row, int col, Color color) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                cells[row][col].setBackground(color);
            }
        });
    }

    private void delay(int milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SudokuSolverGUI solver = new SudokuSolverGUI();
                solver.setVisible(true);
            }
        });
    }
}
