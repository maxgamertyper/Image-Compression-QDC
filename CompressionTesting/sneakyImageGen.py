import numpy as np
from PIL import Image
width = 1920
height = 1080

# 1920x1080 image with 3 color channels (RGB)
data = np.random.randint(0, 256, (height,width, 3), dtype=np.uint8)
Image.fromarray(data).save('all_colors_noise.png')