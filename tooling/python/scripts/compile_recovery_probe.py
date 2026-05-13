from __future__ import annotations

import argparse
import json
import re
import shutil
import subprocess
import tempfile
from collections import Counter
from functools import lru_cache
from pathlib import Path

from project_roots import ROOT
from compile_recovery_support import EXTRA_STUB_OVERRIDES, KNOWN_ANNOTATION_FQCNS, NESTED_ENUM_NAMES

MAIN_SOURCES = [
    ROOT / 'pjb-core' / 'src' / 'main' / 'java',
    ROOT / 'pjb-api' / 'src' / 'main' / 'java',
]
TEST_SOURCES = [
    ROOT / 'pjb-core' / 'src' / 'test' / 'java',
    ROOT / 'pjb-api' / 'src' / 'test' / 'java',
]
REPORT_JSON = ROOT / 'docs' / 'reports' / 'compile_recovery_probe.json'
REPORT_MD = ROOT / 'docs' / 'reports' / 'compile_recovery_probe.md'

STUB_OVERRIDES = {
    'org/apache/pdfbox/pdmodel/PDPageContentStream.java': 'package org.apache.pdfbox.pdmodel; public class PDPageContentStream { public static enum AppendMode { APPEND; } }',
    'org/springframework/http/ResponseEntity.java': 'package org.springframework.http; public class ResponseEntity<T> { public static <T> ResponseEntity<T> ok(T body){return null;} public T getBody(){return null;} }',
    'org/springframework/http/HttpHeaders.java': 'package org.springframework.http; import java.util.ArrayList; import java.util.List; public class HttpHeaders { public void add(String name, String value){} public List<String> get(String name){return new ArrayList<>();} }',
    'org/springframework/data/domain/Page.java': 'package org.springframework.data.domain; import java.util.List; public interface Page<T> extends Iterable<T> { List<T> getContent(); }',
    'org/springframework/data/domain/Slice.java': 'package org.springframework.data.domain; import java.util.List; public interface Slice<T> extends Iterable<T> { List<T> getContent(); }',
    'org/springframework/data/jpa/repository/JpaRepository.java': 'package org.springframework.data.jpa.repository; public interface JpaRepository<T,ID> {}',
    'org/springframework/data/jpa/repository/JpaSpecificationExecutor.java': 'package org.springframework.data.jpa.repository; public interface JpaSpecificationExecutor<T> {}',
    'org/springframework/data/repository/CrudRepository.java': 'package org.springframework.data.repository; public interface CrudRepository<T,ID> {}',
    'org/springframework/data/repository/PagingAndSortingRepository.java': 'package org.springframework.data.repository; public interface PagingAndSortingRepository<T,ID> {}',
    'org/springframework/data/elasticsearch/repository/ElasticsearchRepository.java': 'package org.springframework.data.elasticsearch.repository; public interface ElasticsearchRepository<T,ID> {}',
    'org/springframework/beans/factory/ObjectProvider.java': 'package org.springframework.beans.factory; public interface ObjectProvider<T> { T getIfAvailable(); T getObject(); }',
    'org/springframework/util/MultiValueMap.java': 'package org.springframework.util; import java.util.List; import java.util.Map; public interface MultiValueMap<K,V> extends Map<K,List<V>> { void add(K key, V value); }',
    'org/springframework/web/socket/config/annotation/WebSocketConfigurer.java': 'package org.springframework.web.socket.config.annotation; public interface WebSocketConfigurer {}',
    'org/springframework/web/socket/config/annotation/EnableWebSocket.java': 'package org.springframework.web.socket.config.annotation; public @interface EnableWebSocket {}',
    'org/springframework/web/socket/config/annotation/WebSocketHandlerRegistry.java': 'package org.springframework.web.socket.config.annotation; public class WebSocketHandlerRegistry { public WebSocketHandlerRegistration addHandler(Object handler, String... paths){ return null; } }',
    'org/springframework/web/socket/config/annotation/WebSocketHandlerRegistration.java': 'package org.springframework.web.socket.config.annotation; public class WebSocketHandlerRegistration { public WebSocketHandlerRegistration setAllowedOrigins(String... origins){ return this; } }',
    'org/springframework/web/servlet/config/annotation/WebMvcConfigurer.java': 'package org.springframework.web.servlet.config.annotation; public interface WebMvcConfigurer {}',
    'org/springframework/web/socket/handler/TextWebSocketHandler.java': 'package org.springframework.web.socket.handler; public class TextWebSocketHandler {}',
    'org/springframework/web/filter/OncePerRequestFilter.java': 'package org.springframework.web.filter; public class OncePerRequestFilter {}',
    'org/springframework/kafka/core/KafkaTemplate.java': 'package org.springframework.kafka.core; public class KafkaTemplate<K,V> { public void send(String topic, V value){} }',
    'org/springframework/data/jpa/domain/support/AuditingEntityListener.java': 'package org.springframework.data.jpa.domain.support; public class AuditingEntityListener {}',
    'jakarta/annotation/PostConstruct.java': 'package jakarta.annotation; public @interface PostConstruct {}',
    'jakarta/annotation/PreDestroy.java': 'package jakarta.annotation; public @interface PreDestroy {}',
    'jakarta/transaction/Transactional.java': 'package jakarta.transaction; public @interface Transactional {}',
    'jakarta/servlet/FilterChain.java': 'package jakarta.servlet; public interface FilterChain {}',
    'jakarta/servlet/ServletException.java': 'package jakarta.servlet; public class ServletException extends Exception { public ServletException(){} public ServletException(String m){super(m);} }',
    'jakarta/servlet/http/HttpServletRequest.java': 'package jakarta.servlet.http; public interface HttpServletRequest { String getRequestURI(); String getMethod(); String getHeader(String name); Cookie[] getCookies(); Object getAttribute(String name); void setAttribute(String name, Object value); }',
    'jakarta/servlet/http/HttpServletResponse.java': 'package jakarta.servlet.http; public interface HttpServletResponse { void setHeader(String name, String value); void addHeader(String name, String value); void setStatus(int sc); int getStatus(); }',
    'jakarta/servlet/http/Cookie.java': 'package jakarta.servlet.http; public class Cookie { public Cookie(String name, String value){} public String getName(){return null;} public String getValue(){return null;} public void setHttpOnly(boolean flag){} public void setPath(String path){} public void setSecure(boolean flag){} public void setMaxAge(int expiry){} }',
    'jakarta/validation/Valid.java': 'package jakarta.validation; public @interface Valid {}',
    'jakarta/validation/constraints/NotBlank.java': 'package jakarta.validation.constraints; public @interface NotBlank { String message() default ""; Class<?>[] groups() default {}; Class<?>[] payload() default {}; }',
    'jakarta/validation/constraints/NotNull.java': 'package jakarta.validation.constraints; public @interface NotNull { String message() default ""; Class<?>[] groups() default {}; Class<?>[] payload() default {}; }',
    'jakarta/validation/constraints/Size.java': 'package jakarta.validation.constraints; public @interface Size { int min() default 0; int max() default Integer.MAX_VALUE; String message() default ""; Class<?>[] groups() default {}; Class<?>[] payload() default {}; }',
    'jakarta/validation/constraints/Email.java': 'package jakarta.validation.constraints; public @interface Email { String message() default ""; Class<?>[] groups() default {}; Class<?>[] payload() default {}; }',
    'jakarta/validation/constraints/Positive.java': 'package jakarta.validation.constraints; public @interface Positive { String message() default ""; Class<?>[] groups() default {}; Class<?>[] payload() default {}; }',
    'jakarta/persistence/Entity.java': 'package jakarta.persistence; public @interface Entity { String name() default ""; }',
    'jakarta/persistence/Table.java': 'package jakarta.persistence; public @interface Table { String name() default ""; UniqueConstraint[] uniqueConstraints() default {}; Index[] indexes() default {}; }',
    'jakarta/persistence/UniqueConstraint.java': 'package jakarta.persistence; public @interface UniqueConstraint { String name() default ""; String[] columnNames() default {}; }',
    'jakarta/persistence/Index.java': 'package jakarta.persistence; public @interface Index { String name() default ""; String columnList() default ""; boolean unique() default false; }',
    'jakarta/persistence/Column.java': 'package jakarta.persistence; public @interface Column { String name() default ""; boolean nullable() default true; boolean unique() default false; int length() default 255; String columnDefinition() default ""; boolean updatable() default true; boolean insertable() default true; int precision() default 0; int scale() default 0; }',
    'jakarta/persistence/Id.java': 'package jakarta.persistence; public @interface Id {}',
    'jakarta/persistence/GeneratedValue.java': 'package jakarta.persistence; public @interface GeneratedValue { GenerationType strategy() default GenerationType.AUTO; String generator() default ""; }',
    'jakarta/persistence/GenerationType.java': 'package jakarta.persistence; public enum GenerationType { AUTO, IDENTITY, SEQUENCE, TABLE }',
    'jakarta/persistence/EnumType.java': 'package jakarta.persistence; public enum EnumType { ORDINAL, STRING }',
    'jakarta/persistence/Enumerated.java': 'package jakarta.persistence; public @interface Enumerated { EnumType value() default EnumType.ORDINAL; }',
    'jakarta/persistence/FetchType.java': 'package jakarta.persistence; public enum FetchType { EAGER, LAZY }',
    'jakarta/persistence/CascadeType.java': 'package jakarta.persistence; public enum CascadeType { ALL, PERSIST, MERGE, REMOVE, REFRESH, DETACH }',
    'jakarta/persistence/ManyToOne.java': 'package jakarta.persistence; public @interface ManyToOne { FetchType fetch() default FetchType.EAGER; boolean optional() default true; CascadeType[] cascade() default {}; }',
    'jakarta/persistence/OneToMany.java': 'package jakarta.persistence; public @interface OneToMany { String mappedBy() default ""; FetchType fetch() default FetchType.LAZY; CascadeType[] cascade() default {}; boolean orphanRemoval() default false; }',
    'jakarta/persistence/OneToOne.java': 'package jakarta.persistence; public @interface OneToOne { String mappedBy() default ""; FetchType fetch() default FetchType.EAGER; CascadeType[] cascade() default {}; boolean orphanRemoval() default false; boolean optional() default true; }',
    'jakarta/persistence/ManyToMany.java': 'package jakarta.persistence; public @interface ManyToMany { String mappedBy() default ""; FetchType fetch() default FetchType.LAZY; CascadeType[] cascade() default {}; }',
    'jakarta/persistence/JoinColumn.java': 'package jakarta.persistence; public @interface JoinColumn { String name() default ""; String referencedColumnName() default ""; boolean nullable() default true; boolean unique() default false; boolean updatable() default true; boolean insertable() default true; ForeignKey foreignKey() default @ForeignKey; }',
    'jakarta/persistence/JoinTable.java': 'package jakarta.persistence; public @interface JoinTable { String name() default ""; JoinColumn[] joinColumns() default {}; JoinColumn[] inverseJoinColumns() default {}; }',
    'jakarta/persistence/Transient.java': 'package jakarta.persistence; public @interface Transient {}',
    'jakarta/persistence/EntityListeners.java': 'package jakarta.persistence; public @interface EntityListeners { Class<?>[] value(); }',
    'jakarta/persistence/MappedSuperclass.java': 'package jakarta.persistence; public @interface MappedSuperclass {}',
    'jakarta/persistence/Embeddable.java': 'package jakarta.persistence; public @interface Embeddable {}',
    'jakarta/persistence/Embedded.java': 'package jakarta.persistence; public @interface Embedded {}',
    'jakarta/persistence/EmbeddedId.java': 'package jakarta.persistence; public @interface EmbeddedId {}',
    'jakarta/persistence/MapsId.java': 'package jakarta.persistence; public @interface MapsId { String value() default ""; }',
    'jakarta/persistence/OrderBy.java': 'package jakarta.persistence; public @interface OrderBy { String value() default ""; }',
    'jakarta/persistence/Lob.java': 'package jakarta.persistence; public @interface Lob {}',
    'jakarta/persistence/Version.java': 'package jakarta.persistence; public @interface Version {}',
    'jakarta/persistence/PrePersist.java': 'package jakarta.persistence; public @interface PrePersist {}',
    'jakarta/persistence/PreUpdate.java': 'package jakarta.persistence; public @interface PreUpdate {}',
    'jakarta/persistence/SequenceGenerator.java': 'package jakarta.persistence; public @interface SequenceGenerator { String name(); String sequenceName() default ""; int allocationSize() default 50; }',
    'jakarta/persistence/Inheritance.java': 'package jakarta.persistence; public @interface Inheritance { InheritanceType strategy() default InheritanceType.SINGLE_TABLE; }',
    'jakarta/persistence/InheritanceType.java': 'package jakarta.persistence; public enum InheritanceType { SINGLE_TABLE, TABLE_PER_CLASS, JOINED }',
    'jakarta/persistence/DiscriminatorColumn.java': 'package jakarta.persistence; public @interface DiscriminatorColumn { String name() default ""; }',
    'jakarta/persistence/DiscriminatorValue.java': 'package jakarta.persistence; public @interface DiscriminatorValue { String value(); }',
    'jakarta/persistence/Temporal.java': 'package jakarta.persistence; public @interface Temporal { TemporalType value(); }',
    'jakarta/persistence/TemporalType.java': 'package jakarta.persistence; public enum TemporalType { DATE, TIME, TIMESTAMP }',
    'jakarta/persistence/CollectionTable.java': 'package jakarta.persistence; public @interface CollectionTable { String name() default ""; JoinColumn[] joinColumns() default {}; }',
    'jakarta/persistence/ElementCollection.java': 'package jakarta.persistence; public @interface ElementCollection { FetchType fetch() default FetchType.LAZY; Class<?> targetClass() default void.class; }',
    'jakarta/persistence/QueryHint.java': 'package jakarta.persistence; public @interface QueryHint { String name(); String value(); }',
    'jakarta/persistence/ForeignKey.java': 'package jakarta.persistence; public @interface ForeignKey { String name() default ""; }',
    'jakarta/persistence/LockModeType.java': 'package jakarta.persistence; public enum LockModeType { NONE, READ, WRITE, OPTIMISTIC, PESSIMISTIC_WRITE }',
    'jakarta/persistence/EntityManager.java': 'package jakarta.persistence; public interface EntityManager { void clear(); void flush(); }',
    'jakarta/persistence/EntityNotFoundException.java': 'package jakarta.persistence; public class EntityNotFoundException extends RuntimeException { public EntityNotFoundException(){} public EntityNotFoundException(String m){super(m);} }',
    'jakarta/persistence/Convert.java': 'package jakarta.persistence; public @interface Convert { Class<?> converter(); boolean disableConversion() default false; String attributeName() default ""; }',
}
STUB_OVERRIDES.update(EXTRA_STUB_OVERRIDES)

