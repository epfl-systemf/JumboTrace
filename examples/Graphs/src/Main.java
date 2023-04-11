
public class Main {

    public static void main(String[] args) {

        /*

        1 -> 2 -> 5
        '\   \,
         3 <- 4

         */

        var graph = new DirectedGraph();
        graph.addVertex(1);
        graph.addVertex(2);
        graph.addVertex(3);
        graph.addVertex(4);
        graph.addVertex(5);
        graph.addEdge(1, 2);
        graph.addEdge(2, 5);
        graph.addEdge(2, 4);
        graph.addEdge(4, 3);
        graph.addEdge(3, 1);
        graph.preOrder(1);
    }

}

