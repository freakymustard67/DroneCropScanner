# High-Resolution Crop Analysis: Chennai Small Field

This report details the satellite analysis for a small field area near Red Hills, Chennai, using the highest resolution Sentinel-2 L2A optical bands (10m/pixel).

* **Area of Interest (Bounding Box):** `[80.1650, 13.1650, 80.1700, 13.1700]` (~500m x 550m)
* **Latest Available Acquisition:** June 28, 2026

---

## 1. Imagery Gallery (June 28, 2026)

````carousel
![True Color RGB Visual](/home/abhishek/.gemini/antigravity-cli/brain/c8244516-b7a6-4a8c-8645-91645b24a3ed/small_field_visual.png)
<!-- slide -->
![Colormapped NDVI Map](/home/abhishek/.gemini/antigravity-cli/brain/c8244516-b7a6-4a8c-8645-91645b24a3ed/small_field_ndvi_colored.png)
<!-- slide -->
![Raw NDVI with Scale Bar](/home/abhishek/.gemini/antigravity-cli/brain/c8244516-b7a6-4a8c-8645-91645b24a3ed/small_field_ndvi_raw.png)
````

* **True Color RGB:** Brightened visual snapshot showing crop outlines and ground features.
* **Colormapped NDVI Map:** Visual classification highlighting sparse/moderate crop health (yellowish/light green).
* **Raw NDVI:** Precision float matrix mapping index values directly to a scale bar between -1.0 and +1.0.

---

## 2. Crop Health Timeseries (May – June 2026)

Below is the historical trend of mean NDVI index values calculated across 11 clear flyovers:

![NDVI Timeseries Trend](/home/abhishek/.gemini/antigravity-cli/brain/c8244516-b7a6-4a8c-8645-91645b24a3ed/small_field_timeseries.png)

### Acquisition Log & Classification Table

| Date of Acquisition | Cloud Cover (%) | Mean NDVI | Crop Health Status |
| :--- | :--- | :--- | :--- |
| **2026-05-19** | 7.0% | 0.3711 | Sparse/Moderate Vegetation |
| **2026-05-24** | 0.0% | 0.3199 | Sparse/Moderate Vegetation |
| **2026-05-24** | 1.4% | 0.3199 | Sparse/Moderate Vegetation |
| **2026-06-03** | 19.1% | 0.3138 | Sparse/Moderate Vegetation |
| **2026-06-03** | 12.5% | 0.3138 | Sparse/Moderate Vegetation |
| **2026-06-05** | 5.6% | 0.3380 | Sparse/Moderate Vegetation |
| **2026-06-05** | 4.0% | 0.3380 | Sparse/Moderate Vegetation |
| **2026-06-25** | 6.4% | 0.4586 | Sparse/Moderate Vegetation |
| **2026-06-25** | 15.3% | 0.4586 | Sparse/Moderate Vegetation |
| **2026-06-28** | 15.2% | 0.5256 | Sparse/Moderate Vegetation (Greening) |
| **2026-06-28** | 7.1% | 0.5256 | Sparse/Moderate Vegetation (Greening) |

---

## 3. Analysis & Key Insights

> [!NOTE]
> **Greening Trend Detected:** The field started with moderate greenness in mid-May (`0.37`), experienced a minor dip/harvesting event in late May/early June (`0.31`), and has since shown a steady rise, reaching **`0.52`** by late June. This indicates active vegetative growth or crop emergence.

> [!IMPORTANT]
> **High-Resolution Validity:** Unlike the larger 5km outskirt area which averaged `0.10` NDVI (due to built-up surroundings and roads), this targeted bounding box isolates the actual field, resulting in much cleaner, crop-representative signal data.

---

## 4. Automated Field Boundary Marking

We integrated an automated boundary marking algorithm into the CLI tool using gradient-based boundary extraction and multi-level contour overlays.

To generate this boundary overlay, run:
```bash
python3 sentinel_monitor.py --bbox 80.165 13.165 80.170 13.170 --mode boundary --out small_field_boundary.png --start 2026-06-20 --end 2026-06-30
```

### Boundary Overlay Result
Below is the automatically marked boundaries overlaid on the True Color visual image:
![Automated Field Boundary Overlay](/home/abhishek/.gemini/antigravity-cli/brain/c8244516-b7a6-4a8c-8645-91645b24a3ed/small_field_boundary.png)

### How It Works:
1. **Spatial Filtering:** Smooths high-frequency noise from the 10m resolution NDVI array.
2. **Spatial Gradient Magnitude:** Calculates 2D spatial gradients ($dx, dy$) across the smoothed NDVI values. The magnitude of the gradient $\sqrt{dx^2 + dy^2}$ highlights regions with sharp transitions (e.g., roads, boundaries, paths, or borders between different fields).
3. **High-Gradient Overlay:** Flags the top 10% highest gradient pixels and overlays them as solid cyan lines.
4. **NDVI Contours:** Generates isoline contours at specific NDVI thresholds (`0.25` in Red, `0.35` in Orange, `0.45` in Yellow) to map variations in plant density and vegetation borders.