INTERFACE_NAMES = {
    'JpaRepository', 'CrudRepository', 'PagingAndSortingRepository', 'UserDetails', 'GrantedAuthority',
    'PasswordEncoder', 'Authentication', 'Filter', 'ServletRequest', 'ServletResponse', 'MessageSource',
    'Converter', 'HandlerInterceptor', 'WebMvcConfigurer', 'WebSocketConfigurer', 'CommandLineRunner',
    'Specification', 'Page', 'Slice', 'Sort', 'AuthenticationProvider', 'ApplicationContextAware',
    'InitializingBean', 'DisposableBean', 'AuditorAware', 'BeanPostProcessor', 'ImportOption',
    'JpaSpecificationExecutor', 'ElasticsearchRepository', 'ObjectProvider', 'MultiValueMap',
}
ENUM_NAMES = {
    'HttpStatus', 'RequestMethod', 'Isolation', 'Propagation', 'CascadeType', 'FetchType', 'EnumType',
    'TemporalType', 'GenerationType', 'InheritanceType', 'LockModeType',
}
SOURCE_ROOTS = MAIN_SOURCES + TEST_SOURCES
IMPORT_PATTERN = re.compile(r'^import\s+([^;]+);', re.M)
ANNOTATION_PATTERN = re.compile(r'@([A-Z][A-Za-z0-9_]*)\b')
IMPLEMENTS_PATTERN = re.compile(r'implements\s+([^\{]+)')
ERROR_PATTERN = re.compile(r'^(?P<file>[^:]+\.java):(?P<line>\d+): error: (?P<message>.+)$')
MISSING_PKG_PATTERN = re.compile(r'package ([\w.]+) does not exist')
MISSING_SYMBOL_PATTERN = re.compile(r'cannot find symbol')
TYPE_SYMBOL_PATTERN = re.compile(r'\b(class|interface|record|enum)\s+([A-Z][A-Za-z0-9_]*)\b')
NESTED_SYMBOL_PATTERN = re.compile(r'\b([A-Z][A-Za-z0-9_]*)\.([A-Z][A-Za-z0-9_]*)\b')
LOMBOK_IMPORT_PATTERN = re.compile(r'^import\s+lombok\.(\w+);', re.M)
PACKAGE_PATTERN = re.compile(r'^package\s+([\w.]+);', re.M)
SAME_PACKAGE_SYMBOL_PATTERN = re.compile(r'symbol:\s+(?:class|interface|record|enum)\s+([A-Z][A-Za-z0-9_]*)')


