"""
scheduler.py
============
Configura APScheduler para ejecutar el scrape al arrancar y cada 24 horas.
Diseñado para integrarse con el ciclo de vida de FastAPI (lifespan).
"""

import logging
from contextlib import asynccontextmanager
from typing import AsyncGenerator

from apscheduler.schedulers.background import BackgroundScheduler

from scraper import run_scrape, PRODUCTS_FILE

logger = logging.getLogger(__name__)


def _run_scrape_job() -> None:
    """Wrapper síncrono para APScheduler."""
    try:
        count = run_scrape()
        logger.info("Job de scraping completado: %d productos", count)
    except Exception as exc:
        logger.error("Error en el job de scraping: %s", exc)


@asynccontextmanager
async def lifespan(app) -> AsyncGenerator[None, None]:  # type: ignore[type-arg]
    """
    Lifespan de FastAPI:
    - Al arrancar: lanza el scrape inicial si el JSON no existe aún,
      y programa el cron de 24 horas.
    - Al parar: detiene el scheduler limpiamente.
    """
    scheduler = BackgroundScheduler()

    # Ejecutar inmediatamente en un hilo aparte si el fichero no existe todavía
    if not PRODUCTS_FILE.exists():
        logger.info("products.json no encontrado — ejecutando scrape inicial…")
        scheduler.add_job(_run_scrape_job, id="initial_scrape")
    else:
        logger.info("products.json existente encontrado — omitiendo scrape inicial")

    # Cron: cada 24 horas
    scheduler.add_job(
        _run_scrape_job,
        trigger="interval",
        hours=24,
        id="daily_scrape",
    )

    scheduler.start()
    logger.info("Scheduler iniciado (cron cada 24h)")

    yield  # La app está corriendo

    scheduler.shutdown(wait=False)
    logger.info("Scheduler detenido")
