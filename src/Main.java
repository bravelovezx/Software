import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class Main {
    // Directed graph storing word adjacency and weights
    public static class DirectedGraph {
        private final Map<String, Map<String, Integer>> adj = new HashMap<>();

        // Add a directed edge from "from" to "to" with weight increment
        public void addEdge(String from, String to) {
            adj.computeIfAbsent(from, k -> new HashMap<>());
            adj.computeIfAbsent(to, k -> new HashMap<>());
            Map<String, Integer> targets = adj.get(from);
            targets.put(to, targets.getOrDefault(to, 0) + 1);
        }

        public Set<String> getNodes() {
            return adj.keySet();
        }

        public Map<String, Integer> getTargets(String node) {
            return adj.getOrDefault(node, Collections.emptyMap());
        }

        public List<String> getPredecessors(String node) {
            List<String> preds = new ArrayList<>();
            for (Map.Entry<String, Map<String, Integer>> e : adj.entrySet()) {
                if (e.getValue().containsKey(node)) preds.add(e.getKey());
            }
            return preds;
        }

        public int outDegree(String node) {
            Map<String, Integer> tg = adj.get(node);
            return tg == null ? 0 : tg.size();
        }
    }

    private static DirectedGraph graph;
    private static final Random rand = new Random();

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        String filePath;
        if (args.length > 0) {
            filePath = args[0];
        } else {
            System.out.print("Enter input text file path: ");
            filePath = sc.nextLine().trim();
        }
        graph = buildGraph(filePath);
        if (graph == null) {
            System.err.println("Failed to build graph. Exiting.");
            return;
        }
        while (true) {
            System.out.println("\nSelect function:");
            System.out.println("1. Show directed graph");
            System.out.println("2. Query bridge words");
            System.out.println("3. Generate new text based on bridge words");
            System.out.println("4. Calculate shortest path");
            System.out.println("5. Calculate PageRank");
            System.out.println("6. Random walk");
            System.out.println("0. Exit");
            System.out.print("Your choice: ");
            String choice = sc.nextLine().trim();
            switch (choice) {
                case "1" -> showDirectedGraph(graph);
                case "2" -> {
                    System.out.print("Enter word1: ");
                    String w1 = sc.nextLine().trim();
                    System.out.print("Enter word2: ");
                    String w2 = sc.nextLine().trim();
                    System.out.println(queryBridgeWords(w1, w2));
                }
                case "3" -> {
                    System.out.print("Enter a line of text: ");
                    String line = sc.nextLine().trim();
                    System.out.println(generateNewText(line));
                }
                case "4" -> {
                    System.out.print("Enter start word: ");
                    String w1 = sc.nextLine().trim();
                    System.out.print("Enter end word: ");
                    String w2 = sc.nextLine().trim();
                    System.out.println(calcShortestPath(w1, w2));
                }
                case "5" -> {
                    System.out.print("Enter word: ");
                    String w = sc.nextLine().trim();
                    Double pr = calPageRank(w);
                    System.out.printf("PageRank(%s) = %.6f%n", w, pr);
                }
                case "6" -> {
                    String path = randomWalk();
                    System.out.println("Random walk path: " + path);
                    System.out.println("Path also saved to 'randomWalk.txt'.");
                }
                case "0" -> {
                    System.out.println("Exiting.");
                    sc.close();
                    return;
                }
                default -> System.out.println("Invalid choice.");
            }
        }
    }

    // Build the directed graph from input file
    private static DirectedGraph buildGraph(String filePath) {
        DirectedGraph G = new DirectedGraph();
        List<String> words;
        try {
            String content = Files.readString(Path.of(filePath));
            // split by non-letter characters, ignore empty
            words = new ArrayList<>();
            for (String w : content.split("[^A-Za-z]+")) {
                if (!w.isBlank()) words.add(w.toLowerCase());
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        for (int i = 0; i < words.size() - 1; i++) {
            G.addEdge(words.get(i), words.get(i + 1));
        }
        return G;
    }

    // Display the graph in CLI
    public static void showDirectedGraph(DirectedGraph G) {
        System.out.println("Directed graph adjacency list (node -> [target(weight), ...]):");
        for (String u : G.getNodes()) {
            Map<String, Integer> tg = G.getTargets(u);
            if (tg.isEmpty()) continue;
            System.out.print(u + " -> ");
            List<String> parts = new ArrayList<>();
            for (Map.Entry<String, Integer> e : tg.entrySet()) {
                parts.add(e.getKey() + "(" + e.getValue() + ")");
            }
            System.out.println(String.join(", ", parts));
        }
    }

    // Query bridge words
    public static String queryBridgeWords(String w1, String w2) {
        w1 = w1.toLowerCase();
        w2 = w2.toLowerCase();
        if (!graph.getNodes().contains(w1) || !graph.getNodes().contains(w2)) {
            return String.format("No \"%s\" or \"%s\" in the graph!", w1, w2);
        }
        Set<String> bridges = new HashSet<>();
        for (String mid : graph.getTargets(w1).keySet()) {
            if (graph.getTargets(mid).containsKey(w2)) bridges.add(mid);
        }
        if (bridges.isEmpty()) {
            return String.format("No bridge words from \"%s\" to \"%s\"!", w1, w2);
        }
        List<String> list = new ArrayList<>(bridges);
        Collections.sort(list);
        StringJoiner sj = new StringJoiner(", ");
        for (String b : list) sj.add(b);
        return String.format("The bridge words from \"%s\" to \"%s\" are: %s.", w1, w2, sj);
    }

    // Generate new text by inserting bridge words
    public static String generateNewText(String inputText) {
        String[] tokens = inputText.split("\\s+");
        List<String> out = new ArrayList<>();
        for (int i = 0; i < tokens.length; i++) {
            out.add(tokens[i]);
            if (i < tokens.length - 1) {
                String a = tokens[i].toLowerCase();
                String b = tokens[i + 1].toLowerCase();
                List<String> bridges = new ArrayList<>();
                for (String mid : graph.getTargets(a).keySet()) {
                    if (graph.getTargets(mid).containsKey(b)) bridges.add(mid);
                }
                if (!bridges.isEmpty()) {
                    String pick = bridges.get(rand.nextInt(bridges.size()));
                    out.add(pick);
                }
            }
        }
        return String.join(" ", out);
    }

    // Calculate shortest path using Dijkstra
    public static String calcShortestPath(String w1, String w2) {
        w1 = w1.toLowerCase();
        w2 = w2.toLowerCase();
        if (!graph.getNodes().contains(w1) || !graph.getNodes().contains(w2)) {
            return String.format("No \"%s\" or \"%s\" in the graph!", w1, w2);
        }
        // Dijkstra
        Map<String, Integer> dist = new HashMap<>();
        Map<String, String> prev = new HashMap<>();
        for (String u : graph.getNodes()) dist.put(u, Integer.MAX_VALUE);
        dist.put(w1, 0);
        PriorityQueue<String> pq = new PriorityQueue<>(Comparator.comparingInt(dist::get));
        pq.addAll(graph.getNodes());
        while (!pq.isEmpty()) {
            String u = pq.poll();
            if (u.equals(w2)) break;
            int d = dist.get(u);
            if (d == Integer.MAX_VALUE) break;
            for (var e : graph.getTargets(u).entrySet()) {
                String v = e.getKey();
                int w = e.getValue();
                int nd = d + w;
                if (nd < dist.get(v)) {
                    dist.put(v, nd);
                    prev.put(v, u);
                    // refresh queue
                    pq.remove(v);
                    pq.add(v);
                }
            }
        }
        if (dist.get(w2) == Integer.MAX_VALUE) {
            return String.format("No path from \"%s\" to \"%s\"!", w1, w2);
        }
        List<String> path = new ArrayList<>();
        for (String at = w2; at != null; at = prev.get(at)) path.add(at);
        Collections.reverse(path);
        return String.format("The shortest path from \"%s\" to \"%s\" is: %s (length %d).", w1, w2,
                String.join(" -> ", path), dist.get(w2));
    }

    // Calculate PageRank
    public static Double calPageRank(String word) {
        word = word.toLowerCase();
        Set<String> nodes = graph.getNodes();
        int N = nodes.size();
        double d = 0.85;
        Map<String, Double> pr = new HashMap<>();
        Map<String, Double> newPr = new HashMap<>();
        // init
        for (String u : nodes) pr.put(u, 1.0 / N);
        // precompute dangling nodes
        Set<String> dangling = new HashSet<>();
        for (String u : nodes) if (graph.outDegree(u) == 0) dangling.add(u);
        int iters = 100;
        for (int it = 0; it < iters; it++) {
            double danglingSum = 0;
            for (String u : dangling) danglingSum += pr.get(u);
            for (String u : nodes) {
                double rank = (1 - d) / N;
                rank += d * danglingSum / N;
                for (String v : graph.getPredecessors(u)) {
                    rank += d * pr.get(v) / graph.outDegree(v);
                }
                newPr.put(u, rank);
            }
            // swap
            pr.putAll(newPr);
        }
        return pr.getOrDefault(word, 0.0);
    }

    // Random walk and write to file
    public static String randomWalk() {
        List<String> path = new ArrayList<>();
        List<String> nodes = new ArrayList<>(graph.getNodes());
        if (nodes.isEmpty()) return "";
        String curr = nodes.get(rand.nextInt(nodes.size()));
        path.add(curr);
        Set<String> visitedEdges = new HashSet<>();
        while (true) {
            Map<String, Integer> tg = graph.getTargets(curr);
            if (tg.isEmpty()) break;
            List<String> outs = new ArrayList<>(tg.keySet());
            String next = outs.get(rand.nextInt(outs.size()));
            String edge = curr + "->" + next;
            if (visitedEdges.contains(edge)) break;
            visitedEdges.add(edge);
            path.add(next);
            curr = next;
        }
        String result = String.join(" -> ", path);
        // write to file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("randomWalk.txt"))) {
            bw.write(result);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return result;
    }
}


