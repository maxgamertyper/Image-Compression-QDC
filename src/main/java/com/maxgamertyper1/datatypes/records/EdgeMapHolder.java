package com.maxgamertyper1.datatypes.records;

import java.util.Arrays;

public record EdgeMapHolder(int[][] columnBottomEdges, int[][] rowRightEdges) {

    private static int nearestEdge(int[] edges, int position, boolean forward) {
        int found = Arrays.binarySearch(edges, position);
        // binarySearch returns -(insertion point) - 1 when the key is absent
        int lowerBound = found >= 0 ? found : -(found + 1);
        return forward
                ? edges[Math.min(lowerBound, edges.length - 1)]
                : edges[Math.max(lowerBound - 1, 0)];
    }

    public int nextBottomEdge(int column, int y) {
        return nearestEdge(columnBottomEdges[column], y, true);
    }

    public int prevBottomEdge(int column, int y) {
        return nearestEdge(columnBottomEdges[column], y, false);
    }

    public int nextRightEdge(int row, int x) {
        return nearestEdge(rowRightEdges[row], x, true);
    }

    public int prevRightEdge(int row, int x) {
        return nearestEdge(rowRightEdges[row], x, false);
    }
}
