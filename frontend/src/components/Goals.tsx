import { useEffect, useState, type FormEvent } from "react";
import {
  contributeToGoal,
  createGoal,
  deleteGoal,
  getGoals,
  pauseGoal,
  resumeGoal,
} from "../api/goalApi";
import { getAccounts } from "../api/accountApi";
import type { Account } from "../types/account";
import type { Goal } from "../types/goal";

type GoalsProps = {
  onFinanceDataChanged: () => Promise<void>;
};

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

function getEligibleSourceAccounts(accounts: Account[]): Account[] {
  return accounts.filter(
    (account) =>
      account.type !== "GOAL" &&
      account.type !== "CREDIT_CARD" &&
      account.currency === "INR",
  );
}

export default function Goals({ onFinanceDataChanged }: GoalsProps) {
  const [goals, setGoals] = useState<Goal[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  const [name, setName] = useState("");
  const [targetAmount, setTargetAmount] = useState("");
  const [targetDate, setTargetDate] = useState("");
  const [isCreating, setIsCreating] = useState(false);

  const [contributionAmounts, setContributionAmounts] = useState<
    Record<string, string>
  >({});
  const [selectedSourceAccounts, setSelectedSourceAccounts] = useState<
    Record<string, string>
  >({});
  const [activeAction, setActiveAction] = useState<string | null>(null);

  async function loadGoalsAndAccounts() {
    const [goalResponse, accountResponse] = await Promise.all([
      getGoals(),
      getAccounts(),
    ]);

    setGoals(goalResponse.filter((goal) => goal.status !== "CANCELLED"));
    setAccounts(accountResponse);

    const eligibleAccounts = getEligibleSourceAccounts(accountResponse);

    setSelectedSourceAccounts((current) => {
      const next = { ...current };

      for (const goal of goalResponse) {
        const selectedIsValid = eligibleAccounts.some(
          (account) => account.id === next[goal.id],
        );

        if (!selectedIsValid) {
          if (eligibleAccounts.length > 0) {
            next[goal.id] = eligibleAccounts[0].id;
          } else {
            delete next[goal.id];
          }
        }
      }

      return next;
    });
  }

  useEffect(() => {
    let cancelled = false;

    async function loadInitialData() {
      try {
        const [goalResponse, accountResponse] = await Promise.all([
          getGoals(),
          getAccounts(),
        ]);

        if (cancelled) {
          return;
        }

        setGoals(goalResponse.filter((goal) => goal.status !== "CANCELLED"));
        setAccounts(accountResponse);

        const eligibleAccounts = getEligibleSourceAccounts(accountResponse);

        setSelectedSourceAccounts((current) => {
          const next = { ...current };

          for (const goal of goalResponse) {
            const selectedIsValid = eligibleAccounts.some(
              (account) => account.id === next[goal.id],
            );

            if (!selectedIsValid) {
              if (eligibleAccounts.length > 0) {
                next[goal.id] = eligibleAccounts[0].id;
              } else {
                delete next[goal.id];
              }
            }
          }

          return next;
        });
      } catch (err) {
        if (!cancelled) {
          setError(
            err instanceof Error
              ? err.message
              : "Failed to load goals and accounts.",
          );
        }
      } finally {
        if (!cancelled) {
          setIsLoading(false);
        }
      }
    }

    void Promise.resolve().then(loadInitialData);

    return () => {
      cancelled = true;
    };
  }, []);

  async function refreshAfterSuccessfulChange(message: string) {
    try {
      await Promise.all([loadGoalsAndAccounts(), onFinanceDataChanged()]);
      return true;
    } catch (err) {
      const refreshError = err instanceof Error ? err.message : "Unknown error";

      setError(
        `${message} However, refreshing the displayed financial data failed: ${refreshError}. Reload the page to verify the latest state.`,
      );

      return false;
    }
  }

  async function handleCreateGoal(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();

    const trimmedName = name.trim();
    const parsedTargetAmount = Number(targetAmount);

    if (!trimmedName) {
      setError("Goal name is required.");
      return;
    }

    if (
      !Number.isFinite(parsedTargetAmount) ||
      parsedTargetAmount <= 0 ||
      !Number.isInteger(parsedTargetAmount * 100)
    ) {
      setError(
        "Target amount must be positive and have at most 2 decimal places.",
      );
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

      await refreshAfterSuccessfulChange("Goal created successfully.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to create goal.");
    } finally {
      setIsCreating(false);
    }
  }

  async function handleContribution(goal: Goal) {
    const rawAmount = contributionAmounts[goal.id] ?? "";
    const amount = Number(rawAmount);
    const sourceAccountId = selectedSourceAccounts[goal.id];

    if (!sourceAccountId) {
      setError("Select an eligible INR source account first.");
      return;
    }

    if (
      !Number.isFinite(amount) ||
      amount <= 0 ||
      !Number.isInteger(amount * 100)
    ) {
      setError(
        "Contribution must be positive and have at most 2 decimal places.",
      );
      return;
    }

    try {
      setActiveAction(`contribute-${goal.id}`);
      setError(null);

      await contributeToGoal(goal.id, sourceAccountId, amount);

      setContributionAmounts((current) => ({
        ...current,
        [goal.id]: "",
      }));

      await refreshAfterSuccessfulChange("Contribution recorded successfully.");
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
      await loadGoalsAndAccounts();
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
      await loadGoalsAndAccounts();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to resume goal.");
    } finally {
      setActiveAction(null);
    }
  }

  async function handleDelete(goalId: string) {
    const confirmed = window.confirm(
      "Are you sure you want to permanently delete this goal? This cannot be undone.",
    );

    if (!confirmed) {
      return;
    }

    try {
      setActiveAction(`delete-${goalId}`);
      setError(null);

      await deleteGoal(goalId);

      setContributionAmounts((current) => {
        const updated = { ...current };
        delete updated[goalId];
        return updated;
      });

      setSelectedSourceAccounts((current) => {
        const updated = { ...current };
        delete updated[goalId];
        return updated;
      });

      await refreshAfterSuccessfulChange("Goal deleted successfully.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to delete goal.");
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

  const sourceAccounts = getEligibleSourceAccounts(accounts);

  return (
    <section>
      <h2>Goals</h2>

      {error && <p role="alert">{error}</p>}

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
              required
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
              required
            />
          </div>

          <div>
            <label htmlFor="goal-target-date">Target date</label>
            <input
              id="goal-target-date"
              type="date"
              value={targetDate}
              onChange={(event) => setTargetDate(event.target.value)}
              required
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
              const isPaused = goal.status === "PAUSED";
              const isActive = goal.status === "ACTIVE";
              const actionIsRunning = activeAction !== null;

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
                    <progress value={progress} max={100} />
                    <p>Remaining: ₹{formatAmount(remaining)}</p>
                    <p>Target date: {formatDate(goal.targetDate)}</p>

                    {isActive && (
                      <div>
                        <div>
                          <label htmlFor={`source-account-${goal.id}`}>
                            From account
                          </label>
                          <select
                            id={`source-account-${goal.id}`}
                            value={selectedSourceAccounts[goal.id] ?? ""}
                            onChange={(event) =>
                              setSelectedSourceAccounts((current) => ({
                                ...current,
                                [goal.id]: event.target.value,
                              }))
                            }
                            disabled={
                              sourceAccounts.length === 0 || actionIsRunning
                            }
                          >
                            <option value="">Select an account</option>
                            {sourceAccounts.map((account) => (
                              <option key={account.id} value={account.id}>
                                {account.name} ({account.type} ·{" "}
                                {account.currency})
                              </option>
                            ))}
                          </select>
                        </div>

                        {sourceAccounts.length === 0 && (
                          <p>
                            Create an eligible INR bank, cash, or investment
                            account before contributing to this goal.
                          </p>
                        )}

                        <div>
                          <label htmlFor={`contribution-${goal.id}`}>
                            Contribution amount
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
                            disabled={
                              activeAction === `contribute-${goal.id}` ||
                              sourceAccounts.length === 0
                            }
                          >
                            {activeAction === `contribute-${goal.id}`
                              ? "Adding..."
                              : "Contribute"}
                          </button>
                        </div>
                      </div>
                    )}

                    <div>
                      {isActive && (
                        <button
                          type="button"
                          onClick={() => void handlePause(goal.id)}
                          disabled={actionIsRunning}
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
                          disabled={actionIsRunning}
                        >
                          {activeAction === `resume-${goal.id}`
                            ? "Resuming..."
                            : "Resume"}
                        </button>
                      )}

                      <button
                        type="button"
                        onClick={() => void handleDelete(goal.id)}
                        disabled={actionIsRunning}
                      >
                        {activeAction === `delete-${goal.id}`
                          ? "Deleting..."
                          : "Delete"}
                      </button>
                    </div>

                    {isCompleted && <p>Goal completed.</p>}
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
