FROM node:18-alpine

WORKDIR /app

# Copiar configuración de dependencias
COPY package*.json ./

# Instalar dependencias limpiamente
RUN npm install --omit=dev

# Copiar el código fuente
COPY index.js ./
COPY systemPrompt.md ./

EXPOSE 3000

# Ejecutar el servidor MCP
CMD ["node", "index.js"]
