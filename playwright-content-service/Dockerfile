FROM mcr.microsoft.com/playwright:v1.58.2-noble
WORKDIR /app
COPY package.json server.mjs ./
RUN npm install
EXPOSE 3000
CMD ["xvfb-run", "--auto-servernum", "--server-args=-screen 0 1280x1024x24", "node", "server.mjs"]
