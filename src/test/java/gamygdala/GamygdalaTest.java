package gamygdala;

import agent.Agent;
import agent.data.Belief;
import agent.data.Goal;
import agent.data.map.AgentMap;
import agent.data.map.GoalMap;
import decayfunction.DecayFunction;
import decayfunction.LinearDecay;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Created by svenpopping on 19/06/15.
 */
public class GamygdalaTest {

    Gamygdala gamygdala;
    DecayFunction decayFunction;

    @Before
    public void setUp() throws Exception {
        gamygdala = new Gamygdala();
        decayFunction = mock(DecayFunction.class);

        gamygdala.setDecayFunction(decayFunction);
    }

    @After
    public void tearDown() throws Exception {
        gamygdala = null;
    }

    @Test
    public void testGetDecayFunction() throws Exception {
        assertEquals(decayFunction, gamygdala.getDecayFunction());
    }

    @Test
    public void testSetDecayFunction() throws Exception {
        assertEquals(decayFunction, gamygdala.getDecayFunction());
        decayFunction = new LinearDecay(0.4);

        gamygdala.setDecayFunction(decayFunction);
        assertEquals(decayFunction, gamygdala.getDecayFunction());
    }

    @Test
    public void testGetGoalMap() throws Exception {
        assertEquals(new GoalMap(), gamygdala.getGoalMap());
    }

    @Test
    public void testGetAgentMap() throws Exception {
        assertEquals(new AgentMap(), gamygdala.getAgentMap());
    }

    @Test
    public void testAppraiseExceptions() throws Exception {
        assertFalse(gamygdala.appraise(null, null));

        assertTrue(gamygdala.appraise(mock(Belief.class), null));
    }

    @Test
    public void testAppraise() throws Exception {
        Agent agent = mock(Agent.class);
        when(gamygdala.createAgent("henk")).thenReturn(agent);

        gamygdala.createGoalForAgent(agent, "happy", .5, false);
        assertFalse(gamygdala.appraise(null, null));

        assertTrue(gamygdala.appraise(mock(Belief.class), null));
    }

    @Test
    public void testDecayAll() throws Exception {

    }

    @Test
    public void testSetAgentsGain() throws Exception {
        when(gamygdala.getGoalMap()).thenReturn(mock(GoalMap.class));
    }

    @Test
    public void testCreateAgent() throws Exception {
        Agent agent = mock(Agent.class);
        when(gamygdala.createAgent("henk")).thenReturn(agent);

        assertEquals(agent, gamygdala.createAgent("henk"));
    }

    @Test
    public void testCreateGoalForAgent() throws Exception {
        Agent agent = mock(Agent.class);
        GoalMap goalMap = mock(GoalMap.class);

        when(agent.getGoals()).thenReturn(goalMap);
        when(goalMap.put("happy", new Goal("happy", .5, false))).thenReturn(new Goal("happy", .5, false));

        assertEquals(new Goal("happy", .5, false), gamygdala.createGoalForAgent(agent, "happy", .5, false));
        verify(agent.getGoals(), times(1)).put("happy", new Goal("happy", .5, false));
    }

    @Test
    public void testCreateRelation() throws Exception {

    }
}