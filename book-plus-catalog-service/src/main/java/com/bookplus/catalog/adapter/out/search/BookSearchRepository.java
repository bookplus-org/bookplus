package com.bookplus.catalog.adapter.out.search;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;

/** Spring Data Elasticsearch repository — operaciones CRUD básicas sobre el índice. */
public interface BookSearchRepository extends ElasticsearchRepository<BookDocument, String> {
}
