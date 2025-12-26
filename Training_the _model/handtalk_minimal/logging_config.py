import logging

def setup_logging(log_level=logging.WARNING):
    """Set up logging configuration for the HandTalk project."""
    
    # Configure logging to console only (no file logging)
    # Only show warnings and errors to reduce spam
    logging.basicConfig(
        level=log_level,
        format='%(asctime)s - %(name)s - %(levelname)s - %(message)s',
        handlers=[
            logging.StreamHandler()  # Log to console only
        ]
    )
    
    # Return logger instance
    return logging.getLogger("HandTalk")

def get_logger(name):
    """Get a logger instance with the specified name."""
    return logging.getLogger(name)