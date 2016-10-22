package at.favre.tools.apksigner;

public class Main {
    public static void main(String[] args) {
        int[][] maze = new int[][]{
                {0, 0, 0, 1, 0, 1, 0},
                {0, 1, 0, 0, 0, 1, 0},
                {0, 1, 0, 1, 0, 1, 0},
                {0, 0, 0, 1, 0, 1, 0},
                {0, 0, 0, 1, 0, 1, 0},
                {0, 0, 0, 1, 0, 0, 0},
                {0, 0, 0, 1, 0, 1, 0}
        };
        int shortestPath = findShortestPath(maze, 7, 7, 6, 6);

        System.out.println("shortes path "+shortestPath);
    }

    private static int findShortestPath(int[][] maze, int rows, int columns, int endRow, int endColumn) {

        Node[][] nodes = new Node[rows][columns];

        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                int distance = 0;
                if (i == 0 && j == 0) {

                }
                nodes[i][j] = new Node(i, j);
            }
        }

        visit(nodes[0][0], 0, nodes, maze, rows, columns);

        printNodes(nodes, rows, columns);

        return nodes[endRow][endColumn].distance;
    }

    private static void printNodes(Node[][] nodes, int rows, int columns) {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < columns; j++) {
                if (!nodes[i][j].visited) {
                    System.out.print("[xx]");
                } else {
                    System.out.print("[" + String.format("%02d", nodes[i][j].distance) + "]");
                }
            }
            System.out.println();
        }
    }

    private static void visit(Node node, int distanceSoFar, Node[][] nodes, int[][] maze, int rows, int columns) {
        if (node.distance < distanceSoFar || maze[node.row][node.column] == 1) {
            return;
        }
        node.distance = distanceSoFar;
        node.visited = true;

        int nextDistance = distanceSoFar + 1;
        if (node.row + 1 < rows) {
            visit(nodes[node.row + 1][node.column], nextDistance, nodes, maze, rows, columns);
        }
        if (node.row - 1 >= 0) {
            visit(nodes[node.row - 1][node.column], nextDistance, nodes, maze, rows, columns);
        }
        if (node.column + 1 < columns) {
            visit(nodes[node.row][node.column + 1], nextDistance, nodes, maze, rows, columns);
        }
        if (node.column - 1 >= 0) {
            visit(nodes[node.row][node.column + 1], nextDistance, nodes, maze, rows, columns);
        }
    }


    public static class Node {
        public int row;
        public int column;
        public int distance;
        public boolean visited;

        public Node(int row, int column) {
            this.row = row;
            this.column = column;
        }
    }
}
