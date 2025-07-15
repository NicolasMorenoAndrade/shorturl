FROM clojure:tools-deps

RUN apt-get update && apt-get install -y nodejs npm curl

WORKDIR /usr/src/app

# Copy dependency files first (for better layer caching)
COPY deps.edn shadow-cljs.edn package.json package-lock.json* ./

# Install npm dependencies
RUN npm install

# Copy source code and resources
COPY . .

# Process CSS with Tailwind
RUN npx @tailwindcss/cli -i ./resources/public/assets/css/input.css -o ./resources/public/assets/css/output.css --minify

# Build frontend
RUN npx shadow-cljs release app

# Build backend
RUN clj -T:build uber

# Rename jar for easier reference
RUN mv target/app-1.0.*-standalone.jar target/app-standalone.jar

# Expose the application port
EXPOSE 3001

# Add health check
HEALTHCHECK --interval=30s --timeout=3s CMD curl -f http://localhost:3001/ || exit 1

# Command to run the application
CMD ["java", "-jar", "target/app-standalone.jar"]