@lru_cache(maxsize=None)
def relative_java_path(raw_path: str) -> str:
    path = Path(raw_path).resolve()
    try:
        return str(path.relative_to(ROOT)).replace('\\', '/')
    except ValueError:
        return raw_path


@lru_cache(maxsize=None)
def file_capabilities(raw_path: str) -> dict[str, bool]:
    path = Path(raw_path)
    if not path.exists():
        return {
            'hasGetter': False,
            'hasSetter': False,
            'hasBuilder': False,
            'hasSlf4j': False,
        }
    text = path.read_text(encoding='utf-8', errors='ignore')
    lombok_imports = set(LOMBOK_IMPORT_PATTERN.findall(text))
    return {
        'hasGetter': 'Getter' in lombok_imports or '@Getter' in text,
        'hasSetter': 'Setter' in lombok_imports or '@Setter' in text,
        'hasBuilder': 'Builder' in lombok_imports or '@Builder' in text,
        'hasSlf4j': 'Slf4j' in lombok_imports or '@Slf4j' in text,
    }


@lru_cache(maxsize=None)
def file_package(raw_path: str) -> str | None:
    path = Path(raw_path)
    if not path.exists():
        return None
    text = path.read_text(encoding='utf-8', errors='ignore')
    match = PACKAGE_PATTERN.search(text)
    return match.group(1) if match else None


