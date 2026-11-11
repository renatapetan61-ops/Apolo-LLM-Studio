import { createServer, type IncomingMessage } from "node:http";
import os from "node:os";
import { verifyToken } from "@clerk/backend";

const port = Number(process.env.PORT ?? 8080);
const secretKey = process.env.CLERK_SECRET_KEY;
const allowedOrigins = (process.env.CLERK_AUTHORIZED_PARTIES ?? "")
  .split(",").map((value) => value.trim()).filter(Boolean);

// The shared-host PTY is intentionally disabled. Do not expose shell execution
// until per-user isolated workers, quotas, and workspace boundaries exist.
const terminalEnabled = false;

function bearer(req: IncomingMessage): string | undefined {
  const value = req.headers.authorization;
  return value?.startsWith("Bearer ") ? value.slice(7) : undefined;
}

async function authenticate(req: IncomingMessage) {
  if (!secretKey) throw new Error("Authentication is not configured");
  const token = bearer(req);
  if (!token) throw new Error("Missing bearer token");
  const claims = await verifyToken(token, {
    secretKey,
    authorizedParties: allowedOrigins.length ? allowedOrigins : undefined
  });
  if (!claims.sub) throw new Error("Token has no subject");
  return claims.sub;
}

const server = createServer(async (req, res) => {
  if (req.url === "/health" && req.method === "GET") {
    res.writeHead(200, { "content-type": "application/json", "cache-control": "no-store" });
    res.end(JSON.stringify({
      status: "ok",
      service: "apolo-control-plane",
      arch: process.arch,
      platform: os.platform(),
      authenticationConfigured: Boolean(secretKey),
      terminalEnabled
    }));
    return;
  }
  if (req.url === "/v1/status" && req.method === "GET") {
    if (!secretKey) {
      res.writeHead(503, { "content-type": "application/json" });
      res.end(JSON.stringify({ error: "authentication_not_configured" }));
      return;
    }
    try {
      const userId = await authenticate(req);
      res.writeHead(200, { "content-type": "application/json", "cache-control": "no-store" });
      res.end(JSON.stringify({ authenticated: true, userId, executor: null, ready: false, terminalEnabled }));
    } catch {
      res.writeHead(401, { "content-type": "application/json" });
      res.end(JSON.stringify({ error: "unauthorized" }));
    }
    return;
  }
  res.writeHead(404, { "content-type": "application/json" });
  res.end(JSON.stringify({ error: "not_found" }));
});

server.on("upgrade", async (req, socket) => {
  const url = new URL(req.url ?? "/", "http://localhost");
  if (url.pathname !== "/v1/terminal") {
    socket.destroy();
    return;
  }
  // No shared-host shell sessions are allowed in this deployment.
  socket.write("HTTP/1.1 503 Service Unavailable\r\nConnection: close\r\nContent-Type: application/json\r\n\r\n{\"error\":\"terminal_disabled_until_isolated_workers_are_provisioned\"}");
  socket.destroy();
});

server.listen(port, "0.0.0.0", () => {
  console.log("Apolo control plane listening on :" + port + " (" + process.arch + "); terminal disabled");
});
