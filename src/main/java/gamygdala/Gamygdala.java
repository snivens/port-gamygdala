package gamygdala;

import java.util.Map;

import agent.Agent;
import data.Belief;
import data.Goal;
import data.map.GamygdalaMap;
import debug.Debug;
import decayfunction.DecayFunction;
import decayfunction.LinearDecay;

/**
 * This is the main appraisal engine class taking care of interpreting a
 * situation emotionally. Typically you create one instance of this class and
 * then register all agents (emotional entities) to it, as well as all goals.
 */
public class Gamygdala {

    /**
     * The collection of agents in this Gamygdala instance.
     */
    private final GamygdalaMap gamygdalaMap;

    /**
     * The decay function used to calculate emotion intensity.
     */
    private DecayFunction decayFunction;

    /**
     * The decay factor used in the DecayFunction.
     */
    private double decayFactor;

    /**
     * Constructor for Gamygdala Emotion Engine.
     */
    public Gamygdala() {
        // Init agent and goal maps
        this.gamygdalaMap = new GamygdalaMap();

        // Set default decay factor
        this.decayFactor = .8;

        // Set default decay function
        this.decayFunction = new LinearDecay(this.decayFactor);

    }

    /**
     * Get the GamygdalaMap containing all Agents and Goals.
     *
     * @return GamygdalaMap
     */
    public GamygdalaMap getGamygdalaMap() {
        return gamygdalaMap;
    }

    /**
     * @return the decayFunction
     */
    public DecayFunction getDecayFunction() {
        return decayFunction;
    }

    /**
     * @param decayFunction the decayFunction to set
     */
    public void setDecayFunction(DecayFunction decayFunction) {
        this.decayFunction = decayFunction;
    }

    /**
     * @return the decayFactor
     */
    public double getDecayFactor() {
        return decayFactor;
    }

    /**
     * @param decayFactor the decayFactor to set
     */
    public void setDecayFactor(double decayFactor) {
        this.decayFactor = decayFactor;
    }

    /**
     * Performs the complete appraisal of a single event (belief) for all
     * agents.
     *
     * @param belief        The belief to appraise.
     * @param affectedAgent The agent who appraises the event.
     * @return True on success, false on error.
     */
    public boolean appraise(Belief belief, Agent affectedAgent) {
        Debug.debug("\n===\nStarting appraisal for:\n" + belief + "\nwith affectedAgent: " + affectedAgent + "\n==\n");

        if (belief == null) {
            Debug.debug("Error: belief is null.");
            return false;
        }

        if (this.gamygdalaMap.getGoalMap().size() == 0) {
            Debug.debug("Warning: no goals registered to Gamygdala.");
            return true;
        }

        for (Map.Entry<Goal, Double> goalPair : belief.getGoalCongruenceMap().entrySet()) {
            // If current goal is really a goal
            if (goalPair.getKey() != null) {
                this.processGoal(goalPair, belief, affectedAgent);
                Debug.debug("");
            }
        }

        Debug.debug("\n=====\nFinished appraisal round\n=====\n");
        return true;
    }

    private void processGoal(Map.Entry<Goal, Double> goalPair, Belief belief, Agent affectedAgent) {
        Goal goal = goalPair.getKey();
        Double congruence = goalPair.getValue();

        Debug.debug("Processing goal: " + goal);
        // Calculate goal values
        double deltaLikelihood = this.calculateDeltaLikelihood(goal, congruence,
                belief.getLikelihood(), belief.isIncremental());

        Debug.debug("   deltaLikelihood: " + deltaLikelihood);

        this.evaluateEmotions(affectedAgent, goal, belief, deltaLikelihood);
    }

    private void evaluateEmotions(Agent affectedAgent, Goal goal, Belief belief, double deltaLikelihood) {
        // if affectedAgent is null, calculate emotions for all agents.
        if (affectedAgent == null) {
            for (Map.Entry<String, Agent> pair : gamygdalaMap.getAgentSet()) {
                Agent owner = pair.getValue();

                if (owner != null && owner.hasGoal(goal)) {
                    this.evaluateAgentEmotions(owner, goal, belief, deltaLikelihood);
                }
            }
        } else {
            this.evaluateAgentEmotions(affectedAgent, goal, belief, deltaLikelihood);
        }
    }

