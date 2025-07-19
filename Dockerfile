FROM clojure:tools-deps

# Install dependencies
RUN apt-get update && apt-get install -y nodejs npm curl git-lfs

# Initialize Git LFS
RUN git lfs install

# Set up working directory
WORKDIR /usr/src/app

# First, do a shallow clone with --no-checkout to get just the repo structure
RUN git clone --no-checkout https://github.com/NicolasMorenoAndrade/shorturl.git .

# Checkout only the dependency files first
RUN git checkout HEAD deps.edn shadow-cljs.edn package.json package-lock.json

# Install dependencies (this layer can be cached)
RUN npm install

# Now checkout everything else and pull LFS objects
RUN git checkout HEAD
RUN git lfs pull

# Process and optimize CSS with Tailwind
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
arget/app-standalone.jar"]
