FROM python:3.12-slim

WORKDIR /app

# Instalar dependencias primero (capa cacheada)
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

# Copiar código fuente
COPY . .

# Directorio para el JSON de productos (montado como volumen en docker-compose)
RUN mkdir -p /app/data

EXPOSE 8081

CMD ["uvicorn", "main:app", "--host", "0.0.0.0", "--port", "8081"]
