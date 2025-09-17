package de.uni_koblenz.ptsd.foxtrot.robot.strategy.impl;

import java.util.Deque;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Action;
import de.uni_koblenz.ptsd.foxtrot.robot.strategy.Target;

/** Lightweight value object for a planned target with its action plan and score. */
record PlanCandidate(Target target, Deque<Action> plan, double score) { }
