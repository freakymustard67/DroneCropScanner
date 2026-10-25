import os
import argparse
from datetime import datetime
import numpy as np
import matplotlib.pyplot as plt
from sentinelhub import (
    SHConfig,
    SentinelHubRequest,
    DataCollection,
    bbox_to_dimensions,
    BBox,
    CRS,
    MimeType
)

# Load helper to load .env files if dotenv is installed
try:
    from dotenv import load_dotenv
    load_dotenv()
except ImportError:
    pass

# Default Bounding Box: Semi-agricultural area near Red Hills, Chennai
# Format: [min_longitude, min_latitude, max_longitude, max_latitude]
DEFAULT_BBOX = [80.1500, 13.1500, 80.2000, 13.2000]

# 1. Sentinel Hub Evalscripts
EVALSCRIPT_TRUE_COLOR = """
//VERSION=3
function setup() {
    return {
        input: ["B02", "B03", "B04"],
        output: { bands: 3 }
    };
}

function evaluatePixel(sample) {
    // Brighten the raw optical bands by 2.5x
    return [2.5 * sample.B04, 2.5 * sample.B03, 2.5 * sample.B02];
}
"""

EVALSCRIPT_NDVI_VISUAL = """
//VERSION=3
function setup() {
    return {
        input: ["B04", "B08"],
        output: { bands: 3 }
    };
}

function evaluatePixel(sample) {
    let ndvi = (sample.B08 - sample.B04) / (sample.B08 + sample.B04);
    
    // Color mapping based on NDVI values
    if (ndvi < 0.2) return [0.8, 0.8, 0.8]; // Barren/Urban (Gray)
    if (ndvi < 0.4) return [0.8, 0.8, 0.4]; // Sparse Vegetation (Yellow-ish)
    if (ndvi < 0.6) return [0.4, 0.8, 0.4]; // Moderate Vegetation (Light Green)
    return [0.1, 0.5, 0.1];                 // Dense Vegetation (Dark Green)
}
"""

EVALSCRIPT_NDVI_RAW = """
//VERSION=3
function setup() {
    return {
        input: ["B04", "B08"],
        output: { bands: 1, sampleType: "FLOAT32" }
    };
}

function evaluatePixel(sample) {
    let ndvi = (sample.B08 - sample.B04) / (sample.B08 + sample.B04);
    return [ndvi];
}
"""

def get_config(client_id=None, client_secret=None):
    """Set up configuration for Copernicus Data Space."""
    config = SHConfig()
    
    # Retrieve credentials from arguments, env variables, or fallback
    cid = client_id or os.getenv("SH_CLIENT_ID")
    secret = client_secret or os.getenv("SH_CLIENT_SECRET")
    
    config.sh_client_id = cid or "YOUR_CLIENT_ID"
    config.sh_client_secret = secret or "YOUR_CLIENT_SECRET"
    
    # Route requests to Copernicus Data Space Ecosystem endpoints
    config.sh_base_url = "https://sh.dataspace.copernicus.eu"
    config.sh_token_url = "https://identity.dataspace.copernicus.eu/auth/realms/CDSE/protocol/openid-connect/token"
    
    return config

def fetch_image(config, bbox_coords, start_date, end_date, mode="visual", max_cloud_cover=0.2):
    """Fetch visual image (True Color or NDVI colored) for the specified date range."""
    bbox = BBox(bbox=bbox_coords, crs=CRS.WGS84)
    image_size = bbox_to_dimensions(bbox, resolution=10)
    
    if mode == "visual":
        evalscript = EVALSCRIPT_TRUE_COLOR
        mimetype = MimeType.PNG
    elif mode == "ndvi":
        evalscript = EVALSCRIPT_NDVI_VISUAL
        mimetype = MimeType.PNG
    elif mode == "ndvi_raw":
        evalscript = EVALSCRIPT_NDVI_RAW
        mimetype = MimeType.TIFF
    else:
        raise ValueError(f"Unknown mode: {mode}")

    print(f"Requesting Sentinel-2 {mode} data from {start_date} to {end_date}...")
    
    cdse_collection = DataCollection.SENTINEL2_L2A.define_from(
        "s2-l2a-cdse",
        service_url=config.sh_base_url
    )

    request = SentinelHubRequest(
        evalscript=evalscript,
        input_data=[
            SentinelHubRequest.input_data(
                data_collection=cdse_collection,
                time_interval=(start_date, end_date),
                maxcc=max_cloud_cover
            )
        ],
        responses=[
            SentinelHubRequest.output_response('default', mimetype)
        ],
        bbox=bbox,
        size=image_size,
        config=config
    )
    
    data = request.get_data()
    return data

