package com.spaziocodice.labs.solr.qty;

import com.spaziocodice.labs.solr.qty.cfg.Unit;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.search.LuceneQParserPlugin;
import org.apache.solr.search.QParserPlugin;

/**
 * A {@link QParserPlugin} which produces a boost query according with the detected quantities within a query string.
 * The generated query contains (for each detected quantity) a literal query (e.g. capacity:100) and an optional
 * range query (e.g. capacity:[90 TO 110]) depending on the configured gap.
 *
 * @author agazzarini
 * @since 1.0
 */
public class QuantityDetectionBQParserPlugin extends QuantityDetector {
    private LuceneQParserPlugin qParser;

    @Override
    public void init(final NamedList args) {
        super.init(args);
        this.qParser = new LuceneQParserPlugin();
    }

    @Override
    QueryBuilder queryBuilder(final StringBuilder query) {
        return new QueryBuilder() {
            final StringBuilder buffer = new StringBuilder();

            @Override
            public void newQuantityDetected(final Unit unit, final QuantityOccurrence occurrence) {
                addLiteralQuery(unit, buffer, occurrence);
                gap(occurrence.fieldName).ifPresent(gap -> addRangeQuery(buffer, occurrence, gap.value().intValue()));
            }

            @Override
            public String product() {
                return buffer.length() > 0 ? buffer.toString() : "*:*";
            }

            @Override
            public QParserPlugin qparserPlugin() {
                return qParser;
            }
        };
    }

    /**
     * Adds a new boolean, literal filter to the result of this builder.
     *
     * @param unit the unit associated with the detected quantity occurrence.
     * @param builder the query buffer.
     * @param occurrence the quantity instance occurrence.
     * @return the same query buffer with the new filter definition.
     */
    private StringBuilder addLiteralQuery(final Unit unit, final StringBuilder builder, final QuantityOccurrence occurrence) {
        builder
            .append(occurrence.fieldName)
            .append(":")
            .append(occurrence.amount);
        unit.boost().ifPresent(boost -> builder.append("^").append(boost));
        builder.append(" ");
        return builder;
    }

    /**
     * Adds a new boolean, range filter to the result of this builder.
     *
     * @param builder the query buffer.
     * @param occurrence the quantity instance occurrence.
     * @return the same query buffer with the new filter definition.
     */
    private StringBuilder addRangeQuery(final StringBuilder builder, final QuantityOccurrence occurrence, final int distance) {
        final int leftBound = occurrence.amount.intValue() >= distance ? occurrence.amount.intValue() - distance : 0;
        return builder
                .append(occurrence.fieldName)
                .append(":[")
                .append(leftBound)
                .append(" TO ")
                .append(occurrence.amount.intValue() + distance)
                .append("]")
                .append(" ");
    }
}