import logging
import os
from datetime import datetime

def setup_logging(log_level=logging.INFO):
    """Set up logging configuration for the HandTalk project."""
    
    # Create logs directory if it doesn't exist
    os.makedirs("logs", exist_ok=True)
    
    # Use a single log file instead of timestamped files
    log_filename = "logs/handtalk.log"
    
    # Clear any existing handlers to avoid duplicates
    logger = logging.getLogger("HandTalk")
    logger.handlers.clear()
    
    # Configure logging to both console and file
    logging.basicConfig(
        level=log_level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.StreamHandler(),  # Log to console
            logging.FileHandler(log_filename)  # Log to single file
        ],
        force=True  # This ensures we override any existing configuration
    )
    
    # Return logger instance
    return logging.getLogger("HandTalk")

def get_logger(name):
    """Get a logger instance with the specified name."""
    return logging.getLogger(name)