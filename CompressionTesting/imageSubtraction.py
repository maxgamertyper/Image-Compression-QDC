from PIL import Image
import numpy as np
from rich import print

IMAGE1_FILE = "QDCHelloWorld.png"
IMAGE2_FILE = "outputTest.png"

#checks parent directory by default as well
#fyi: files in this directory are kind of bad since they're just here for testing

import os

image1_path = os.path.abspath(os.path.join(os.getcwd(), IMAGE1_FILE))
image2_path = os.path.abspath(os.path.join(os.getcwd(), IMAGE2_FILE))


if not os.path.isfile(image1_path):
    image1_path = os.path.abspath(os.path.join(os.getcwd(), "..", IMAGE1_FILE))
if not os.path.isfile(image1_path):
    print("[bold red]Image 1 could not be found![/bold red]")

if not os.path.isfile(image2_path):
    image2_path = os.path.abspath(os.path.join(os.getcwd(), "..", IMAGE2_FILE))
if not os.path.isfile(image2_path):
    print("[bold red]Image 2 could not be found![/bold red]")


print("[bold yellow]Using image path: \'"+image1_path+"\' and \'" + image2_path + "\' for comparison.[/bold yellow]")


img = Image.open(image1_path).convert("RGB")
color_array = np.array(img)

img2 = Image.open(image2_path).convert("RGB")
color_array2 = np.array(img2)


equivalentImages = np.array_equal(img, img2)

if not equivalentImages:
    print("[bold red]Image content not equal...[/bold red]")
else:
    print("[bold green]Image content equal![/bold green]")



subtractedImage = np.subtract(img, img2)

subtractedPIL = Image.fromarray(subtractedImage)
subtractedPIL.save("subtractionResult.png")