@lru_cache(maxsize=None)
def same_package_symbol_exists(raw_path: str, symbol_name: str) -> bool:
    path = Path(raw_path)
    if not path.exists() or not symbol_name:
        return False
    package_name = file_package(raw_path)
    if not package_name:
        return False
    symbol_file = path.with_name(f'{symbol_name}.java')
    if symbol_file.exists():
        sibling_text = symbol_file.read_text(encoding='utf-8', errors='ignore')
        sibling_package = PACKAGE_PATTERN.search(sibling_text)
        if sibling_package and sibling_package.group(1) == package_name:
            return True
    return False


def java_files(roots: list[Path]) -> list[Path]:
    files: list[Path] = []
    for root in roots:
        if root.exists():
            files.extend(sorted(root.rglob('*.java')))
    return files


def scan_usage(files: list[Path]) -> tuple[set[str], set[str]]:
    annotations: set[str] = set()
    interfaces: set[str] = set()
    for path in files:
        text = path.read_text(encoding='utf-8', errors='ignore')
        annotations.update(ANNOTATION_PATTERN.findall(text))
        for clause in IMPLEMENTS_PATTERN.findall(text):
            for token in re.findall(r'\b([A-Z][A-Za-z0-9_]*)\b', clause):
                interfaces.add(token)
    return annotations, interfaces


