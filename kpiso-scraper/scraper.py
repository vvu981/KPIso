"""
scraper.py
==========
Descarga productos desde OpenFoodFacts para una lista de términos semilla
y los serializa en data/products.json.

Cada entrada del JSON tiene la estructura:
  {
    "name": "Leche entera",
    "imageUrl": "https://...",
    "mainCategory": "en:dairies",
    "categoryTags": "en:dairies,en:milks"
  }
"""

import json
import logging
import os
import time
from pathlib import Path
from typing import Optional

import httpx

logger = logging.getLogger(__name__)

# ---------------------------------------------------------------------------
# Configuración
# ---------------------------------------------------------------------------
DATA_DIR = Path(os.getenv("DATA_DIR", "/app/data"))
PRODUCTS_FILE = DATA_DIR / "products.json"
OFF_BASE_URL = "https://world.openfoodfacts.org/cgi/search.pl"
DEFAULT_IMAGE = "https://via.placeholder.com/150?text=No+Image"

# Máximo de productos a guardar por término semilla
PAGE_SIZE = 20

# Términos semilla en español — base para el catálogo pre-cargado
SEED_TERMS: list[str] = [
    "leche", "pan", "arroz", "aceite de oliva", "pasta", "tomate",
    "huevos", "pollo", "ternera", "cerdo", "atún", "sardina",
    "mantequilla", "queso", "yogur", "nata", "zumo naranja", "agua",
    "cerveza", "vino", "café", "té", "azúcar", "sal", "harina",
    "lentejas", "garbanzos", "judías", "maíz", "guisantes",
    "lechuga", "tomates cherry", "zanahoria", "cebolla", "ajo",
    "patata", "manzana", "plátano", "naranja", "fresa",
    "chocolate", "galletas", "cereales", "avena", "miel",
    "ketchup", "mayonesa", "mostaza", "vinagre", "pimienta",
    "jabón", "champú", "papel higiénico", "detergente",
]


# ---------------------------------------------------------------------------
# Lógica de scraping
# ---------------------------------------------------------------------------

def _parse_product(raw: dict) -> Optional[dict]:
    """Extrae y normaliza los campos de un producto de la respuesta de OFF."""
    name = (
        raw.get("product_name", "")
        or raw.get("product_name_es", "")
        or raw.get("generic_name", "")
    ).strip()

    if not name:
        return None

    # Imagen — intentar varias claves en orden de preferencia
    image_url = (
        raw.get("image_front_small_url")
        or raw.get("image_small_url")
        or raw.get("image_front_url")
        or raw.get("image_url")
        or DEFAULT_IMAGE
    )
    if not image_url.startswith("http"):
        image_url = DEFAULT_IMAGE

    # Categorías
    tags: list[str] = raw.get("categories_tags", []) or []
    main_category = tags[0] if tags else None
    category_tags = ",".join(tags)

    return {
        "name": name,
        "imageUrl": image_url,
        "mainCategory": main_category,
        "categoryTags": category_tags,
    }


def _fetch_products_for_term(client: httpx.Client, term: str) -> list[dict]:
    """Llama a OFF y devuelve lista de productos parseados para un término."""
    try:
        response = client.get(
            OFF_BASE_URL,
            params={
                "search_terms": term,
                "search_simple": 1,
                "action": "process",
                "json": 1,
                "page_size": PAGE_SIZE,
                "lc": "es",
            },
            timeout=15.0,
        )
        response.raise_for_status()
        data = response.json()
        raw_products: list[dict] = data.get("products", []) or []

        results = []
        for raw in raw_products:
            parsed = _parse_product(raw)
            if parsed:
                results.append(parsed)
        return results

    except httpx.TimeoutException:
        logger.warning("Timeout al buscar '%s' en OpenFoodFacts", term)
        return []
    except Exception as exc:
        logger.error("Error al buscar '%s': %s", term, exc)
        return []


def run_scrape() -> int:
    """
    Ejecuta el scraping completo para todos los SEED_TERMS.
    Deduplica por nombre (case-insensitive) y escribe products.json.
    Devuelve el número de productos guardados.
    """
    DATA_DIR.mkdir(parents=True, exist_ok=True)
    logger.info("Iniciando scrape de %d términos semilla…", len(SEED_TERMS))

    seen_names: set[str] = set()
    all_products: list[dict] = []

    with httpx.Client(follow_redirects=True) as client:
        for i, term in enumerate(SEED_TERMS, start=1):
            logger.debug("[%d/%d] Buscando: %s", i, len(SEED_TERMS), term)
            products = _fetch_products_for_term(client, term)
            for p in products:
                key = p["name"].lower()
                if key not in seen_names:
                    seen_names.add(key)
                    all_products.append(p)
            # Pausa cortés para no saturar la API
            time.sleep(0.5)

    PRODUCTS_FILE.write_text(
        json.dumps(all_products, ensure_ascii=False, indent=2),
        encoding="utf-8",
    )
    logger.info("Scrape completado: %d productos guardados en %s", len(all_products), PRODUCTS_FILE)
    return len(all_products)
