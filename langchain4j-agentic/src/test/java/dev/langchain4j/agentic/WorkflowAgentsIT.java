package dev.langchain4j.agentic;

import dev.langchain4j.agentic.internal.AgentInvocation;
import dev.langchain4j.agentic.workflow.LoopAgentService;
import dev.langchain4j.agentic.workflow.SequentialAgentService;
import org.junit.jupiter.api.Test;
import java.util.List;
import java.util.Map;

import dev.langchain4j.agentic.Agents.CreativeWriter;
import dev.langchain4j.agentic.Agents.AudienceEditor;
import dev.langchain4j.agentic.Agents.StyleEditor;
import dev.langchain4j.agentic.Agents.StyleScorer;
import dev.langchain4j.agentic.Agents.StyledWriter;

import static dev.langchain4j.agentic.Models.BASE_MODEL;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

public class WorkflowAgentsIT {

    @Test
    void sequential_agents_tests() {
        CreativeWriter creativeWriter = spy(AgentServices.builder(CreativeWriter.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build());

        AudienceEditor audienceEditor = spy(AgentServices.builder(AudienceEditor.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build());

        StyleEditor styleEditor = spy(AgentServices.builder(StyleEditor.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build());

        UntypedAgent novelCreator = SequentialAgentService.builder()
                .subAgents(creativeWriter, audienceEditor, styleEditor)
                .build();

        Map<String, Object> input = Map.of(
                "topic", "dragons and wizards",
                "style", "fantasy",
                "audience", "young adults"
        );

        Map<String, Object> output = novelCreator.invoke(input);
        System.out.println(output.get("story"));

        verify(creativeWriter).generateStory("dragons and wizards");
        verify(audienceEditor).editStory(any(), eq("young adults"));
        verify(styleEditor).editStory(any(), eq("fantasy"));
    }

    @Test
    void loop_agents_tests() {
        CreativeWriter creativeWriter = AgentServices.builder(CreativeWriter.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build();

        StyleEditor styleEditor = AgentServices.builder(StyleEditor.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build();

        StyleScorer styleScorer = AgentServices.builder(StyleScorer.class)
                .chatModel(BASE_MODEL)
                .outputName("score")
                .build();

        Object styleReviewLoop = LoopAgentService.builder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(5)
                .exitCondition( cognisphere -> cognisphere.readState("score", 0.0) >= 0.8)
                .build();

        UntypedAgent styledWriter = SequentialAgentService.builder()
                .subAgents(creativeWriter, styleReviewLoop)
                .build();

        Map<String, Object> input = Map.of(
                "topic", "dragons and wizards",
                "style", "comedy"
        );

        Map<String, Object> output = styledWriter.invoke(input);
        String story = (String) output.get("story");
        System.out.println(story);

        Cognisphere cognisphere = ((CognisphereOwner) styledWriter).cognisphere();
        assertThat(story).isEqualTo(cognisphere.readState("story"));
        assertThat(cognisphere.readState("score", 0.0)).isGreaterThanOrEqualTo(0.8);
    }

    @Test
    void typed_loop_agents_tests() {
        CreativeWriter creativeWriter = AgentServices.builder(CreativeWriter.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build();

        StyleEditor styleEditor = AgentServices.builder(StyleEditor.class)
                .chatModel(BASE_MODEL)
                .outputName("story")
                .build();

        StyleScorer styleScorer = AgentServices.builder(StyleScorer.class)
                .chatModel(BASE_MODEL)
                .outputName("score")
                .build();

        Object styleReviewLoop = LoopAgentService.builder()
                .subAgents(styleScorer, styleEditor)
                .maxIterations(5)
                .exitCondition( cognisphere -> cognisphere.readState("score", 0.0) >= 0.8)
                .build();

        StyledWriter styledWriter = SequentialAgentService.builder(StyledWriter.class)
                .subAgents(creativeWriter, styleReviewLoop)
                .outputName("story")
                .build();

        String story = styledWriter.writeStoryWithStyle("dragons and wizards", "comedy");
        System.out.println(story);

        Cognisphere cognisphere = ((CognisphereOwner) styledWriter).cognisphere();
        assertThat(cognisphere.readState("topic")).isEqualTo("dragons and wizards");
        assertThat(cognisphere.readState("style")).isEqualTo("comedy");
        assertThat(story).isEqualTo(cognisphere.readState("story"));
        assertThat(cognisphere.readState("score", 0.0)).isGreaterThanOrEqualTo(0.8);

        assertThat(cognisphere.getAgentInvocations("generateStory")).hasSize(1);

        List<AgentInvocation> scoreAgentInvocations = cognisphere.getAgentInvocations("scoreStyle");
        assertThat(scoreAgentInvocations).hasSizeBetween(1, 5);
        System.out.println("Score agent invocations: " + scoreAgentInvocations);
        assertThat((Double) scoreAgentInvocations.get(scoreAgentInvocations.size() - 1).response()).isGreaterThanOrEqualTo(0.8);
    }
}