    /**
     * Re-evaluate an Agent's emotions by processing a Goal and a Belief.
     * 
     * @param owner The Goal owner.
     * @param currentGoal the currentGoal of the Agent.
     * @param belief the Belief of the current Agent.
     * @param deltaLikelihood the likelihood that the Goal will be achieved.
     */
    private void evaluateAgentEmotions(Agent owner, Goal currentGoal, Belief belief, double deltaLikelihood) {

        double utility = currentGoal.getUtility();
        double likelihood = currentGoal.getLikelihood();
        double desirability = utility * deltaLikelihood;

        Debug.debug("   utility: " + utility + "\n   likelihood: " + likelihood);

        // Determine new emotions for Agent
        owner.evaluateInternalEmotion(utility, deltaLikelihood, likelihood);

        // also add remorse and gratification if conditions are met
        // (i.e., agent did something bad/good for owner)
        owner.agentActions(owner, belief.getCausalAgent(), utility * deltaLikelihood);

        // Now check if anyone has a relation with this goal owner, and update
        // the social emotions accordingly.
        for (Map.Entry<String, Agent> pair : gamygdalaMap.getAgentSet()) {
            owner.evaluateRelationWithAgent(pair.getValue(), belief.getCausalAgent(), desirability);
        }
    }

    /**
     * Decay emotional state of all Agents.
     */
    public void decayAll(long lastMillis, long currentMillis) {
        long millisPassed = currentMillis - lastMillis;

        Agent agent;
        for (Map.Entry<String, Agent> pair : gamygdalaMap.getAgentSet()) {
            agent = pair.getValue();

            if (agent != null) {
                agent.decay(decayFunction, millisPassed);
            }
        }
    }

    /**
     * Defines the change in a goal's likelihood due to the congruence and
     * likelihood of a current event. We cope with two types of beliefs:
     * incremental and absolute beliefs. Incrementals have their likelihood
     * added to the goal, absolute define the current likelihood of the goal And
     * two types of goals: maintenance and achievement. If an achievement goal
     * (the default) is -1 or 1, we can't change it any more (unless externally
     * and explicitly by changing the goal.likelihood).
     *
     * @param goal          the goal for which to calculate the likelihood.
     * @param congruence    how much is it affecting the agent.
     * @param likelihood    the likelihood.
     * @param isIncremental if the goal is incremental
     * @return the delta likelihood.
     */
    private double calculateDeltaLikelihood(Goal goal, double congruence, double likelihood, boolean isIncremental) {
        Double oldLikelihood = goal.getLikelihood();

        if (goal.isMaintenanceGoal() || (oldLikelihood < 1 && oldLikelihood > -1)) {
            if (isIncremental) {
                goal.setLikelihood(Math.max(Math.min(oldLikelihood + likelihood * congruence, 1), -1));
            } else {
                goal.setLikelihood((congruence * likelihood + 1d) / 2d);
            }
            return goal.getLikelihood() - oldLikelihood;
        } else {
            return 0;
        }
    }

    /**
     * Facilitator method to print all emotional states to the console.
     *
     * @param gain Whether you want to print the gained (true) emotional states
     *             or non-gained (false).
     */
    public void printAllEmotions(boolean gain) {
        Agent agent;
        for (Map.Entry<String, Agent> stringAgentEntry : this.getGamygdalaMap().getAgentMap().entrySet()) {
            agent = stringAgentEntry.getValue();

            agent.printEmotionalState(gain);
            agent.printRelations(null);
        }
    }

    /**
     * Sets the gain for each Agent.
     * @param gain Double the gain for an Agent
     */
    public void setAgentsGain(double gain) {
        for (Map.Entry<String, Agent> stringAgentEntry : this.gamygdalaMap.getAgentMap().entrySet()) {
            if (stringAgentEntry.getValue() != null) {
                stringAgentEntry.getValue().setGain(gain);
            } else {
                this.gamygdalaMap.getAgentMap().remove(stringAgentEntry.getKey());
            }
        }
    }

    /**
     * Creates a new Agent.
     * @param name String the name of the new Agent
     * @return Agent the newly created Agent
     */
    public Agent createAgent(String name) {
        Agent agent = new Agent(name);
        this.getGamygdalaMap().registerAgent(agent);

        return agent;
    }

    /**
     * Create a Goal for an Agent.
     * @param agent Agent the Agent which the Goal is for
     * @param goalName String the name of the Goal
     * @param goalUtility Double Goal utility
     * @param isMaintenanceGoal Whether or not this goal is a maintenance goal.
     * @return Goal The newly created Goal
     */
    public Goal createGoalForAgent(Agent agent, String goalName, double goalUtility, boolean isMaintenanceGoal) {
        Goal goal = new Goal(goalName, goalUtility, isMaintenanceGoal);
        agent.addGoal(goal);
        this.getGamygdalaMap().registerGoal(goal);

        return goal;
    }

    /**
     * Create a Relation between two Agents.
     * @param source Agent The source Agent
     * @param target Agent The target Agent
     * @param relation Relation the relation (between -1 and 1).
     */
    public void createRelation(Agent source, Agent target, double relation) {
        source.updateRelation(target, relation);
    }

}