def external_imports(files: list[Path]) -> set[str]:
    imports: set[str] = set()
    for path in files:
        text = path.read_text(encoding='utf-8', errors='ignore')
        for item in IMPORT_PATTERN.findall(text):
            if item.startswith('static ') or item.endswith('.*'):
                continue
            if item.startswith(('java.', 'javax.', 'com.tcc.')):
                continue
            imports.add(item)
    return imports


def nested_import_map(imports: set[str]) -> dict[str, set[str]]:
    nested: dict[str, set[str]] = {}
    for item in imports:
        parts = item.split('.')
        upper_positions = [index for index, part in enumerate(parts) if part and part[0].isupper()]
        if len(upper_positions) < 2:
            continue
        outer_index = upper_positions[0]
        outer_fqcn = '.'.join(parts[:outer_index + 1])
        nested_name = parts[-1]
        nested.setdefault(outer_fqcn, set()).add(nested_name)
    return nested


def normalize_imports(imports: set[str]) -> set[str]:
    normalized = set(imports)
    for item in imports:
        parts = item.split('.')
        upper_positions = [index for index, part in enumerate(parts) if part and part[0].isupper()]
        if len(upper_positions) >= 2:
            normalized.discard(item)
            normalized.add('.'.join(parts[:upper_positions[0] + 1]))
    return normalized


def kind_for_import(fqcn: str, simple_name: str, annotations: set[str], interfaces: set[str]) -> str:
    if fqcn in KNOWN_ANNOTATION_FQCNS or simple_name in annotations:
        return 'annotation'
    if simple_name in INTERFACE_NAMES or simple_name in interfaces:
        return 'interface'
    if simple_name in ENUM_NAMES:
        return 'enum'
    return 'class'


def write_stub(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content + '\n', encoding='utf-8')


