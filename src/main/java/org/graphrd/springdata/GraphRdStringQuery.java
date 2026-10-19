package org.graphrd.springdata;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.projection.ProjectionFactory;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.query.QueryMethod;
import org.springframework.data.repository.query.RepositoryQuery;

import org.graphrd.rdf.RdfEntityManager;

/**
 * A {@link Query @Query} method: the SPARQL runs as written (parameters
 * bound as literals), the answer turns into the declared return type — a
 * record whose components match the SELECT's variable names, a list of them,
 * an {@code Optional} of one, {@code long} for a single number, or raw maps.
 */
class GraphRdStringQuery implements RepositoryQuery {

    // not preceded by a word character, so prefixed names (ex:Person) stay untouched
    private static final Pattern NAMED = Pattern.compile("(?<![\\w]):([a-zA-Z][a-zA-Z0-9_]*)");

    private final Method method;
    private final QueryMethod queryMethod;
    private final Query annotation;
    private final RdfEntityManager em;

    GraphRdStringQuery(Method method, RepositoryMetadata metadata,
                       ProjectionFactory projections, Query annotation, RdfEntityManager em) {
        this.method = method;
        this.queryMethod = new QueryMethod(method, metadata, projections);
        this.annotation = annotation;
        this.em = em;
    }

    @Override
    public Object execute(Object[] parameters) {
        String sparql = bind(annotation.value(), parameters);
        List<Map<String, String>> rows = em.select(sparql);

        Class<?> returns = method.getReturnType();
        Class<?> rowType = returns;
        boolean list = false;
        boolean optional = false;
        if (List.class.isAssignableFrom(returns)) {
            list = true;
            rowType = (Class<?>) ((java.lang.reflect.ParameterizedType) method.getGenericReturnType())
                    .getActualTypeArguments()[0];
        } else if (Optional.class == returns) {
            optional = true;
            rowType = (Class<?>) ((java.lang.reflect.ParameterizedType) method.getGenericReturnType())
                    .getActualTypeArguments()[0];
        }

        if ((returns == long.class || returns == Long.class)) {
            return rows.isEmpty() ? 0L : firstNumber(rows.get(0));
        }
        List<Object> mapped = new ArrayList<>();
        for (Map<String, String> row : rows) {
            mapped.add(mapRow(row, rowType));
        }
        if (list) {
            return mapped;
        }
        if (optional) {
            return mapped.stream().findFirst();
        }
        return mapped.isEmpty() ? null : mapped.get(0);
    }

    private static long firstNumber(Map<String, String> row) {
        return (Long) convert(row.values().iterator().next(), long.class);
    }

    /** :name placeholders become literals of the same-named parameters. */
    private String bind(String sparql, Object[] parameters) {
        Parameter[] names = method.getParameters();
        Map<String, Object> byName = new LinkedHashMap<>();
        for (int i = 0; i < names.length && i < parameters.length; i++) {
            byName.put(names[i].getName(), parameters[i]);
        }
        Matcher m = NAMED.matcher(sparql);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            Object v = byName.get(m.group(1));
            if (v == null && parameters.length == 1 && !byName.containsKey(m.group(1))) {
                v = parameters[0]; // one-parameter convenience
            }
            if (v == null) {
                throw new IllegalArgumentException("no parameter named :" + m.group(1) + " for " + method.getName());
            }
            m.appendReplacement(out, Matcher.quoteReplacement(RdfEntityManager.literal(v)));
        }
        m.appendTail(out);
        return out.toString();
    }

    /** A row to a record (component names = variable names), a scalar of the
     *  first variable, or a raw map. */
    private static Object mapRow(Map<String, String> row, Class<?> type) {
        if (type == Map.class) {
            return new LinkedHashMap<>(row);
        }
        if (!type.isRecord()) {
            // a single-variable SELECT mapped to a scalar: List<String>, Optional<Integer>, …
            return convert(row.values().iterator().next(), type);
        }
        for (Constructor<?> c : type.getDeclaredConstructors()) {
            if (c.getParameterCount() == type.getRecordComponents().length) {
                Object[] args = new Object[c.getParameterCount()];
                for (int i = 0; i < args.length; i++) {
                    String name = type.getRecordComponents()[i].getName();
                    Class<?> ct = c.getParameterTypes()[i];
                    args[i] = convert(row.get(name), ct);
                }
                try {
                    c.setAccessible(true);
                    return c.newInstance(args);
                } catch (ReflectiveOperationException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
        throw new IllegalStateException("no canonical constructor on " + type);
    }

    private static Object convert(String v, Class<?> t) {
        if (v == null) {
            return null;
        }
        if (t == String.class) {
            return v;
        }
        if (t == boolean.class || t == Boolean.class) {
            return Boolean.parseBoolean(v) || "1".equals(v);
        }
        if (t == long.class || t == Long.class) {
            try {
                return Long.parseLong(v); // exact beyond 2^53
            } catch (NumberFormatException e) {
                return Math.round(Double.parseDouble(v)); // a decimal answer, e.g. a SUM
            }
        }
        if (t == int.class || t == Integer.class) {
            try {
                return Integer.parseInt(v);
            } catch (NumberFormatException e) {
                return (int) Math.round(Double.parseDouble(v));
            }
        }
        if (t == double.class || t == Double.class) {
            return Double.parseDouble(v); // as answered: no rounding
        }
        throw new UnsupportedOperationException("@Query record component type " + t);
    }

    @Override
    public QueryMethod getQueryMethod() {
        return queryMethod;
    }
}
