package io.quarkiverse.mcp.servers.kubernetes;

import java.util.List;

import jakarta.inject.Singleton;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.fabric8.kubernetes.api.model.ManagedFieldsEntry;
import io.fabric8.kubernetes.api.model.ObjectMeta;
import io.quarkus.kubernetes.client.KubernetesClientObjectMapperCustomizer;

/**
 * Default Kubernetes Client serialization customizer.
 * <p>
 * The main purpose is to customize the serialization of Kubernetes resources to minimize the amount of data sent to the LLM.
 * <p>
 * LLMs are limited in the amount of input and output tokens they can process.
 * This is especially important for smaller models.
 * In addition, inference providers may also impose rate limits, usually involving the number of tokens per minute.
 * <p>
 * Any data that's not needed for the LLM to make a decision should be removed to reduce the amount of input tokens.
 * Currently, we remove the following fields:
 * <ul>
 * <li>managedFields: Only useful for server-side apply, completely useless for LLMs (they contain redundant information already
 * present in spec)</li>
 * </ul>
 */
@Singleton
public class ObjectMapperCustomizer implements KubernetesClientObjectMapperCustomizer {

    @Override
    public void customize(ObjectMapper objectMapper) {
        objectMapper.addMixIn(ObjectMeta.class, ObjectMetaMixin.class);
    }

    @SuppressWarnings("unused")
    public static abstract class ObjectMetaMixin extends ObjectMeta {

        @JsonIgnore
        private List<ManagedFieldsEntry> managedFields;

        @JsonIgnore
        public abstract List<ManagedFieldsEntry> getManagedFields();

    }
}