def build_stub_tree(temp_root: Path, imports: set[str], annotations: set[str], interfaces: set[str]) -> tuple[Path, int]:
    stub_root = temp_root / 'stubs'
    created = 0
    handled = set()
    normalized_imports = normalize_imports(imports)
    nested_imports = nested_import_map(imports)
    for rel, content in STUB_OVERRIDES.items():
        write_stub(stub_root / rel, content)
        handled.add(rel.replace('/', '.').removesuffix('.java'))
        created += 1
    for outer_fqcn, nested_names in sorted(nested_imports.items()):
        if outer_fqcn in handled:
            continue
        parts = outer_fqcn.split('.')
        pkg = '.'.join(parts[:-1])
        outer_name = parts[-1]
        path = stub_root.joinpath(*parts[:-1], f'{outer_name}.java')
        if path.exists():
            continue
        nested_members = []
        for nested_name in sorted(nested_names):
            if nested_name in NESTED_ENUM_NAMES:
                nested_members.append(f'public static enum {nested_name} {{ VALUE }}')
            else:
                nested_members.append(f'public static class {nested_name} {{}}')
        content = f'package {pkg}; public class {outer_name} {{ {' '.join(nested_members)} }}'
        write_stub(path, content)
        handled.add(outer_fqcn)
        created += 1
    for item in sorted(normalized_imports):
        parts = item.split('.')
        if len(parts) < 2:
            continue
        fqcn = '.'.join(parts)
        if fqcn in handled:
            continue
        name = parts[-1]
        pkg = '.'.join(parts[:-1])
        path = stub_root.joinpath(*parts[:-1], f'{name}.java')
        if path.exists():
            continue
        kind = kind_for_import(fqcn, name, annotations, interfaces)
        if kind == 'annotation':
            content = f'package {pkg}; public @interface {name} {{ String value() default ""; String[] path() default {{}}; Class<?>[] classes() default {{}}; boolean required() default true; String[] name() default {{}}; String prefix() default ""; String havingValue() default ""; boolean matchIfMissing() default false; }}'
        elif kind == 'interface':
            content = f'package {pkg}; public interface {name} {{}}'
        elif kind == 'enum':
            content = f'package {pkg}; public enum {name} {{ VALUE }}'
        else:
            content = f'package {pkg}; public class {name} {{}}'
        write_stub(path, content)
        created += 1
    return stub_root, created


def compile_java(source_files: list[Path], classpath: list[Path], destination: Path, error_limit: int = 250) -> subprocess.CompletedProcess[str]:
    args = [
        'javac',
        '--release',
        '21',
        '-Xmaxerrs',
        str(error_limit),
        '-cp',
        ':'.join(str(item) for item in classpath if item),
        '-d',
        str(destination),
    ] + [str(path) for path in source_files]
    return subprocess.run(args, text=True, capture_output=True, check=False)


def classify_missing_symbol(raw_path: str, context_lines: list[str]) -> str:
    capabilities = file_capabilities(raw_path)
    context = '\n'.join(context_lines)
    if capabilities['hasBuilder'] and 'symbol:   method builder()' in context:
        return 'likely-lombok-builder'
    if capabilities['hasGetter'] and re.search(r'symbol:\s+method\s+(get|is)[A-Z][A-Za-z0-9_]*\(', context):
        return 'likely-lombok-getter'
    if capabilities['hasSetter'] and re.search(r'symbol:\s+method\s+set[A-Z][A-Za-z0-9_]*\(', context):
        return 'likely-lombok-setter'
    if capabilities['hasSlf4j'] and ('symbol:   variable log' in context or 'symbol:   method getLogger' in context):
        return 'likely-lombok-slf4j'
    if 'fixedDelayString' in context or 'autoComplete' in context or 'parameters' in context or 'condition' in context:
        return 'framework-annotation-shape'
    match = SAME_PACKAGE_SYMBOL_PATTERN.search(context)
    if match and same_package_symbol_exists(raw_path, match.group(1)):
        return 'same-package-symbol-candidate'
    return 'missing-symbol'


def classify_errors(stderr: str) -> dict[str, object]:
    lines = [line for line in stderr.splitlines() if line.strip()]
    bucket_counts: Counter[str] = Counter()
    missing_packages: Counter[str] = Counter()
    touched_files: Counter[str] = Counter()
    samples: list[dict[str, object]] = []
    current: dict[str, object] | None = None
    for line in lines:
        match = ERROR_PATTERN.match(line)
        if match:
            message = match.group('message')
            raw_path = match.group('file')
            relative_path = relative_java_path(raw_path)
            if MISSING_PKG_PATTERN.search(message):
                bucket = 'missing-package'
                missing_packages[MISSING_PKG_PATTERN.search(message).group(1)] += 1
            elif MISSING_SYMBOL_PATTERN.search(message):
                bucket = 'missing-symbol'
            elif 'does not take parameters' in message:
                bucket = 'generic-shape-mismatch'
            elif 'no interface expected here' in message:
                bucket = 'inheritance-shape-mismatch'
            else:
                bucket = 'other'
            bucket_counts[bucket] += 1
            touched_files[relative_path] += 1
            current = {
                'file': relative_path,
                'line': int(match.group('line')),
                'message': message,
                'bucket': bucket,
                'rawFile': raw_path,
            }
            samples.append(current)
            continue
        if current is not None and len(samples) <= 40:
            current.setdefault('context', []).append(line)
            if current.get('bucket') == 'missing-symbol':
                new_bucket = classify_missing_symbol(current['rawFile'], current.get('context', []))
                if new_bucket != 'missing-symbol':
                    bucket_counts['missing-symbol'] -= 1
                    if bucket_counts['missing-symbol'] <= 0:
                        bucket_counts.pop('missing-symbol', None)
                    bucket_counts[new_bucket] += 1
                    current['bucket'] = new_bucket
    for sample in samples:
        sample.pop('rawFile', None)
    return {
        'lineCount': len(lines),
        'errorCount': sum(bucket_counts.values()),
        'bucketCounts': dict(sorted(bucket_counts.items())),
        'missingPackages': dict(missing_packages.most_common(25)),
        'touchedFiles': dict(touched_files.most_common(25)),
        'samples': samples[:40],
    }


