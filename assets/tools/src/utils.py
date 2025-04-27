import contextlib
import time


@contextlib.contextmanager
def timed_step(name: str):
    t = time.perf_counter()
    print(name, end="")
    yield
    print(f" {time.perf_counter() - t:.2f}s")