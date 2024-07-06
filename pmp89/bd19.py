import datetime
from lunarcalendar import Converter, Lunar, Solar
import collections


def diff(a: Lunar, b: Lunar) -> int | None:
    ma = a.month
    da = a.day
    mb = b.month
    db = b.day
    if ma == mb:
        return db - da    
    return None

def lunar_diff_57(y: int) -> list[int]:
    """get the lunar month and day delta btw a day in y (solar) and same
    day 57 solar years later."""

    d0 = datetime.date(y, 1, 1)
    hist = collections.defaultdict(int)
    for i in range(365):
        d = d0 + datetime.timedelta(days=i)
        l_birth = Converter.Solar2Lunar(Solar.from_date(d))
        l_later = Converter.Solar2Lunar(Solar(d.year + 57, d.month, d.day))
        print(d, "\t\tbirth lunar", f"{l_birth.month}-{l_birth.day}",
              "\t\tbday57 lunnar",  f"{l_later.month}-{l_later.day}")
        y = diff(l_birth, l_later)
        hist[y] += 1
    print(", ".join(f"{k}: {v}" for k, v in hist.items()))


if __name__ == "__main__":
    lunar_diff_57(1967)