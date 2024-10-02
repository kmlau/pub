from collections.abc import Mapping, Sequence


class FF:
    def __init__(self, targets: Sequence[str]) -> None:
        self._targets = targets
        self._state = False

    def process(self, src: str, pulse: bool) -> list[tuple[str, bool]]:
        """Process a pulse from node src.
        
        Returns:
            output pulses as tuples of (receiving node id, high/low)
        """
        if not pulse:
            self._state = not self._state
            return [(t, self._state) for t in self._targets]
        else:
            return []


class Conj:
    def __init__(self, targets: Sequence[str]) -> None:
        self._targets = targets
        # pulse state of input nodes.
        self._inputs: dict[str, bool] = {}
    
    def add_input(self, inp: str) -> None:
        self._inputs[inp] = False
    
    def process(self, src: str, pulse: bool) -> list[tuple[str, bool]]:
        """Process a pulse from node src.
        
        Returns:
            output pulses as tuples of (receiving node id, high/low)
        """
        self._inputs[src] = pulse
        out = not all(self._inputs.values())
        return [(t, out) for t in self._targets]


class Rx:
    def process(self, src: str, pulse: bool) -> list[tuple[str, bool]]:
        return []


class Button:
    def __init__(self, broadcasts: Sequence[str], g: Mapping[str, FF | Conj]) -> None:
        self._broardcasts = broadcasts
        self._g = g
        self._lows = 0
        self._highs = 0

    def press(self) -> None:
        """Simulate pressing the button once."""
        output_buffer: list[tuple[str, str, bool]] = []
        self._lows += 1 + len(self._broardcasts)
        for b in self._broardcasts:
            output_buffer.extend((b, t, s) for t, s in self._g[b].process("broadcaster", False))
        while output_buffer:
            h = sum(p for *_, p in output_buffer)
            self._highs += h
            self._lows += len(output_buffer) - h
            # pulse outputs for next round.
            buffer_next: list[tuple[str, str, bool]] = []
            for s, t, p in output_buffer:
                buffer_next.extend((t, t2, p2) for t2, p2 in self._g[t].process(s, p))
            output_buffer = buffer_next


def parse_input() -> Button:
    # a dict from node id to Node object
    g: dict[str, FF | Conj] = {"rx": Rx()}
    # set of ids of Conj nodes
    conj_names = set()
    broadcasts: list[str]
    # constrct g
    for l in _inputs.split("\n"):
        if not l:
            continue
        src, ts = l.split(" -> ")
        targets = ts.split(", ")
        if src[0] == "%":
            g[src[1:]] = FF(targets)
        elif src[0] == "&":
            g[src[1:]] = Conj(targets)
            conj_names.add(src[1:])
        elif src == "broadcaster":
            broadcasts = targets
    # set input node ids for Conj nodes
    for l in _inputs.split("\n"):
        if not l:
            continue
        src, ts = l.split(" -> ")
        for t in ts.split(", "):
            if t in conj_names:
                g[t].add_input(src[1:])
    return Button(broadcasts, g)


_inputs = r"""
%cd -> jx, gh
%bk -> jp, cn
%px -> xc, hg
%tv -> gh, xl
&xc -> bm, zq, jf, hg, bd, hn
%bd -> px
&bh -> mf
%dx -> cn, rb
%vv -> pp, gh
broadcaster -> cx, zq, tv, rh
%rb -> cn, qr
&jf -> mf
%jd -> mm
%cx -> xd, cn
%zs -> cz
%hn -> bm
%xr -> bd, xc
&mf -> rx
%zq -> kg, xc
&cn -> sh, jd, cx, tc, xd
%cs -> xj
%fb -> tc, cn
%mm -> cn, bk
%sq -> th, hz
%sz -> vx
%xl -> gh, sz
%vm -> gh, vv
%jp -> cn
%qr -> cn, jd
%bq -> xc, zv
&sh -> mf
%gz -> gs, hz
%qc -> qg, xc
%hg -> bq
%dt -> sq, hz
%xj -> fz
%qs -> gh
%fz -> hz, zs
%qg -> xc
%pp -> qs, gh
%zv -> xc, qc
%rh -> hz, mr
&gh -> tv, lk, sz, bh, vx
%th -> hz
&mz -> mf
%bm -> xr
%lk -> pg
%jx -> lk, gh
&hz -> xj, cs, zs, rh, mz
%tc -> dx
%mr -> hz, gz
%xd -> jk
%pg -> vm, gh
%kg -> hn, xc
%gs -> cs, hz
%vx -> cd
%cz -> hz, dt
%jk -> cn, fb
"""


def part1() -> None:
    b = parse_input()
    for _ in range(1000):
        b.press()
    print(b._highs * b._lows)


if __name__ == "__main__":
    part1()