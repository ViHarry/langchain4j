package dev.langchain4j.agentic;

import io.a2a.spec.A2A;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;

public class AgentServices {

    private AgentServices() { }

    public static <T> AgentBuilder<T> builder(Class<T> agentServiceClass) {
        return new AgentBuilder<>(agentServiceClass);
    }

    public static A2AClientBuilder fromA2AUrl(String a2aServerUrl) {
        try {
            return new A2AClientBuilder(A2A.getAgentCard(a2aServerUrl));
        } catch (A2AServerException e) {
            throw new RuntimeException(e);
        }
    }
}
