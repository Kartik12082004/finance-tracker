import { useEffect, useState } from "react";
import {
  cancelGoal,
  contributeToGoal,
  createGoal,
  getGoals,
  pauseGoal,
  resumeGoal,
} from "../api/goalApi";
import type { Goal } from "../types/goal";

function formatAmount(amount: number): string {
  return amount.toFixed(2);
}

function formatDate(date: string): string {
  return new Date(`${date}T00:00:00`).toLocaleDateString("en-GB");
}

function getProgress(goal: Goal): number {
  if (goal.targetAmount <= 0) {
    return 0;
  }

  return Math.min(100, (goal.currentAmount / goal.targetAmount) * 100);
}

export default function Goals() {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [targetAmount, setTargetAmount] = useState("");
  const [targetDate, setTargetDate] = useState("");
  const [isCreating, setIsCreating] = useState(false);

  const [contributionAmounts, setContributionAmounts] = useState<
    Record<string, string>
  >({});
  const [activeAction, setActiveAction] = useState<string | null>(null);

  async function loadGoals() {
    try {
      setIsLoading(true);
      setError(null);

      const response = await getGoals();
      setGoals(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to load goals.");
    } finally {
      setIsLoading(false);
    }
  }

  useEffect(() => {
    async function initializeGoals() {
      try {
        setIsLoading(true);
        setError(null);

        const response = await getGoals();
        setGoals(response);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Failed to load goals.");
      } finally {
        setIsLoading(false);
      }
    }

    void initializeGoals();
  }, []);

  async function handleCreateGoal(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmedName = name.trim();
    const parsedTargetAmount = Number(targetAmount);

    if (!trimmedName) {
      setError("Goal name is required.");
      return;
    }

    if (!Number.isFinite(parsedTargetAmount) || parsedTargetAmount <= 0) {
      setError("Target amount must be greater than zero.");
      return;
    }

    if (!targetDate) {
      setError("Target date is required.");
      return;
    }

    try {
      setIsCreating(true);
      setError(null);

      await createGoal({
        name: trimmedName,
        targetAmount: parsedTargetAmount,
        targetDate,
      });

      setName("");
      setTargetAmount("");
      setTargetDate("");

      await loadGoals();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create goal.");
    } finally {
      setIsCreating(false);
    }
  }

  async function handleContribution(goal: Goal) {
    const rawAmount = contributionAmounts[goal.id] ?? "";
    const amount = Number(rawAmount);

    if (!Number.isFinite(amount) || amount <= 0) {
      setError("Contribution must be greater than zero.");
      return;
    }

    try {
      setActiveAction(`contribute-${goal.id}`);
      setError(null);

      await contributeToGoal(goal.id, amount);

      setContributionAmounts((current) => ({
        ...current,
        [goal.id]: "",
      }));

      await loadGoals();
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to contribute to goal.",
      );
    } finally {
      setActiveAction(null);
    }
  }

  async function handlePause(goalId: string) {
    try {
      setActiveAction(`pause-${goalId}`);
      setError(null);

      await pauseGoal(goalId);
      await loadGoals();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to pause goal.");
    } finally {
      setActiveAction(null);
    }
  }

  async function handleResume(goalId: string) {
    try {
      setActiveAction(`resume-${goalId}`);
      setError(null);

      await resumeGoal(goalId);
      await loadGoals();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to resume goal.");
    } finally {
      setActiveAction(null);
    }
  }

  async function handleCancel(goalId: string) {
    const confirmed = window.confirm(
      "Are you sure you want to cancel this goal?",
    );

    if (!confirmed) {
      return;
    }

    try {
      setActiveAction(`cancel-${goalId}`);
      setError(null);

      await cancelGoal(goalId);
      await loadGoals();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to cancel goal.");
    } finally {
      setActiveAction(null);
    }
  }

  if (isLoading) {
    return (
      <section>
        <h2>Goals</h2>
        <p>Loading goals...</p>
      </section>
    );
  }

  return (
    <section>
      <h2>Goals</h2>

      {error && <p>{error}</p>}

      <section>
        <h3>Create Goal</h3>

        <form onSubmit={handleCreateGoal}>
          <div>
            <label htmlFor="goal-name">Name</label>
            <input
              id="goal-name"
              type="text"
              value={name}
              onChange={(event) => setName(event.target.value)}
            />
          </div>

          <div>
            <label htmlFor="goal-target-amount">Target amount</label>
            <input
              id="goal-target-amount"
              type="number"
              min="0.01"
              step="0.01"
              value={targetAmount}
              onChange={(event) => setTargetAmount(event.target.value)}
            />
          </div>

          <div>
            <label htmlFor="goal-target-date">Target date</label>
            <input
              id="goal-target-date"
              type="date"
              value={targetDate}
              onChange={(event) => setTargetDate(event.target.value)}
            />
          </div>

          <button type="submit" disabled={isCreating}>
            {isCreating ? "Creating..." : "Create Goal"}
          </button>
        </form>
      </section>

      <section>
        <h3>Your Goals</h3>

        {goals.length === 0 ? (
          <p>No goals found.</p>
        ) : (
          <ul>
            {goals.map((goal) => {
              const progress = getProgress(goal);
              const remaining = Math.max(
                0,
                goal.targetAmount - goal.currentAmount,
              );

              const isCompleted = goal.status === "COMPLETED";
              const isCancelled = goal.status === "CANCELLED";
              const isPaused = goal.status === "PAUSED";
              const isActive = goal.status === "ACTIVE";

              return (
                <li key={goal.id}>
                  <article>
                    <h4>{goal.name}</h4>

                    <p>Status: {goal.status}</p>

                    <p>
                      ₹{formatAmount(goal.currentAmount)} / ₹
                      {formatAmount(goal.targetAmount)}
                    </p>

                    <p>Progress: {progress.toFixed(1)}%</p>

                    <p>Remaining: ₹{formatAmount(remaining)}</p>

                    <p>Target date: {formatDate(goal.targetDate)}</p>

                    {isActive && (
                      <div>
                        <label htmlFor={`contribution-${goal.id}`}>
                          Contribution
                        </label>

                        <input
                          id={`contribution-${goal.id}`}
                          type="number"
                          min="0.01"
                          step="0.01"
                          value={contributionAmounts[goal.id] ?? ""}
                          onChange={(event) =>
                            setContributionAmounts((current) => ({
                              ...current,
                              [goal.id]: event.target.value,
                            }))
                          }
                          placeholder="5000"
                        />

                        <button
                          type="button"
                          onClick={() => void handleContribution(goal)}
                          disabled={activeAction === `contribute-${goal.id}`}
                        >
                          {activeAction === `contribute-${goal.id}`
                            ? "Adding..."
                            : "Contribute"}
                        </button>
                      </div>
                    )}

                    <div>
                      {isActive && (
                        <button
                          type="button"
                          onClick={() => void handlePause(goal.id)}
                          disabled={activeAction === `pause-${goal.id}`}
                        >
                          {activeAction === `pause-${goal.id}`
                            ? "Pausing..."
                            : "Pause"}
                        </button>
                      )}

                      {isPaused && (
                        <button
                          type="button"
                          onClick={() => void handleResume(goal.id)}
                          disabled={activeAction === `resume-${goal.id}`}
                        >
                          {activeAction === `resume-${goal.id}`
                            ? "Resuming..."
                            : "Resume"}
                        </button>
                      )}

                      {(isActive || isPaused) && (
                        <button
                          type="button"
                          onClick={() => void handleCancel(goal.id)}
                          disabled={activeAction === `cancel-${goal.id}`}
                        >
                          {activeAction === `cancel-${goal.id}`
                            ? "Cancelling..."
                            : "Cancel"}
                        </button>
                      )}
                    </div>

                    {isCompleted && <p>Goal completed.</p>}

                    {isCancelled && <p>Goal cancelled.</p>}
                  </article>
                </li>
              );
            })}
          </ul>
        )}
      </section>
    </section>
  );
}
