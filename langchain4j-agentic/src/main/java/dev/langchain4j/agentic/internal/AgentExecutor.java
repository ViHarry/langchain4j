package dev.langchain4j.agentic.internal;

import dev.langchain4j.agentic.Agent;
import dev.langchain4j.agentic.Cognisphere;
import dev.langchain4j.agentic.CognisphereOwner;
import dev.langchain4j.service.UserMessage;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static dev.langchain4j.internal.Utils.getAnnotatedMethod;

public record AgentExecutor(AgentSpecification agentSpecification, AgentInstance agent) {

    public String agentName() {
        return agentSpecification.name();
    }

    public boolean isWorkflowAgent() {
        return agentSpecification.isWorkflowAgent();
    }

    public Object invoke(Cognisphere cognisphere) {
        Object invokedAgent = agent instanceof CognisphereOwner co ? co.withCognisphere(cognisphere) : agent;
        Object[] args = agentSpecification.toInvocationArguments(cognisphere.getState());

        Object response = agentSpecification.invoke(invokedAgent, args);
        String outputName = agent.outputName();
        if (outputName != null) {
            cognisphere.writeState(outputName, response);
        }
        cognisphere.registerAgentInvocation(agentSpecification, args, response);
        return response;
    }

    public static List<AgentExecutor> agentsToExecutors(List<AgentInstance> agents) {
        List<AgentExecutor> agentExecutors = new ArrayList<>();
        for (AgentInstance agent : agents) {
            if (agent instanceof A2AClientInstance a2a) {
                agentExecutors.add(a2aClientToAgentExecutor(a2a));
                continue;
            }
            for (Method method : agent.getClass().getDeclaredMethods()) {
                methodToAgentExecutor(agent, method).ifPresent(agentExecutors::add);
            }
        }
        return agentExecutors;
    }

    private static Optional<AgentExecutor> methodToAgentExecutor(AgentInstance agent, Method method) {
        return getAnnotatedMethod(method, Agent.class)
                .or(() -> getAnnotatedMethod(method, UserMessage.class))
                .map(agentMethod -> new AgentExecutor(AgentSpecification.fromMethod(agentMethod), agent));
    }

    private static AgentExecutor a2aClientToAgentExecutor(A2AClientInstance a2aClient) {
        return new AgentExecutor(new A2AClientAgentSpecification(a2aClient), a2aClient);
    }
}
