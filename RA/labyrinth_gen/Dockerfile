FROM python:3.11-slim

WORKDIR /app

# Install runtime dependencies
COPY requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt

# Copy repository into the image
COPY . /app

# Use the repository folder as workdir for runtime (labyrinthWebService.py is copied here when building from RA/labyrinth_gen)
WORKDIR /app

# Expose Flask default port
EXPOSE 5000

# Run the Flask app
CMD ["python", "labyrinthWebService.py"]
