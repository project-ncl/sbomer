package org.redhat.sbomer.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.Collection;
import java.util.Collections;

/**
 * Collection REST response.
 *
 */
@Data
@AllArgsConstructor
public class Page<T> {

    /**
     * Page index.
     */
    private int pageIndex;

    /**
     * Number of records per page.
     */
    private int pageSize;

    /**
     * Total pages provided by this query or -1 if unknown.
     */
    private int totalPages;

    /**
     * Number of all hits (not only this page).
     */
    private long totalHits;

    /**
     * Embedded collection of data.
     */
    private Collection<T> content;

    public Page() {
        content = Collections.emptyList();
    }
}