def run_one_off(config, bbox_coords, start_date, end_date, mode, maxcc, output_path):
    """Run one-off fetch and plot."""
    data = fetch_image(config, bbox_coords, start_date, end_date, mode, maxcc)
    
    if not data or len(data) == 0:
        print(f"Error: No cloud-free data found for period {start_date} to {end_date} with maxcc={maxcc}.")
        print("Tip: Expand the time interval or increase maxcc.")
        return
        
    latest_image = data[-1]
    
    plt.figure(figsize=(10, 10))
    if mode == "ndvi_raw":
        ndvi_array = latest_image[:, :, 0] if latest_image.ndim == 3 else latest_image
        im = plt.imshow(ndvi_array, cmap="RdYlGn", vmin=-1.0, vmax=1.0)
        plt.colorbar(im, label="NDVI (Normalized Difference Vegetation Index)")
        title = "Sentinel-2 NDVI Raw Analysis"
    else:
        plt.imshow(latest_image)
        title = "Sentinel-2 True Color Image" if mode == "visual" else "Sentinel-2 NDVI Colored Map"
        
    plt.axis('off')
    plt.title(f"{title}\nBBox {bbox_coords} | Date Range: {start_date} to {end_date}")
    
    plt.savefig(output_path, bbox_inches='tight', dpi=300)
    print(f"Success! Image saved as {output_path}")

def run_timeseries(config, bbox_coords, start_date, end_date, maxcc, output_path):
    """
    Run timeseries monitoring. 
    Downloads images, computes average NDVI, and plots the trend over time.
    """
    from sentinelhub import SentinelHubCatalog
    
    bbox = BBox(bbox=bbox_coords, crs=CRS.WGS84)
    
    print(f"Starting crop health timeseries monitoring pipeline from {start_date} to {end_date}...")
    
    catalog = SentinelHubCatalog(config=config)
    cdse_collection = DataCollection.SENTINEL2_L2A.define_from(
        "s2-l2a-cdse",
        service_url=config.sh_base_url
    )
    
    print("Searching Catalog API for cloud-free acquisitions...")
    results = list(catalog.search(
        collection=cdse_collection,
        bbox=bbox,
        time=(start_date, end_date)
    ))
    
    # Filter by cloud cover
    filtered_items = []
    for item in results:
        cc = item.get("properties", {}).get("eo:cloud_cover", 100.0)
        if cc <= maxcc * 100:
            filtered_items.append(item)
            
    if not filtered_items:
        print(f"No acquisitions found with cloud cover <= {maxcc * 100}%.")
        return
        
    # Sort by date ascending
    filtered_items.sort(key=lambda x: x["properties"]["datetime"])
    
    print(f"Found {len(filtered_items)} valid acquisitions. Downloading and processing...")
    
    mean_ndvis = []
    plot_dates = []
    
    for item in filtered_items:
        dt_str = item["properties"]["datetime"]
        date_str = dt_str.split("T")[0]
        cc = item["properties"].get("eo:cloud_cover", 0.0)
        
        try:
            # We request with max_cloud_cover=1.0 since we already filtered at the catalog level
            data = fetch_image(config, bbox_coords, date_str, date_str, "ndvi_raw", max_cloud_cover=1.0)
            
            if data and len(data) > 0:
                ndvi_img = data[-1]
                ndvi_array = ndvi_img[:, :, 0] if ndvi_img.ndim == 3 else ndvi_img
                # Remove any NaN or anomalous values outside standard NDVI range
                valid_ndvi = ndvi_array[(ndvi_array >= -1.0) & (ndvi_array <= 1.0)]
                if len(valid_ndvi) > 0:
                    mean_ndvi = np.mean(valid_ndvi)
                    mean_ndvis.append(mean_ndvi)
                    # Parse date for plotting
                    date_obj = datetime.strptime(date_str, "%Y-%m-%d")
                    plot_dates.append(date_obj)
                    print(f"  Processed {date_str} (Cloud cover: {cc:.1f}%) | Mean NDVI: {mean_ndvi:.4f}")
                else:
                    print(f"  Skipped {date_str}: No valid NDVI pixels.")
            else:
                print(f"  Warning: No data returned for date {date_str}")
        except Exception as e:
            print(f"  Error processing date {date_str}: {e}")
            
    if not mean_ndvis:
        print("No valid NDVI data could be calculated.")
        return
        
    # Plot the timeseries
    plt.figure(figsize=(10, 6))
    plt.plot(plot_dates, mean_ndvis, marker='o', linestyle='-', color='forestgreen', linewidth=2)
    plt.title(f"Crop Health (NDVI) Timeseries Trend\nField Coordinates: {bbox_coords}")
    plt.xlabel("Date of Acquisition")
    plt.ylabel("Mean NDVI")
    plt.grid(True, linestyle='--', alpha=0.5)
    plt.ylim(-0.1, 1.0)
    
    # Highlight vegetation health levels
    plt.axhspan(0.6, 1.0, color='green', alpha=0.1, label='Healthy/Dense Vegetation')
    plt.axhspan(0.2, 0.6, color='yellowgreen', alpha=0.1, label='Sparse/Moderate Vegetation')
    plt.axhspan(-0.1, 0.2, color='sandybrown', alpha=0.1, label='Barren Land/Water/Urban')
    plt.legend(loc='lower left')
    
    plt.xticks(rotation=45)
    plt.tight_layout()
    
    plt.savefig(output_path, dpi=300)
    print(f"\nTimeseries monitoring complete! Plot saved as {output_path}")
    
    # Print summary table
    print("\nDate       | Mean NDVI | Classification")
    print("-" * 42)
    for date, val in zip(plot_dates, mean_ndvis):
        if val > 0.6:
            status = "Healthy/Dense"
        elif val > 0.2:
            status = "Sparse/Moderate"
        else:
            status = "Barren/Urban/Water"
        print(f"{date.strftime('%Y-%m-%d')} | {val:9.4f} | {status}")

