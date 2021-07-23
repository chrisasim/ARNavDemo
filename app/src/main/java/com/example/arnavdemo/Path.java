package com.example.arnavdemo;

import java.util.ArrayList;
import java.util.LinkedList;

public class Path {

    private int src;
    private int dest;

    public Path(int src, int dest) {
        this.src = src;
        this.dest = dest;
    }

    public ArrayList<Integer> createPath() {
        // No of vertices
        int v = 7;
        ArrayList<Integer> path;

        // Adjacency list for storing which vertices are connected
        ArrayList<ArrayList<Integer>> adj = new ArrayList<>(v);
        for (int i = 0; i < v; i++) {
            adj.add(new ArrayList<Integer>());
        }

        addEdge(adj, 1, 2);
        addEdge(adj, 1, 3);
        addEdge(adj, 1, 4);
        addEdge(adj, 1, 5);
        addEdge(adj, 1, 6);
        addEdge(adj, 2, 3);
        addEdge(adj, 2, 4);
        addEdge(adj, 2, 5);
        addEdge(adj, 2, 6);
        addEdge(adj, 3, 4);
        addEdge(adj, 3, 5);
        addEdge(adj, 3, 6);
        addEdge(adj, 4, 5);
        addEdge(adj, 4, 6);
        addEdge(adj, 5, 6);

        path = findShortestPath(adj, src, dest, v);
        return path;
    }

    // function to form edge between two vertices
    // source and dest
    private static void addEdge(ArrayList<ArrayList<Integer>> adj, int i, int j) {
        adj.get(i).add(j);
        adj.get(j).add(i);
    }

    // function to find the shortest path
    // between source vertex and destination vertex
    private static ArrayList<Integer> findShortestPath(ArrayList<ArrayList<Integer>> adj, int s, int dest, int v) {

        // predecessor[i] array stores predecessor of i
        int pred[] = new int[v];

        ArrayList<Integer> shortestPath = new ArrayList<>();

        if (BFS(adj, s, dest, v, pred) == false) {
            return null;
        }

        // LinkedList to store path
        LinkedList<Integer> path = new LinkedList<Integer>();
        int crawl = dest;
        path.add(crawl);
        while (pred[crawl] != -1) {
            path.add(pred[crawl]);
            crawl = pred[crawl];
        }

        // Save path
        System.out.println("Path is ::");
        for (int i = path.size() - 1; i >= 0; i--) {
            shortestPath.add(path.get(i));
        }

        return shortestPath;
    }

    // a modified version of BFS that stores predecessor
    // of each vertex in array pred
    private static boolean BFS(ArrayList<ArrayList<Integer>> adj, int src, int dest, int v, int pred[]) {
        // a queue to maintain queue of vertices whose
        // adjacency list is to be scanned as per normal
        // BFS algorithm using LinkedList of Integer type
        LinkedList<Integer> queue = new LinkedList<Integer>();

        // boolean array visited[] which stores the
        // information whether ith vertex is reached
        // at least once in the Breadth first search
        boolean visited[] = new boolean[v];

        // initially all vertices are unvisited
        // so v[i] for all i is false
        for (int i = 0; i < v; i++) {
            visited[i] = false;
            pred[i] = -1;
        }

        // now source is first to be visited
        visited[src] = true;
        queue.add(src);

        // bfs Algorithm
        while (!queue.isEmpty()) {
            int u = queue.remove();
            for (int i = 0; i < adj.get(u).size(); i++) {
                if (visited[adj.get(u).get(i)] == false) {
                    visited[adj.get(u).get(i)] = true;
                    pred[adj.get(u).get(i)] = u;
                    queue.add(adj.get(u).get(i));

                    // stopping condition (when we find
                    // our destination)
                    if (adj.get(u).get(i) == dest)
                        return true;
                }
            }
        }
        return false;
    }
}
