FROM --platform=linux/amd64 node:22-bookworm-slim
RUN apt-get update && apt-get install -y --no-install-recommends bash ca-certificates python3 python3-pip git build-essential && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY services/control-plane/package.json ./package.json
COPY services/control-plane/tsconfig.json ./tsconfig.json
RUN npm install
COPY services/control-plane/src ./src
RUN npm run build
RUN useradd --create-home --uid 10001 apolo && mkdir -p /workspace && chown -R apolo:apolo /app /workspace
USER apolo
ENV NODE_ENV=production PORT=8080 WORKSPACE_ROOT=/workspace
EXPOSE 8080
CMD ["npm", "start"]
