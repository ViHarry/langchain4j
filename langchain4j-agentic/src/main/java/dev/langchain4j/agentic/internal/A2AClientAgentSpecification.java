package dev.langchain4j.agentic.internal;

import dev.langchain4j.agentic.A2AAgent;
import dev.langchain4j.agentic.UntypedAgent;
import io.a2a.spec.AgentCard;
import java.lang.reflect.Method;
import java.util.Map;

public class A2AClientAgentSpecification implements AgentSpecification {

    private final String inputName;
    private final AgentCard agentCard;

    public A2AClientAgentSpecification(A2AClientInstance a2AClientInstance) {
        this.agentCard = a2AClientInstance.agentCard();
        this.inputName = a2AClientInstance.inputName();
    }

    @Override
    public boolean isWorkflowAgent() {
        return false;
    }

    @Override
    public String name() {
        return agentCard.name();
    }

    @Override
    public String description() {
        return agentCard.description();
    }

    @Override
    public Method method() {
        try {
            return A2AAgent.class.getDeclaredMethod("invoke", Object.class);
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String toCard() {
        return "{" + name() + ": " + description() + ", [" + inputName + "]}";
    }

    @Override
    public Object[] toInvocationArguments(final Map<String, ?> arguments) {
        return new Object[] { arguments.get(inputName) };
    }
}
