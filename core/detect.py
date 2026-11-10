"""CSV delimiter detection module.

Detects delimiter among comma (,), semicolon (;), tab (\\t), and pipe (|)
by scoring frequency and line-to-line consistency across sample lines.
"""

from typing import Sequence

CANDIDATE_DELIMITERS: Sequence[str] = (",", ";", "\t", "|")


def _count_delimiters_in_line(line: str, delimiter: str) -> int:
    """Count occurrences of delimiter in line, ignoring sections enclosed in double quotes."""
    count = 0
    in_quotes = False
    for char in line:
        if char == '"':
            in_quotes = not in_quotes
        elif char == delimiter and not in_quotes:
            count += 1
    return count


def detect_delimiter(raw_bytes: bytes) -> str:
    """Detect delimiter from raw CSV bytes.

    Tests candidate delimiters (',', ';', '\\t', '|').
    Reads the first 10 lines of the bytes, counts occurrences outside quoted strings,
    and scores candidates by consistency and frequency.
    Falls back to ',' if undecided.
    """
    if not raw_bytes:
        return ","

    try:
        sample_text = raw_bytes[:65536].decode("utf-8", errors="ignore")
    except Exception:
        return ","

    lines = [line for line in sample_text.splitlines() if line.strip()][:10]
    if not lines:
        return ","

    best_delimiter = ","
    best_score = -1.0

    for delim in CANDIDATE_DELIMITERS:
        counts = [_count_delimiters_in_line(line, delim) for line in lines]
        total = sum(counts)
        if total == 0:
            continue

        num_lines = len(counts)
        mean = total / num_lines
        variance = sum((c - mean) ** 2 for c in counts) / num_lines

        # Consistency boost when every line has identical count > 0
        if variance == 0 and counts[0] > 0:
            score = total * 10.0
        else:
            score = total / (1.0 + variance)

        if score > best_score:
            best_score = score
            best_delimiter = delim

    return best_delimiter
