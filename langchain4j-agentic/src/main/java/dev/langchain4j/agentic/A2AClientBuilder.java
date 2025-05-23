package dev.langchain4j.agentic;

import dev.langchain4j.agentic.internal.A2AClientInstance;
import dev.langchain4j.agentic.internal.AgentInstance;
import io.a2a.client.A2AClient;
import io.a2a.spec.A2A;
import io.a2a.spec.A2AServerException;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.MessageSendParams;
import io.a2a.spec.SendMessageResponse;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.stream.Collectors;

public class A2AClientBuilder {

    private final AgentCard agentCard;
    private final A2AClient a2aClient;

    private String inputName;
    private String outputName;

    A2AClientBuilder(AgentCard agentCard) {
        this.agentCard = agentCard;
        this.a2aClient = new A2AClient(agentCard);
    }

    public A2AAgent build() {
        Object agent = Proxy.newProxyInstance(
                UntypedAgent.class.getClassLoader(),
                new Class<?>[] {A2AAgent.class, A2AClientInstance.class},
                new InvocationHandler() {
                    @Override
                    public Object invoke(Object proxy, Method method, Object[] args) throws Exception {
                        if (method.getDeclaringClass() == AgentInstance.class) {
                            return switch (method.getName()) {
                                case "trySetChatMemory" -> false;
                                case "outputName" -> outputName;
                                default ->
                                        throw new UnsupportedOperationException(
                                                "Unknown method on AgentInstance class : " + method.getName());
                            };
                        }

                        if (method.getDeclaringClass() == A2AClientInstance.class) {
                            return switch (method.getName()) {
                                case "agentCard" -> agentCard;
                                case "inputName" -> inputName;
                                default ->
                                        throw new UnsupportedOperationException(
                                                "Unknown method on A2AClientInstance class : " + method.getName());
                            };
                        }

                        return invokeAgent((String) args[0]);
                    }
                });

        return (A2AAgent) agent;
    }

    private Object invokeAgent(String arg) throws A2AServerException {
        Message message = A2A.toUserMessage(arg);
        MessageSendParams params = new MessageSendParams.Builder()
                .message(message)
                .build();

        SendMessageResponse response = a2aClient.sendMessage(params);

        return ((Task)response.getResult()).getArtifacts().stream()
                .flatMap(a -> a.parts().stream())
                .filter(TextPart.class::isInstance)
                .map(TextPart.class::cast)
                .map(TextPart::getText)
                .collect(Collectors.joining());
    }

    public A2AClientBuilder inputName(String inputName) {
        this.inputName = inputName;
        return this;
    }

    public A2AClientBuilder outputName(String outputName) {
        this.outputName = outputName;
        return this;
    }
}