def run_boundary_detection(config, bbox_coords, start_date, end_date, maxcc, output_path):
    """
    Fetch both True Color and raw NDVI data, use NDVI gradients/contours to
    automatically detect field boundaries, and overlay them on the True Color image.
    """
    # 1. Fetch True Color image
    print("Fetching True Color visual image...")
    visual_data = fetch_image(config, bbox_coords, start_date, end_date, "visual", maxcc)
    if not visual_data or len(visual_data) == 0:
        print("Error: No True Color data found.")
        return
    visual_image = visual_data[-1]
    
    # 2. Fetch raw NDVI image
    print("Fetching raw NDVI data...")
    ndvi_data = fetch_image(config, bbox_coords, start_date, end_date, "ndvi_raw", maxcc)
    if not ndvi_data or len(ndvi_data) == 0:
        print("Error: No NDVI data found.")
        return
    ndvi_image = ndvi_data[-1]
    ndvi_array = ndvi_image[:, :, 0] if ndvi_image.ndim == 3 else ndvi_image
    
    # 3. Perform edge detection and boundary extraction using NumPy gradients
    print("Extracting boundary contours...")
    # Smooth the NDVI array to reduce noise using a simple box blur
    def smooth_image(img, box_size=3):
        kernel = np.ones((box_size, box_size)) / (box_size ** 2)
        pad = box_size // 2
        padded = np.pad(img, pad, mode='edge')
        smoothed = np.zeros_like(img)
        for y in range(img.shape[0]):
            for x in range(img.shape[1]):
                smoothed[y, x] = np.sum(padded[y:y+box_size, x:x+box_size] * kernel)
        return smoothed

    smoothed_ndvi = smooth_image(ndvi_array, box_size=3)
    
    # Calculate gradient magnitude
    dy, dx = np.gradient(smoothed_ndvi)
    grad_mag = np.sqrt(dx**2 + dy**2)
    
    # Normalize gradient magnitude
    if np.max(grad_mag) > 0:
        grad_mag_normalized = grad_mag / np.max(grad_mag)
    else:
        grad_mag_normalized = grad_mag

    # Create the overlay figure
    plt.figure(figsize=(10, 10))
    
    # Display the background True Color image
    plt.imshow(visual_image)
    
    # Generate contours of the NDVI values to mark field transitions
    # Let's draw contours at reasonable NDVI thresholds (e.g. 0.25, 0.35, 0.45)
    levels = [0.25, 0.35, 0.45]
    levels = [lvl for lvl in levels if lvl >= np.min(smoothed_ndvi) and lvl <= np.max(smoothed_ndvi)]
    if not levels:
        levels = [np.mean(smoothed_ndvi)]
        
    plt.contour(smoothed_ndvi, levels=levels, colors=['red', 'orange', 'yellow'], linewidths=1.5)
    
    # Overlay the high-gradient boundaries (edges) in cyan to mark sharp borders
    # Only plot pixels where gradient magnitude is in the top 10%
    edge_threshold = np.percentile(grad_mag_normalized, 90)
    edges = np.zeros_like(grad_mag_normalized)
    edges[grad_mag_normalized > edge_threshold] = 1.0
    
    # Create masked overlay
    masked_edges = np.zeros((*edges.shape, 4))
    masked_edges[edges > 0] = [0.0, 1.0, 1.0, 0.6]  # Cyan lines with 60% opacity
    plt.imshow(masked_edges)

    plt.axis('off')
    plt.title(f"Automated Field Boundary Detection & Overlay\nBBox: {bbox_coords} | Date Range: {start_date} to {end_date}")
    
    # Add a custom legend
    from matplotlib.lines import Line2D
    custom_lines = [
        Line2D([0], [0], color='cyan', lw=2, label='Sharp Field Boundaries (High Gradient)'),
        Line2D([0], [0], color='red', lw=1.5, label='NDVI Contour (0.25)'),
        Line2D([0], [0], color='orange', lw=1.5, label='NDVI Contour (0.35)'),
        Line2D([0], [0], color='yellow', lw=1.5, label='NDVI Contour (0.45)')
    ]
    plt.legend(handles=custom_lines, loc='upper right')

    plt.savefig(output_path, bbox_inches='tight', dpi=300)
    print(f"Success! Boundary overlay saved as {output_path}")


