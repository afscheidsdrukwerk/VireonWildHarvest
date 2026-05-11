package nl.artsystudios.vireon.wildharvest.forge;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import nl.artsystudios.vireon.wildharvest.VireonWildHarvest;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.file.Files;
import java.util.concurrent.Executors;

/**
 * Tiny HTTP server that serves the generated resource pack zip.
 *
 * <p>Endpoint: {@code GET /vireon-pack.zip} returns the latest built pack.
 * No auth, intentionally simple — pack URLs are short-lived and the body
 * is the same zip every client downloads.</p>
 */
public final class PackHostServer {

    private final VireonWildHarvest plugin;
    private HttpServer server;
    private volatile File packFile;

    public PackHostServer(VireonWildHarvest plugin) {
        this.plugin = plugin;
    }

    public synchronized void start(String host, int port, File packFile) throws IOException {
        if (server != null) stop();
        this.packFile = packFile;
        this.server = HttpServer.create(new InetSocketAddress(host, port), 0);
        server.createContext("/vireon-pack.zip", this::handle);
        server.setExecutor(Executors.newFixedThreadPool(2, r -> {
            Thread t = new Thread(r, "Vireon-PackHost");
            t.setDaemon(true);
            return t;
        }));
        server.start();
        plugin.getLogger().info("Vireon Forge: pack server listening on " + host + ":" + port);
    }

    /** Swap the served file without restarting the HTTP listener. */
    public synchronized void updatePack(File packFile) {
        this.packFile = packFile;
    }

    public synchronized void stop() {
        if (server != null) {
            server.stop(0);
            server = null;
            plugin.getLogger().info("Vireon Forge: pack server stopped.");
        }
    }

    public boolean isRunning() {
        return server != null;
    }

    // ─── Request handler ──────────────────────────────────────

    private void handle(HttpExchange exchange) throws IOException {
        try {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(405, -1);
                return;
            }
            File f = this.packFile;
            if (f == null || !f.exists()) {
                exchange.sendResponseHeaders(404, -1);
                return;
            }
            byte[] data = Files.readAllBytes(f.toPath());
            exchange.getResponseHeaders().add("Content-Type", "application/zip");
            exchange.getResponseHeaders().add("Cache-Control", "no-store");
            exchange.sendResponseHeaders(200, data.length);
            try (OutputStream out = exchange.getResponseBody()) {
                out.write(data);
            }
        } finally {
            exchange.close();
        }
    }
}
