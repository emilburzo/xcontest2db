FROM node:20-bookworm-slim
WORKDIR /app
COPY package.json ./
RUN npm install
RUN npx playwright install --with-deps chromium
RUN apt-get update && apt-get install -y --no-install-recommends xvfb xauth \
    && rm -rf /var/lib/apt/lists/* \
    && which Xvfb && which xvfb-run && which xauth
COPY server.mjs ./
EXPOSE 3000
CMD xvfb-run --auto-servernum --server-args="-screen 0 1280x1024x24" node server.mjs
