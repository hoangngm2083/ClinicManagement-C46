import asyncio
import logging
from apscheduler.schedulers.asyncio import AsyncIOScheduler
from apscheduler.triggers.interval import IntervalTrigger
from typing import Optional
from ..services.clinic_api import ClinicAPIService
from .data_loader import DataLoader
from ..config.settings import settings

logger = logging.getLogger(__name__)


class DataSyncService:
    """Service for synchronizing data from microservices to vector database"""

    def __init__(self, clinic_api: ClinicAPIService, data_loader: DataLoader):
        self.clinic_api = clinic_api
        self.data_loader = data_loader
        self.scheduler = AsyncIOScheduler()
        self.is_running = False

        logger.info("DataSyncService initialized")

    async def start(self):
        """Start the data synchronization service"""
        if self.is_running:
            logger.warning("Data sync service is already running")
            return

        try:
            # Add sync jobs
            self.scheduler.add_job(
                self._sync_doctors,
                trigger=IntervalTrigger(minutes=settings.sync_doctors_interval_minutes),
                id="sync_doctors",
                name="Sync Doctor Data",
                max_instances=1
            )

            self.scheduler.add_job(
                self._sync_packages,
                trigger=IntervalTrigger(minutes=settings.sync_packages_interval_minutes),
                id="sync_packages",
                name="Sync Package Data",
                max_instances=1
            )

            self.scheduler.add_job(
                self._sync_slots,
                trigger=IntervalTrigger(minutes=settings.sync_slots_interval_minutes),
                id="sync_slots",
                name="Sync Slot Data",
                max_instances=1
            )

            # Start scheduler
            self.scheduler.start()
            self.is_running = True

            logger.info("Data sync service started successfully")

        except Exception as e:
            logger.error(f"Error starting data sync service: {e}")
            raise

    async def stop(self):
        """Stop the data synchronization service"""
        if not self.is_running:
            return

        try:
            self.scheduler.shutdown(wait=True)
            self.is_running = False
            logger.info("Data sync service stopped")
        except Exception as e:
            logger.error(f"Error stopping data sync service: {e}")

    async def _sync_doctors(self):
        """Sync doctor data periodically"""
        try:
            logger.info("Starting doctor data sync...")
            await self.data_loader.sync_doctors()
            logger.info("Doctor data sync completed")
        except Exception as e:
            logger.error(f"Error syncing doctor data: {e}")

    async def _sync_packages(self):
        """Sync medical package data periodically"""
        try:
            logger.info("Starting package data sync...")
            await self.data_loader.sync_packages()

            # Clear system prompt cache to reflect new packages
            from ..models.prompts import clear_system_prompt_cache
            clear_system_prompt_cache()
            logger.info("Package data sync completed and cache cleared")
        except Exception as e:
            logger.error(f"Error syncing package data: {e}")

    async def _sync_slots(self):
        """Sync slot availability data (for RAG context)"""
        try:
            logger.info("Starting slot data sync...")
            # Note: Slot data is typically queried in real-time,
            # but we can cache recent availability for RAG
            # Don't use async with - clinic_api is a long-lived shared instance
            slots = await self.clinic_api.get_all_slots_next_week()
            # Here we could update a cache or vector store with slot info
            # For now, we'll just log the count
            logger.info(f"Fetched {len(slots)} slots for next week")
        except Exception as e:
            logger.error(f"Error syncing slot data: {e}")

    async def manual_sync_all(self):
        """Manually trigger full data sync"""
        try:
            logger.info("Starting manual full data sync...")

            await self._sync_doctors()
            await asyncio.sleep(1)  # Small delay between syncs

            await self._sync_packages()
            await asyncio.sleep(1)

            await self._sync_slots()

            logger.info("Manual full data sync completed")
            return {"status": "success", "message": "All data synced successfully"}

        except Exception as e:
            error_msg = f"Manual sync failed: {str(e)}"
            logger.error(error_msg)
            return {"status": "error", "message": error_msg}

    def get_sync_status(self) -> dict:
        """Get current sync service status"""
        jobs = []
        if self.scheduler.running:
            for job in self.scheduler.get_jobs():
                jobs.append({
                    "id": job.id,
                    "name": job.name,
                    "next_run_time": job.next_run_time.isoformat() if job.next_run_time else None,
                    "trigger": str(job.trigger)
                })

        return {
            "is_running": self.is_running,
            "jobs": jobs,
            "scheduler_running": self.scheduler.running if hasattr(self.scheduler, 'running') else False
        }

    async def force_sync_doctors(self):
        """Force immediate doctor sync"""
        return await self._sync_doctors()

    async def force_sync_packages(self):
        """Force immediate package sync"""
        return await self._sync_packages()

    async def force_sync_slots(self):
        """Force immediate slot sync"""
        return await self._sync_slots()
