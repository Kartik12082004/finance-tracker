import { useEffect, useState, type FormEvent } from "react";
import {
  archiveGoal,
  contributeToGoal,
  createGoal,
  deleteGoal,
  getArchivedGoals,
  getGoals,
  pauseGoal,
  resumeGoal,
  unarchiveGoal,
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
  const [archivedGoals, setArchivedGoals] = useState<Goal[]>([]);
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [isLoading, setIsLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [retryKey, setRetryKey] = useState(0);
  const [isCreateModalOpen, setIsCreateModalOpen] = useState(false);
  const [showArchivedGoals, setShowArchivedGoals] = useState(false);

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
    const [goalResponse, accountResponse, archivedGoalResponse] =
      await Promise.all([getGoals(), getAccounts(), getArchivedGoals()]);

    setGoals(goalResponse.filter((goal) => goal.status !== "CANCELLED"));
    setArchivedGoals(archivedGoalResponse);
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
        setIsLoading(true);
        setLoadError(null);

        const [goalResponse, accountResponse, archivedGoalResponse] =
          await Promise.all([getGoals(), getAccounts(), getArchivedGoals()]);

        if (cancelled) {
          return;
        }

        setGoals(goalResponse.filter((goal) => goal.status !== "CANCELLED"));
        setArchivedGoals(archivedGoalResponse);
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
      } catch {
        if (!cancelled) {
          setLoadError(
            "Could not load goals and accounts. Check your connection and try again.",
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
  }, [retryKey]);

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

      setIsCreateModalOpen(false);
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

  async function handleArchive(goalId: string) {
    const confirmed = window.confirm(
      "Archive this completed goal? It will disappear from Goals and Accounts, but its financial history will be preserved.",
    );

    if (!confirmed) {
      return;
    }

    try {
      setActiveAction(`archive-${goalId}`);
      setError(null);

      await archiveGoal(goalId);
      await refreshAfterSuccessfulChange("Goal archived successfully.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to archive goal.");
    } finally {
      setActiveAction(null);
    }
  }

  async function handleUnarchive(goalId: string) {
    try {
      setActiveAction(`unarchive-${goalId}`);
      setError(null);

      await unarchiveGoal(goalId);
      await refreshAfterSuccessfulChange("Goal unarchived successfully.");
    } catch (err) {
      setError(
        err instanceof Error ? err.message : "Failed to unarchive goal.",
      );
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
        <p role="status">Loading goals...</p>
      </section>
    );
  }

  if (loadError) {
    return (
      <section className="goals-page">
        <header className="goals-page-header">
          <div className="goals-page-heading">
            <span className="goals-eyebrow">PLAN WITH PURPOSE</span>
            <h2>Goals</h2>
            <p>Turn your savings plans into milestones you can track.</p>
          </div>
        </header>
        <div className="goals-alert" role="alert">
          <p>{loadError}</p>
          <button
            type="button"
            className="goals-secondary-button"
            onClick={() => setRetryKey((current) => current + 1)}
            disabled={isLoading}
          >
            Retry
          </button>
        </div>
      </section>
    );
  }

  const sourceAccounts = getEligibleSourceAccounts(accounts);

  function closeCreateModal() {
    if (isCreating) return;

    setIsCreateModalOpen(false);
    setName("");
    setTargetAmount("");
    setTargetDate("");
  }

  return (
    <section className="goals-page">
      <header className="goals-page-header">
        <div className="goals-page-heading">
          <span className="goals-eyebrow">PLAN WITH PURPOSE</span>
          <h2>Goals</h2>
          <p>Turn your savings plans into milestones you can track.</p>
        </div>

        <button
          type="button"
          className="goals-primary-button"
          onClick={() => {
            setError(null);
            setIsCreateModalOpen(true);
          }}
        >
          <span aria-hidden="true">＋</span> Add Goal
        </button>
      </header>

      {loadError && (
        <div className="goals-alert" role="alert">
          <p>{loadError}</p>
          <button
            type="button"
            onClick={() => setRetryKey((current) => current + 1)}
            disabled={isLoading}
          >
            {isLoading ? "Retrying..." : "Retry loading goals"}
          </button>
        </div>
      )}

      {error && (
        <div className="goals-alert" role="alert">
          <p>{error}</p>
          <button type="button" onClick={() => setError(null)}>
            Dismiss
          </button>
        </div>
      )}

      <section className="goals-content">
        <div className="goals-section-heading">
          <div>
            <h3>Your goals</h3>
            <p>Track your progress and keep moving forward.</p>
          </div>
          <span className="goals-count">
            {goals.length} {goals.length === 1 ? "goal" : "goals"}
          </span>
        </div>

        {goals.length === 0 ? (
          <div className="goals-empty-state">
            <div className="goals-empty-icon" aria-hidden="true">
              ◎
            </div>
            <h3>No goals yet</h3>
            <p>
              Start with something you want to save for. You can track
              contributions and progress here.
            </p>
            <button
              type="button"
              className="goals-primary-button"
              onClick={() => {
                setError(null);
                setIsCreateModalOpen(true);
              }}
            >
              ＋ Create your first goal
            </button>
          </div>
        ) : (
          <div className="goals-grid">
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
                <article className="goal-card" key={goal.id}>
                  <div className="goal-card-top">
                    <div className="goal-card-icon" aria-hidden="true">
                      {isCompleted ? "✓" : "◎"}
                    </div>

                    <span
                      className={`goal-status goal-status-${goal.status.toLowerCase()}`}
                    >
                      {goal.status === "COMPLETED"
                        ? "Completed"
                        : goal.status === "PAUSED"
                          ? "Paused"
                          : goal.status === "ACTIVE"
                            ? "In progress"
                            : goal.status}
                    </span>
                  </div>

                  <h4 className="goal-card-title">{goal.name}</h4>

                  <div className="goal-amounts">
                    <p className="goal-current-amount">
                      ₹{formatAmount(goal.currentAmount)}
                    </p>
                    <p className="goal-target-amount">
                      of ₹{formatAmount(goal.targetAmount)}
                    </p>
                  </div>

                  <div
                    className="goal-progress-track"
                    role="progressbar"
                    aria-label={`${goal.name} progress`}
                    aria-valuenow={Number(progress.toFixed(1))}
                    aria-valuemin={0}
                    aria-valuemax={100}
                  >
                    <div
                      className={`goal-progress-fill${isCompleted ? " goal-progress-complete" : ""}`}
                      style={{ width: `${progress}%` }}
                    />
                  </div>

                  <div className="goal-progress-labels">
                    <span>{progress.toFixed(1)}% saved</span>
                    <span>
                      {isCompleted
                        ? "Target reached"
                        : `₹${formatAmount(remaining)} to go`}
                    </span>
                  </div>

                  <div className="goal-card-date">
                    <span className="goal-detail-label">Target date</span>
                    <span className="goal-detail-value">
                      {formatDate(goal.targetDate)}
                    </span>
                  </div>

                  {isActive && (
                    <div className="goal-contribution">
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
                            {account.name} ({account.type} · {account.currency})
                          </option>
                        ))}
                      </select>

                      {sourceAccounts.length === 0 && (
                        <p className="goal-field-hint">
                          Create an eligible INR bank, cash, or investment
                          account before contributing.
                        </p>
                      )}

                      <label htmlFor={`contribution-${goal.id}`}>
                        Contribution amount (₹)
                      </label>

                      <div className="goal-contribution-row">
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
                          placeholder="e.g. 5000"
                          disabled={actionIsRunning}
                        />

                        <button
                          type="button"
                          className="goals-primary-button goal-contribute-button"
                          onClick={() => void handleContribution(goal)}
                          disabled={
                            actionIsRunning || sourceAccounts.length === 0
                          }
                        >
                          {activeAction === `contribute-${goal.id}`
                            ? "Adding..."
                            : "Contribute"}
                        </button>
                      </div>
                    </div>
                  )}

                  <div className="goal-card-actions">
                    {isActive && (
                      <button
                        type="button"
                        className="goal-secondary-button"
                        onClick={() => void handlePause(goal.id)}
                        disabled={actionIsRunning}
                      >
                        {activeAction === `pause-${goal.id}`
                          ? "Pausing..."
                          : "Pause goal"}
                      </button>
                    )}

                    {isPaused && (
                      <button
                        type="button"
                        className="goal-secondary-button"
                        onClick={() => void handleResume(goal.id)}
                        disabled={actionIsRunning}
                      >
                        {activeAction === `resume-${goal.id}`
                          ? "Resuming..."
                          : "Resume goal"}
                      </button>
                    )}

                    {isCompleted && (
                      <button
                        type="button"
                        className="goal-secondary-button"
                        onClick={() => void handleArchive(goal.id)}
                        disabled={actionIsRunning}
                      >
                        {activeAction === `archive-${goal.id}`
                          ? "Archiving..."
                          : "Archive"}
                      </button>
                    )}

                    <button
                      type="button"
                      className="goal-delete-button"
                      onClick={() => void handleDelete(goal.id)}
                      disabled={actionIsRunning}
                    >
                      {activeAction === `delete-${goal.id}`
                        ? "Deleting..."
                        : "Delete"}
                    </button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section className="goals-content">
        <div className="goals-section-heading">
          <div>
            <h3>Archived goals</h3>
            <p>Completed milestones you've chosen to put away.</p>
          </div>

          <button
            type="button"
            className="goal-secondary-button"
            onClick={() => setShowArchivedGoals((current) => !current)}
          >
            {showArchivedGoals
              ? "Hide archived"
              : `View archived (${archivedGoals.length})`}
          </button>
        </div>

        {showArchivedGoals &&
          (archivedGoals.length === 0 ? (
            <div className="goals-empty-state">
              <h3>No archived goals</h3>
              <p>Goals you archive will appear here.</p>
            </div>
          ) : (
            <div className="goals-grid">
              {archivedGoals.map((goal) => (
                <article className="goal-card" key={goal.id}>
                  <div className="goal-card-top">
                    <div className="goal-card-icon" aria-hidden="true">
                      ✓
                    </div>
                    <span className="goal-status goal-status-completed">
                      Archived
                    </span>
                  </div>

                  <h4 className="goal-card-title">{goal.name}</h4>

                  <div className="goal-amounts">
                    <p className="goal-current-amount">
                      ₹{formatAmount(goal.currentAmount)}
                    </p>
                    <p className="goal-target-amount">
                      of ₹{formatAmount(goal.targetAmount)}
                    </p>
                  </div>

                  <div className="goal-card-date">
                    <span className="goal-detail-label">Target date</span>
                    <span className="goal-detail-value">
                      {formatDate(goal.targetDate)}
                    </span>
                  </div>

                  <div className="goal-card-actions">
                    <button
                      type="button"
                      className="goal-secondary-button"
                      onClick={() => void handleUnarchive(goal.id)}
                      disabled={activeAction !== null}
                    >
                      {activeAction === `unarchive-${goal.id}`
                        ? "Restoring..."
                        : "Unarchive"}
                    </button>
                  </div>
                </article>
              ))}
            </div>
          ))}
      </section>

      {isCreateModalOpen && (
        <div
          className="goal-modal-overlay"
          onMouseDown={(event) => {
            if (event.target === event.currentTarget) {
              closeCreateModal();
            }
          }}
        >
          <section
            className="goal-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="goal-modal-title"
          >
            <header className="goal-modal-header">
              <div>
                <span className="goals-eyebrow">NEW SAVINGS MILESTONE</span>
                <h3 id="goal-modal-title">Create a goal</h3>
                <p>Give your next milestone a name, amount, and deadline.</p>
              </div>

              <button
                type="button"
                className="goal-modal-close"
                onClick={closeCreateModal}
                disabled={isCreating}
                aria-label="Close create goal dialog"
              >
                ×
              </button>
            </header>

            <form className="goal-modal-form" onSubmit={handleCreateGoal}>
              <div className="goal-form-field">
                <label htmlFor="goal-name">Goal name</label>
                <input
                  id="goal-name"
                  type="text"
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  placeholder="e.g. Emergency fund"
                  maxLength={100}
                  required
                  autoFocus
                />
              </div>

              <div className="goal-form-field">
                <label htmlFor="goal-target-amount">Target amount (₹)</label>
                <input
                  id="goal-target-amount"
                  type="number"
                  min="0.01"
                  step="0.01"
                  value={targetAmount}
                  onChange={(event) => setTargetAmount(event.target.value)}
                  placeholder="e.g. 100000"
                  required
                />
              </div>

              <div className="goal-form-field">
                <label htmlFor="goal-target-date">Target date</label>
                <input
                  id="goal-target-date"
                  type="date"
                  value={targetDate}
                  onChange={(event) => setTargetDate(event.target.value)}
                  min={new Date().toLocaleDateString("en-CA")}
                  required
                />
              </div>

              <div className="goal-modal-actions">
                <button
                  type="button"
                  className="goal-secondary-button"
                  onClick={closeCreateModal}
                  disabled={isCreating}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  className="goals-primary-button"
                  disabled={isCreating}
                >
                  {isCreating ? "Creating..." : "Create goal"}
                </button>
              </div>
            </form>
          </section>
        </div>
      )}
    </section>
  );
}
