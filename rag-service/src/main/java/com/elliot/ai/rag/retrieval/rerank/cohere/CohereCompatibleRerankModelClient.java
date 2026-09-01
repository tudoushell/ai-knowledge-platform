package com.elliot.ai.rag.retrieval.rerank.cohere;

import com.elliot.ai.common.enums.ResultCode;
import com.elliot.ai.common.exception.BusinessException;
import com.elliot.ai.rag.config.RerankProperties;
import com.elliot.ai.rag.retrieval.rerank.RerankModelClient;
import com.elliot.ai.rag.retrieval.rerank.cohere.model.CohereRerankRequest;
import com.elliot.ai.rag.retrieval.rerank.cohere.model.CohereRerankResponse;
import com.elliot.ai.rag.retrieval.rerank.exception.RerankException;
import com.elliot.ai.rag.retrieval.rerank.model.RerankDocument;
import com.elliot.ai.rag.retrieval.rerank.model.RerankModelResult;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.List;

@Component
public class CohereCompatibleRerankModelClient
        implements RerankModelClient {

    private final RerankProperties properties;
    private final RestClient restClient;

    public CohereCompatibleRerankModelClient(RestClient.Builder restClientBuilder, RerankProperties properties) {
        this.properties = properties;

        HttpClient httpClient = HttpClient
                .newBuilder()
                .connectTimeout(properties.getConnectTimeout())
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(properties.getReadTimeout());
        this.restClient = restClientBuilder
                .baseUrl(properties.getBaseUrl())
                .requestFactory(requestFactory)
                .build();
    }


    @Override
    public List<RerankModelResult> rerank(String query, List<RerankDocument> documents) {

        if (documents == null || documents.isEmpty()) {
            return List.of();
        }

        try {
            CohereRerankRequest request = new CohereRerankRequest(
                    properties.getModel(),
                    query,
                    buildDocuments(documents),
                    documents.size()
            );
            CohereRerankResponse response = restClient
                    .post()
                    .uri(properties.getPath())
                    .headers(headers -> {
                        if (StringUtils.hasText(properties.getApiKey())) {
                            headers.setBearerAuth(properties.getApiKey());
                        }
                    })
                    .body(request)
                    .retrieve()
                    .body(
                            CohereRerankResponse.class
                    );
            if (response == null
                    || response.results() == null) {
                throw new BusinessException(
                        ResultCode.FAIL,
                        "Rerank service returned invalid response"
                );
            }
            return response.results()
                    .stream()
                    .map(result ->
                            new RerankModelResult(result.index(),
                                    result.relevanceScore()))
                    .toList();
        } catch (RerankException e) {
            throw e;
        } catch (Exception e) {
            throw new RerankException("Failed to invoke rerank model", e);
        }
    }


    private List<String> buildDocuments(
            List<RerankDocument> documents
    ) {
        return documents.stream()
                .map(RerankDocument::content)
                .toList();
    }
}