def parse_args() -> argparse.Namespace:
    parser = argparse.ArgumentParser(description='Heurística auxiliar para recuperação de compile do PJB.')
    parser.add_argument('--paths-file', help='Arquivo UTF-8 com uma lista de arquivos Java relativos à raiz, um por linha.')
    parser.add_argument('--limit-files', type=int, help='Limita a quantidade final de arquivos main analisados.')
    parser.add_argument('--report-suffix', help='Sufixo do relatório gerado em docs/reports/.')
    parser.add_argument('--skip-stub-compile', action='store_true', help='Pula a compilação dos stubs para acelerar a sonda.')
    return parser.parse_args()


def report_paths(suffix: str | None) -> tuple[Path, Path]:
    if not suffix:
        return REPORT_JSON, REPORT_MD
    safe_suffix = re.sub(r'[^a-zA-Z0-9._-]+', '-', suffix.strip()).strip('-')
    if not safe_suffix:
        return REPORT_JSON, REPORT_MD
    return (
        REPORT_JSON.with_name(f'{REPORT_JSON.stem}_{safe_suffix}{REPORT_JSON.suffix}'),
        REPORT_MD.with_name(f'{REPORT_MD.stem}_{safe_suffix}{REPORT_MD.suffix}'),
    )


def selected_main_files(paths_file: str | None, limit_files: int | None) -> list[Path]:
    main_files = java_files(MAIN_SOURCES)
    if paths_file:
        selected: list[Path] = []
        for raw_line in Path(paths_file).read_text(encoding='utf-8').splitlines():
            line = raw_line.strip()
            if not line or line.startswith('#'):
                continue
            candidate = (ROOT / line).resolve()
            if candidate.exists() and candidate.suffix == '.java':
                selected.append(candidate)
        main_files = selected
    if limit_files is not None:
        main_files = main_files[:max(limit_files, 0)]
    return main_files


def resolve_internal_import_path(fqcn: str) -> Path | None:
    parts = fqcn.split('.')
    for end in range(len(parts), 0, -1):
        relative = Path(*parts[:end]).with_suffix('.java')
        for root in MAIN_SOURCES:
            candidate = root / relative
            if candidate.exists():
                return candidate
    return None


def expand_internal_dependency_closure(seed_files: list[Path]) -> list[Path]:
    visited = {path.resolve() for path in seed_files}
    queue = list(visited)
    while queue:
        current = queue.pop()
        for sibling in sorted(current.parent.glob('*.java')):
            sibling_resolved = sibling.resolve()
            if sibling_resolved not in visited:
                visited.add(sibling_resolved)
                queue.append(sibling_resolved)
        text = current.read_text(encoding='utf-8', errors='ignore')
        for item in IMPORT_PATTERN.findall(text):
            if item.startswith('static ') or not item.startswith('com.tcc.'):
                continue
            candidate = resolve_internal_import_path(item)
            if candidate is None:
                continue
            resolved = candidate.resolve()
            if resolved in visited:
                continue
            visited.add(resolved)
            queue.append(resolved)
    return sorted(visited)


