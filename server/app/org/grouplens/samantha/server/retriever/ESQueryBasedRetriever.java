/*
 * Copyright (c) [2016-2017] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;

import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilders;
import org.grouplens.samantha.server.common.ElasticSearchService;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.libs.Json;

import java.util.List;

public class ESQueryBasedRetriever extends AbstractRetriever {
    private final ElasticSearchService elasticSearchService;
    private String scrollId = null;
    private final List<EntityExpander> expanders;
    private final String elasticSearchReqKey;
    private final String setScrollKey;
    private final String elasticSearchIndex;
    private final String retrieveType;
    private final List<String> retrieveFields;
    private final String[] defaultMatchFields;
    private final String queryKey;
    private final String elasticSearchScoreName;

    public ESQueryBasedRetriever(ElasticSearchService elasticSearchService,
                                 List<EntityExpander> expanders, String elasticSearchScoreName,
                                 String elasticSearchReqKey,
                                 String setScrollKey, String elasticSearchIndex, String retrieveType,
                                 List<String> retrieveFields, Configuration config,
                                 List<String> defaultMatchFields, String queryKey) {
        super(config);
        this.elasticSearchService = elasticSearchService;
        this.expanders = expanders;
        this.elasticSearchScoreName= elasticSearchScoreName;
        this.elasticSearchIndex = elasticSearchIndex;
        this.elasticSearchReqKey = elasticSearchReqKey;
        this.setScrollKey = setScrollKey;
        this.retrieveFields = retrieveFields;
        this.retrieveType = retrieveType;
        if (defaultMatchFields != null) {
            this.defaultMatchFields = defaultMatchFields.toArray(new String[defaultMatchFields.size()]);
        } else {
            this.defaultMatchFields = new String[0];
        }
        this.queryKey = queryKey;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        final JsonNode requestBody = requestContext.getRequestBody();
        JsonNode elasticSearchRequest;
        if (requestBody.has(elasticSearchReqKey)) {
            elasticSearchRequest = JsonHelpers.getRequiredJson(requestBody, elasticSearchReqKey);
        } else if (defaultMatchFields.length > 0 && requestBody.has(queryKey)) {
            String query = JsonHelpers.getRequiredString(requestBody, queryKey);
            elasticSearchRequest = Json.parse(QueryBuilders.multiMatchQuery(query, defaultMatchFields).toString());
        } else {
            elasticSearchRequest = Json.parse(QueryBuilders.matchAllQuery().toString());
        }
        boolean setScroll = JsonHelpers.getOptionalBoolean(requestBody, setScrollKey, false);
        Integer size = JsonHelpers.getOptionalInt(elasticSearchRequest, "size", null);
        Integer from = JsonHelpers.getOptionalInt(elasticSearchRequest, "from", null);
        SearchResponse searchResponse = elasticSearchService
                .search(elasticSearchIndex,
                        retrieveType,
                        elasticSearchRequest,
                        retrieveFields,
                        setScroll,
                        scrollId, size, from);
        if (setScroll) {
            scrollId = searchResponse.getScrollId();
        }
        RetrievedResult initialResult = ESRetrieverUtilities.parse(elasticSearchScoreName, requestContext,
                searchResponse, expanders, retrieveFields);
        return initialResult;
    }

    public void resetScroll() {
        if (scrollId != null) {
            elasticSearchService.resetScroll(scrollId);
        }
        scrollId = null;
    }
}
