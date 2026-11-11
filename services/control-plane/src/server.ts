import { createServer, type IncomingMessage } from "node:http";
import { randomUUID } from "node:crypto";
import { mkdirSync } from "node:fs";
import path from "node:path";
import os from "node:os";
import pty from "node-pty";
import { WebSocket, WebSocketServer } from "ws";
import { verifyToken } from "@clerk/backend";

const port = Number(process.env.PORT ?? 8080);
const secretKey = process.env.CLERK_SECRET_KEY;
const workspaceRoot = process.env.WORKSPACE_ROOT ?? "/workspace";
const allowedOrigins = (process.env.CLERK_AUTHORIZED_PARTIES ?? "")
  .split(",").map((value) => value.trim()).filter(Boolean);

if (!secretKey) {
  console.error("CLERK_SECRET_KEY is required. Refusing to start without authentication.");
  process.exit(1);
}

function bearer(req: IncomingMessage): string | undefined {
  const value = req.headers.authorization;
  return value?.startsWith("Bearer ") ? value.slice(7) : undefined;
}

async function authenticate(req: IncomingMessage) {
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
    res.writeHead(200, { "content-type": "application/json" });
    res.end(JSON.stringify({ status: "ok", service: "apolo-control-plane", arch: process.arch, platform: os.platform() }));
    return;
  }
  if (req.url === "/v1/status" && req.method === "GET") {
    try {
      const userId = await authenticate(req);
      res.writeHead(200, { "content-type": "application/json" });
      res.end(JSON.stringify({ authenticated: true, userId, executor: "pty-container", ready: true }));
    } catch {
      res.writeHead(401, { "content-type": "application/json" });
      res.end(JSON.stringify({ error: "unauthorized" }));
    }
    return;
  }
  res.writeHead(404, { "content-type": "application/json" });
  res.end(JSON.stringify({ error: "not_found" }));
});

const sockets = new WebSocketServer({ noServer: true, maxPayload: 16 * 1024 });
server.on("upgrade", async (req, socket, head) => {
  const url = new URL(req.url ?? "/", "http://localhost");
  if (url.pathname !== "/v1/terminal") {
    socket.destroy();
    return;
  }
  try {
    const userId = await authenticate(req);
    const userDir = path.join(workspaceRoot, Buffer.from(userId).toString("hex").slice(0, 48));
    mkdirSync(userDir, { recursive: true, mode: 0o700 });

    sockets.handleUpgrade(req, socket, head, (ws) => {
      const shell = process.env.SHELL ?? "/bin/bash";
      const terminal = pty.spawn(shell, ["-l"], {
        name: "xterm-256color", cols: 100, rows: 30, cwd: userDir,
        env: { ...process.env, TERM: "xterm-256color", HOME: userDir }
      });
      const sessionId = randomUUID();
      ws.send(JSON.stringify({ type: "ready", sessionId, cwd: userDir, arch: process.arch }));
      terminal.onData((data) => {
        if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify({ type: "output", data }));
      });
      terminal.onExit(({ exitCode, signal }) => {
        if (ws.readyState === WebSocket.OPEN) ws.send(JSON.stringify({ type: "exit", exitCode, signal }));
        ws.close();
      });
      ws.on("message", (raw) => {
        try {
          const message = JSON.parse(raw.toString()) as { type?: string; data?: string; cols?: number; rows?: number };
          if (message.type === "input" && typeof message.data === "string") terminal.write(message.data);
          if (message.type === "resize" && Number.isInteger(message.cols) && Number.isInteger(message.rows)) {
            terminal.resize(Math.max(20, Math.min(240, message.cols!)), Math.max(5, Math.min(100, message.rows!)));
          }
        } catch {
          ws.send(JSON.stringify({ type: "error", error: "invalid_message" }));
        }
      });
      ws.on("close", () => terminal.kill());
      ws.on("error", () => terminal.kill());
    });
  } catch {
    socket.write("HTTP/1.1 401 Unauthorized\r\nConnection: close\r\n\r\n");
    socket.destroy();
  }
});

server.listen(port, "0.0.0.0", () => {
  console.log("Apolo control plane listening on :" + port + " (" + process.arch + ")");
});
