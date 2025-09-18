package com.payroc.interviews;

import java.io.IOException;
import java.util.Locale;

public class LoadBalancerApplication {
    private static final int DEFAULT_LB_PORT = 8080;

    private static void usage() {
        System.out.println("Usage: java -jar LoadBalancerApplication-1.0-SNAPSHOT.jar --config <path> [--port <port>] [--strategy <roundrobin|random|leastconn>]\n" +
            "Environment: LB_PORT may override default port if --port not supplied.\n" +
            "Example: ./gradlew run --args=\"--config backends.json --port 9000 --strategy leastconn\"\n" +
            "Config file: JSON array of {\"host\":\"..\", \"port\":<int>} entries.\n");
    }

    public static void main(String[] args) {
        Integer port = null;
        String configPath = null;
        String strategyName = "roundrobin"; // default

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "--port":
                    if (i + 1 >= args.length) {
                        usage();
                        return;
                    }
                    port = Integer.parseInt(args[++i]);
                    break;
                case "--config":
                    if (i + 1 >= args.length) {
                        usage();
                        return;
                    }
                    configPath = args[++i];
                    break;
                case "--strategy":
                    if (i + 1 >= args.length) {
                        usage();
                        return;
                    }
                    strategyName = args[++i];
                    break;
                case "--help":
                case "-h":
                    usage();
                    return;
                default:
                    System.out.println("Unknown argument: " + args[i]);
                    usage();
                    return;
            }
        }

        if (configPath == null) {
            System.err.println("Missing --config <path>");
            usage();
            return;
        }

        if (port == null) {
            String env = System.getenv("LB_PORT");
            if (env != null && !env.isBlank()) {
                try {
                    port = Integer.parseInt(env.trim());
                } catch (NumberFormatException ex) {
                    System.err.println("Invalid LB_PORT environment value '" + env + "' - using default/CLI value. Error: " + ex.getMessage());
                }
            }
        }
        if (port == null) port = DEFAULT_LB_PORT;

        ServerSelectionStrategy strategy;
        switch (strategyName.toLowerCase(Locale.ROOT)) {
            case "random":
                strategy = new RandomSelectionStrategy();
                break;
            case "leastconn":
                strategy = new LeastConnectionsSelectionStrategy();
                break;
            case "roundrobin":
            default:
                strategy = new RoundRobinSelectionStrategy();
        }

        System.out.printf("Starting Load Balancer on port %d using strategy '%s' with config '%s'%n", port, strategyName, configPath);

        try {
            LoadBalancer lb = new LoadBalancer(port, configPath, strategy);
            if (lb.getBackends().isEmpty()) {
                System.out.println("WARNING: No backend servers loaded (empty or invalid config). Incoming connections will be closed.");
            } else {
                System.out.println("Loaded backends: " + lb.getBackends());
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                System.out.println("Shutdown requested. Stopping load balancer...");
                lb.stop();
            }));
            lb.start();
        } catch (IOException e) {
            System.err.println("Failed to start load balancer: " + e.getMessage());
        }
    }
}