def main():
    parser = argparse.ArgumentParser(description="Sentinel-2 Crop Health & Visual Monitoring Tool")
    
    parser.add_argument("--mode", type=str, choices=["visual", "ndvi", "ndvi_raw", "timeseries", "boundary"], default="visual",
                        help="Mode of operation: 'visual' (True Color PNG), 'ndvi' (Visual colormapped PNG), "
                             "'ndvi_raw' (Raw NDVI TIFF + colorbar), 'timeseries' (Pipeline to plot NDVI change over time), "
                             "'boundary' (Automatically mark field boundaries)")
    parser.add_argument("--start", type=str, default="2026-05-29", help="Start date (YYYY-MM-DD)")
    parser.add_argument("--end", type=str, default="2026-06-29", help="End date (YYYY-MM-DD)")
    parser.add_argument("--bbox", type=float, nargs=4, default=DEFAULT_BBOX,
                        help="Bounding box coordinates: min_lon min_lat max_lon max_lat")
    parser.add_argument("--maxcc", type=float, default=0.2, help="Maximum cloud coverage threshold (0.0 to 1.0)")
    parser.add_argument("--out", type=str, default=None, help="Output file name")
    parser.add_argument("--client-id", type=str, help="Copernicus Data Space client ID")
    parser.add_argument("--client-secret", type=str, help="Copernicus Data Space client secret")
    
    args = parser.parse_args()
    
    # Load configuration
    config = get_config(args.client_id, args.client_secret)
    
    # Determine default output file
    if args.out is None:
        if args.mode == "visual":
            output_file = "chennai_field_output.png"
        elif args.mode == "ndvi":
            output_file = "chennai_field_ndvi_colored.png"
        elif args.mode == "ndvi_raw":
            output_file = "chennai_field_ndvi_raw.png"
        elif args.mode == "boundary":
            output_file = "chennai_field_boundary.png"
        else:
            output_file = "chennai_field_timeseries.png"
    else:
        output_file = args.out
        
    if args.mode == "timeseries":
        run_timeseries(config, args.bbox, args.start, args.end, args.maxcc, output_file)
    elif args.mode == "boundary":
        run_boundary_detection(config, args.bbox, args.start, args.end, args.maxcc, output_file)
    else:
        run_one_off(config, args.bbox, args.start, args.end, args.mode, args.maxcc, output_file)


if __name__ == "__main__":
    main()
