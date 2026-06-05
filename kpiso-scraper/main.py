"""
main.py
=======
FastAPI app que sirve búsquedas de productos desde el catálogo JSON local.

Endpoints:
  GET /health          → estado del servicio
  GET /products?q=...  → sugerencias de productos (máx. 5)
  POST /scrape         → fuerza un re-scraping inmediato (útil para tests)
"""

import json
import logging
import threading
from typing import Optional

from fastapi import FastAPI, Query, HTTPException
from fastapi.responses import JSONResponse

from scheduler import lifespan
from scraper import PRODUCTS_FILE, run_scrape

# ---------------------------------------------------------------------------
# Configuración de logging
# ---------------------------------------------------------------------------
logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s [%(levelname)s] %(name)s: %(message)s",
)
logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Aplicación
# ---------------------------------------------------------------------------
app = FastAPI(
    title="KPIso Scraper Service",
    description="Microservicio que pre-carga y sirve sugerencias de productos desde OpenFoodFacts.",
    version="1.0.0",
    lifespan=lifespan,
)

MAX_RESULTS = 5


def _load_products() -> list[dict]:
    """Carga el catálogo JSON desde disco. Devuelve lista vacía si no existe."""
    if not PRODUCTS_FILE.exists():
        return []
    try:
        return json.loads(PRODUCTS_FILE.read_text(encoding="utf-8"))
    except Exception as exc:
        logger.error("Error leyendo products.json: %s", exc)
        return []


# ---------------------------------------------------------------------------
# Endpoints
# ---------------------------------------------------------------------------

@app.get("/health")
def health() -> dict:
    """Comprueba si el servicio está listo y cuántos productos tiene en caché."""
    products = _load_products()
    return {
        "status": "ok",
        "catalogSize": len(products),
        "catalogReady": PRODUCTS_FILE.exists(),
    }


@app.get("/products")
def search_products(
    q: Optional[str] = Query(default=None, min_length=1, description="Término de búsqueda"),
) -> JSONResponse:
    """
    Devuelve hasta 5 sugerencias de productos que coincidan con el término `q`.
    La búsqueda es case-insensitive y coincide con subcadenas en el nombre.
    """
    if not q:
        raise HTTPException(status_code=400, detail="El parámetro 'q' es obligatorio")

    products = _load_products()

    if not products:
        # El JSON todavía no existe (scrape en progreso al arrancar)
        return JSONResponse(content=[], status_code=200)

    query_lower = q.strip().lower()
    matches = [
        p for p in products
        if query_lower in p.get("name", "").lower()
    ]

    return JSONResponse(content=matches[:MAX_RESULTS])


@app.post("/scrape")
def trigger_scrape() -> dict:
    """
    Fuerza un re-scraping en background (no bloquea la respuesta).
    Útil para forzar una actualización manual o en tests de integración.
    """
    def _run():
        try:
            count = run_scrape()
            logger.info("Scrape manual completado: %d productos", count)
        except Exception as exc:
            logger.error("Error en scrape manual: %s", exc)

    thread = threading.Thread(target=_run, daemon=True)
    thread.start()
    return {"status": "started", "message": "Scraping iniciado en background"}