def render_report_markdown(report: dict[str, object]) -> str:
    lines = [
        '# Compile Recovery Probe',
        '',
        f"- Modo: **{report['mode']}**",
        f"- Arquivos seed analisados: **{report.get('seedMainSourceFiles', report['mainSourceFiles'])}**",
        f"- Arquivos main analisados após fechamento interno: **{report['mainSourceFiles']}**",
        f"- Arquivos Java totais varridos: **{report['allJavaFilesScanned']}**",
        f"- Imports externos detectados: **{report['externalImportCount']}**",
        f"- Stubs transitórios gerados: **{report['generatedStubFiles']}**",
        '',
        '## Resultado da compilação auxiliar dos stubs',
        '',
        f"- Erros detectados: **{report['stubCompile']['errorCount']}**",
        '',
        '## Resultado da compilação auxiliar do main',
        '',
        f"- Erros detectados: **{report['mainCompile']['errorCount']}**",
        '',
        '### Buckets',
        '',
    ]
    for bucket, count in report['mainCompile']['bucketCounts'].items():
        lines.append(f'- `{bucket}` -> {count}')
    lines.extend(['', '### Pacotes externos ainda bloqueando a probe', ''])
    if report['mainCompile']['missingPackages']:
        for pkg, count in report['mainCompile']['missingPackages'].items():
            lines.append(f'- `{pkg}` -> {count}')
    else:
        lines.append('- Nenhum pacote externo ausente detectado nesta execução.')
    lines.extend(['', '### Arquivos mais tocados pelos erros', ''])
    if report['mainCompile']['touchedFiles']:
        for path, count in report['mainCompile']['touchedFiles'].items():
            lines.append(f'- `{path}` -> {count}')
    else:
        lines.append('- Nenhum arquivo do main apareceu na saída de erro da probe.')
    lines.extend(['', '### Amostra inicial de erros', ''])
    if report['mainCompile']['samples']:
        for sample in report['mainCompile']['samples'][:15]:
            lines.append(f"- `{sample['file']}:{sample['line']}` · `{sample['bucket']}` · {sample['message']}")
    else:
        lines.append('- Nenhuma amostra disponível.')
    lines.extend(['', '## Notas', ''])
    for note in report['notes']:
        lines.append(f'- {note}')
    return '\n'.join(lines) + '\n'


def main() -> int:
    args = parse_args()
    report_json, report_md = report_paths(args.report_suffix)
    if shutil.which('javac') is None:
        report_json.parent.mkdir(parents=True, exist_ok=True)
        report = {
            'status': 'javac-unavailable',
            'message': 'javac não encontrado no ambiente. Probe não executada.',
        }
        report_json.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
        report_md.write_text('# Compile Recovery Probe\n\n- javac não encontrado no ambiente.\n', encoding='utf-8')
        return 0

    source_files = java_files(SOURCE_ROOTS)
    main_files = selected_main_files(args.paths_file, args.limit_files)
    seed_count = len(main_files)
    if args.paths_file:
        main_files = expand_internal_dependency_closure(main_files)
    annotations, interfaces = scan_usage(source_files)
    imports = external_imports(source_files)

    with tempfile.TemporaryDirectory(prefix='pjb-compile-recovery-') as temp_dir:
        temp_root = Path(temp_dir)
        stub_root, stub_count = build_stub_tree(temp_root, imports, annotations, interfaces)
        stub_classes = temp_root / 'stub-classes'
        main_classes = temp_root / 'main-classes'
        stub_classes.mkdir(parents=True, exist_ok=True)
        main_classes.mkdir(parents=True, exist_ok=True)
        stub_files = sorted(stub_root.rglob('*.java'))
        if args.skip_stub_compile:
            stub_result = subprocess.CompletedProcess(args=['javac'], returncode=0, stdout='', stderr='')
        else:
            stub_result = compile_java(stub_files, [], stub_classes, error_limit=50)
        main_result = compile_java(main_files, [stub_classes], main_classes, error_limit=250)

    mode = 'targeted' if args.paths_file or args.limit_files is not None else 'full'
    report = {
        'status': 'executed',
        'mode': mode,
        'seedMainSourceFiles': seed_count,
        'javaVersionProbe': subprocess.run(['java', '-version'], text=True, capture_output=True, check=False).stderr.splitlines()[:1],
        'mainSourceFiles': len(main_files),
        'allJavaFilesScanned': len(source_files),
        'externalImportCount': len(imports),
        'generatedStubFiles': stub_count,
        'stubCompile': classify_errors(stub_result.stderr),
        'mainCompile': classify_errors(main_result.stderr),
        'notes': [
            'Probe heurística usada para recuperação quando o Maven Wrapper não consegue baixar o Maven no ambiente.',
            'Os stubs são transitórios e existem apenas durante a execução da probe.',
            'O objetivo é separar bloqueio de classpath externo de possível drift interno do repositório.',
            'A classificação também marca same-package-symbol-candidate quando o símbolo ausente parece existir como tipo irmão no mesmo pacote, reduzindo falso positivo de fechamento parcial do lote.',
        ],
    }
    if args.paths_file:
        report['pathsFile'] = args.paths_file
    if args.limit_files is not None:
        report['limitFiles'] = args.limit_files
    report_json.parent.mkdir(parents=True, exist_ok=True)
    report_json.write_text(json.dumps(report, ensure_ascii=False, indent=2), encoding='utf-8')
    report_md.write_text(render_report_markdown(report), encoding='utf-8')
    return 0


if __name__ == '__main__':
    raise SystemExit(main())
