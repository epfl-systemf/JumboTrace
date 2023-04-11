import java.util.*;

public class DirectedGraph {
    private final Map<Integer, Set<Integer>> adjList = new HashMap<>();

    void addVertex(int i){
        adjList.put(i, new HashSet<>());
    }

    void addEdge(int from, int to){
        if (!(adjList.containsKey(from) && adjList.containsKey(to))){
            throw new IllegalArgumentException();
        }
        adjList.get(from).add(to);
    }

    void preOrder(int start){
        if (!adjList.containsKey(start)){
            throw new IllegalArgumentException();
        }
        var toExplore = new LinkedList<Integer>();
        var explored = new HashSet<Integer>();
        toExplore.addFirst(start);
        explored.add(start);
        while (!toExplore.isEmpty()){
            var curr = toExplore.removeLast();
            System.out.println(curr);
            for (int i : adjList.get(curr)) {
                if (!explored.contains(i)){
                    toExplore.addFirst(i);
                    explored.add(i);
                }
            }
        }
    }

}
