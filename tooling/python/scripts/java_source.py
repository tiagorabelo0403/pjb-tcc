from __future__ import annotations

import re

TEXT_BLOCK = re.compile(r'"""(?:\\.|[^\\])*?"""', re.DOTALL)
BLOCK_COMMENT = re.compile(r'/\*.*?\*/', re.DOTALL)
LINE_COMMENT = re.compile(r'//[^\n]*')
STRING_LITERAL = re.compile(r'"(?:\\.|[^"\\\n])*"')
CHAR_LITERAL = re.compile(r"'(?:\\.|[^'\\\n])'")


def strip_comments_and_strings(text: str) -> str:
    without_text_blocks = TEXT_BLOCK.sub('""', text)
    without_block = BLOCK_COMMENT.sub('', without_text_blocks)
    without_line = LINE_COMMENT.sub('', without_block)
    without_strings = STRING_LITERAL.sub('""', without_line)
    return CHAR_LITERAL.sub("' '", without_strings)


def matching_brace(text: str, open_index: int) -> int:
    depth = 0
    index = open_index
    while index < len(text):
        char = text[index]
        if char == '{':
            depth += 1
        elif char == '}':
            depth -= 1
            if depth == 0:
                return index
        index += 1
    return -1


def matching_paren(text: str, open_index: int) -> int:
    depth = 0
    index = open_index
    while index < len(text):
        char = text[index]
        if char == '(':
            depth += 1
        elif char == ')':
            depth -= 1
            if depth == 0:
                return index
        index += 1
    return -1


def split_parameters(signature: str) -> list[str]:
    parameters = []
    depth = 0
    current = ''
    for char in signature:
        if char in '<(':
            depth += 1
        elif char in '>)':
            depth -= 1
        elif char == ',' and depth == 0:
            if current.strip():
                parameters.append(current.strip())
            current = ''
            continue
        current += char
    if current.strip():
        parameters.append(current.strip())
    return parameters


def count_parameters(signature: str) -> int:
    return len(split_parameters(signature))
