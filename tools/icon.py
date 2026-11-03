"""Draws the mod icon: an open enchanting book whose left page is unreadable table glyphs
and whose right page lists the enchantments in plain lines.

  uv run --with pillow python -I tools/icon.py src/main/resources/assets/enchantreveal/icon.png
"""
import sys
from PIL import Image

N = 32
img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
px = img.load()


def hexc(h, a=255):
    h = h.lstrip("#")
    return (int(h[0:2], 16), int(h[2:4], 16), int(h[4:6], 16), a)


def rect(x0, y0, x1, y1, c):
    for y in range(y0, y1 + 1):
        for x in range(x0, x1 + 1):
            px[x, y] = c


OUT = hexc("120c1e")
BG = hexc("24173a"); BG_GLOW = [hexc("31204f"), hexc("41296c"), hexc("553591")]
COVER = hexc("8e2a2a"); COVER_D = hexc("5e1a1a")
PAGE = hexc("efe4c4"); PAGE_D = hexc("cbbd96"); SPINE = hexc("a8996e")
GLYPH = hexc("9a8fb0")
TEXT = [hexc("5a2fb0"), hexc("24734a"), hexc("a3361f")]
SPARK = hexc("e9d8ff"); SPARK_M = hexc("b48cff")

# Dark purple tile with rounded corners and a glow behind the book.
rect(1, 0, 30, 31, OUT); rect(0, 1, 31, 30, OUT)
rect(1, 1, 30, 30, BG)
cx, cy = 15.5, 18
for y in range(1, 31):
    for x in range(1, 31):
        d = abs(x - cx) + abs(y - cy) * 1.4
        if d <= 18:
            px[x, y] = BG_GLOW[min(2, int((18 - d) // 5))]

# Open book: cover peeking out under two pages that dip toward the spine.
rect(3, 12, 28, 26, COVER); rect(3, 25, 28, 26, COVER_D)
for x in range(4, 28):
    top = 11 if 14 <= x <= 17 else 10
    rect(x, top, x, 24, PAGE)
    px[x, 24] = PAGE_D
rect(15, 11, 16, 24, SPINE)
rect(4, 10, 4, 24, PAGE_D); rect(27, 10, 27, 24, PAGE_D)

# Left page: the table's unreadable glyphs.
glyphs = [(13, "x.xx.x.x"), (16, "xx.x.xx."), (19, "x.x.xx.x"), (22, "xx.x.x..")]
for y, row in glyphs:
    for i, ch in enumerate(row):
        if ch == "x":
            px[6 + i, y] = GLYPH
            if (i + y) % 3 == 0:
                px[6 + i, y - 1] = GLYPH

# Right page: three readable lines, one per enchantment ("Name III").
lines = [(13, "xxx.x.x.x"), (17, "xxxx.x.x"), (21, "xxxxx.x")]
for i, (y, row) in enumerate(lines):
    for j, ch in enumerate(row):
        if ch == "x":
            rect(18 + j, y, 18 + j, y + 1, TEXT[i])

# Enchanting glints above the book.
for sx, sy in [(9, 5), (23, 4), (16, 3)]:
    px[sx, sy] = SPARK
    for dx, dy in [(-1, 0), (1, 0), (0, -1), (0, 1)]:
        px[sx + dx, sy + dy] = SPARK_M
px[5, 9] = SPARK_M; px[27, 8] = SPARK_M

scale = int(sys.argv[2]) if len(sys.argv) > 2 else 4
img.resize((N * scale, N * scale), Image.NEAREST).save(sys.argv[1])
